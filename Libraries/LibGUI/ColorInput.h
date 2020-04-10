/*
 * Copyright (c) 2020, Hüseyin Aslıtürk <asliturk@hotmail.com>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#pragma once

#include <AK/Function.h>
#include <LibGUI/TextEditor.h>

namespace GUI {

class ColorInput : public TextEditor {
    C_OBJECT(ColorInput)

public:
    ColorInput();
    virtual ~ColorInput() override;

    void set_color(Color color);
    Color color() { return m_color; }

    void set_color_picker_title(String title) { m_color_picker_title = title; }
    String color_picker_title() { return m_color_picker_title; }

    Function<void()> on_change;

protected:
    virtual void mousedown_event(MouseEvent&) override;
    virtual void mouseup_event(MouseEvent&) override;
    virtual void enter_event(Core::Event&) override;
    virtual void paint_event(PaintEvent&) override;

private:
    virtual void click();

    Color m_color;
    String m_color_picker_title { "Select Color" };

    int m_auto_repeat_interval { 0 };
    bool m_being_pressed { false };
    RefPtr<Core::Timer> m_auto_repeat_timer;
};

}
