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

#include <AK/Badge.h>
#include <LibGfx/CharacterBitmap.h>
#include <LibGfx/Font.h>
#include <LibGfx/Painter.h>
#include <LibGfx/StylePainter.h>
#include <WindowServer/Button.h>
#include <WindowServer/Compositor.h>
#include <WindowServer/Event.h>
#include <WindowServer/Window.h>
#include <WindowServer/WindowFrame.h>
#include <WindowServer/WindowManager.h>

namespace WindowServer {

static const int window_titlebar_height = 19;

static const char* s_close_button_bitmap_data = {
    "##    ##"
    "###  ###"
    " ###### "
    "  ####  "
    "   ##   "
    "  ####  "
    " ###### "
    "###  ###"
    "##    ##"
};

static Gfx::CharacterBitmap* s_close_button_bitmap;
static const int s_close_button_bitmap_width = 8;
static const int s_close_button_bitmap_height = 9;

static const char* s_minimize_button_bitmap_data = {
    "        "
    "        "
    "        "
    " ###### "
    "  ####  "
    "   ##   "
    "        "
    "        "
    "        "
};

static Gfx::CharacterBitmap* s_minimize_button_bitmap;
static const int s_minimize_button_bitmap_width = 8;
static const int s_minimize_button_bitmap_height = 9;

static const char* s_maximize_button_bitmap_data = {
    "        "
    "        "
    "        "
    "   ##   "
    "  ####  "
    " ###### "
    "        "
    "        "
    "        "
};

static Gfx::CharacterBitmap* s_maximize_button_bitmap;
static const int s_maximize_button_bitmap_width = 8;
static const int s_maximize_button_bitmap_height = 9;

static const char* s_unmaximize_button_bitmap_data = {
    "        "
    "   ##   "
    "  ####  "
    " ###### "
    "        "
    " ###### "
    "  ####  "
    "   ##   "
    "        "
};

static Gfx::CharacterBitmap* s_unmaximize_button_bitmap;
static const int s_unmaximize_button_bitmap_width = 8;
static const int s_unmaximize_button_bitmap_height = 9;

WindowFrame::WindowFrame(Window& window)
    : m_window(window)
{
    if (!s_close_button_bitmap)
        s_close_button_bitmap = &Gfx::CharacterBitmap::create_from_ascii(s_close_button_bitmap_data, s_close_button_bitmap_width, s_close_button_bitmap_height).leak_ref();

    if (!s_minimize_button_bitmap)
        s_minimize_button_bitmap = &Gfx::CharacterBitmap::create_from_ascii(s_minimize_button_bitmap_data, s_minimize_button_bitmap_width, s_minimize_button_bitmap_height).leak_ref();

    if (!s_maximize_button_bitmap)
        s_maximize_button_bitmap = &Gfx::CharacterBitmap::create_from_ascii(s_maximize_button_bitmap_data, s_maximize_button_bitmap_width, s_maximize_button_bitmap_height).leak_ref();

    if (!s_unmaximize_button_bitmap)
        s_unmaximize_button_bitmap = &Gfx::CharacterBitmap::create_from_ascii(s_unmaximize_button_bitmap_data, s_unmaximize_button_bitmap_width, s_unmaximize_button_bitmap_height).leak_ref();

    m_buttons.append(make<Button>(*this, *s_close_button_bitmap, [this](auto&) {
        m_window.request_close();
    }));

    if (window.is_resizable()) {
        auto button = make<Button>(*this, *s_maximize_button_bitmap, [this](auto&) {
            m_window.set_maximized(!m_window.is_maximized());
        });
        m_maximize_button = button.ptr();
        m_buttons.append(move(button));
    }

    if (window.is_minimizable()) {
        auto button = make<Button>(*this, *s_minimize_button_bitmap, [this](auto&) {
            m_window.set_minimized(true);
        });
        m_minimize_button = button.ptr();
        m_buttons.append(move(button));
    }
}

WindowFrame::~WindowFrame()
{
}

void WindowFrame::did_set_maximized(Badge<Window>, bool maximized)
{
    ASSERT(m_maximize_button);
    m_maximize_button->set_bitmap(maximized ? *s_unmaximize_button_bitmap : *s_maximize_button_bitmap);
}

Gfx::Rect WindowFrame::title_bar_rect() const
{
    return { 3, 3, m_window.width(), window_titlebar_height };
}

Gfx::Rect WindowFrame::title_bar_icon_rect() const
{
    auto titlebar_rect = title_bar_rect();
    return {
        titlebar_rect.x() + 1,
        titlebar_rect.y() + 2,
        16,
        titlebar_rect.height(),
    };
}

Gfx::Rect WindowFrame::title_bar_text_rect() const
{
    auto titlebar_rect = title_bar_rect();
    auto titlebar_icon_rect = title_bar_icon_rect();
    return {
        titlebar_rect.x() + 2 + titlebar_icon_rect.width() + 2,
        titlebar_rect.y(),
        titlebar_rect.width() - 4 - titlebar_icon_rect.width() - 2,
        titlebar_rect.height()
    };
}

void WindowFrame::paint(Gfx::Painter& painter)
{
    Gfx::PainterStateSaver saver(painter);
    painter.translate(rect().location());

    if (m_window.type() != WindowType::Normal)
        return;

    auto palette = WindowManager::the().palette();
    auto& window = m_window;

    auto titlebar_rect = title_bar_rect();
    auto titlebar_icon_rect = title_bar_icon_rect();
    auto titlebar_inner_rect = title_bar_text_rect();
    Gfx::Rect outer_rect = { {}, rect().size() };

    auto titlebar_title_rect = titlebar_inner_rect;
    titlebar_title_rect.set_width(Gfx::Font::default_bold_font().width(window.title()));

    Color title_color;
    Color border_color;
    Color border_color2;

    auto& wm = WindowManager::the();

    if (&window == wm.m_highlight_window) {
        border_color = palette.highlight_window_border1();
        border_color2 = palette.highlight_window_border2();
        title_color = palette.highlight_window_title();
    } else if (&window == wm.m_move_window) {
        border_color = palette.moving_window_border1();
        border_color2 = palette.moving_window_border2();
        title_color = palette.moving_window_title();
    } else if (&window == wm.m_active_window) {
        border_color = palette.active_window_border1();
        border_color2 = palette.active_window_border2();
        title_color = palette.active_window_title();
    } else {
        border_color = palette.inactive_window_border1();
        border_color2 = palette.inactive_window_border2();
        title_color = palette.inactive_window_title();
    }

    Gfx::StylePainter::paint_window_frame(painter, outer_rect, palette);

    if (!window.show_titlebar())
        return;

    painter.draw_line(titlebar_rect.bottom_left().translated(0, 1), titlebar_rect.bottom_right().translated(0, 1), palette.button());

    auto leftmost_button_rect = m_buttons.is_empty() ? Gfx::Rect() : m_buttons.last().relative_rect();

    painter.fill_rect_with_gradient(titlebar_rect, border_color, border_color2);

    int stripe_left = titlebar_title_rect.right() + 4;
    int stripe_right = leftmost_button_rect.left() - 3;
    if (stripe_left && stripe_right && stripe_left < stripe_right) {
        for (int i = 2; i <= titlebar_inner_rect.height() - 2; i += 2) {
            painter.draw_line({ stripe_left, titlebar_inner_rect.y() + i }, { stripe_right, titlebar_inner_rect.y() + i }, border_color);
        }
    }

    auto clipped_title_rect = titlebar_title_rect;
    clipped_title_rect.set_width(stripe_right - clipped_title_rect.x());
    if (!clipped_title_rect.is_empty()) {
        painter.draw_text(clipped_title_rect.translated(1, 2), window.title(), wm.window_title_font(), Gfx::TextAlignment::CenterLeft, border_color.darkened(0.4), Gfx::TextElision::Right);
        // FIXME: The translated(0, 1) wouldn't be necessary if we could center text based on its baseline.
        painter.draw_text(clipped_title_rect.translated(0, 1), window.title(), wm.window_title_font(), Gfx::TextAlignment::CenterLeft, title_color, Gfx::TextElision::Right);
    }

    painter.blit(titlebar_icon_rect.location(), window.icon(), window.icon().rect());

    for (auto& button : m_buttons) {
        button.paint(painter);
    }
}

static Gfx::Rect frame_rect_for_window(Window& window, const Gfx::Rect& rect)
{
    auto type = window.type();
    auto offset = !window.show_titlebar() ? (window_titlebar_height + 1) : 0;

    switch (type) {
    case WindowType::Normal:
        return { rect.x() - 3,
            rect.y() - window_titlebar_height - 4 + offset,
            rect.width() + 6,
            rect.height() + 7 + window_titlebar_height - offset };
    default:
        return rect;
    }
}

static Gfx::Rect frame_rect_for_window(Window& window)
{
    return frame_rect_for_window(window, window.rect());
}

Gfx::Rect WindowFrame::rect() const
{
    return frame_rect_for_window(m_window);
}

void WindowFrame::invalidate_title_bar()
{
    WindowManager::the().invalidate(title_bar_rect().translated(rect().location()));
}

void WindowFrame::notify_window_rect_changed(const Gfx::Rect& old_rect, const Gfx::Rect& new_rect)
{
    int window_button_width = 15;
    int window_button_height = 15;
    int x = title_bar_text_rect().right() + 1;

    for (auto& button : m_buttons) {
        x -= window_button_width;
        Gfx::Rect rect { x, 0, window_button_width, window_button_height };
        rect.center_vertically_within(title_bar_text_rect());
        button.set_relative_rect(rect);
    }

    auto& wm = WindowManager::the();
    wm.invalidate(frame_rect_for_window(m_window, old_rect));
    wm.invalidate(frame_rect_for_window(m_window, new_rect));
    wm.notify_rect_changed(m_window, old_rect, new_rect);
}

void WindowFrame::on_mouse_event(const MouseEvent& event)
{
    ASSERT(!m_window.is_fullscreen());

    if (m_window.is_blocked_by_modal_window())
        return;

    auto& wm = WindowManager::the();
    if (m_window.type() != WindowType::Normal)
        return;

    if (event.type() == Event::MouseDown && (event.button() == MouseButton::Left || event.button() == MouseButton::Right) && title_bar_icon_rect().contains(event.position())) {
        wm.move_to_front_and_make_active(m_window);
        m_window.popup_window_menu(event.position().translated(rect().location()));
        return;
    }

    // This is slightly hackish, but expand the title bar rect by one pixel downwards,
    // so that mouse events between the title bar and window contents don't act like
    // mouse events on the border.
    auto adjusted_title_bar_rect = title_bar_rect();
    adjusted_title_bar_rect.set_height(adjusted_title_bar_rect.height() + 1);

    if (adjusted_title_bar_rect.contains(event.position())) {
        wm.clear_resize_candidate();

        if (event.type() == Event::MouseDown)
            wm.move_to_front_and_make_active(m_window);

        for (auto& button : m_buttons) {
            if (button.relative_rect().contains(event.position()))
                return button.on_mouse_event(event.translated(-button.relative_rect().location()));
        }
        if (event.type() == Event::MouseDown) {
            if (event.button() == MouseButton::Right) {
                m_window.popup_window_menu(event.position().translated(rect().location()));
                return;
            }
            if (event.button() == MouseButton::Left)
                wm.start_window_move(m_window, event.translated(rect().location()));
        }
        return;
    }

    if (m_window.is_resizable() && event.type() == Event::MouseMove && event.buttons() == 0) {
        constexpr ResizeDirection direction_for_hot_area[3][3] = {
            { ResizeDirection::UpLeft, ResizeDirection::Up, ResizeDirection::UpRight },
            { ResizeDirection::Left, ResizeDirection::None, ResizeDirection::Right },
            { ResizeDirection::DownLeft, ResizeDirection::Down, ResizeDirection::DownRight },
        };
        Gfx::Rect outer_rect = { {}, rect().size() };
        ASSERT(outer_rect.contains(event.position()));
        int window_relative_x = event.x() - outer_rect.x();
        int window_relative_y = event.y() - outer_rect.y();
        int hot_area_row = min(2, window_relative_y / (outer_rect.height() / 3));
        int hot_area_column = min(2, window_relative_x / (outer_rect.width() / 3));
        wm.set_resize_candidate(m_window, direction_for_hot_area[hot_area_row][hot_area_column]);
        Compositor::the().invalidate_cursor();
        return;
    }

    if (m_window.is_resizable() && event.type() == Event::MouseDown && event.button() == MouseButton::Left)
        wm.start_window_resize(m_window, event.translated(rect().location()));
}

}
