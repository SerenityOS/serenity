/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
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

#include <LibGUI/BoxLayout.h>
#include <LibGUI/Button.h>
#include <LibGUI/InputBox.h>
#include <LibGUI/Label.h>
#include <LibGUI/TextBox.h>
#include <LibGfx/Font.h>
#include <stdio.h>

namespace GUI {

InputBox::InputBox(Window* parent_window, const StringView& prompt, const StringView& title)
    : Dialog(parent_window)
    , m_prompt(prompt)
{
    set_title(title);
    build();
}

InputBox::~InputBox()
{
}

int InputBox::show(String& text_value, Window* parent_window, const StringView& prompt, const StringView& title)
{
    auto box = InputBox::construct(parent_window, prompt, title);
    box->set_resizable(false);
    if (parent_window)
        box->set_icon(parent_window->icon());
    auto result = box->exec();
    text_value = box->text_value();
    return result;
}

void InputBox::build()
{
    auto& widget = set_main_widget<Widget>();

    int text_width = widget.font().width(m_prompt);
    int title_width = widget.font().width(title()) + 24 /* icon, plus a little padding -- not perfect */;
    int max_width = AK::max(text_width, title_width);

    set_rect(x(), y(), max_width + 140, 62);

    widget.set_layout<VerticalBoxLayout>();
    widget.set_fill_with_background_color(true);

    widget.layout()->set_margins({ 6, 6, 6, 6 });
    widget.layout()->set_spacing(6);

    auto& label_editor_container = widget.add<Widget>();
    label_editor_container.set_size_policy(SizePolicy::Fill, SizePolicy::Fill);
    label_editor_container.set_layout<HorizontalBoxLayout>();

    auto& label = label_editor_container.add<Label>(m_prompt);
    label.set_size_policy(SizePolicy::Fixed, SizePolicy::Fixed);
    label.set_preferred_size(text_width, 16);

    m_text_editor = label_editor_container.add<TextBox>();
    m_text_editor->set_size_policy(SizePolicy::Fill, SizePolicy::Fixed);
    m_text_editor->set_preferred_size(0, 19);

    auto& button_container_outer = widget.add<Widget>();
    button_container_outer.set_size_policy(SizePolicy::Fill, SizePolicy::Fixed);
    button_container_outer.set_preferred_size(0, 20);
    button_container_outer.set_layout<VerticalBoxLayout>();

    auto& button_container_inner = button_container_outer.add<Widget>();
    button_container_inner.set_layout<HorizontalBoxLayout>();
    button_container_inner.layout()->set_spacing(6);
    button_container_inner.layout()->set_margins({ 4, 4, 0, 4 });
    button_container_inner.layout()->add_spacer();

    m_ok_button = button_container_inner.add<Button>();
    m_ok_button->set_size_policy(SizePolicy::Fill, SizePolicy::Fixed);
    m_ok_button->set_preferred_size(0, 20);
    m_ok_button->set_text("OK");
    m_ok_button->on_click = [this](auto) {
        dbgprintf("GUI::InputBox: OK button clicked\n");
        m_text_value = m_text_editor->text();
        done(ExecOK);
    };

    m_cancel_button = button_container_inner.add<Button>();
    m_cancel_button->set_size_policy(SizePolicy::Fill, SizePolicy::Fixed);
    m_cancel_button->set_preferred_size(0, 20);
    m_cancel_button->set_text("Cancel");
    m_cancel_button->on_click = [this](auto) {
        dbgprintf("GUI::InputBox: Cancel button clicked\n");
        done(ExecCancel);
    };

    m_text_editor->on_return_pressed = [this] {
        m_ok_button->click();
    };
    m_text_editor->on_escape_pressed = [this] {
        m_cancel_button->click();
    };
    m_text_editor->set_focus(true);
}

}
