/*
 * Copyright (c) 2023, Torsten Engelmann <engelTorsten@gmx.de>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGUI/LabelWithEventDispatcher.h>

REGISTER_WIDGET(GUI, LabelWithEventDispatcher)

namespace GUI {

void LabelWithEventDispatcher::update_cursor(Gfx::StandardCursor cursor)
{
    if (override_cursor() == cursor)
        return;
    set_override_cursor(cursor);
    update();
}

void LabelWithEventDispatcher::doubleclick_event(MouseEvent& event)
{
    if (on_double_click)
        on_double_click(event);
}

void LabelWithEventDispatcher::mouseup_event(MouseEvent& event)
{
    if (on_mouseup_event)
        on_mouseup_event(event);
}

void LabelWithEventDispatcher::mousemove_event(MouseEvent& event)
{
    if (on_mousemove_event)
        on_mousemove_event(event);
}
}
