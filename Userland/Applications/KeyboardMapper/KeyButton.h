/*
 * Copyright (c) 2020, Hüseyin Aslıtürk <asliturk@hotmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/AbstractButton.h>

class KeyButton final : public GUI::AbstractButton {
    C_OBJECT(KeyButton)

public:
    virtual ~KeyButton() override;

    void set_pressed(bool value) { m_pressed = value; }

    Function<void()> on_click;

protected:
    virtual void click(unsigned modifiers = 0) override;
    virtual void leave_event(Core::Event&) override;
    virtual void mousemove_event(GUI::MouseEvent&) override;
    virtual void paint_event(GUI::PaintEvent&) override;

private:
    KeyButton() = default;

    bool m_pressed { false };
};
