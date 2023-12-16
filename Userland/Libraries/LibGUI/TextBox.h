/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/Action.h>
#include <LibGUI/TextEditor.h>

namespace GUI {

class TextBox : public TextEditor {
    C_OBJECT(TextBox)
public:
    virtual ~TextBox() override = default;

    Function<void()> on_up_pressed;
    Function<void()> on_down_pressed;

    void set_history_enabled(bool enabled) { m_history_enabled = enabled; }
    void add_current_text_to_history();

protected:
    TextBox();

private:
    virtual void keydown_event(GUI::KeyEvent&) override;

    bool has_no_history() const { return !m_history_enabled || m_history.is_empty(); }
    bool can_go_backwards_in_history() const { return m_history_index > 0; }
    bool can_go_forwards_in_history() const { return m_history_index < static_cast<int>(m_history.size()) - 1; }
    void add_input_to_history(ByteString);

    bool m_history_enabled { false };
    Vector<ByteString> m_history;
    int m_history_index { -1 };
    ByteString m_saved_input;
};

class PasswordBox : public TextBox {
    C_OBJECT(PasswordBox)
public:
    bool is_showing_reveal_button() const { return m_show_reveal_button; }
    void set_show_reveal_button(bool show)
    {
        m_show_reveal_button = show;
        update();
    }

private:
    PasswordBox();

    virtual void paint_event(PaintEvent&) override;
    virtual void mousedown_event(GUI::MouseEvent&) override;

    Gfx::IntRect reveal_password_button_rect() const;

    bool m_show_reveal_button { false };
};

}
