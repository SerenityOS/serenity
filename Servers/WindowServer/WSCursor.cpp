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

RetainPtr<WSCursor> WSCursor::create(WSStandardCursor standard_cursor)
{
    switch (standard_cursor) {
    case WSStandardCursor::None:
        return nullptr;
    case WSStandardCursor::Arrow:
        return create(*GraphicsBitmap::load_from_file("/res/cursors/arrow.png"));
    case WSStandardCursor::IBeam:
        return create(*GraphicsBitmap::load_from_file("/res/cursors/i-beam.png"));
    }
    ASSERT_NOT_REACHED();
}
