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
    virtual void on_second_paint(Layer const&, GUI::PaintEvent&) override;

private:
    bool m_selecting { false };
    Gfx::IntPoint m_selection_start;
    Gfx::IntPoint m_selection_end;
};

}
