#include "ToolboxWidget.h"
#include <LibGUI/GBoxLayout.h>
#include <LibGUI/GButton.h>

ToolboxWidget::ToolboxWidget(GWidget* parent)
    : GFrame(parent)
{
    set_background_color(Color::LightGray);
    set_fill_with_background_color(true);

    set_frame_thickness(1);
    set_frame_shape(FrameShape::Panel);
    set_frame_shadow(FrameShadow::Raised);

    set_size_policy(SizePolicy::Fixed, SizePolicy::Fill);
    set_preferred_size({ 48, 0 });

    set_layout(make<GBoxLayout>(Orientation::Vertical));
    layout()->set_margins({ 4, 4, 4, 4 });

    auto add_tool = [&] (const StringView& name) {
        auto* button = new GButton(name, this);
        button->set_size_policy(SizePolicy::Fill, SizePolicy::Fixed);
        button->set_preferred_size({ 0, 32 });
        button->set_checkable(true);
        button->set_exclusive(true);
    };

    add_tool("Pen");
    add_tool("Buck");
    add_tool("Pick");
}

ToolboxWidget::~ToolboxWidget()
{
}
