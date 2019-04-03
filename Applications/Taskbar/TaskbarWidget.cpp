#include "TaskbarWidget.h"
#include <LibGUI/GLabel.h>
#include <LibGUI/GButton.h>
#include <LibGUI/GBoxLayout.h>
#include <LibGUI/GPainter.h>
#include <stdio.h>

TaskbarWidget::TaskbarWidget(GWidget* parent)
    : GFrame(parent)
{
    set_fill_with_background_color(true);
    set_layout(make<GBoxLayout>(Orientation::Vertical));
    layout()->set_margins({ 0, 8, 0, 8 });
    layout()->set_spacing(8);

    set_frame_thickness(1);
    set_frame_shape(GFrame::Shape::Panel);
    set_frame_shadow(GFrame::Shadow::Raised);
}

TaskbarWidget::~TaskbarWidget()
{
}

void TaskbarWidget::paint_event(GPaintEvent& event)
{
    GFrame::paint_event(event);

    GPainter painter(*this);
    painter.add_clip_rect(event.rect());
}
