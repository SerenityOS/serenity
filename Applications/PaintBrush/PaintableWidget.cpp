#include "PaintableWidget.h"
#include <LibGUI/GPainter.h>
#include <SharedGraphics/GraphicsBitmap.h>

PaintableWidget::PaintableWidget(GWidget* parent)
    : GWidget(parent)
{
    set_fill_with_background_color(true);
    set_background_color(Color::LightGray);
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
    if (event.button() != GMouseButton::Left && event.button() != GMouseButton::Right)
        return;

    GPainter painter(*m_bitmap);
    painter.set_pixel(event.position(), color_for(event));
    update({ event.position(), { 1, 1 } });
    m_last_drawing_event_position = event.position();
}

void PaintableWidget::mouseup_event(GMouseEvent& event)
{
    if (event.button() == GMouseButton::Left || event.button() == GMouseButton::Right)
        m_last_drawing_event_position = { -1, -1 };
}

void PaintableWidget::mousemove_event(GMouseEvent& event)
{
    if (!rect().contains(event.position()))
        return;

    if (event.buttons() & GMouseButton::Left || event.buttons() & GMouseButton::Right) {
        GPainter painter(*m_bitmap);

        if (m_last_drawing_event_position != Point(-1, -1)) {
            painter.draw_line(m_last_drawing_event_position, event.position(), color_for(event));
            update();
        } else {
            painter.set_pixel(event.position(), color_for(event));
            update({ event.position(), { 1, 1 } });
        }

        m_last_drawing_event_position = event.position();
    }
}
