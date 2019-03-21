#include <LibGUI/GDesktop.h>
#include <LibGUI/GEventLoop.h>
#include <string.h>

GDesktop& GDesktop::the()
{
    static GDesktop* s_the;
    if (!s_the)
        s_the = new GDesktop;
    return *s_the;
}

GDesktop::GDesktop()
{
}

bool GDesktop::set_wallpaper(const String& path)
{
    WSAPI_ClientMessage message;
    message.type = WSAPI_ClientMessage::Type::SetWallpaper;
    ASSERT(path.length() < (int)sizeof(message.text));
    strncpy(message.text, path.characters(), path.length());
    message.text_length = path.length();
    auto response = GEventLoop::current().sync_request(message, WSAPI_ServerMessage::Type::DidSetWallpaper);
    return response.value;
}

String GDesktop::wallpaper() const
{
    WSAPI_ClientMessage message;
    message.type = WSAPI_ClientMessage::Type::GetWallpaper;
    auto response = GEventLoop::current().sync_request(message, WSAPI_ServerMessage::Type::DidGetWallpaper);
    return String(response.text, response.text_length);
}
