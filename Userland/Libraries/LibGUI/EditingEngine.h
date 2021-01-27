/*
 * Copyright (c) 2021, the SerenityOS developers.
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

#include <AK/Noncopyable.h>
#include <LibGUI/Event.h>
#include <LibGUI/TextDocument.h>

namespace GUI {

enum CursorWidth {
    NARROW,
    WIDE
};

enum EditingEngineType {
    Regular,
    Vim
};

class EditingEngine {
    AK_MAKE_NONCOPYABLE(EditingEngine);
    AK_MAKE_NONMOVABLE(EditingEngine);

public:
    virtual ~EditingEngine();

    virtual CursorWidth cursor_width() const { return NARROW; }

    void attach(TextEditor& editor);
    void detach();

    virtual bool on_key(const KeyEvent& event);

    EditingEngineType type() const { return m_editing_engine_type; }

protected:
    EditingEngine() { }

    WeakPtr<TextEditor> m_editor;

    EditingEngineType m_editing_engine_type;

    void move_one_left(const KeyEvent& event);
    void move_one_right(const KeyEvent& event);
    void move_one_up(const KeyEvent& event);
    void move_one_down(const KeyEvent& event);
    void move_to_previous_span(const KeyEvent& event);
    void move_to_next_span(const KeyEvent& event);
    void move_to_line_beginning(const KeyEvent& event);
    void move_to_line_end(const KeyEvent& event);
    void move_page_up(const KeyEvent& event);
    void move_page_down(const KeyEvent& event);
    void move_to_first_line();
    void move_to_last_line();
    TextPosition find_beginning_of_next_word();
    void move_to_beginning_of_next_word();
    TextPosition find_end_of_next_word();
    void move_to_end_of_next_word();
    TextPosition find_end_of_previous_word();
    void move_to_end_of_previous_word();
    TextPosition find_beginning_of_previous_word();
    void move_to_beginning_of_previous_word();

    void move_up(const KeyEvent& event, double page_height_factor);
    void move_down(const KeyEvent& event, double page_height_factor);

    void get_selection_line_boundaries(size_t& first_line, size_t& last_line);

    void delete_line();
    void delete_char();

private:
    void move_selected_lines_up();
    void move_selected_lines_down();
};

}
