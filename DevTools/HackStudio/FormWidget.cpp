#include "FormWidget.h"
#include "FormEditorWidget.h"
#include <LibGUI/GPainter.h>

FormWidget::FormWidget(FormEditorWidget& parent)
    : GWidget(&parent)
{
    set_fill_with_background_color(true);
    set_background_color(Color::WarmGray);
    set_relative_rect(5, 5, 400, 300);
}

FormWidget::~FormWidget()
{
}

void FormWidget::paint_event(GPaintEvent& event)
{
    GPainter painter(*this);
    painter.add_clip_rect(event.rect());

    for (int y = 0; y < height(); y += m_grid_size) {
        for (int x = 0; x < width(); x += m_grid_size) {
            painter.set_pixel({ x, y }, Color::from_rgb(0x404040));
        }
    }
}
