/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "ToolboxWidget.h"
#include "Tools/BrushTool.h"
#include "Tools/BucketTool.h"
#include "Tools/CloneTool.h"
#include "Tools/EllipseTool.h"
#include "Tools/EraseTool.h"
#include "Tools/GuideTool.h"
#include "Tools/LineTool.h"
#include "Tools/MoveTool.h"
#include "Tools/PenTool.h"
#include "Tools/PickerTool.h"
#include "Tools/PolygonalSelectTool.h"
#include "Tools/RectangleSelectTool.h"
#include "Tools/RectangleTool.h"
#include "Tools/SprayTool.h"
#include "Tools/WandSelectTool.h"
#include "Tools/ZoomTool.h"
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
    layout()->set_margins(2);

    m_action_group.set_exclusive(true);
    m_action_group.set_unchecking_allowed(false);

    m_toolbar = add<GUI::Toolbar>(Gfx::Orientation::Vertical);
    m_toolbar->set_collapsible(true);
    setup_tools();
}

void ToolboxWidget::setup_tools()
{
    auto add_tool = [&](StringView icon_name, GUI::Shortcut const& shortcut, NonnullOwnPtr<Tool> tool) {
        auto action = GUI::Action::create_checkable(tool->tool_name(), shortcut, Gfx::Bitmap::try_load_from_file(String::formatted("/res/icons/pixelpaint/{}.png", icon_name)).release_value_but_fixme_should_propagate_errors(),
            [this, tool = tool.ptr()](auto& action) {
                if (action.is_checked()) {
                    on_tool_selection(tool);
                    m_active_tool = tool;
                } else {
                    on_tool_selection(nullptr);
                }
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

    add_tool("move"sv, { 0, Key_M }, make<MoveTool>());
    add_tool("pen"sv, { 0, Key_N }, make<PenTool>());
    add_tool("brush"sv, { 0, Key_P }, make<BrushTool>());
    add_tool("bucket"sv, { Mod_Shift, Key_B }, make<BucketTool>());
    add_tool("spray"sv, { Mod_Shift, Key_S }, make<SprayTool>());
    add_tool("picker"sv, { 0, Key_O }, make<PickerTool>());
    add_tool("eraser"sv, { Mod_Shift, Key_E }, make<EraseTool>());
    add_tool("line"sv, { Mod_Ctrl | Mod_Shift, Key_L }, make<LineTool>());
    add_tool("rectangle"sv, { Mod_Ctrl | Mod_Shift, Key_R }, make<RectangleTool>());
    add_tool("circle"sv, { Mod_Ctrl | Mod_Shift, Key_E }, make<EllipseTool>());
    add_tool("zoom"sv, { 0, Key_Z }, make<ZoomTool>());
    add_tool("rectangle-select"sv, { 0, Key_R }, make<RectangleSelectTool>());
    add_tool("wand-select"sv, { 0, Key_W }, make<WandSelectTool>());
    add_tool("polygonal-select"sv, { Mod_Shift, Key_P }, make<PolygonalSelectTool>());
    add_tool("guides"sv, { 0, Key_G }, make<GuideTool>());
    add_tool("clone"sv, { 0, Key_C }, make<CloneTool>());
}

}
