/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Jakob-Niklas See <git@nwex.de>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGUI/BoxLayout.h>
#include <LibGUI/Button.h>
#include <LibGUI/InputBox.h>
#include <LibGUI/Label.h>
#include <LibGUI/TextBox.h>
#include <LibGfx/Font.h>

namespace GUI {

InputBox::InputBox(Window* parent_window, String& text_value, StringView prompt, StringView title, StringView placeholder, InputType input_type)
    : Dialog(parent_window)
    , m_text_value(text_value)
    , m_prompt(prompt)
    , m_placeholder(placeholder)
{
    set_title(title);
    build(input_type);
}

InputBox::~InputBox()
{
}

int InputBox::show(Window* parent_window, String& text_value, StringView prompt, StringView title, StringView placeholder, InputType input_type)
{
    auto box = InputBox::construct(parent_window, text_value, prompt, title, placeholder, input_type);
    box->set_resizable(false);
    if (parent_window)
        box->set_icon(parent_window->icon());
    auto result = box->exec();
    text_value = box->text_value();
    return result;
}

void InputBox::build(InputType input_type)
{
    auto& widget = set_main_widget<Widget>();

    int text_width = widget.font().width(m_prompt);
    int title_width = widget.font().width(title()) + 24 /* icon, plus a little padding -- not perfect */;
    int max_width = max(text_width, title_width);

    set_rect(x(), y(), max_width + 140, 66);

    widget.set_layout<VerticalBoxLayout>();
    widget.set_fill_with_background_color(true);

    widget.layout()->set_margins(6);
    widget.layout()->set_spacing(6);

    auto& label_editor_container = widget.add<Widget>();
    label_editor_container.set_layout<HorizontalBoxLayout>();

    auto& label = label_editor_container.add<Label>(m_prompt);
    label.set_fixed_size(text_width, 16);

    switch (input_type) {
    case InputType::Text:
        m_text_editor = label_editor_container.add<TextBox>();
        break;
    case InputType::Password:
        m_text_editor = label_editor_container.add<PasswordBox>();
        break;
    }

    m_text_editor->set_text(m_text_value);

    if (!m_placeholder.is_null())
        m_text_editor->set_placeholder(m_placeholder);

    auto& button_container_outer = widget.add<Widget>();
    button_container_outer.set_fixed_height(22);
    button_container_outer.set_layout<VerticalBoxLayout>();

    auto& button_container_inner = button_container_outer.add<Widget>();
    button_container_inner.set_layout<HorizontalBoxLayout>();
    button_container_inner.layout()->set_spacing(6);
    button_container_inner.layout()->set_margins({ 4, 0, 4, 4 });
    button_container_inner.layout()->add_spacer();

    m_ok_button = button_container_inner.add<Button>();
    m_ok_button->set_text("OK");
    m_ok_button->on_click = [this](auto) {
        dbgln("GUI::InputBox: OK button clicked");
        m_text_value = m_text_editor->text();
        done(ExecOK);
    };

    m_cancel_button = button_container_inner.add<Button>();
    m_cancel_button->set_text("Cancel");
    m_cancel_button->on_click = [this](auto) {
        dbgln("GUI::InputBox: Cancel button clicked");
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
