/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
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

enum EngineType {
    Regular,
    Vim,
};

class EditingEngine {
    AK_MAKE_NONCOPYABLE(EditingEngine);
    AK_MAKE_NONMOVABLE(EditingEngine);

public:
    virtual ~EditingEngine();

    virtual CursorWidth cursor_width() const { return NARROW; }

    void attach(TextEditor& editor);
    void detach();

    TextEditor& editor()
    {
        VERIFY(!m_editor.is_null());
        return *m_editor.unsafe_ptr();
    }

    virtual bool on_key(const KeyEvent& event);

    bool is_regular() const { return engine_type() == EngineType::Regular; }
    bool is_vim() const { return engine_type() == EngineType::Vim; }

protected:
    EditingEngine() { }

    WeakPtr<TextEditor> m_editor;

    void move_one_left();
    void move_one_right();
    void move_one_up(const KeyEvent& event);
    void move_one_down(const KeyEvent& event);
    void move_to_previous_span();
    void move_to_next_span();
    void move_to_logical_line_beginning();
    void move_to_logical_line_end();
    void move_to_line_beginning();
    void move_to_line_end();
    void move_page_up();
    void move_page_down();
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

    void move_up(double page_height_factor);
    void move_down(double page_height_factor);

    void get_selection_line_boundaries(size_t& first_line, size_t& last_line);

    void delete_line();
    void delete_char();

    virtual EngineType engine_type() const = 0;

private:
    void move_selected_lines_up();
    void move_selected_lines_down();
};

}
