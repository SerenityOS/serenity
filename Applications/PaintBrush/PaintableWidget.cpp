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

void PaintableWidget::set_tool(Tool* tool)
{
    if (m_tool)
        m_tool->clear();
    m_tool = tool;
    if (m_tool)
        m_tool->setup(*this);
}

Tool* PaintableWidget::tool()
{
    return m_tool;
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
        m_tool->on_mousedown(event);
}

void PaintableWidget::mouseup_event(GMouseEvent& event)
{
    if (m_tool)
        m_tool->on_mouseup(event);
}

void PaintableWidget::mousemove_event(GMouseEvent& event)
{
    if (m_tool)
        m_tool->on_mousemove(event);
}

void PaintableWidget::set_primary_color(Color color)
{
    if (m_primary_color == color)
        return;
    m_primary_color = color;
    if (on_primary_color_change)
        on_primary_color_change(color);
}

void PaintableWidget::set_secondary_color(Color color)
{
    if (m_secondary_color == color)
        return;
    m_secondary_color = color;
    if (on_secondary_color_change)
        on_secondary_color_change(color);
}
