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

namespace GUI {

ErrorOr<NonnullRefPtr<InputBox>> InputBox::create(Window* parent_window, String text_value, StringView prompt, StringView title, InputType input_type)
{
    auto box = TRY(adopt_nonnull_ref_or_enomem(new (nothrow) InputBox(parent_window, text_value, TRY(String::from_utf8(title)), TRY(String::from_utf8(prompt)), input_type)));
    TRY(box->build());
    return box;
}

InputBox::InputBox(Window* parent_window, String text_value, String title, String prompt, InputType input_type)
    : Dialog(parent_window)
    , m_text_value(move(text_value))
    , m_prompt(move(prompt))
    , m_input_type(input_type)
{
    set_title(move(title).to_deprecated_string());
    set_resizable(false);
    set_auto_shrink(true);
}

Dialog::ExecResult InputBox::show(Window* parent_window, String& text_value, StringView prompt, StringView title, InputType input_type, StringView placeholder)
{
    return MUST(try_show(parent_window, text_value, prompt, title, input_type, placeholder));
}

ErrorOr<Dialog::ExecResult> InputBox::try_show(Window* parent_window, String& text_value, StringView prompt, StringView title, InputType input_type, StringView placeholder)
{
    auto box = TRY(InputBox::create(parent_window, text_value, prompt, title, input_type));
    if (parent_window)
        box->set_icon(parent_window->icon());
    box->set_placeholder(placeholder);
    auto result = box->exec();
    text_value = box->text_value();
    return result;
}

void InputBox::set_placeholder(StringView view)
{
    m_text_editor->set_placeholder(view);
}

void InputBox::set_text_value(String value)
{
    if (m_text_value == value)
        return;
    m_text_value = move(value);
    m_text_editor->set_text(m_text_value);
}

void InputBox::on_done(ExecResult result)
{
    if (result != ExecResult::OK)
        return;

    if (auto value = String::from_deprecated_string(m_text_editor->text()); !value.is_error())
        m_text_value = value.release_value();

    switch (m_input_type) {
    case InputType::Text:
    case InputType::Password:
        break;

    case InputType::NonemptyText:
        VERIFY(!m_text_value.is_empty());
        break;
    }
}

ErrorOr<void> InputBox::build()
{
    auto main_widget = TRY(set_main_widget<Widget>());
    TRY(main_widget->try_set_layout<VerticalBoxLayout>(4, 6));
    main_widget->set_fill_with_background_color(true);

    auto input_container = TRY(main_widget->try_add<Widget>());
    input_container->set_layout<HorizontalBoxLayout>();

    m_prompt_label = TRY(input_container->try_add<Label>());
    m_prompt_label->set_autosize(true);
    m_prompt_label->set_text(move(m_prompt).to_deprecated_string());

    TRY(input_container->add_spacer());

    switch (m_input_type) {
    case InputType::Text:
    case InputType::NonemptyText:
        m_text_editor = TRY(input_container->try_add<TextBox>());
        break;
    case InputType::Password:
        m_text_editor = TRY(input_container->try_add<PasswordBox>());
        break;
    }

    auto button_container = TRY(main_widget->try_add<Widget>());
    TRY(button_container->try_set_layout<HorizontalBoxLayout>(0, 6));
    TRY(button_container->add_spacer());

    m_ok_button = TRY(button_container->try_add<DialogButton>("OK"_short_string));
    m_ok_button->on_click = [this](auto) { done(ExecResult::OK); };
    m_ok_button->set_default(true);

    m_cancel_button = TRY(button_container->try_add<DialogButton>("Cancel"_short_string));
    m_cancel_button->on_click = [this](auto) { done(ExecResult::Cancel); };

    auto resize_editor = [this, button_container] {
        auto width = button_container->effective_min_size().width().as_int();
        m_text_editor->set_min_width(width);
    };
    resize_editor();
    on_font_change = [resize_editor] { resize_editor(); };

    m_text_editor->set_text(m_text_value);

    if (m_input_type == InputType::NonemptyText) {
        m_text_editor->on_change = [this] {
            m_ok_button->set_enabled(!m_text_editor->text().is_empty());
        };
        m_text_editor->on_change();
    }

    auto size = main_widget->effective_min_size();
    resize(TRY(size.width().shrink_value()), TRY(size.height().shrink_value()));

    return {};
}

}
