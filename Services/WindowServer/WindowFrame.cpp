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

#include "ClientConnection.h"
#include <AK/Badge.h>
#include <LibGfx/Font.h>
#include <LibGfx/Painter.h>
#include <LibGfx/StylePainter.h>
#include <LibGfx/WindowTheme.h>
#include <WindowServer/Button.h>
#include <WindowServer/Compositor.h>
#include <WindowServer/Event.h>
#include <WindowServer/Window.h>
#include <WindowServer/WindowFrame.h>
#include <WindowServer/WindowManager.h>

namespace WindowServer {

static Gfx::WindowTheme::WindowType to_theme_window_type(WindowType type)
{
    switch (type) {
    case WindowType::Normal:
        return Gfx::WindowTheme::WindowType::Normal;
    case WindowType::Notification:
        return Gfx::WindowTheme::WindowType::Notification;
    default:
        return Gfx::WindowTheme::WindowType::Other;
    }
}

static Gfx::Bitmap* s_minimize_icon;
static Gfx::Bitmap* s_maximize_icon;
static Gfx::Bitmap* s_restore_icon;
static Gfx::Bitmap* s_close_icon;

static String s_last_title_button_icons_path;

WindowFrame::WindowFrame(Window& window)
    : m_window(window)
{
    auto button = make<Button>(*this, [this](auto&) {
        m_window.request_close();
    });
    m_close_button = button.ptr();
    m_buttons.append(move(button));

    if (window.is_resizable()) {
        auto button = make<Button>(*this, [this](auto&) {
            WindowManager::the().maximize_windows(m_window, !m_window.is_maximized());
        });
        m_maximize_button = button.ptr();
        m_buttons.append(move(button));
    }

    if (window.is_minimizable()) {
        auto button = make<Button>(*this, [this](auto&) {
            WindowManager::the().minimize_windows(m_window, true);
        });
        m_minimize_button = button.ptr();
        m_buttons.append(move(button));
    }

    set_button_icons();
}

WindowFrame::~WindowFrame()
{
}

void WindowFrame::set_button_icons()
{
    if (m_window.is_frameless())
        return;

    String icons_path = WindowManager::the().palette().title_button_icons_path();

    StringBuilder full_path;
    if (!s_minimize_icon || s_last_title_button_icons_path != icons_path) {
        full_path.append(icons_path);
        full_path.append("window-minimize.png");
        if (!(s_minimize_icon = Gfx::Bitmap::load_from_file(full_path.to_string()).leak_ref()))
            s_minimize_icon = Gfx::Bitmap::load_from_file("/res/icons/16x16/window-minimize.png").leak_ref();
        full_path.clear();
    }
    if (!s_maximize_icon || s_last_title_button_icons_path != icons_path) {
        full_path.append(icons_path);
        full_path.append("window-maximize.png");
        if (!(s_maximize_icon = Gfx::Bitmap::load_from_file(full_path.to_string()).leak_ref()))
            s_maximize_icon = Gfx::Bitmap::load_from_file("/res/icons/16x16/window-maximize.png").leak_ref();
        full_path.clear();
    }
    if (!s_restore_icon || s_last_title_button_icons_path != icons_path) {
        full_path.append(icons_path);
        full_path.append("window-restore.png");
        if (!(s_restore_icon = Gfx::Bitmap::load_from_file(full_path.to_string()).leak_ref()))
            s_restore_icon = Gfx::Bitmap::load_from_file("/res/icons/16x16/window-restore.png").leak_ref();
        full_path.clear();
    }
    if (!s_close_icon || s_last_title_button_icons_path != icons_path) {
        full_path.append(icons_path);
        full_path.append("window-close.png");
        if (!(s_close_icon = Gfx::Bitmap::load_from_file(full_path.to_string()).leak_ref()))
            s_close_icon = Gfx::Bitmap::load_from_file("/res/icons/16x16/window-close.png").leak_ref();
        full_path.clear();
    }

    m_close_button->set_icon(*s_close_icon);
    if (m_window.is_minimizable())
        m_minimize_button->set_icon(*s_minimize_icon);
    if (m_window.is_resizable())
        m_maximize_button->set_icon(m_window.is_maximized() ? *s_restore_icon : *s_maximize_icon);

    s_last_title_button_icons_path = icons_path;
}

void WindowFrame::did_set_maximized(Badge<Window>, bool maximized)
{
    ASSERT(m_maximize_button);
    m_maximize_button->set_icon(maximized ? *s_restore_icon : *s_maximize_icon);
}

Gfx::IntRect WindowFrame::title_bar_rect() const
{
    return Gfx::WindowTheme::current().title_bar_rect(to_theme_window_type(m_window.type()), m_window.rect(), WindowManager::the().palette());
}

Gfx::IntRect WindowFrame::title_bar_icon_rect() const
{
    return Gfx::WindowTheme::current().title_bar_icon_rect(to_theme_window_type(m_window.type()), m_window.rect(), WindowManager::the().palette());
}

Gfx::IntRect WindowFrame::title_bar_text_rect() const
{
    return Gfx::WindowTheme::current().title_bar_text_rect(to_theme_window_type(m_window.type()), m_window.rect(), WindowManager::the().palette());
}

Gfx::WindowTheme::WindowState WindowFrame::window_state_for_theme() const
{
    auto& wm = WindowManager::the();
    if (&m_window == wm.m_highlight_window)
        return Gfx::WindowTheme::WindowState::Highlighted;
    if (&m_window == wm.m_move_window)
        return Gfx::WindowTheme::WindowState::Moving;
    if (wm.is_active_window_or_accessory(m_window))
        return Gfx::WindowTheme::WindowState::Active;
    return Gfx::WindowTheme::WindowState::Inactive;
}

void WindowFrame::paint_notification_frame(Gfx::Painter& painter)
{
    auto palette = WindowManager::the().palette();
    Gfx::WindowTheme::current().paint_notification_frame(painter, m_window.rect(), palette, m_buttons.last().relative_rect());
}

void WindowFrame::paint_normal_frame(Gfx::Painter& painter)
{
    auto palette = WindowManager::the().palette();
    auto& window = m_window;
    String title_text;
    if (window.client() && window.client()->is_unresponsive()) {
        StringBuilder builder;
        builder.append(window.title());
        builder.append(" (Not responding)");
        title_text = builder.to_string();
    } else {
        title_text = window.title();
    }

    auto leftmost_button_rect = m_buttons.is_empty() ? Gfx::IntRect() : m_buttons.last().relative_rect();
    Gfx::WindowTheme::current().paint_normal_frame(painter, window_state_for_theme(), m_window.rect(), title_text, m_window.icon(), palette, leftmost_button_rect);
}

void WindowFrame::paint(Gfx::Painter& painter)
{
    if (m_window.is_frameless())
        return;

    Gfx::PainterStateSaver saver(painter);
    painter.translate(rect().location());

    if (m_window.type() == WindowType::Notification)
        paint_notification_frame(painter);
    else if (m_window.type() == WindowType::Normal)
        paint_normal_frame(painter);
    else
        return;

    for (auto& button : m_buttons) {
        button.paint(painter);
    }
}

static Gfx::IntRect frame_rect_for_window(Window& window, const Gfx::IntRect& rect)
{
    if (window.is_frameless())
        return rect;
    return Gfx::WindowTheme::current().frame_rect_for_window(to_theme_window_type(window.type()), rect, WindowManager::the().palette());
}

static Gfx::IntRect frame_rect_for_window(Window& window)
{
    return frame_rect_for_window(window, window.rect());
}

Gfx::IntRect WindowFrame::rect() const
{
    return frame_rect_for_window(m_window);
}

void WindowFrame::invalidate_title_bar()
{
    invalidate(title_bar_rect());
}

void WindowFrame::invalidate(Gfx::IntRect relative_rect)
{
    auto frame_rect = rect();
    auto window_rect = m_window.rect();
    relative_rect.move_by(frame_rect.x() - window_rect.x(), frame_rect.y() - window_rect.y());
    m_window.invalidate(relative_rect, true);
}

void WindowFrame::notify_window_rect_changed(const Gfx::IntRect& old_rect, const Gfx::IntRect& new_rect)
{
    layout_buttons();

    auto old_frame_rect = frame_rect_for_window(m_window, old_rect);
    auto& compositor = Compositor::the();
    for (auto& dirty : old_frame_rect.shatter(rect()))
        compositor.invalidate_screen(dirty);
    if (!m_window.is_opaque())
        compositor.invalidate_screen(rect());

    compositor.invalidate_occlusions();

    WindowManager::the().notify_rect_changed(m_window, old_rect, new_rect);
}

void WindowFrame::layout_buttons()
{
    auto button_rects = Gfx::WindowTheme::current().layout_buttons(to_theme_window_type(m_window.type()), m_window.rect(), WindowManager::the().palette(), m_buttons.size());
    for (size_t i = 0; i < m_buttons.size(); i++)
        m_buttons[i].set_relative_rect(button_rects[i]);
}

void WindowFrame::on_mouse_event(const MouseEvent& event)
{
    ASSERT(!m_window.is_fullscreen());

    auto& wm = WindowManager::the();
    if (m_window.type() != WindowType::Normal && m_window.type() != WindowType::Notification)
        return;

    if (m_window.type() == WindowType::Normal) {
        if (event.type() == Event::MouseDown)
            wm.move_to_front_and_make_active(m_window);

        if (m_window.is_blocked_by_modal_window())
            return;

        if (title_bar_icon_rect().contains(event.position())) {
            if (event.type() == Event::MouseDown && (event.button() == MouseButton::Left || event.button() == MouseButton::Right)) {
                // Manually start a potential double click. Since we're opening
                // a menu, we will only receive the MouseDown event, so we
                // need to record that fact. If the user subsequently clicks
                // on the same area, the menu will get closed, and we will
                // receive a MouseUp event, but because windows have changed
                // we don't get a MouseDoubleClick event. We can however record
                // this click, and when we receive the MouseUp event check if
                // it would have been considered a double click, if it weren't
                // for the fact that we opened and closed a window in the meanwhile
                auto& wm = WindowManager::the();
                wm.start_menu_doubleclick(m_window, event);

                m_window.popup_window_menu(title_bar_rect().bottom_left().translated(rect().location()), WindowMenuDefaultAction::Close);
                return;
            } else if (event.type() == Event::MouseUp && event.button() == MouseButton::Left) {
                // Since the MouseDown event opened a menu, another MouseUp
                // from the second click outside the menu wouldn't be considered
                // a double click, so let's manually check if it would otherwise
                // have been be considered to be one
                auto& wm = WindowManager::the();
                if (wm.is_menu_doubleclick(m_window, event)) {
                    // It is a double click, so perform activate the default item
                    m_window.window_menu_activate_default();
                }
                return;
            }
        }
    }

    // This is slightly hackish, but expand the title bar rect by two pixels downwards,
    // so that mouse events between the title bar and window contents don't act like
    // mouse events on the border.
    auto adjusted_title_bar_rect = title_bar_rect();
    adjusted_title_bar_rect.set_height(adjusted_title_bar_rect.height() + 2);

    if (adjusted_title_bar_rect.contains(event.position())) {
        wm.clear_resize_candidate();

        if (event.type() == Event::MouseDown)
            wm.move_to_front_and_make_active(m_window);

        for (auto& button : m_buttons) {
            if (button.relative_rect().contains(event.position()))
                return button.on_mouse_event(event.translated(-button.relative_rect().location()));
        }
        if (event.type() == Event::MouseDown) {
            if (m_window.type() == WindowType::Normal && event.button() == MouseButton::Right) {
                auto default_action = m_window.is_maximized() ? WindowMenuDefaultAction::Restore : WindowMenuDefaultAction::Maximize;
                m_window.popup_window_menu(event.position().translated(rect().location()), default_action);
                return;
            }
            if (m_window.is_movable() && event.button() == MouseButton::Left)
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
        Gfx::IntRect outer_rect = { {}, rect().size() };
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
