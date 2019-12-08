#pragma once

#include <LibDraw/GraphicsBitmap.h>

enum class WSStandardCursor {
    None = 0,
    Arrow,
    IBeam,
    ResizeHorizontal,
    ResizeVertical,
    ResizeDiagonalTLBR,
    ResizeDiagonalBLTR,
    Hand,
    Drag,
};

class WSCursor : public RefCounted<WSCursor> {
public:
    static NonnullRefPtr<WSCursor> create(NonnullRefPtr<GraphicsBitmap>&&, const Point& hotspot);
    static NonnullRefPtr<WSCursor> create(NonnullRefPtr<GraphicsBitmap>&&);
    static RefPtr<WSCursor> create(WSStandardCursor);
    ~WSCursor();

    Point hotspot() const { return m_hotspot; }
    const GraphicsBitmap& bitmap() const { return *m_bitmap; }

    Rect rect() const { return m_bitmap->rect(); }
    Size size() const { return m_bitmap->size(); }

private:
    WSCursor(NonnullRefPtr<GraphicsBitmap>&&, const Point&);

    RefPtr<GraphicsBitmap> m_bitmap;
    Point m_hotspot;
};
