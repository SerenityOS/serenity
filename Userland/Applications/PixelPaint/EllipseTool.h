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

class EllipseTool final : public Tool {
public:
    EllipseTool();
    virtual ~EllipseTool() override;

    virtual void on_mousedown(Layer&, GUI::MouseEvent& layer_event, GUI::MouseEvent& image_event) override;
    virtual void on_mousemove(Layer&, GUI::MouseEvent& layer_event, GUI::MouseEvent& image_event) override;
    virtual void on_mouseup(Layer&, GUI::MouseEvent& layer_event, GUI::MouseEvent& image_event) override;
    virtual void on_tool_button_contextmenu(GUI::ContextMenuEvent&) override;
    virtual void on_second_paint(const Layer&, GUI::PaintEvent&) override;
    virtual void on_keydown(GUI::KeyEvent&) override;

private:
    enum class Mode {
        Outline,
        Fill,
    };

    void draw_using(GUI::Painter&, const Gfx::IntRect&);

    GUI::MouseButton m_drawing_button { GUI::MouseButton::None };
    Gfx::IntPoint m_ellipse_start_position;
    Gfx::IntPoint m_ellipse_end_position;
    RefPtr<GUI::Menu> m_context_menu;
    int m_thickness { 1 };
    GUI::ActionGroup m_thickness_actions;
    Mode m_mode { Mode::Outline };
};

}
