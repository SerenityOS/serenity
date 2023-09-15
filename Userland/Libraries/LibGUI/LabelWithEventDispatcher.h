/*
 * Copyright (c) 2023, Torsten Engelmann <engelTorsten@gmx.de>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/Frame.h>
#include <LibGUI/Label.h>
#include <LibGUI/Window.h>

namespace GUI {
class LabelWithEventDispatcher : public GUI::Label {
    C_OBJECT(LabelWithEventDispatcher);

public:
    void update_cursor(Gfx::StandardCursor);
    Function<void(MouseEvent&)> on_double_click;
    Function<void(MouseEvent&)> on_mouseup_event;
    Function<void(MouseEvent&)> on_mousemove_event;
    virtual ~LabelWithEventDispatcher() override = default;

protected:
    void doubleclick_event(MouseEvent&) override;
    virtual void mouseup_event(MouseEvent&) override;
    virtual void mousemove_event(MouseEvent&) override;
};
}
