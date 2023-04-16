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
    Password
};

class InputBox : public Dialog {
    C_OBJECT_ABSTRACT(InputBox)
public:
    virtual ~InputBox() override = default;

    static ExecResult show(Window* parent_window, String& text_value, StringView prompt, StringView title, InputType input_type = InputType::Text, StringView placeholder = {});
    static ErrorOr<ExecResult> try_show(Window* parent_window, String& text_value, StringView prompt, StringView title, InputType input_type = InputType::Text, StringView placeholder = {});
    static ErrorOr<NonnullRefPtr<InputBox>> create(Window* parent_window, String text_value, StringView prompt, StringView title, InputType input_type);

    String const& text_value() const { return m_text_value; }
    void set_text_value(String);

    void set_placeholder(StringView);

private:
    InputBox(Window* parent_window, String text_value, String title, String prompt, InputType input_type);

    virtual void on_done(ExecResult) override;
    ErrorOr<void> build();

    String m_text_value;
    String m_prompt;
    InputType m_input_type;

    RefPtr<Button> m_ok_button;
    RefPtr<Button> m_cancel_button;
    RefPtr<TextEditor> m_text_editor;
    RefPtr<Label> m_prompt_label;
};

}
