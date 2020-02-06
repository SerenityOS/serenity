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

#include <LibCore/ConfigFile.h>
#include <LibGUI/GDesktop.h>
#include <LibGUI/GWindowServerConnection.h>
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

void Desktop::did_receive_screen_rect(Badge<WindowServerConnection>, const Gfx::Rect& rect)
{
    if (m_rect == rect)
        return;
    m_rect = rect;
    if (on_rect_change)
        on_rect_change(rect);
}

bool Desktop::set_wallpaper(const StringView& path)
{
    WindowServerConnection::the().post_message(WindowServer::AsyncSetWallpaper(path));
    auto ret_val = WindowServerConnection::the().wait_for_specific_message<WindowClient::AsyncSetWallpaperFinished>()->success();

    if (ret_val) {
        RefPtr<Core::ConfigFile> config = Core::ConfigFile::get_for_app("WindowManager");
        dbg() << "Saving wallpaper path '" << path << "' to config file at " << config->file_name();
        config->write_entry("Background", "Wallpaper", path);
        config->sync();
    }

    return ret_val;
}

String Desktop::wallpaper() const
{
    return WindowServerConnection::the().send_sync<WindowServer::GetWallpaper>()->path();
}

}
