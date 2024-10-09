/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGfx/Bitmap.h>
#include <LibGfx/Font/Font.h>
#include <LibGfx/StylePainter.h>
#include <WindowServer/Compositor.h>
#include <WindowServer/Event.h>
#include <WindowServer/Screen.h>
#include <WindowServer/WindowManager.h>
#include <WindowServer/WindowSwitcher.h>

namespace WindowServer {

static WindowSwitcher* s_the;

WindowSwitcher& WindowSwitcher::the()
{
    VERIFY(s_the);
    return *s_the;
}

WindowSwitcher::WindowSwitcher()
{
    s_the = this;
}

void WindowSwitcher::set_visible(bool visible)
{
    if (m_visible == visible)
        return;
    m_visible = visible;
    Compositor::the().invalidate_occlusions();
    if (m_switcher_window)
        m_switcher_window->set_visible(visible);
    if (!m_visible)
        return;
    clear_hovered_index();
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
    if (event.type() == Event::WindowLeft) {
        clear_hovered_index();
        return;
    }

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

void WindowSwitcher::on_key_event(KeyEvent const& event)
{
    if (event.type() == Event::KeyUp) {
        if (event.key() == (m_mode == Mode::ShowAllWindows ? Key_LeftSuper : Key_LeftAlt)) {
            if (auto* window = selected_window()) {
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
    VERIFY(!m_windows.is_empty());

    int new_selected_index;

    if (!event.shift()) {
        new_selected_index = (m_selected_index + 1) % static_cast<int>(m_windows.size());
    } else {
        new_selected_index = (m_selected_index - 1) % static_cast<int>(m_windows.size());
        if (new_selected_index < 0)
            new_selected_index = static_cast<int>(m_windows.size()) - 1;
    }
    VERIFY(new_selected_index < static_cast<int>(m_windows.size()));

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
    VERIFY(highlight_window);
    auto& wm = WindowManager::the();
    if (m_mode == Mode::ShowAllWindows) {
        if (auto& window_stack = highlight_window->window_stack(); &window_stack != &wm.current_window_stack())
            wm.switch_to_window_stack(window_stack, nullptr, false);
    }
    wm.set_highlight_window(highlight_window);
    redraw();
}

void WindowSwitcher::redraw()
{
    draw();
    Compositor::the().invalidate_screen(m_rect);
}

Gfx::IntRect WindowSwitcher::item_rect(int index) const
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

    Gfx::IntRect rect = { {}, m_rect.size() };
    Gfx::Painter painter(*m_switcher_window->backing_store());
    painter.clear_rect(rect, Color::Transparent);

    // FIXME: Perhaps the WindowSwitcher could render as an overlay instead.
    //        That would require adding support for event handling to overlays.
    if (auto* shadow_bitmap = WindowManager::the().overlay_rect_shadow()) {
        // FIXME: Support other scale factors.
        int scale_factor = 1;
        Gfx::StylePainter::paint_simple_rect_shadow(painter, rect, shadow_bitmap->bitmap(scale_factor), true, true);
    }

    for (size_t index = 0; index < m_windows.size(); ++index) {
        // FIXME: Ideally we wouldn't be in draw() without having pruned destroyed windows from the list already.
        if (m_windows.at(index) == nullptr)
            continue;
        auto& window = *m_windows.at(index);
        auto item_rect = this->item_rect(index);
        Color text_color;
        Color rect_text_color;
        if (static_cast<int>(index) == m_selected_index) {
            painter.fill_rect(item_rect, palette.selection());
            text_color = palette.selection_text();
            rect_text_color = palette.selection_text().with_alpha(0xcc);
        } else {
            if (static_cast<int>(index) == m_hovered_index)
                Gfx::StylePainter::paint_frame(painter, item_rect, palette, Gfx::FrameStyle::RaisedPanel);
            text_color = Color::White;
            rect_text_color = Color(Color::White).with_alpha(0xcc);
        }
        item_rect.shrink(item_padding(), 0);
        Gfx::IntRect thumbnail_rect = { item_rect.location().translated(0, 5), { thumbnail_width(), thumbnail_height() } };
        if (window.backing_store())
            painter.draw_scaled_bitmap(thumbnail_rect, *window.backing_store(), window.backing_store()->rect(), 1.0f, Gfx::ScalingMode::BilinearBlend);
        Gfx::IntRect icon_rect = { thumbnail_rect.bottom_right().translated(-window.icon().width() - 1, -window.icon().height() - 1), { window.icon().width(), window.icon().height() } };
        painter.blit(icon_rect.location(), window.icon(), window.icon().rect());
        painter.draw_text(item_rect.translated(thumbnail_width() + 12, 0).translated(1), window.computed_title(), WindowManager::the().window_title_font(), Gfx::TextAlignment::CenterLeft, text_color.inverted());
        painter.draw_text(item_rect.translated(thumbnail_width() + 12, 0), window.computed_title(), WindowManager::the().window_title_font(), Gfx::TextAlignment::CenterLeft, text_color);
        auto window_details = m_windows_on_multiple_stacks ? ByteString::formatted("{} on {}:{}", window.rect().to_byte_string(), window.window_stack().row() + 1, window.window_stack().column() + 1) : window.rect().to_byte_string();
        painter.draw_text(item_rect, window_details, Gfx::TextAlignment::CenterRight, rect_text_color);
    }
}

void WindowSwitcher::refresh()
{
    auto& wm = WindowManager::the();
    Window const* selected_window = nullptr;
    if (m_selected_index > 0 && m_windows[m_selected_index])
        selected_window = m_windows[m_selected_index].ptr();
    if (!selected_window)
        selected_window = wm.highlight_window() ? wm.highlight_window() : wm.active_window();
    m_windows.clear();
    m_windows_on_multiple_stacks = false;
    m_selected_index = 0;
    int window_count = 0;
    int longest_title_width = 0;

    WindowStack* last_added_on_window_stack = nullptr;
    auto add_window_stack_windows = [&](WindowStack& window_stack) {
        window_stack.for_each_window_of_type_from_front_to_back(
            WindowType::Normal, [&](Window& window) {
                if (window.is_frameless() || window.is_modal())
                    return IterationDecision::Continue;
                ++window_count;
                longest_title_width = max(longest_title_width, wm.font().width(window.computed_title()));
                if (selected_window == &window)
                    m_selected_index = m_windows.size();
                m_windows.append(window);
                auto& window_stack = window.window_stack();
                if (!last_added_on_window_stack) {
                    last_added_on_window_stack = &window_stack;
                } else if (last_added_on_window_stack != &window_stack) {
                    last_added_on_window_stack = &window_stack;
                    m_windows_on_multiple_stacks = true;
                }
                return IterationDecision::Continue;
            },
            true);
    };
    if (m_mode == Mode::ShowAllWindows) {
        wm.for_each_window_stack([&](auto& window_stack) {
            add_window_stack_windows(window_stack);
            return IterationDecision::Continue;
        });
    } else {
        add_window_stack_windows(wm.current_window_stack());
    }

    if (m_windows.is_empty()) {
        hide();
        return;
    }
    int space_for_window_details = 200;
    m_rect.set_width(thumbnail_width() + longest_title_width + space_for_window_details + padding() * 2 + item_padding() * 2);
    m_rect.set_height(window_count * item_height() + padding() * 2);
    m_rect.center_within(Screen::main().rect());
    if (!m_switcher_window) {
        m_switcher_window = Window::construct(*this, WindowType::WindowSwitcher);
        m_switcher_window->set_has_alpha_channel(true);
    }
    m_switcher_window->set_rect(m_rect);
    redraw();
}

void WindowSwitcher::refresh_if_needed()
{
    if (m_visible)
        refresh();
}

void WindowSwitcher::clear_hovered_index()
{
    if (m_hovered_index == -1)
        return;
    m_hovered_index = -1;
    redraw();
}

}
