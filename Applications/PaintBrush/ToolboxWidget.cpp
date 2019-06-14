#include "ToolboxWidget.h"
#include "BucketTool.h"
#include "PaintableWidget.h"
#include "PenTool.h"
#include <LibGUI/GBoxLayout.h>
#include <LibGUI/GButton.h>

class ToolButton final : public GButton {
public:
    ToolButton(const String& name, GWidget* parent, OwnPtr<Tool>&& tool)
        : GButton(name, parent)
        , m_tool(move(tool))
    {
    }

    const Tool& tool() const { return *m_tool; }
    Tool& tool() { return *m_tool; }

private:
    OwnPtr<Tool> m_tool;
};

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

    auto add_tool = [&](const StringView& name, OwnPtr<Tool>&& tool) {
        auto* button = new ToolButton(name, this, move(tool));
        button->set_size_policy(SizePolicy::Fill, SizePolicy::Fixed);
        button->set_preferred_size({ 0, 32 });
        button->set_checkable(true);
        button->set_exclusive(true);

        button->on_checked = [button](auto checked) {
            if (checked)
                PaintableWidget::the().set_tool(&button->tool());
            else
                PaintableWidget::the().set_tool(nullptr);
        };
    };

    add_tool("Pen", make<PenTool>());
    add_tool("Buck", make<BucketTool>());
}

ToolboxWidget::~ToolboxWidget()
{
}
