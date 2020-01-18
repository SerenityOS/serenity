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

#include <LibGUI/GDesktop.h>
#include <LibGUI/GWindowServerConnection.h>
#include <string.h>
#include <unistd.h>

GDesktop& GDesktop::the()
{
    static GDesktop* the;
    if (!the)
        the = new GDesktop;
    return *the;
}

GDesktop::GDesktop()
{
}

void GDesktop::did_receive_screen_rect(Badge<GWindowServerConnection>, const Rect& rect)
{
    if (m_rect == rect)
        return;
    m_rect = rect;
    if (on_rect_change)
        on_rect_change(rect);
}

bool GDesktop::set_wallpaper(const StringView& path)
{
    GWindowServerConnection::the().post_message(WindowServer::AsyncSetWallpaper(path));
    return GWindowServerConnection::the().wait_for_specific_message<WindowClient::AsyncSetWallpaperFinished>()->success();
}

String GDesktop::wallpaper() const
{
    return GWindowServerConnection::the().send_sync<WindowServer::GetWallpaper>()->path();
}
