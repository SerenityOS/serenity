/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, networkException <networkexception@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGUI/BoxLayout.h>
#include <LibGUI/Button.h>
#include <LibGUI/ImageWidget.h>
#include <LibGUI/InputBox.h>
#include <LibGUI/Label.h>
#include <LibGUI/SpinBox.h>
#include <LibGUI/TextBox.h>

namespace GUI {

ErrorOr<NonnullRefPtr<InputBox>> InputBox::create(Window* parent_window, String text_value, StringView prompt, StringView title, InputType input_type, RefPtr<Gfx::Bitmap const> icon)
{
    VERIFY(input_type != InputType::Numeric);
    auto title_string = TRY(String::from_utf8(title));
    auto prompt_string = TRY(String::from_utf8(prompt));
    auto box = TRY(adopt_nonnull_ref_or_enomem(new (nothrow) InputBox(parent_window, text_value, title_string, prompt_string, input_type, move(icon))));
    TRY(box->build());
    return box;
}

ErrorOr<NonnullRefPtr<InputBox>> InputBox::create_numeric(Window* parent_window, int value, StringView title, StringView prompt, RefPtr<Gfx::Bitmap const> icon)
{
    auto title_string = TRY(String::from_utf8(title));
    auto prompt_string = TRY(String::from_utf8(prompt));
    auto box = TRY(adopt_nonnull_ref_or_enomem(new (nothrow) InputBox(parent_window, value, title_string, prompt_string, move(icon))));
    TRY(box->build());
    return box;
}

InputBox::InputBox(Window* parent_window, String text_value, String title, String prompt, InputType input_type, RefPtr<Gfx::Bitmap const> icon)
    : Dialog(parent_window)
    , m_text_value(move(text_value))
    , m_prompt(move(prompt))
    , m_input_type(input_type)
    , m_icon(move(icon))
{
    set_title(move(title).to_byte_string());
    set_resizable(false);
    set_auto_shrink(true);
}

InputBox::InputBox(Window* parent_window, int value, String title, String prompt, RefPtr<Gfx::Bitmap const> icon)
    : Dialog(parent_window)
    , m_numeric_value(value)
    , m_prompt(move(prompt))
    , m_input_type(InputType::Numeric)
    , m_icon(move(icon))
{
    set_title(move(title).to_byte_string());
    set_resizable(false);
    set_auto_shrink(true);
}

Dialog::ExecResult InputBox::show(Window* parent_window, String& text_value, StringView prompt, StringView title, InputType input_type, StringView placeholder, RefPtr<Gfx::Bitmap const> icon)
{
    return MUST(try_show(parent_window, text_value, prompt, title, input_type, placeholder, move(icon)));
}

ErrorOr<Dialog::ExecResult> InputBox::try_show(Window* parent_window, String& text_value, StringView prompt, StringView title, InputType input_type, StringView placeholder, RefPtr<Gfx::Bitmap const> icon)
{
    VERIFY(input_type != InputType::Numeric);
    auto box = TRY(InputBox::create(parent_window, text_value, prompt, title, input_type, move(icon)));
    if (parent_window)
        box->set_icon(parent_window->icon());
    box->set_placeholder(placeholder);
    auto result = box->exec();
    text_value = box->text_value();
    return result;
}

ErrorOr<Dialog::ExecResult> InputBox::show_numeric(Window* parent_window, int& value, int min, int max, StringView title, StringView prompt, RefPtr<Gfx::Bitmap const> icon)
{
    auto box = TRY(InputBox::create_numeric(parent_window, value, title, prompt, move(icon)));
    if (parent_window)
        box->set_icon(parent_window->icon());
    box->set_range(min, max);
    auto result = box->exec();
    value = box->numeric_value();
    return result;
}

void InputBox::set_placeholder(StringView view)
{
    m_text_editor->set_placeholder(view);
}

void InputBox::set_range(int min, int max)
{
    m_spinbox->set_range(min, max);
}

void InputBox::set_text_value(String value)
{
    if (m_text_value == value)
        return;
    m_text_value = move(value);
    m_text_editor->set_text(m_text_value);
}

void InputBox::set_numeric_value(int value)
{
    if (m_numeric_value == value)
        return;
    m_numeric_value = value;
    m_spinbox->set_value(value);
}

void InputBox::on_done(ExecResult result)
{
    if (result != ExecResult::OK)
        return;

    if (m_text_editor) {
        auto value = String::from_byte_string(m_text_editor->text());
        if (!value.is_error())
            m_text_value = value.release_value();
    } else if (m_spinbox)
        m_numeric_value = m_spinbox->value();

    if (m_input_type == InputType::NonemptyText)
        VERIFY(!m_text_value.is_empty());
}

ErrorOr<void> InputBox::build()
{
    auto main_widget = set_main_widget<Widget>();
    main_widget->set_layout<VerticalBoxLayout>(6, 6);
    main_widget->set_fill_with_background_color(true);

    if (!m_prompt.is_empty()) {
        auto& prompt_container = main_widget->add<Widget>();
        prompt_container.set_layout<HorizontalBoxLayout>(0, 8);
        if (m_icon) {
            auto& image_widget = prompt_container.add<ImageWidget>();
            image_widget.set_bitmap(m_icon);
        }
        m_prompt_label = prompt_container.add<Label>();
        m_prompt_label->set_autosize(true);
        m_prompt_label->set_text_wrapping(Gfx::TextWrapping::DontWrap);
        m_prompt_label->set_text(m_prompt);
    }

    switch (m_input_type) {
    case InputType::Text:
    case InputType::NonemptyText:
        m_text_editor = main_widget->add<TextBox>();
        break;
    case InputType::Password:
        m_text_editor = main_widget->add<PasswordBox>();
        break;
    case InputType::Numeric:
        m_spinbox = main_widget->add<SpinBox>();
        break;
    }

    auto& button_container = main_widget->add<Widget>();
    button_container.set_layout<HorizontalBoxLayout>(0, 6);
    button_container.add_spacer();

    m_ok_button = button_container.add<DialogButton>("OK"_string);
    m_ok_button->on_click = [this](auto) {
        if (m_spinbox)
            m_spinbox->set_value_from_current_text();
        done(ExecResult::OK);
    };
    m_ok_button->set_default(true);

    m_cancel_button = button_container.add<DialogButton>("Cancel"_string);
    m_cancel_button->on_click = [this](auto) { done(ExecResult::Cancel); };

    auto guarantee_width = [this, &button_container] {
        if (m_prompt.is_empty())
            return;
        auto width = button_container.calculated_min_size().value().width().as_int();
        auto constexpr golden_ratio = 1.618;
        button_container.set_min_width(width * golden_ratio);
    };
    guarantee_width();
    on_font_change = [guarantee_width] { guarantee_width(); };

    if (m_text_editor) {
        m_text_editor->set_text(m_text_value);

        if (m_input_type == InputType::NonemptyText) {
            m_text_editor->on_change = [this] {
                m_ok_button->set_enabled(!m_text_editor->text().is_empty());
            };
            m_text_editor->on_change();
        }
    }

    if (m_spinbox)
        m_spinbox->set_value(m_numeric_value);

    auto size = main_widget->effective_min_size();
    resize(TRY(size.width().shrink_value()), TRY(size.height().shrink_value()));

    return {};
}

}
