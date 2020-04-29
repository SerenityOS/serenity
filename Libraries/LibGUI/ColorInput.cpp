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

#include <LibCore/Timer.h>
#include <LibGUI/ColorPicker.h>
#include <LibGUI/Painter.h>
#include <LibGUI/ColorInput.h>
#include <LibGfx/Palette.h>

namespace GUI {

ColorInput::ColorInput()
    : TextEditor(TextEditor::SingleLine)
{
    set_readonly(true);
}

ColorInput::~ColorInput()
{
}

void ColorInput::set_color(Color color)
{
    m_color = color;
    set_text(color.to_string());

    update();

    if (on_change)
        on_change();
};

void ColorInput::mousedown_event(MouseEvent& event)
{
    if (event.button() == MouseButton::Left) {
        if (is_enabled()) {
            m_being_pressed = true;
            update();
        }
    }

    Widget::mousedown_event(event);
}

void ColorInput::mouseup_event(MouseEvent& event)
{
    if (event.button() == MouseButton::Left) {
        if (is_enabled()) {
            bool was_being_pressed = m_being_pressed;
            m_being_pressed = false;
            update();
            if (was_being_pressed)
                click();
        }
    }
    Widget::mouseup_event(event);
}

void ColorInput::enter_event(Core::Event&)
{
    ASSERT(window());
    window()->set_override_cursor(StandardCursor::Arrow);
}

void ColorInput::paint_event(PaintEvent& event)
{
    // Set cursor color to base color and stop timer. FIXME: Find better way to hide cursor.
    auto pal = palette();
    pal.set_color(ColorRole::TextCursor, palette().base());
    set_palette(pal);
    stop_timer();

    TextEditor::paint_event(event);

    auto color_box_padding = 3;
    auto color_box_size = event.rect().height() - color_box_padding - color_box_padding;

    Painter painter(*this);
    painter.fill_rect({ event.rect().width() - color_box_size - color_box_padding , color_box_padding, color_box_size, color_box_size}, m_color);
}

void ColorInput::click()
{
    if (!is_enabled())
        return;

    auto dialog = GUI::ColorPicker::construct(m_color, window(), m_color_picker_title);
    if (dialog->exec() == GUI::Dialog::ExecOK) {
        auto tmp = dialog->color();
        set_color(tmp);
    }
}

}
