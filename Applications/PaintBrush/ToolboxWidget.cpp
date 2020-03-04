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
#include "PaintableWidget.h"
#include "PenTool.h"
#include "PickerTool.h"
#include "RectangleTool.h"
#include "SprayTool.h"
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Button.h>

class ToolButton final : public GUI::Button {
    C_OBJECT(ToolButton)
public:
    ToolButton(const String& name, OwnPtr<Tool>&& tool)
        : m_tool(move(tool))
    {
        set_tooltip(name);
    }

    const Tool& tool() const { return *m_tool; }
    Tool& tool() { return *m_tool; }

    virtual void context_menu_event(GUI::ContextMenuEvent& event) override
    {
        set_checked(true);
        m_tool->on_contextmenu(event);
    }

private:
    OwnPtr<Tool> m_tool;
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

    auto add_tool = [&](const StringView& name, const StringView& icon_name, OwnPtr<Tool>&& tool) {
        auto button = add<ToolButton>(name, move(tool));
        button->set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fixed);
        button->set_preferred_size(0, 32);
        button->set_checkable(true);
        button->set_exclusive(true);

        button->set_icon(Gfx::Bitmap::load_from_file(String::format("/res/icons/paintbrush/%s.png", String(icon_name).characters())));

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
    add_tool("Rectangle", "rectangle", make<RectangleTool>());
    add_tool("Ellipse", "circle", make<EllipseTool>());
}

ToolboxWidget::~ToolboxWidget()
{
}
