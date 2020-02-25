/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <LibGfx/Bitmap.h>
#include <LibGfx/Font.h>
#include <LibGfx/StylePainter.h>
#include <WindowServer/Event.h>
#include <WindowServer/Screen.h>
#include <WindowServer/WindowManager.h>
#include <WindowServer/WindowSwitcher.h>

namespace WindowServer {

static WindowSwitcher* s_the;

WindowSwitcher& WindowSwitcher::the()
{
    ASSERT(s_the);
    return *s_the;
}

WindowSwitcher::WindowSwitcher()
{
    s_the = this;
}

WindowSwitcher::~WindowSwitcher()
{
}

void WindowSwitcher::set_visible(bool visible)
{
    if (m_visible == visible)
        return;
    m_visible = visible;
    WindowManager::the().recompute_occlusions();
    if (m_switcher_window)
        m_switcher_window->set_visible(visible);
    if (!m_visible)
        return;
    refresh();
}

Window* WindowSwitcher::selected_window()
{
    if (m_selected_index < 0 || m_selected_index >= static_cast<int>(m_windows.size()))
        return nullptr;
    return m_windows[m_selected_index].ptr();
}

void WindowSwitcher::event(Core::Event& event)
{
    if (!static_cast<Event&>(event).is_mouse_event())
        return;

    auto& mouse_event = static_cast<MouseEvent&>(event);
    int new_hovered_index = -1;
    for (size_t i = 0; i < m_windows.size(); ++i) {
        auto item_rect = this->item_rect(i);
        if (item_rect.contains(mouse_event.position())) {
            new_hovered_index = i;
            break;
        }
    }

    if (mouse_event.type() == Event::MouseMove) {
        if (m_hovered_index != new_hovered_index) {
            m_hovered_index = new_hovered_index;
            redraw();
        }
    }

    if (new_hovered_index == -1)
        return;

    if (mouse_event.type() == Event::MouseDown)
        select_window_at_index(new_hovered_index);

    event.accept();
}

void WindowSwitcher::on_key_event(const KeyEvent& event)
{
    if (event.type() == Event::KeyUp) {
        if (event.key() == Key_Logo) {
            if (auto* window = selected_window()) {
                window->set_minimized(false);
                WindowManager::the().move_to_front_and_make_active(*window);
            }
            WindowManager::the().set_highlight_window(nullptr);
            hide();
        }
        return;
    }

    if (event.key() == Key_LeftShift || event.key() == Key_RightShift)
        return;
    if (event.key() != Key_Tab) {
        WindowManager::the().set_highlight_window(nullptr);
        hide();
        return;
    }
    ASSERT(!m_windows.is_empty());

    int new_selected_index;

    if (!event.shift()) {
        new_selected_index = (m_selected_index + 1) % static_cast<int>(m_windows.size());
    } else {
        new_selected_index = (m_selected_index - 1) % static_cast<int>(m_windows.size());
        if (new_selected_index < 0)
            new_selected_index = static_cast<int>(m_windows.size()) - 1;
    }
    ASSERT(new_selected_index < static_cast<int>(m_windows.size()));

    select_window_at_index(new_selected_index);
}

void WindowSwitcher::select_window(Window& window)
{
    for (size_t i = 0; i < m_windows.size(); ++i) {
        if (m_windows.at(i) == &window) {
            select_window_at_index(i);
            return;
        }
    }
}

void WindowSwitcher::select_window_at_index(int index)
{
    m_selected_index = index;
    auto* highlight_window = m_windows.at(index).ptr();
    ASSERT(highlight_window);
    WindowManager::the().set_highlight_window(highlight_window);
    redraw();
}

void WindowSwitcher::redraw()
{
    draw();
    WindowManager::the().invalidate(m_rect);
}

Gfx::Rect WindowSwitcher::item_rect(int index) const
{
    return {
        padding(),
        padding() + index * item_height(),
        m_rect.width() - padding() * 2,
        item_height()
    };
}

void WindowSwitcher::draw()
{
    auto palette = WindowManager::the().palette();
    Gfx::Painter painter(*m_switcher_window->backing_store());
    painter.fill_rect({ {}, m_rect.size() }, palette.window());
    painter.draw_rect({ {}, m_rect.size() }, palette.threed_shadow2());
    for (size_t index = 0; index < m_windows.size(); ++index) {
        auto& window = *m_windows.at(index);
        auto item_rect = this->item_rect(index);
        Color text_color;
        Color rect_text_color;
        if (static_cast<int>(index) == m_selected_index) {
            painter.fill_rect(item_rect, palette.selection());
            text_color = palette.selection_text();
            rect_text_color = palette.threed_shadow1();
        } else {
            if (static_cast<int>(index) == m_hovered_index)
                Gfx::StylePainter::paint_button(painter, item_rect, palette, Gfx::ButtonStyle::CoolBar, false, true);
            text_color = palette.window_text();
            rect_text_color = palette.threed_shadow2();
        }
        item_rect.shrink(item_padding(), 0);
        Gfx::Rect thumbnail_rect = { item_rect.location().translated(0, 5), { thumbnail_width(), thumbnail_height() } };
        if (window.backing_store()) {
            painter.draw_scaled_bitmap(thumbnail_rect, *window.backing_store(), window.backing_store()->rect());
            Gfx::StylePainter::paint_frame(painter, thumbnail_rect.inflated(4, 4), palette, Gfx::FrameShape::Container, Gfx::FrameShadow::Sunken, 2);
        }
        Gfx::Rect icon_rect = { thumbnail_rect.bottom_right().translated(-window.icon().width(), -window.icon().height()), { window.icon().width(), window.icon().height() } };
        painter.fill_rect(icon_rect, palette.window());
        painter.blit(icon_rect.location(), window.icon(), window.icon().rect());
        painter.draw_text(item_rect.translated(thumbnail_width() + 12, 0), window.title(), WindowManager::the().window_title_font(), Gfx::TextAlignment::CenterLeft, text_color);
        painter.draw_text(item_rect, window.rect().to_string(), Gfx::TextAlignment::CenterRight, rect_text_color);
    }
}

void WindowSwitcher::refresh()
{
    auto& wm = WindowManager::the();
    Window* selected_window = nullptr;
    if (m_selected_index > 0 && m_windows[m_selected_index])
        selected_window = m_windows[m_selected_index].ptr();
    if (!selected_window)
        selected_window = wm.highlight_window() ? wm.highlight_window() : wm.active_window();
    m_windows.clear();
    m_selected_index = 0;
    int window_count = 0;
    int longest_title_width = 0;
    wm.for_each_window_of_type_from_front_to_back(
        WindowType::Normal, [&](Window& window) {
            ++window_count;
            longest_title_width = max(longest_title_width, wm.font().width(window.title()));
            if (selected_window == &window)
                m_selected_index = m_windows.size();
            m_windows.append(window.make_weak_ptr());
            return IterationDecision::Continue;
        },
        true);
    if (m_windows.is_empty()) {
        hide();
        return;
    }
    int space_for_window_rect = 180;
    m_rect.set_width(thumbnail_width() + longest_title_width + space_for_window_rect + padding() * 2 + item_padding() * 2);
    m_rect.set_height(window_count * item_height() + padding() * 2);
    m_rect.center_within(Screen::the().rect());
    if (!m_switcher_window)
        m_switcher_window = Window::construct(*this, WindowType::WindowSwitcher);
    m_switcher_window->set_rect(m_rect);
    redraw();
}

void WindowSwitcher::refresh_if_needed()
{
    if (m_visible)
        refresh();
}

}
