#pragma once

#include <AK/AKString.h>
#include <AK/Badge.h>
#include <SharedGraphics/Rect.h>

class GEventLoop;

class GDesktop {
public:
    static GDesktop& the();
    GDesktop();

    String wallpaper() const;
    bool set_wallpaper(const String& path);

    Rect rect() const { return m_rect; }
    void did_receive_screen_rect(Badge<GEventLoop>, const Rect&);

private:
    Rect m_rect;
};
