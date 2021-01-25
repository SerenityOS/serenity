/*
 * Copyright (c) 2020, the SerenityOS developers.
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

#include <LibGUI/Event.h>
#include <LibGUI/TextEditor.h>
#include <LibGUI/VimEditingEngine.h>

namespace GUI {

CursorWidth VimEditingEngine::cursor_width() const
{
    return m_vim_mode == VimMode::Insert ? CursorWidth::NARROW : CursorWidth::WIDE;
}

bool VimEditingEngine::on_key(const KeyEvent& event)
{
    if (EditingEngine::on_key(event))
        return true;

    switch (m_vim_mode) {
    case (VimMode::Insert):
        return on_key_in_insert_mode(event);
    case (VimMode::Visual):
        return on_key_in_visual_mode(event);
    case (VimMode::Normal):
        return on_key_in_normal_mode(event);
    default:
        ASSERT_NOT_REACHED();
    }

    return false;
}

bool VimEditingEngine::on_key_in_insert_mode(const KeyEvent& event)
{
    if (event.key() == KeyCode::Key_Escape || (event.ctrl() && event.key() == KeyCode::Key_LeftBracket) || (event.ctrl() && event.key() == KeyCode::Key_C)) {
        switch_to_normal_mode();
        return true;
    }
    return false;
}

bool VimEditingEngine::on_key_in_normal_mode(const KeyEvent& event)
{
    if (m_previous_key == KeyCode::Key_D) {
        if (event.key() == KeyCode::Key_D) {
            yank(Line);
            delete_line();
        }
        m_previous_key = {};
    } else if (m_previous_key == KeyCode::Key_G) {
        if (event.key() == KeyCode::Key_G) {
            move_to_first_line();
        } else if (event.key() == KeyCode::Key_E) {
            move_to_end_of_previous_word();
        }
        m_previous_key = {};
    } else if (m_previous_key == KeyCode::Key_Y) {
        if (event.key() == KeyCode::Key_Y) {
            yank(Line);
        }
        m_previous_key = {};
    } else if (m_previous_key == KeyCode::Key_C) {
        if (event.key() == KeyCode::Key_C) {
            // Needed because the code to replace the deleted line is called after delete_line() so
            // what was the second last line before the delete, is now the last line.
            bool was_second_last_line = m_editor->cursor().line() == m_editor->line_count() - 2;
            yank(Line);
            delete_line();
            if (was_second_last_line || (m_editor->cursor().line() != 0 && m_editor->cursor().line() != m_editor->line_count() - 1)) {
                move_one_up(event);
                move_to_line_end(event);
                m_editor->add_code_point(0x0A);
            } else if (m_editor->cursor().line() == 0) {
                move_to_line_beginning(event);
                m_editor->add_code_point(0x0A);
                move_one_up(event);
            } else if (m_editor->cursor().line() == m_editor->line_count() - 1) {
                m_editor->add_code_point(0x0A);
            }
            switch_to_insert_mode();
        }
        m_previous_key = {};
    } else {
        // Handle first any key codes that are to be applied regardless of modifiers.
        switch (event.key()) {
        case (KeyCode::Key_Dollar):
            move_to_line_end(event);
            break;
        case (KeyCode::Key_Escape):
            if (m_editor->on_escape_pressed)
                m_editor->on_escape_pressed();
            break;
        default:
            break;
        }

        // SHIFT is pressed.
        if (event.shift() && !event.ctrl() && !event.alt()) {
            switch (event.key()) {
            case (KeyCode::Key_A):
                move_to_line_end(event);
                switch_to_insert_mode();
                break;
            case (KeyCode::Key_G):
                move_to_last_line();
                break;
            case (KeyCode::Key_I):
                move_to_line_beginning(event);
                switch_to_insert_mode();
                break;
            case (KeyCode::Key_O):
                move_to_line_beginning(event);
                m_editor->add_code_point(0x0A);
                move_one_up(event);
                switch_to_insert_mode();
                break;
            default:
                break;
            }
        }

        // CTRL is pressed.
        if (event.ctrl() && !event.shift() && !event.alt()) {
            switch (event.key()) {
            case (KeyCode::Key_D):
                move_half_page_down(event);
                break;
            case (KeyCode::Key_R):
                m_editor->redo();
                break;
            case (KeyCode::Key_U):
                move_half_page_up(event);
                break;
            default:
                break;
            }
        }

        // FIXME: H and L movement keys will move to the previous or next line when reaching the beginning or end
        //  of the line and pressed again.

        // No modifier is pressed.
        if (!event.ctrl() && !event.shift() && !event.alt()) {
            switch (event.key()) {
            case (KeyCode::Key_A):
                move_one_right(event);
                switch_to_insert_mode();
                break;
            case (KeyCode::Key_B):
                move_to_beginning_of_previous_word();
                break;
            case (KeyCode::Key_C):
                m_previous_key = event.key();
                break;
            case (KeyCode::Key_Backspace):
            case (KeyCode::Key_H):
            case (KeyCode::Key_Left):
                move_one_left(event);
                break;
            case (KeyCode::Key_D):
                m_previous_key = event.key();
                break;
            case (KeyCode::Key_E):
                move_to_end_of_next_word();
                break;
            case (KeyCode::Key_G):
                m_previous_key = event.key();
                break;
            case (KeyCode::Key_Down):
            case (KeyCode::Key_J):
                move_one_down(event);
                break;
            case (KeyCode::Key_I):
                switch_to_insert_mode();
                break;
            case (KeyCode::Key_K):
            case (KeyCode::Key_Up):
                move_one_up(event);
                break;
            case (KeyCode::Key_L):
            case (KeyCode::Key_Right):
                move_one_right(event);
                break;
            case (KeyCode::Key_O):
                move_to_line_end(event);
                m_editor->add_code_point(0x0A);
                switch_to_insert_mode();
                break;
            case (KeyCode::Key_U):
                m_editor->undo();
                break;
            case (KeyCode::Key_W):
                move_to_beginning_of_next_word();
                break;
            case (KeyCode::Key_X):
                yank({ m_editor->cursor(), { m_editor->cursor().line(), m_editor->cursor().column() + 1 } });
                delete_char();
                break;
            case (KeyCode::Key_0):
                move_to_line_beginning(event);
                break;
            case (KeyCode::Key_V):
                switch_to_visual_mode();
                break;
            case (KeyCode::Key_Y):
                m_previous_key = event.key();
                break;
            case (KeyCode::Key_P):
                put(event);
                break;
            default:
                break;
            }
        }
    }
    return true;
}

bool VimEditingEngine::on_key_in_visual_mode(const KeyEvent& event)
{
    if (m_previous_key == KeyCode::Key_G) {
        if (event.key() == KeyCode::Key_G) {
            move_to_first_line();
            update_selection_on_cursor_move();
        } else if (event.key() == KeyCode::Key_E) {
            move_to_end_of_previous_word();
            update_selection_on_cursor_move();
        }
        m_previous_key = {};
    } else {
        // Handle first any key codes that are to be applied regardless of modifiers.
        switch (event.key()) {
        case (KeyCode::Key_Dollar):
            move_to_line_end(event);
            update_selection_on_cursor_move();
            break;
        case (KeyCode::Key_Escape):
            switch_to_normal_mode();
            if (m_editor->on_escape_pressed)
                m_editor->on_escape_pressed();
            break;
        default:
            break;
        }

        // SHIFT is pressed.
        if (event.shift() && !event.ctrl() && !event.alt()) {
            switch (event.key()) {
            case (KeyCode::Key_A):
                move_to_line_end(event);
                switch_to_insert_mode();
                break;
            case (KeyCode::Key_G):
                move_to_last_line();
                break;
            case (KeyCode::Key_I):
                move_to_line_beginning(event);
                switch_to_insert_mode();
                break;
            default:
                break;
            }
        }

        // CTRL is pressed.
        if (event.ctrl() && !event.shift() && !event.alt()) {
            switch (event.key()) {
            case (KeyCode::Key_D):
                move_half_page_down(event);
                update_selection_on_cursor_move();
                break;
            case (KeyCode::Key_U):
                move_half_page_up(event);
                update_selection_on_cursor_move();
                break;
            default:
                break;
            }
        }

        // No modifier is pressed.
        if (!event.ctrl() && !event.shift() && !event.alt()) {
            switch (event.key()) {
            case (KeyCode::Key_B):
                move_to_beginning_of_previous_word();
                update_selection_on_cursor_move();
                break;
            case (KeyCode::Key_Backspace):
            case (KeyCode::Key_H):
            case (KeyCode::Key_Left):
                move_one_left(event);
                update_selection_on_cursor_move();
                break;
            case (KeyCode::Key_D):
                yank(Selection);
                m_editor->do_delete();
                switch_to_normal_mode();
                break;
            case (KeyCode::Key_E):
                move_to_end_of_next_word();
                update_selection_on_cursor_move();
                break;
            case (KeyCode::Key_G):
                m_previous_key = event.key();
                break;
            case (KeyCode::Key_Down):
            case (KeyCode::Key_J):
                move_one_down(event);
                update_selection_on_cursor_move();
                break;
            case (KeyCode::Key_K):
            case (KeyCode::Key_Up):
                move_one_up(event);
                update_selection_on_cursor_move();
                break;
            case (KeyCode::Key_L):
            case (KeyCode::Key_Right):
                move_one_right(event);
                update_selection_on_cursor_move();
                break;
            case (KeyCode::Key_U):
                // FIXME: Set selection to uppercase.
                break;
            case (KeyCode::Key_W):
                move_to_beginning_of_next_word();
                update_selection_on_cursor_move();
                break;
            case (KeyCode::Key_X):
                yank(Selection);
                m_editor->do_delete();
                switch_to_normal_mode();
                break;
            case (KeyCode::Key_0):
                move_to_line_beginning(event);
                update_selection_on_cursor_move();
                break;
            case (KeyCode::Key_V):
                switch_to_normal_mode();
                break;
            case (KeyCode::Key_C):
                yank(Selection);
                m_editor->do_delete();
                switch_to_insert_mode();
                break;
            case (KeyCode::Key_Y):
                yank(Selection);
                switch_to_normal_mode();
                break;
            default:
                break;
            }
        }
    }
    return true;
}

void VimEditingEngine::switch_to_normal_mode()
{
    m_vim_mode = VimMode::Normal;
    m_editor->reset_cursor_blink();
    m_previous_key = {};
    clear_visual_mode_data();
};

void VimEditingEngine::switch_to_insert_mode()
{
    m_vim_mode = VimMode::Insert;
    m_editor->reset_cursor_blink();
    m_previous_key = {};
    clear_visual_mode_data();
};

void VimEditingEngine::switch_to_visual_mode()
{
    m_vim_mode = VimMode::Visual;
    m_editor->reset_cursor_blink();
    m_previous_key = {};
    m_selection_start_position = m_editor->cursor();
    m_editor->selection()->set(m_editor->cursor(), { m_editor->cursor().line(), m_editor->cursor().column() + 1 });
    m_editor->did_update_selection();
}

void VimEditingEngine::update_selection_on_cursor_move()
{
    auto cursor = m_editor->cursor();
    auto start = m_selection_start_position < cursor ? m_selection_start_position : cursor;
    auto end = m_selection_start_position < cursor ? cursor : m_selection_start_position;
    end.set_column(end.column() + 1);
    m_editor->selection()->set(start, end);
    m_editor->did_update_selection();
}

void VimEditingEngine::clear_visual_mode_data()
{
    if (m_editor->has_selection()) {
        m_editor->selection()->clear();
        m_editor->did_update_selection();
    }
    m_selection_start_position = {};
}

void VimEditingEngine::move_half_page_up(const KeyEvent& event)
{
    move_up(event, 0.5);
};

void VimEditingEngine::move_half_page_down(const KeyEvent& event)
{
    move_down(event, 0.5);
};

void VimEditingEngine::yank(YankType type)
{
    m_yank_type = type;
    if (type == YankType::Line) {
        m_yank_buffer = m_editor->current_line().to_utf8();
    } else {
        m_yank_buffer = m_editor->selected_text();
    }

    // When putting this, auto indentation (if enabled) will indent as far as
    // is necessary, then any whitespace captured before the yanked text will be placed
    // after the indentation, doubling the indentation.
    if (m_editor->is_automatic_indentation_enabled())
        m_yank_buffer = m_yank_buffer.trim_whitespace(TrimMode::Left);
}

void VimEditingEngine::yank(TextRange range)
{
    m_yank_type = YankType::Selection;
    m_yank_buffer = m_editor->document().text_in_range(range);
}

void VimEditingEngine::put(const GUI::KeyEvent& event)
{
    if (m_yank_type == YankType::Line) {
        move_to_line_end(event);
        StringBuilder sb = StringBuilder(m_yank_buffer.length() + 1);
        sb.append_code_point(0x0A);
        sb.append(m_yank_buffer);
        m_editor->insert_at_cursor_or_replace_selection(sb.to_string());
        m_editor->set_cursor({ m_editor->cursor().line(), m_editor->current_line().first_non_whitespace_column() });
    } else {
        // FIXME: If attempting to put on the last column a line,
        // the buffer will bne placed on the next line due to the move_one_left/right behaviour.
        move_one_right(event);
        m_editor->insert_at_cursor_or_replace_selection(m_yank_buffer);
        move_one_left(event);
    }
}

}
