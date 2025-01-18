/*
 * Copyright (c) 2024, MacDue <macdue@dueutil.tech>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "TaskbarFrame.h"
#include <LibGUI/Painter.h>
#include <LibGfx/Palette.h>

void TaskbarFrame::paint_event(GUI::PaintEvent& event)
{
    GUI::Painter painter(*this);
    painter.fill_rect(rect(), palette().button());
    Frame::paint_event(event);
}
