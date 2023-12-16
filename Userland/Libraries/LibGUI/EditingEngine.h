/*
 * Copyright (c) 2021-2022, the SerenityOS developers.
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

class MoveLineUpOrDownCommand;

class EditingEngine {
    AK_MAKE_NONCOPYABLE(EditingEngine);
    AK_MAKE_NONMOVABLE(EditingEngine);

public:
    virtual ~EditingEngine() = default;

    virtual CursorWidth cursor_width() const { return NARROW; }

    void attach(TextEditor& editor);
    void detach();

    TextEditor& editor()
    {
        VERIFY(!m_editor.is_null());
        return *m_editor.unsafe_ptr();
    }

    virtual bool on_key(KeyEvent const& event);

    bool is_regular() const { return engine_type() == EngineType::Regular; }
    bool is_vim() const { return engine_type() == EngineType::Vim; }

    void get_selection_line_boundaries(Badge<MoveLineUpOrDownCommand>, size_t& first_line, size_t& last_line);

protected:
    EditingEngine() = default;

    WeakPtr<TextEditor> m_editor;

    enum class DidMoveALine {
        No,
        Yes,
    };

    void move_one_left();
    void move_one_right();
    void move_one_helper(KeyEvent const& event, VerticalDirection direction);
    DidMoveALine move_one_up(KeyEvent const& event);
    DidMoveALine move_one_down(KeyEvent const& event);
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

    void move_up(double page_height_factor);
    void move_down(double page_height_factor);

    void get_selection_line_boundaries(size_t& first_line, size_t& last_line);

    void delete_line();
    void delete_char();

    virtual EngineType engine_type() const = 0;
};

class MoveLineUpOrDownCommand : public TextDocumentUndoCommand {
public:
    MoveLineUpOrDownCommand(TextDocument&, KeyEvent event, EditingEngine&);
    virtual void undo() override;
    virtual void redo() override;
    bool merge_with(GUI::Command const&) override;
    ByteString action_text() const override;

    static bool valid_operation(EditingEngine& engine, VerticalDirection direction);

private:
    void move_lines(VerticalDirection);
    TextRange retrieve_selection(VerticalDirection);

    KeyEvent m_event;
    VerticalDirection m_direction;
    EditingEngine& m_engine;
    TextRange m_selection;
    TextPosition m_cursor;
};

}
