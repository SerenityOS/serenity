/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Tool.h"
#include <LibGUI/ActionGroup.h>
#include <LibGfx/Point.h>

namespace PixelPaint {

class LineTool final : public Tool {
public:
    LineTool();
    virtual ~LineTool() override;

    virtual void on_mousedown(Layer&, GUI::MouseEvent& layer_event, GUI::MouseEvent& image_event) override;
    virtual void on_mousemove(Layer&, GUI::MouseEvent& layer_event, GUI::MouseEvent& image_event) override;
    virtual void on_mouseup(Layer&, GUI::MouseEvent& layer_event, GUI::MouseEvent& image_event) override;
    virtual void on_tool_button_contextmenu(GUI::ContextMenuEvent&) override;
    virtual void on_second_paint(const Layer&, GUI::PaintEvent&) override;
    virtual void on_keydown(GUI::KeyEvent&) override;
    virtual void on_keyup(GUI::KeyEvent&) override;

private:
    GUI::MouseButton m_drawing_button { GUI::MouseButton::None };
    Gfx::IntPoint m_line_start_position;
    Gfx::IntPoint m_line_end_position;

    RefPtr<GUI::Menu> m_context_menu;
    GUI::ActionGroup m_thickness_actions;
    int m_thickness { 1 };
    bool m_constrain_angle { false };
};

}
