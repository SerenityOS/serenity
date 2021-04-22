/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "WidgetTool.h"
#include <AK/Format.h>

namespace HackStudio {

void WidgetTool::on_mousedown([[maybe_unused]] GUI::MouseEvent& event)
{
    dbgln("WidgetTool::on_mousedown");
}

void WidgetTool::on_mouseup([[maybe_unused]] GUI::MouseEvent& event)
{
    dbgln("WidgetTool::on_mouseup");
}

void WidgetTool::on_mousemove([[maybe_unused]] GUI::MouseEvent& event)
{
    dbgln("WidgetTool::on_mousemove");
}

void WidgetTool::on_keydown([[maybe_unused]] GUI::KeyEvent& event)
{
    dbgln("WidgetTool::on_keydown");
}

}
