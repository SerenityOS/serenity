/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/ByteString.h>
#include <AK/Function.h>
#include <LibGUI/Forward.h>
#include <LibGUI/SystemEffects.h>
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
    Desktop() = default;

    void set_background_color(StringView background_color);

    void set_wallpaper_mode(StringView mode);

    ByteString wallpaper_path() const;
    RefPtr<Gfx::Bitmap> wallpaper_bitmap() const;
    bool set_wallpaper(RefPtr<Gfx::Bitmap const> wallpaper_bitmap, Optional<StringView> path);

    void set_system_effects(Vector<bool> effects) { m_system_effects = { effects }; }
    SystemEffects const& system_effects() const { return m_system_effects; }

    Gfx::IntRect rect() const { return m_bounding_rect; }
    Vector<Gfx::IntRect, 4> const& rects() const { return m_rects; }
    size_t main_screen_index() const { return m_main_screen_index; }

    unsigned workspace_rows() const { return m_workspace_rows; }
    unsigned workspace_columns() const { return m_workspace_columns; }

    int taskbar_height() const { return TaskbarWindow::taskbar_height(); }

    void did_receive_screen_rects(Badge<ConnectionToWindowServer>, Vector<Gfx::IntRect, 4> const&, size_t, unsigned, unsigned);

    template<typename F>
    void on_receive_screen_rects(F&& callback)
    {
        m_receive_rects_callbacks.append(forward<F>(callback));
    }

private:
    Vector<Gfx::IntRect, default_screen_rect_count> m_rects;
    size_t m_main_screen_index { 0 };
    Gfx::IntRect m_bounding_rect;
    unsigned m_workspace_rows { 1 };
    unsigned m_workspace_columns { 1 };
    Vector<Function<void(Desktop&)>> m_receive_rects_callbacks;
    bool m_is_setting_desktop_wallpaper { false };
    SystemEffects m_system_effects;
};

}
