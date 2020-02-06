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
#include <LibGfx/Point.h>
#include <LibGUI/ActionGroup.h>

namespace GUI {
class Menu;
}

class LineTool final : public Tool {
public:
    LineTool();
    virtual ~LineTool() override;

    virtual void on_mousedown(GUI::MouseEvent&) override;
    virtual void on_mousemove(GUI::MouseEvent&) override;
    virtual void on_mouseup(GUI::MouseEvent&) override;
    virtual void on_contextmenu(GUI::ContextMenuEvent&) override;
    virtual void on_second_paint(GUI::PaintEvent&) override;
    virtual void on_keydown(GUI::KeyEvent&) override;
    virtual void on_keyup(GUI::KeyEvent&) override;

private:
    virtual const char* class_name() const override { return "LineTool"; }

    GUI::MouseButton m_drawing_button { GUI::MouseButton::None };
    Gfx::Point m_line_start_position;
    Gfx::Point m_line_end_position;
    RefPtr<GUI::Menu> m_context_menu;
    GUI::ActionGroup m_thickness_actions;
    int m_thickness { 1 };
    bool m_constrain_angle { false };
};
