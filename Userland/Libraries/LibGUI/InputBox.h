/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/Dialog.h>

namespace GUI {

class InputBox : public Dialog {
    C_OBJECT(InputBox)
public:
    virtual ~InputBox() override;

    static int show(Window* parent_window, String& text_value, const StringView& prompt, const StringView& title);

private:
    explicit InputBox(Window* parent_window, String& text_value, const StringView& prompt, const StringView& title);

    String text_value() const { return m_text_value; }

    void build();
    String m_text_value;
    String m_prompt;

    RefPtr<Button> m_ok_button;
    RefPtr<Button> m_cancel_button;
    RefPtr<TextEditor> m_text_editor;
};

}
