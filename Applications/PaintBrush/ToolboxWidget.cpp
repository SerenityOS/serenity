#include "ToolboxWidget.h"
#include "BucketTool.h"
#include "EraseTool.h"
#include "LineTool.h"
#include "PaintableWidget.h"
#include "PenTool.h"
#include "PickerTool.h"
#include "SprayTool.h"
#include <LibDraw/PNGLoader.h>
#include <LibGUI/GBoxLayout.h>
#include <LibGUI/GButton.h>

class ToolButton final : public GButton {
    C_OBJECT(ToolButton)
public:
    ToolButton(const String& name, GWidget* parent, OwnPtr<Tool>&& tool)
        : GButton(parent)
        , m_tool(move(tool))
    {
        set_tooltip(name);
    }

    const Tool& tool() const { return *m_tool; }
    Tool& tool() { return *m_tool; }

    virtual void context_menu_event(GContextMenuEvent& event) override
    {
        m_tool->on_contextmenu(event);
    }

private:
    OwnPtr<Tool> m_tool;
};

ToolboxWidget::ToolboxWidget(GWidget* parent)
    : GFrame(parent)
{
    set_fill_with_background_color(true);

    set_frame_thickness(1);
    set_frame_shape(FrameShape::Panel);
    set_frame_shadow(FrameShadow::Raised);

    set_size_policy(SizePolicy::Fixed, SizePolicy::Fill);
    set_preferred_size(48, 0);

    set_layout(make<GBoxLayout>(Orientation::Vertical));
    layout()->set_margins({ 4, 4, 4, 4 });

    auto add_tool = [&](const StringView& name, const StringView& icon_name, OwnPtr<Tool>&& tool) {
        auto button = ToolButton::construct(name, this, move(tool));
        button->set_size_policy(SizePolicy::Fill, SizePolicy::Fixed);
        button->set_preferred_size(0, 32);
        button->set_checkable(true);
        button->set_exclusive(true);

        button->set_icon(load_png(String::format("/res/icons/paintbrush/%s.png", String(icon_name).characters())));

        button->on_checked = [button = button.ptr()](auto checked) {
            if (checked)
                PaintableWidget::the().set_tool(&button->tool());
            else
                PaintableWidget::the().set_tool(nullptr);
        };
    };

    add_tool("Pen", "pen", make<PenTool>());
    add_tool("Bucket Fill", "bucket", make<BucketTool>());
    add_tool("Spray", "spray", make<SprayTool>());
    add_tool("Color Picker", "picker", make<PickerTool>());
    add_tool("Erase", "eraser", make<EraseTool>());
    add_tool("Line", "line", make<LineTool>());
}

ToolboxWidget::~ToolboxWidget()
{
}
