/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, networkException <networkexception@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGUI/BoxLayout.h>
#include <LibGUI/Button.h>
#include <LibGUI/InputBox.h>
#include <LibGUI/Label.h>
#include <LibGUI/TextBox.h>
#include <LibGfx/Font/Font.h>

namespace GUI {

InputBox::InputBox(Window* parent_window, DeprecatedString text_value, StringView prompt, StringView title, InputType input_type, StringView placeholder)
    : Dialog(parent_window)
    , m_text_value(move(text_value))
    , m_prompt(prompt)
    , m_input_type(input_type)
    , m_placeholder(placeholder)
{
    set_title(title);
    build();
}

Dialog::ExecResult InputBox::show(Window* parent_window, DeprecatedString& text_value, StringView prompt, StringView title, InputType input_type, StringView placeholder)
{
    auto box = InputBox::construct(parent_window, text_value, prompt, title, input_type, placeholder);
    box->set_resizable(false);
    if (parent_window)
        box->set_icon(parent_window->icon());
    auto result = box->exec();
    text_value = box->text_value();
    return result;
}

void InputBox::set_text_value(DeprecatedString text_value)
{
    m_text_editor->set_text(move(text_value));
}

void InputBox::on_done(ExecResult result)
{
    if (result == ExecResult::OK) {
        m_text_value = m_text_editor->text();

        switch (m_input_type) {
        case InputType::Text:
        case InputType::Password:
            break;

        case InputType::NonemptyText:
            VERIFY(!m_text_value.is_empty());
            break;
        }
    }
}

void InputBox::build()
{
    auto widget = set_main_widget<Widget>().release_value_but_fixme_should_propagate_errors();

    int text_width = widget->font().width(m_prompt);
    int title_width = widget->font().width(title()) + 24 /* icon, plus a little padding -- not perfect */;
    int max_width = max(text_width, title_width);

    widget->set_layout<VerticalBoxLayout>(6, 6);
    widget->set_fill_with_background_color(true);
    widget->set_preferred_height(SpecialDimension::Fit);

    auto& label_editor_container = widget->add<Widget>();
    label_editor_container.set_layout<HorizontalBoxLayout>();
    label_editor_container.set_preferred_height(SpecialDimension::Fit);

    auto& label = label_editor_container.add<Label>(m_prompt);
    label.set_preferred_width(text_width);

    switch (m_input_type) {
    case InputType::Text:
    case InputType::NonemptyText:
        m_text_editor = label_editor_container.add<TextBox>();
        break;
    case InputType::Password:
        m_text_editor = label_editor_container.add<PasswordBox>();
        break;
    }

    m_text_editor->set_text(m_text_value);

    if (!m_placeholder.is_null())
        m_text_editor->set_placeholder(m_placeholder);

    auto& button_container_outer = widget->add<Widget>();
    button_container_outer.set_preferred_height(SpecialDimension::Fit);
    button_container_outer.set_layout<VerticalBoxLayout>();

    auto& button_container_inner = button_container_outer.add<Widget>();
    button_container_inner.set_layout<HorizontalBoxLayout>(GUI::Margins {}, 6);
    button_container_inner.set_preferred_height(SpecialDimension::Fit);
    button_container_inner.add_spacer().release_value_but_fixme_should_propagate_errors();

    m_ok_button = button_container_inner.add<DialogButton>();
    m_ok_button->set_text("OK"_short_string);
    m_ok_button->on_click = [this](auto) {
        dbgln("GUI::InputBox: OK button clicked");
        done(ExecResult::OK);
    };
    m_ok_button->set_default(true);

    m_cancel_button = button_container_inner.add<DialogButton>();
    m_cancel_button->set_text("Cancel"_short_string);
    m_cancel_button->on_click = [this](auto) {
        dbgln("GUI::InputBox: Cancel button clicked");
        done(ExecResult::Cancel);
    };

    m_text_editor->on_escape_pressed = [this] {
        m_cancel_button->click();
    };
    m_text_editor->set_focus(true);

    if (m_input_type == InputType::NonemptyText) {
        m_text_editor->on_change = [this] {
            m_ok_button->set_enabled(!m_text_editor->text().is_empty());
        };
        m_text_editor->on_change();
    }

    set_rect(x(), y(), max_width + 140, widget->effective_preferred_size().height().as_int());
}

}
