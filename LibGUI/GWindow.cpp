#include "GWindow.h"
#include "GEvent.h"
#include "GEventLoop.h"
#include <SharedGraphics/GraphicsBitmap.h>

GWindow::GWindow(int window_id)
    : m_window_id(window_id)
{
}

GWindow::~GWindow()
{
}

void GWindow::set_title(String&& title)
{
    if (m_title == title)
        return;

    m_title = move(title);
}
void GWindow::set_rect(const Rect& rect)
{
    if (m_rect == rect)
        return;
    m_rect = rect;
    dbgprintf("GWindow::setRect %d,%d %dx%d\n", m_rect.x(), m_rect.y(), m_rect.width(), m_rect.height());
}

void GWindow::event(GEvent& event)
{
}

bool GWindow::is_visible() const
{
    return false;
}

void GWindow::close()
{
}

