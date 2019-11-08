#include "FormEditorWidget.h"
#include "FormWidget.h"
#include <LibGUI/GPainter.h>

FormEditorWidget::FormEditorWidget(GWidget* parent)
    : GScrollableWidget(parent)
{
    set_fill_with_background_color(true);
    set_background_color(Color::White);

    set_frame_shape(FrameShape::Container);
    set_frame_shadow(FrameShadow::Sunken);
    set_frame_thickness(2);

    m_form_widget = FormWidget::construct(*this);
}

FormEditorWidget::~FormEditorWidget()
{
}

void FormEditorWidget::paint_event(GPaintEvent& event)
{
    GFrame::paint_event(event);

    GPainter painter(*this);
    painter.add_clip_rect(event.rect());
}
