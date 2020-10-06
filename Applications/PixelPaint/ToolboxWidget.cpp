/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include "ToolboxWidget.h"
#include "BucketTool.h"
#include "EllipseTool.h"
#include "EraseTool.h"
#include "LineTool.h"
#include "MoveTool.h"
#include "PenTool.h"
#include "PickerTool.h"
#include "RectangleTool.h"
#include "SprayTool.h"
#include <AK/StringBuilder.h>
#include <LibGUI/Action.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Button.h>
#include <LibGUI/Window.h>

namespace PixelPaint {

class ToolButton final : public GUI::Button {
    C_OBJECT(ToolButton)
public:
    ToolButton(ToolboxWidget& toolbox, const String& name, const GUI::Shortcut& shortcut, OwnPtr<Tool> tool)
        : m_toolbox(toolbox)
        , m_tool(move(tool))
    {
        StringBuilder builder;
        builder.append(name);
        builder.append(" (");
        builder.append(shortcut.to_string());
        builder.append(")");
        set_tooltip(builder.to_string());

        m_action = GUI::Action::create_checkable(
            name, shortcut, [this](auto& action) {
                if (action.is_checked())
                    m_toolbox.on_tool_selection(m_tool);
                else
                    m_toolbox.on_tool_selection(nullptr);
            },
            toolbox.window());

        m_tool->set_action(m_action);
        set_action(*m_action);
        m_toolbox.m_action_group.add_action(*m_action);
    }

    const Tool& tool() const { return *m_tool; }
    Tool& tool() { return *m_tool; }

    virtual bool is_uncheckable() const override { return false; }

    virtual void context_menu_event(GUI::ContextMenuEvent& event) override
    {
        m_action->activate();
        m_tool->on_tool_button_contextmenu(event);
    }

private:
    ToolboxWidget& m_toolbox;
    OwnPtr<Tool> m_tool;
    RefPtr<GUI::Action> m_action;
};

ToolboxWidget::ToolboxWidget()
{
    set_fill_with_background_color(true);

    set_frame_thickness(1);
    set_frame_shape(Gfx::FrameShape::Panel);
    set_frame_shadow(Gfx::FrameShadow::Raised);

    set_size_policy(GUI::SizePolicy::Fixed, GUI::SizePolicy::Fill);
    set_preferred_size(48, 0);

    set_layout<GUI::VerticalBoxLayout>();
    layout()->set_margins({ 4, 4, 4, 4 });

    m_action_group.set_exclusive(true);
    m_action_group.set_unchecking_allowed(false);

    deferred_invoke([this](auto&) {
        setup_tools();
    });
}

ToolboxWidget::~ToolboxWidget()
{
}

void ToolboxWidget::setup_tools()
{
    auto add_tool = [&](const StringView& name, const StringView& icon_name, const GUI::Shortcut& shortcut, NonnullOwnPtr<Tool> tool) -> ToolButton& {
        m_tools.append(tool.ptr());
        auto& button = add<ToolButton>(*this, name, shortcut, move(tool));
        button.set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fixed);
        button.set_preferred_size(0, 32);
        button.set_checkable(true);
        button.set_icon(Gfx::Bitmap::load_from_file(String::formatted("/res/icons/pixelpaint/{}.png", icon_name)));
        return button;
    };

    add_tool("Move", "move", { 0, Key_M }, make<MoveTool>());
    add_tool("Pen", "pen", { 0, Key_N }, make<PenTool>());
    add_tool("Bucket Fill", "bucket", { Mod_Shift, Key_B }, make<BucketTool>());
    add_tool("Spray", "spray", { Mod_Shift, Key_S }, make<SprayTool>());
    add_tool("Color Picker", "picker", { 0, Key_O }, make<PickerTool>());
    add_tool("Erase", "eraser", { Mod_Shift, Key_E }, make<EraseTool>());
    add_tool("Line", "line", { Mod_Ctrl | Mod_Shift, Key_L }, make<LineTool>());
    add_tool("Rectangle", "rectangle", { Mod_Ctrl | Mod_Shift, Key_R }, make<RectangleTool>());
    add_tool("Ellipse", "circle", { Mod_Ctrl | Mod_Shift, Key_E }, make<EllipseTool>());
}

}
