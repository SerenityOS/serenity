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

    virtual void on_mousedown(Layer*, MouseEvent&) override;
    virtual void on_mousemove(Layer*, MouseEvent&) override;
    virtual void on_mouseup(Layer*, MouseEvent&) override;
    virtual void on_second_paint(Layer const*, GUI::PaintEvent&) override;
    virtual void on_keydown(GUI::KeyEvent&) override;
    virtual GUI::Widget* get_properties_widget() override;
    virtual Gfx::StandardCursor cursor() override { return Gfx::StandardCursor::Crosshair; }

private:
    enum class Mode {
        Outline,
        Fill,
    };

    void draw_using(GUI::Painter&, Gfx::IntRect const&);

    RefPtr<GUI::Widget> m_properties_widget;
    GUI::MouseButton m_drawing_button { GUI::MouseButton::None };
    Gfx::IntPoint m_ellipse_start_position;
    Gfx::IntPoint m_ellipse_end_position;
    int m_thickness { 1 };
    Mode m_mode { Mode::Outline };
};

}
