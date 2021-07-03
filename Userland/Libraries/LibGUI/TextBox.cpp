/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGUI/TextBox.h>

REGISTER_WIDGET(GUI, TextBox)

namespace GUI {

TextBox::TextBox()
    : TextEditor(TextEditor::SingleLine)
{
    set_min_width(32);
    set_fixed_height(22);
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
