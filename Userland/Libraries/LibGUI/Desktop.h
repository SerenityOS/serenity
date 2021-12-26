/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Function.h>
#include <AK/String.h>
#include <LibGUI/Forward.h>
#include <LibGfx/Rect.h>
#include <Services/Taskbar/TaskbarWindow.h>
#include <Services/WindowServer/ScreenLayout.h>

namespace GUI {

using ScreenLayout = WindowServer::ScreenLayout;

class Desktop {
public:
    // Most people will probably have 4 screens or less
    static constexpr size_t default_screen_rect_count = 4;

    static Desktop& the();
    Desktop();

    void set_background_color(StringView background_color);

    void set_wallpaper_mode(StringView mode);

    String wallpaper() const;
    bool set_wallpaper(StringView path, bool save_config = true);

    Gfx::IntRect rect() const { return m_bounding_rect; }
    const Vector<Gfx::IntRect, 4>& rects() const { return m_rects; }
    size_t main_screen_index() const { return m_main_screen_index; }

    unsigned virtual_desktop_rows() const { return m_virtual_desktop_rows; }
    unsigned virtual_desktop_columns() const { return m_virtual_desktop_columns; }

    int taskbar_height() const { return TaskbarWindow::taskbar_height(); }

    void did_receive_screen_rects(Badge<WindowServerConnection>, const Vector<Gfx::IntRect, 4>&, size_t, unsigned, unsigned);

    template<typename F>
    void on_receive_screen_rects(F&& callback)
    {
        m_receive_rects_callbacks.append(forward<F>(callback));
    }

private:
    Vector<Gfx::IntRect, default_screen_rect_count> m_rects;
    size_t m_main_screen_index { 0 };
    Gfx::IntRect m_bounding_rect;
    unsigned m_virtual_desktop_rows { 1 };
    unsigned m_virtual_desktop_columns { 1 };
    Vector<Function<void(Desktop&)>> m_receive_rects_callbacks;
};

}
