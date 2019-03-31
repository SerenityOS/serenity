#include <WindowServer/WSCursor.h>

WSCursor::WSCursor(Retained<GraphicsBitmap>&& bitmap, const Point& hotspot)
    : m_bitmap(move(bitmap))
    , m_hotspot(hotspot)
{
}

WSCursor::~WSCursor()
{
}

Retained<WSCursor> WSCursor::create(Retained<GraphicsBitmap>&& bitmap)
{
    return adopt(*new WSCursor(move(bitmap), bitmap->rect().center()));
}

Retained<WSCursor> WSCursor::create(Retained<GraphicsBitmap>&& bitmap, const Point& hotspot)
{
    return adopt(*new WSCursor(move(bitmap), hotspot));
}
