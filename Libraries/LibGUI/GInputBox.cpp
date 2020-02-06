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

#include <LibGUI/GBoxLayout.h>
#include <LibGUI/GButton.h>
#include <LibGUI/GInputBox.h>
#include <LibGUI/GLabel.h>
#include <LibGUI/GTextEditor.h>
#include <stdio.h>

namespace GUI {

InputBox::InputBox(const StringView& prompt, const StringView& title, Core::Object* parent)
    : Dialog(parent)
    , m_prompt(prompt)
{
    set_title(title);
    build();
}

InputBox::~InputBox()
{
}

void InputBox::build()
{
    auto widget = Widget::construct();
    set_main_widget(widget);

    int text_width = widget->font().width(m_prompt);
    int title_width = widget->font().width(title()) + 24 /* icon, plus a little padding -- not perfect */;
    int max_width = AK::max(text_width, title_width);

    set_rect(x(), y(), max_width + 80, 80);

    widget->set_layout(make<VerticalBoxLayout>());
    widget->set_fill_with_background_color(true);

    widget->layout()->set_margins({ 8, 8, 8, 8 });
    widget->layout()->set_spacing(8);

    auto label = Label::construct(m_prompt, widget);
    label->set_size_policy(SizePolicy::Fixed, SizePolicy::Fixed);
    label->set_preferred_size(text_width, 16);

    m_text_editor = TextEditor::construct(TextEditor::SingleLine, widget);
    m_text_editor->set_size_policy(SizePolicy::Fill, SizePolicy::Fixed);
    m_text_editor->set_preferred_size(0, 19);

    auto button_container_outer = Widget::construct(widget.ptr());
    button_container_outer->set_size_policy(SizePolicy::Fill, SizePolicy::Fixed);
    button_container_outer->set_preferred_size(0, 20);
    button_container_outer->set_layout(make<VerticalBoxLayout>());

    auto button_container_inner = Widget::construct(button_container_outer.ptr());
    button_container_inner->set_layout(make<HorizontalBoxLayout>());
    button_container_inner->layout()->set_spacing(8);

    m_cancel_button = Button::construct(button_container_inner);
    m_cancel_button->set_size_policy(SizePolicy::Fill, SizePolicy::Fixed);
    m_cancel_button->set_preferred_size(0, 20);
    m_cancel_button->set_text("Cancel");
    m_cancel_button->on_click = [this](auto&) {
        dbgprintf("GInputBox: Cancel button clicked\n");
        done(ExecCancel);
    };

    m_ok_button = Button::construct(button_container_inner);
    m_ok_button->set_size_policy(SizePolicy::Fill, SizePolicy::Fixed);
    m_ok_button->set_preferred_size(0, 20);
    m_ok_button->set_text("OK");
    m_ok_button->on_click = [this](auto&) {
        dbgprintf("GInputBox: OK button clicked\n");
        m_text_value = m_text_editor->text();
        done(ExecOK);
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
