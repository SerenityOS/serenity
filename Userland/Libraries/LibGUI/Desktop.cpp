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
#include <unistd.h>

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

void Desktop::did_receive_screen_rect(Badge<WindowServerConnection>, const Gfx::IntRect& rect)
{
    if (m_rect == rect)
        return;
    m_rect = rect;
}

void Desktop::set_background_color(const StringView& background_color)
{
    WindowServerConnection::the().post_message(Messages::WindowServer::SetBackgroundColor(background_color));
}

void Desktop::set_wallpaper_mode(const StringView& mode)
{
    WindowServerConnection::the().post_message(Messages::WindowServer::SetWallpaperMode(mode));
}

bool Desktop::set_wallpaper(const StringView& path, bool save_config)
{
    WindowServerConnection::the().post_message(Messages::WindowServer::AsyncSetWallpaper(path));
    auto ret_val = WindowServerConnection::the().wait_for_specific_message<Messages::WindowClient::AsyncSetWallpaperFinished>()->success();

    if (ret_val && save_config) {
        RefPtr<Core::ConfigFile> config = Core::ConfigFile::get_for_app("WindowManager");
        dbgln("Saving wallpaper path '{}' to config file at {}", path, config->filename());
        config->write_entry("Background", "Wallpaper", path);
        config->sync();
    }

    return ret_val;
}

String Desktop::wallpaper() const
{
    return WindowServerConnection::the().send_sync<Messages::WindowServer::GetWallpaper>()->path();
}

}
