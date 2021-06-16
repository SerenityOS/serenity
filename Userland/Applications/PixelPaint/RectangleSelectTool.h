/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Tool.h"

namespace PixelPaint {

class RectangleSelectTool final : public Tool {
public:
    RectangleSelectTool();
    virtual ~RectangleSelectTool();

    virtual void on_mousedown(Layer&, GUI::MouseEvent&, GUI::MouseEvent&) override;
    virtual void on_mousemove(Layer&, GUI::MouseEvent&, GUI::MouseEvent&) override;
    virtual void on_mouseup(Layer&, GUI::MouseEvent&, GUI::MouseEvent&) override;
    virtual void on_keydown(GUI::KeyEvent&) override;
    virtual void on_keyup(GUI::KeyEvent&) override;
    virtual void on_second_paint(Layer const&, GUI::PaintEvent&) override;

private:
    enum class MovingMode {
        MovingOrigin,
        AroundCenter,
        None,
    };

    bool m_selecting { false };
    MovingMode m_moving_mode { MovingMode::None };
    Gfx::IntPoint m_selection_start;
    Gfx::IntPoint m_selection_end;
};

}
