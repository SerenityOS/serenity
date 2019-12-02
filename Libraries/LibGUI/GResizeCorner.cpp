#include <LibGUI/GPainter.h>
#include <LibGUI/GResizeCorner.h>
#include <LibGUI/GWindow.h>
#include <LibDraw/GraphicsBitmap.h>

GResizeCorner::GResizeCorner(GWidget* parent)
    : GWidget(parent)
{
    set_size_policy(SizePolicy::Fixed, SizePolicy::Fixed);
    set_preferred_size(16, 16);
    m_bitmap = GraphicsBitmap::load_from_file("/res/icons/resize-corner.png");
    ASSERT(m_bitmap);
}

GResizeCorner::~GResizeCorner()
{
}

void GResizeCorner::paint_event(GPaintEvent& event)
{
    GPainter painter(*this);
    painter.add_clip_rect(event.rect());
    painter.fill_rect(rect(), background_color());
    painter.blit({ 0, 0 }, *m_bitmap, m_bitmap->rect());
    GWidget::paint_event(event);
}

void GResizeCorner::mousedown_event(GMouseEvent& event)
{
    if (event.button() == GMouseButton::Left)
        window()->start_wm_resize();
    GWidget::mousedown_event(event);
}

void GResizeCorner::enter_event(CEvent& event)
{
    window()->set_override_cursor(GStandardCursor::ResizeDiagonalTLBR);
    GWidget::enter_event(event);
}

void GResizeCorner::leave_event(CEvent& event)
{
    window()->set_override_cursor(GStandardCursor::None);
    GWidget::leave_event(event);
}
