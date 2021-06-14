/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "RectangleSelectTool.h"

namespace PixelPaint {

RectangleSelectTool::RectangleSelectTool()
{
}

RectangleSelectTool::~RectangleSelectTool()
{
}

void RectangleSelectTool::on_mousedown(Layer&, GUI::MouseEvent&, GUI::MouseEvent&)
{
}

void RectangleSelectTool::on_mousemove(Layer&, GUI::MouseEvent&, GUI::MouseEvent&)
{
}

void RectangleSelectTool::on_mouseup(Layer&, GUI::MouseEvent&, GUI::MouseEvent&)
{
}

void RectangleSelectTool::on_second_paint(Layer const&, GUI::PaintEvent&)
{
}

}
