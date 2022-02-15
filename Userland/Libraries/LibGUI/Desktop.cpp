/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Badge.h>
#include <AK/TemporaryChange.h>
#include <LibGUI/Desktop.h>
#include <LibGUI/WindowServerConnection.h>
#include <string.h>

// Including this after to avoid LibIPC errors
#include <LibConfig/Client.h>

namespace GUI {

Desktop& Desktop::the()
{
    static Desktop s_the;
    return s_the;
}

Desktop::Desktop()
{
}

void Desktop::did_receive_screen_rects(Badge<WindowServerConnection>, const Vector<Gfx::IntRect, 4>& rects, size_t main_screen_index, unsigned workspace_rows, unsigned workspace_columns)
{
    m_main_screen_index = main_screen_index;
    m_rects = rects;
    if (!m_rects.is_empty()) {
        m_bounding_rect = m_rects[0];
        for (size_t i = 1; i < m_rects.size(); i++)
            m_bounding_rect = m_bounding_rect.united(m_rects[i]);
    } else {
        m_bounding_rect = {};
    }

    m_workspace_rows = workspace_rows;
    m_workspace_columns = workspace_columns;

    for (auto& callback : m_receive_rects_callbacks)
        callback(*this);
}

void Desktop::set_background_color(StringView background_color)
{
    WindowServerConnection::the().async_set_background_color(background_color);
}

void Desktop::set_wallpaper_mode(StringView mode)
{
    WindowServerConnection::the().async_set_wallpaper_mode(mode);
}

String Desktop::wallpaper_path() const
{
    return Config::read_string("WindowManager", "Background", "Wallpaper");
}

RefPtr<Gfx::Bitmap> Desktop::wallpaper_bitmap() const
{
    return WindowServerConnection::the().get_wallpaper().bitmap();
}

bool Desktop::set_wallpaper(RefPtr<Gfx::Bitmap> wallpaper_bitmap, Optional<String> path)
{
    if (m_is_setting_desktop_wallpaper)
        return false;

    TemporaryChange is_setting_desktop_wallpaper_change(m_is_setting_desktop_wallpaper, true);
    WindowServerConnection::the().async_set_wallpaper(wallpaper_bitmap ? wallpaper_bitmap->to_shareable_bitmap() : Gfx::ShareableBitmap {});
    auto ret_val = WindowServerConnection::the().wait_for_specific_message<Messages::WindowClient::SetWallpaperFinished>()->success();

    if (ret_val && path.has_value()) {
        dbgln("Saving wallpaper path '{}' to ConfigServer", *path);
        Config::write_string("WindowManager", "Background", "Wallpaper", *path);
    }

    return ret_val;
}
}
