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
