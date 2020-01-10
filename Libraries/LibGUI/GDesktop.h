#pragma once

#include <AK/Badge.h>
#include <AK/Function.h>
#include <AK/String.h>
#include <LibDraw/Rect.h>

class GWindowServerConnection;

class GDesktop {
public:
    static GDesktop& the();
    GDesktop();

    String wallpaper() const;
    bool set_wallpaper(const StringView& path);

    Rect rect() const { return m_rect; }
    void did_receive_screen_rect(Badge<GWindowServerConnection>, const Rect&);

    Function<void(const Rect&)> on_rect_change;

private:
    Rect m_rect;
};
