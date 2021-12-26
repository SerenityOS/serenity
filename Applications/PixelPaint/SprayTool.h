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

#pragma once

#include "Tool.h"
#include <LibCore/Timer.h>
#include <LibGUI/ActionGroup.h>
#include <LibGUI/Painter.h>

namespace PixelPaint {

class SprayTool final : public Tool {
public:
    SprayTool();
    virtual ~SprayTool() override;

    virtual void on_mousedown(Layer&, GUI::MouseEvent& layer_event, GUI::MouseEvent& image_event) override;
    virtual void on_mouseup(Layer&, GUI::MouseEvent& layer_event, GUI::MouseEvent& image_event) override;
    virtual void on_mousemove(Layer&, GUI::MouseEvent& layer_event, GUI::MouseEvent& image_event) override;
    virtual void on_tool_button_contextmenu(GUI::ContextMenuEvent&) override;

private:
    virtual const char* class_name() const override { return "SprayTool"; }
    void paint_it();
    RefPtr<Core::Timer> m_timer;
    Gfx::IntPoint m_last_pos;
    Color m_color;
    RefPtr<GUI::Menu> m_context_menu;
    GUI::ActionGroup m_thickness_actions;
    int m_thickness { 1 };
};

}
