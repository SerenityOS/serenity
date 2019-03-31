#pragma once

#include <SharedGraphics/GraphicsBitmap.h>

class WSCursor : public Retainable<WSCursor> {
public:
    static Retained<WSCursor> create(Retained<GraphicsBitmap>&&, const Point& hotspot);
    static Retained<WSCursor> create(Retained<GraphicsBitmap>&&);
    ~WSCursor();

    Point hotspot() const { return m_hotspot; }
    const GraphicsBitmap& bitmap() const { return *m_bitmap; }

    Rect rect() const { return m_bitmap->rect(); }
    Size size() const { return m_bitmap->size(); }

private:
    WSCursor(Retained<GraphicsBitmap>&&, const Point&);

    RetainPtr<GraphicsBitmap> m_bitmap;
    Point m_hotspot;
};
