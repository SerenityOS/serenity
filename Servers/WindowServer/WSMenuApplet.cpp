#include <WindowServer/WSMenuApplet.h>
#include <WindowServer/WSMenuManager.h>
#include <WindowServer/WSWindowManager.h>

static i32 s_next_applet_id = 1;

WSMenuApplet::WSMenuApplet(const Size& size)
    : m_applet_id(s_next_applet_id++)
    , m_size(size)
{
}

WSMenuApplet::~WSMenuApplet()
{
}

void WSMenuApplet::set_bitmap(GraphicsBitmap* bitmap)
{
    m_bitmap = bitmap;
}

void WSMenuApplet::invalidate(const Rect& rect)
{
    WSWindowManager::the().menu_manager().invalidate_applet(*this, rect);
}
