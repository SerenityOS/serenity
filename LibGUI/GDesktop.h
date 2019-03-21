#pragma once

#include <AK/AKString.h>
#include <SharedGraphics/Rect.h>

class GDesktop {
public:
    static GDesktop& the();

    String wallpaper() const;
    bool set_wallpaper(const String& path);

private:
    GDesktop();

    Rect m_rect;
};
