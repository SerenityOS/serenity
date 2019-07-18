#include "PickerTool.h"
#include <LibDraw/GraphicsBitmap.h>

PickerTool::PickerTool()
{
}

PickerTool::~PickerTool()
{
}

void PickerTool::on_mousedown(GMouseEvent& event)
{
    ASSERT(m_widget);
    if (!m_widget->bitmap().rect().contains(event.position()))
        return;
    auto color = m_widget->bitmap().get_pixel(event.position());
    if (event.button() == GMouseButton::Left)
        m_widget->set_primary_color(color);
    else if (event.button() == GMouseButton::Right)
        m_widget->set_secondary_color(color);
}
