#include "PaintableWidget.h"
#include "Tool.h"
#include <LibGUI/GPainter.h>
#include <SharedGraphics/GraphicsBitmap.h>

static PaintableWidget* s_the;

PaintableWidget& PaintableWidget::the()
{
    return *s_the;
}

PaintableWidget::PaintableWidget(GWidget* parent)
    : GWidget(parent)
{
    ASSERT(!s_the);
    s_the = this;
    set_fill_with_background_color(true);
    set_background_color(Color::MidGray);
    m_bitmap = GraphicsBitmap::create(GraphicsBitmap::Format::RGB32, { 600, 400 });
    m_bitmap->fill(Color::White);
}

PaintableWidget::~PaintableWidget()
{
}

void PaintableWidget::paint_event(GPaintEvent& event)
{
    GPainter painter(*this);
    painter.add_clip_rect(event.rect());
    painter.blit({ 0, 0 }, *m_bitmap, m_bitmap->rect());
}

Color PaintableWidget::color_for(const GMouseEvent& event)
{
    if (event.buttons() & GMouseButton::Left)
        return m_primary_color;
    if (event.buttons() & GMouseButton::Right)
        return m_secondary_color;
    ASSERT_NOT_REACHED();
}

void PaintableWidget::mousedown_event(GMouseEvent& event)
{
    if (m_tool)
        m_tool->on_mousedown(*this, event);
}

void PaintableWidget::mouseup_event(GMouseEvent& event)
{
    if (m_tool)
        m_tool->on_mouseup(*this, event);
}

void PaintableWidget::mousemove_event(GMouseEvent& event)
{
    if (m_tool)
        m_tool->on_mousemove(*this, event);
}
