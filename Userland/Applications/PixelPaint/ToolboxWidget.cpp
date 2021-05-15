/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "ToolboxWidget.h"
#include "BrushTool.h"
#include "BucketTool.h"
#include "EllipseTool.h"
#include "EraseTool.h"
#include "LineTool.h"
#include "MoveTool.h"
#include "PenTool.h"
#include "PickerTool.h"
#include "RectangleTool.h"
#include "SprayTool.h"
#include "ZoomTool.h"
#include <LibGUI/Action.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Button.h>
#include <LibGUI/Toolbar.h>

REGISTER_WIDGET(PixelPaint, ToolboxWidget);

namespace PixelPaint {

ToolboxWidget::ToolboxWidget()
{
    set_fill_with_background_color(true);

    set_fixed_width(26);

    set_layout<GUI::VerticalBoxLayout>();
    layout()->set_spacing(0);
    layout()->set_margins({ 2, 2, 2, 2 });

    m_action_group.set_exclusive(true);
    m_action_group.set_unchecking_allowed(false);

    m_toolbar = add<GUI::Toolbar>(Gfx::Orientation::Vertical);
    setup_tools();
}

ToolboxWidget::~ToolboxWidget()
{
}

void ToolboxWidget::setup_tools()
{
    auto add_tool = [&](String name, StringView const& icon_name, GUI::Shortcut const& shortcut, NonnullOwnPtr<Tool> tool) {
        auto action = GUI::Action::create_checkable(move(name), shortcut, Gfx::Bitmap::load_from_file(String::formatted("/res/icons/pixelpaint/{}.png", icon_name)),
            [this, tool = tool.ptr()](auto& action) {
                if (action.is_checked())
                    on_tool_selection(tool);
                else
                    on_tool_selection(nullptr);
            });
        m_action_group.add_action(action);
        auto& button = m_toolbar->add_action(action);
        button.on_context_menu_request = [action = action.ptr(), tool = tool.ptr()](auto& event) {
            action->activate();
            tool->on_tool_button_contextmenu(event);
        };
        tool->set_action(action);
        m_tools.append(move(tool));
    };

    add_tool("Move", "move", { 0, Key_M }, make<MoveTool>());
    add_tool("Pen", "pen", { 0, Key_N }, make<PenTool>());
    add_tool("Brush", "brush", { 0, Key_P }, make<BrushTool>());
    add_tool("Bucket Fill", "bucket", { Mod_Shift, Key_B }, make<BucketTool>());
    add_tool("Spray", "spray", { Mod_Shift, Key_S }, make<SprayTool>());
    add_tool("Color Picker", "picker", { 0, Key_O }, make<PickerTool>());
    add_tool("Erase", "eraser", { Mod_Shift, Key_E }, make<EraseTool>());
    add_tool("Line", "line", { Mod_Ctrl | Mod_Shift, Key_L }, make<LineTool>());
    add_tool("Rectangle", "rectangle", { Mod_Ctrl | Mod_Shift, Key_R }, make<RectangleTool>());
    add_tool("Ellipse", "circle", { Mod_Ctrl | Mod_Shift, Key_E }, make<EllipseTool>());
    add_tool("Zoom", "zoom", { 0, Key_Z }, make<ZoomTool>());
}

}
