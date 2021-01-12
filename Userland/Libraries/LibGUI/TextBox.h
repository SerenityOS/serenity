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

#pragma once

#include <LibGUI/TextEditor.h>

namespace GUI {

class TextBox : public TextEditor {
    C_OBJECT(TextBox)
public:
    TextBox();
    virtual ~TextBox() override;

    Function<void()> on_up_pressed;
    Function<void()> on_down_pressed;

    void set_history_enabled(bool enabled) { m_history_enabled = enabled; }
    void add_current_text_to_history();

private:
    virtual void keydown_event(GUI::KeyEvent&) override;

    bool has_no_history() const { return !m_history_enabled || m_history.is_empty(); }
    bool can_go_backwards_in_history() const { return m_history_index > 0; }
    bool can_go_forwards_in_history() const { return m_history_index < static_cast<int>(m_history.size()) - 1; }
    void add_input_to_history(String);

    bool m_history_enabled { false };
    Vector<String> m_history;
    int m_history_index { -1 };
    String m_saved_input;
};

}
