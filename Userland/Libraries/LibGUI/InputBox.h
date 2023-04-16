/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, networkException <networkexception@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/Dialog.h>

namespace GUI {

enum class InputType {
    Text,
    NonemptyText,
    Password,
    Numeric
};

class InputBox : public Dialog {
    C_OBJECT_ABSTRACT(InputBox)
public:
    virtual ~InputBox() override = default;

    static ExecResult show(Window* parent_window, String& text_value, StringView prompt, StringView title, InputType input_type = InputType::Text, StringView placeholder = {}, RefPtr<Gfx::Bitmap const> icon = nullptr);
    static ErrorOr<ExecResult> try_show(Window* parent_window, String& text_value, StringView prompt, StringView title, InputType input_type = InputType::Text, StringView placeholder = {}, RefPtr<Gfx::Bitmap const> icon = nullptr);
    static ErrorOr<NonnullRefPtr<InputBox>> create(Window* parent_window, String text_value, StringView prompt, StringView title, InputType input_type, RefPtr<Gfx::Bitmap const> icon = nullptr);

    static ErrorOr<ExecResult> show_numeric(Window* parent_window, int& value, int min, int max, StringView title, StringView prompt = {}, RefPtr<Gfx::Bitmap const> icon = nullptr);
    static ErrorOr<NonnullRefPtr<InputBox>> create_numeric(Window* parent_window, int value, StringView title, StringView prompt = {}, RefPtr<Gfx::Bitmap const> icon = nullptr);

    String const& text_value() const { return m_text_value; }
    void set_text_value(String);

    int numeric_value() const { return m_numeric_value; }
    void set_numeric_value(int);

    void set_placeholder(StringView);
    void set_range(int min, int max);

private:
    InputBox(Window* parent_window, String text_value, String title, String prompt, InputType input_type, RefPtr<Gfx::Bitmap const> icon);
    InputBox(Window* parent_window, int value, String title, String prompt, RefPtr<Gfx::Bitmap const> icon);

    virtual void on_done(ExecResult) override;
    ErrorOr<void> build();

    int m_numeric_value { 0 };
    String m_text_value;
    String m_prompt;
    InputType m_input_type;

    RefPtr<Button> m_ok_button;
    RefPtr<Button> m_cancel_button;
    RefPtr<TextEditor> m_text_editor;
    RefPtr<SpinBox> m_spinbox;
    RefPtr<Label> m_prompt_label;
    RefPtr<Widget> m_label_container;
    RefPtr<Gfx::Bitmap const> m_icon;
};

}
