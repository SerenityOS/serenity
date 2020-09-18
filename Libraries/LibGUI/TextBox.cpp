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

#include <LibGUI/TextBox.h>

namespace GUI {

TextBox::TextBox()
    : TextEditor(TextEditor::SingleLine)
{
}

TextBox::~TextBox()
{
}

void TextBox::keydown_event(GUI::KeyEvent& event)
{
    TextEditor::keydown_event(event);

    if (event.key() == Key_Up) {
        if (on_up_pressed)
            on_up_pressed();

        if (has_no_history() || !can_go_backwards_in_history())
            return;

        if (m_history_index >= static_cast<int>(m_history.size()))
            m_saved_input = text();

        m_history_index--;
        set_text(m_history[m_history_index]);
    } else if (event.key() == Key_Down) {
        if (on_down_pressed)
            on_down_pressed();

        if (has_no_history())
            return;

        if (can_go_forwards_in_history()) {
            m_history_index++;
            set_text(m_history[m_history_index]);
        } else if (m_history_index < static_cast<int>(m_history.size())) {
            m_history_index++;
            set_text(m_saved_input);
        }
    }
}

void TextBox::add_current_text_to_history()
{
    if (!m_history_enabled)
        return;

    auto input = text();
    if (m_history.is_empty() || m_history.last() != input)
        add_input_to_history(input);
    m_history_index = static_cast<int>(m_history.size());
    m_saved_input = {};
}

void TextBox::add_input_to_history(String input)
{
    m_history.append(move(input));
    m_history_index++;
}

}
