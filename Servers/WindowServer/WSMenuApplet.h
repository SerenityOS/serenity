#pragma once

#include <AK/Noncopyable.h>
#include <AK/Weakable.h>
#include <LibDraw/Rect.h>
#include <LibDraw/Size.h>

class GraphicsBitmap;

class WSMenuApplet : public Weakable<WSMenuApplet> {
    AK_MAKE_NONCOPYABLE(WSMenuApplet)
    AK_MAKE_NONMOVABLE(WSMenuApplet)
public:
    explicit WSMenuApplet(const Size&);
    ~WSMenuApplet();

    i32 applet_id() const { return m_applet_id; }
    Size size() const { return m_size; }

    void set_bitmap(GraphicsBitmap*);
    const GraphicsBitmap* bitmap() const { return m_bitmap; }

    void invalidate(const Rect&);

    const Rect& rect_in_menubar() const { return m_rect_in_menubar; }
    void set_rect_in_menubar(const Rect& rect) { m_rect_in_menubar = rect; }

private:
    i32 m_applet_id { -1 };
    Size m_size;
    Rect m_rect_in_menubar;
    RefPtr<GraphicsBitmap> m_bitmap;
};
