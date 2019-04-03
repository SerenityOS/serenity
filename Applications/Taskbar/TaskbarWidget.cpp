#include "TaskbarWidget.h"
#include <LibGUI/GLabel.h>
#include <LibGUI/GButton.h>
#include <LibGUI/GBoxLayout.h>
#include <LibGUI/GPainter.h>
#include <stdio.h>

TaskbarWidget::TaskbarWidget(WindowList& window_list, GWidget* parent)
    : GFrame(parent)
    , m_window_list(window_list)
{
    set_fill_with_background_color(true);
    set_layout(make<GBoxLayout>(Orientation::Horizontal));
    layout()->set_margins({ 0, 3, 0, 3 });
    layout()->set_spacing(3);

    set_frame_thickness(1);
    set_frame_shape(GFrame::Shape::Panel);
    set_frame_shadow(GFrame::Shadow::Raised);
}

TaskbarWidget::~TaskbarWidget()
{
}
