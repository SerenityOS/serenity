#pragma once

#include <AK/AKString.h>
#include <SharedGraphics/Rect.h>

class GDesktop {
public:
    static GDesktop& the();
    GDesktop();

    String wallpaper() const;
    bool set_wallpaper(const String& path);

private:
    Rect m_rect;
};
