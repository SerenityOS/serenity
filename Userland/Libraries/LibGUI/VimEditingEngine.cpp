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
            delete_line();
        }
        m_previous_key = {};
    } else if (m_previous_key == KeyCode::Key_G) {
        if (event.key() == KeyCode::Key_G) {
            move_to_first_line();
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

        // No modifier is pressed.
        if (!event.ctrl() && !event.shift() && !event.alt()) {
            switch (event.key()) {
            case (KeyCode::Key_A):
                move_one_right(event);
                switch_to_insert_mode();
                break;
            case (KeyCode::Key_B):
                move_to_previous_span(event); // FIXME: This probably isn't 100% correct.
                break;
            case (KeyCode::Key_Backspace):
            case (KeyCode::Key_H):
            case (KeyCode::Key_Left):
                move_one_left(event);
                break;
            case (KeyCode::Key_D):
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
                move_to_next_span(event); // FIXME: This probably isn't 100% correct.
                break;
            case (KeyCode::Key_X):
                delete_char();
                break;
            case (KeyCode::Key_0):
                move_to_line_beginning(event);
                break;
            case (KeyCode::Key_V):
                switch_to_visual_mode();
                break;
            case (KeyCode::Key_C):
                m_editor->do_delete();
                switch_to_insert_mode();
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
                move_to_previous_span(event); // FIXME: This probably isn't 100% correct.
                update_selection_on_cursor_move();
                break;
            case (KeyCode::Key_Backspace):
            case (KeyCode::Key_H):
            case (KeyCode::Key_Left):
                move_one_left(event);
                update_selection_on_cursor_move();
                break;
            case (KeyCode::Key_D):
                // TODO: Yank selected text
                m_editor->do_delete();
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
                move_to_next_span(event); // FIXME: This probably isn't 100% correct.
                update_selection_on_cursor_move();
                break;
            case (KeyCode::Key_X):
                // TODO: Yank selected text
                m_editor->do_delete();
                break;
            case (KeyCode::Key_0):
                move_to_line_beginning(event);
                update_selection_on_cursor_move();
                break;
            case (KeyCode::Key_V):
                switch_to_normal_mode();
                break;
            case (KeyCode::Key_C):
                // TODO: Yank selected text
                m_editor->do_delete();
                switch_to_insert_mode();
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

}
