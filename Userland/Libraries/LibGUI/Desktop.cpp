/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Badge.h>
#include <LibCore/ConfigFile.h>
#include <LibGUI/Desktop.h>
#include <LibGUI/WindowServerConnection.h>
#include <string.h>

namespace GUI {

Desktop& Desktop::the()
{
    static Desktop* the;
    if (!the)
        the = new Desktop;
    return *the;
}

Desktop::Desktop()
{
}

void Desktop::did_receive_screen_rects(Badge<WindowServerConnection>, const Vector<Gfx::IntRect, 4>& rects, size_t main_screen_index, unsigned virtual_desktop_rows, unsigned virtual_desktop_columns)
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

    m_virtual_desktop_rows = virtual_desktop_rows;
    m_virtual_desktop_columns = virtual_desktop_columns;

    for (auto& callback : m_receive_rects_callbacks)
        callback(*this);
}

void Desktop::set_background_color(const StringView& background_color)
{
    WindowServerConnection::the().async_set_background_color(background_color);
}

void Desktop::set_wallpaper_mode(const StringView& mode)
{
    WindowServerConnection::the().async_set_wallpaper_mode(mode);
}

bool Desktop::set_wallpaper(const StringView& path, bool save_config)
{
    WindowServerConnection::the().async_set_wallpaper(path);
    auto ret_val = WindowServerConnection::the().wait_for_specific_message<Messages::WindowClient::SetWallpaperFinished>()->success();

    if (ret_val && save_config) {
        RefPtr<Core::ConfigFile> config = Core::ConfigFile::get_for_app("WindowManager", Core::ConfigFile::AllowWriting::Yes);
        dbgln("Saving wallpaper path '{}' to config file at {}", path, config->filename());
        config->write_entry("Background", "Wallpaper", path);
        config->sync();
    }

    return ret_val;
}

String Desktop::wallpaper() const
{
    return WindowServerConnection::the().get_wallpaper();
}

}
