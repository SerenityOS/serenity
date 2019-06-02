#pragma once

#include <AK/AKString.h>
#include <AK/Badge.h>
#include <AK/Function.h>
#include <SharedGraphics/Rect.h>

class GEventLoop;

class GDesktop {
public:
    static GDesktop& the();
    GDesktop();

    String wallpaper() const;
    bool set_wallpaper(const StringView& path);

    Rect rect() const { return m_rect; }
    void did_receive_screen_rect(Badge<GEventLoop>, const Rect&);

    Function<void(const Rect&)> on_rect_change;

private:
    Rect m_rect;
};
