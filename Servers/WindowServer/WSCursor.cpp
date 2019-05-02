#include <WindowServer/WSCursor.h>
#include <WindowServer/WSWindowManager.h>

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
        return WSWindowManager::the().arrow_cursor();
    case WSStandardCursor::IBeam:
        return WSWindowManager::the().i_beam_cursor();
    case WSStandardCursor::ResizeHorizontal:
        return WSWindowManager::the().resize_horizontally_cursor();
    case WSStandardCursor::ResizeVertical:
        return WSWindowManager::the().resize_vertically_cursor();
    case WSStandardCursor::ResizeDiagonalTLBR:
        return WSWindowManager::the().resize_diagonally_tlbr_cursor();
    case WSStandardCursor::ResizeDiagonalBLTR:
        return WSWindowManager::the().resize_diagonally_bltr_cursor();
    }
    ASSERT_NOT_REACHED();
}
