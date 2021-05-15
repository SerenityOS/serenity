/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/Dialog.h>

namespace GUI {

class MessageBox : public Dialog {
    C_OBJECT(MessageBox)
public:
    enum class Type {
        None,
        Information,
        Warning,
        Error,
        Question
    };

    enum class InputType {
        OK,
        OKCancel,
        YesNo,
        YesNoCancel,
    };

    virtual ~MessageBox() override;

    static int show(Window* parent_window, const StringView& text, const StringView& title, Type type = Type::None, InputType input_type = InputType::OK);
    static int show_error(Window* parent_window, const StringView& text);

private:
    explicit MessageBox(Window* parent_window, const StringView& text, const StringView& title, Type type = Type::None, InputType input_type = InputType::OK);

    bool should_include_ok_button() const;
    bool should_include_cancel_button() const;
    bool should_include_yes_button() const;
    bool should_include_no_button() const;
    void build();
    RefPtr<Gfx::Bitmap> icon() const;

    String m_text;
    Type m_type { Type::None };
    InputType m_input_type { InputType::OK };
};

}
