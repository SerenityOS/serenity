/*
 * Copyright (c) 2021-2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/CharacterTypes.h>
#include <LibGUI/EditingEngine.h>
#include <LibGUI/Event.h>
#include <LibGUI/TextEditor.h>

namespace GUI {

void EditingEngine::attach(TextEditor& editor)
{
    VERIFY(!m_editor);
    m_editor = editor;
}

void EditingEngine::detach()
{
    VERIFY(m_editor);
    m_editor = nullptr;
}

bool EditingEngine::on_key(KeyEvent const& event)
{
    if (event.key() == KeyCode::Key_Left) {
        if (!event.shift() && m_editor->selection().is_valid()) {
            m_editor->set_cursor(m_editor->selection().normalized().start());
            m_editor->selection().clear();
            m_editor->did_update_selection();
            if (!event.ctrl()) {
                m_editor->update();
                return true;
            }
        }
        if (event.ctrl()) {
            m_editor->update_selection(event.shift());
            move_to_previous_span();
            if (event.shift() && m_editor->selection().start().is_valid()) {
                m_editor->selection().set_end(m_editor->cursor());
                m_editor->did_update_selection();
            }
            return true;
        }
        m_editor->update_selection(event.shift());
        move_one_left();
        if (event.shift() && m_editor->selection().start().is_valid()) {
            m_editor->selection().set_end(m_editor->cursor());
            m_editor->did_update_selection();
        }
        return true;
    }

    if (event.key() == KeyCode::Key_Right) {
        if (!event.shift() && m_editor->selection().is_valid()) {
            m_editor->set_cursor(m_editor->selection().normalized().end());
            m_editor->selection().clear();
            m_editor->did_update_selection();
            if (!event.ctrl()) {
                m_editor->update();
                return true;
            }
        }
        if (event.ctrl()) {
            m_editor->update_selection(event.shift());
            move_to_next_span();
            if (event.shift() && m_editor->selection().start().is_valid()) {
                m_editor->selection().set_end(m_editor->cursor());
                m_editor->did_update_selection();
            }
            return true;
        }
        m_editor->update_selection(event.shift());
        move_one_right();
        if (event.shift() && m_editor->selection().start().is_valid()) {
            m_editor->selection().set_end(m_editor->cursor());
            m_editor->did_update_selection();
        }
        return true;
    }

    if (event.key() == KeyCode::Key_Up || event.key() == KeyCode::Key_Down) {
        auto const direction = key_code_to_vertical_direction(event.key());

        bool const condition_for_up = direction == VerticalDirection::Up && m_editor->cursor().line() > 0;
        bool const condition_for_down = direction == VerticalDirection::Down && m_editor->cursor().line() < (m_editor->line_count() - 1);

        bool const condition_for_up_to_beginning = direction == VerticalDirection::Up && m_editor->cursor().line() == 0;
        bool const condition_for_down_to_end = direction == VerticalDirection::Down && m_editor->cursor().line() == (m_editor->line_count() - 1);

        if (condition_for_up || condition_for_down || m_editor->is_wrapping_enabled())
            m_editor->update_selection(event.shift());

        // Shift + Up on the top line (or only line) selects from the cursor to the start of the line.
        if (condition_for_up_to_beginning) {
            m_editor->update_selection(event.shift());
            move_to_line_beginning();
        }

        // Shift + Down on the bottom line (or only line) selects from the cursor to the end of the line.
        if (condition_for_down_to_end) {
            m_editor->update_selection(event.shift());
            move_to_line_end();
        }

        move_one_helper(event, direction);
        return true;
    }

    if (event.key() == KeyCode::Key_Home) {
        m_editor->update_selection(event.shift());
        if (event.ctrl()) {
            move_to_first_line();
        } else {
            move_to_line_beginning();
        }
        if (event.shift() && m_editor->selection().start().is_valid()) {
            m_editor->selection().set_end(m_editor->cursor());
            m_editor->did_update_selection();
        }
        return true;
    }

    if (event.key() == KeyCode::Key_End) {
        m_editor->update_selection(event.shift());
        if (event.ctrl()) {
            move_to_last_line();
        } else {
            move_to_line_end();
        }
        if (event.shift() && m_editor->selection().start().is_valid()) {
            m_editor->selection().set_end(m_editor->cursor());
            m_editor->did_update_selection();
        }
        return true;
    }

    if (event.key() == KeyCode::Key_PageUp) {
        if (m_editor->cursor().line() > 0 || m_editor->is_wrapping_enabled()) {
            m_editor->update_selection(event.shift());
        }
        move_page_up();
        if (event.shift() && m_editor->selection().start().is_valid()) {
            m_editor->selection().set_end(m_editor->cursor());
            m_editor->did_update_selection();
        }
        return true;
    }

    if (event.key() == KeyCode::Key_PageDown) {
        if (m_editor->cursor().line() < (m_editor->line_count() - 1) || m_editor->is_wrapping_enabled()) {
            m_editor->update_selection(event.shift());
        }
        move_page_down();
        if (event.shift() && m_editor->selection().start().is_valid()) {
            m_editor->selection().set_end(m_editor->cursor());
            m_editor->did_update_selection();
        }
        return true;
    }

    return false;
}

void EditingEngine::move_one_left()
{
    if (m_editor->cursor().column() > 0) {
        auto new_column = m_editor->document().get_previous_grapheme_cluster_boundary(m_editor->cursor());
        m_editor->set_cursor(m_editor->cursor().line(), new_column);
    } else if (m_editor->cursor().line() > 0) {
        auto new_line = m_editor->cursor().line() - 1;
        auto new_column = m_editor->lines()[new_line]->length();
        m_editor->set_cursor(new_line, new_column);
    }
}

void EditingEngine::move_one_right()
{
    auto new_line = m_editor->cursor().line();
    auto new_column = m_editor->cursor().column();

    if (m_editor->cursor().column() < m_editor->current_line().length()) {
        new_line = m_editor->cursor().line();
        new_column = m_editor->document().get_next_grapheme_cluster_boundary(m_editor->cursor());
    } else if (m_editor->cursor().line() != m_editor->line_count() - 1) {
        new_line = m_editor->cursor().line() + 1;
        new_column = 0;
    }

    m_editor->set_cursor(new_line, new_column);
}

void EditingEngine::move_to_previous_span()
{
    TextPosition new_cursor;
    if (m_editor->document().has_spans()) {
        auto span = m_editor->document().first_non_skippable_span_before(m_editor->cursor());
        if (span.has_value()) {
            new_cursor = span.value().range.start();
        } else {
            // No remaining spans, just use word break calculation
            new_cursor = m_editor->document().first_word_break_before(m_editor->cursor(), true);
        }
    } else {
        new_cursor = m_editor->document().first_word_break_before(m_editor->cursor(), true);
    }
    m_editor->set_cursor(new_cursor);
}

void EditingEngine::move_to_next_span()
{
    TextPosition new_cursor;
    if (m_editor->document().has_spans()) {
        auto span = m_editor->document().first_non_skippable_span_after(m_editor->cursor());
        if (span.has_value()) {
            new_cursor = span.value().range.start();
        } else {
            // No remaining spans, just use word break calculation
            new_cursor = m_editor->document().first_word_break_after(m_editor->cursor());
        }
    } else {
        new_cursor = m_editor->document().first_word_break_after(m_editor->cursor());
    }
    m_editor->set_cursor(new_cursor);
}

void EditingEngine::move_to_logical_line_beginning()
{
    TextPosition new_cursor;
    size_t first_nonspace_column = m_editor->current_line().first_non_whitespace_column();
    if (m_editor->cursor().column() == first_nonspace_column) {
        new_cursor = { m_editor->cursor().line(), 0 };
    } else {
        new_cursor = { m_editor->cursor().line(), first_nonspace_column };
    }
    m_editor->set_cursor(new_cursor);
}

void EditingEngine::move_to_line_beginning()
{
    if (m_editor->is_wrapping_enabled()) {
        TextPosition new_cursor;

        auto home_position = m_editor->cursor_content_rect().location().translated(-m_editor->width(), 0);
        auto start_of_visual_line = m_editor->text_position_at_content_position(home_position);
        auto first_non_space_column = m_editor->current_line().first_non_whitespace_column();

        // Subsequent "move_to_line_beginning()" calls move us in the following way:
        // 1. To the start of the current visual line
        // 2. To the first non-whitespace character on the logical line
        // 3. To the first character on the logical line
        // ...and then repeat 2 and 3.
        if (m_editor->cursor() == start_of_visual_line) {
            // Already at 1 so go to 2
            new_cursor = { m_editor->cursor().line(), first_non_space_column };
        } else if (m_editor->cursor().column() == first_non_space_column) {
            // At 2 so go to 3
            new_cursor = { m_editor->cursor().line(), 0 };
        } else {
            // Anything else, so go to 1
            new_cursor = start_of_visual_line;
        }

        m_editor->set_cursor(new_cursor);
    } else {
        move_to_logical_line_beginning();
    }
}

void EditingEngine::move_to_line_end()
{
    if (m_editor->is_wrapping_enabled())
        m_editor->set_cursor_to_end_of_visual_line();
    else
        move_to_logical_line_end();
}

void EditingEngine::move_to_logical_line_end()
{
    m_editor->set_cursor({ m_editor->cursor().line(), m_editor->current_line().length() });
}

void EditingEngine::move_one_helper(KeyEvent const& event, VerticalDirection direction)
{
    auto const result = direction == VerticalDirection::Up ? move_one_up(event) : move_one_down(event);
    if (result != DidMoveALine::Yes && event.shift() && m_editor->selection().start().is_valid()) {
        m_editor->selection().set_end(m_editor->cursor());
        m_editor->did_update_selection();
    }
}

EditingEngine::DidMoveALine EditingEngine::move_one_up(KeyEvent const& event)
{
    if (m_editor->cursor().line() > 0 || m_editor->is_wrapping_enabled()) {
        if (event.ctrl() && event.shift()) {
            if (MoveLineUpOrDownCommand::valid_operation(*this, VerticalDirection::Up)) {
                m_editor->execute<MoveLineUpOrDownCommand>(Badge<EditingEngine> {}, event, *this);
                return DidMoveALine::Yes;
            }
            return DidMoveALine::No;
        }
        auto position_above = m_editor->cursor_content_rect().location().translated(0, -m_editor->line_height());
        TextPosition new_cursor = m_editor->text_position_at_content_position(position_above);
        m_editor->set_cursor(new_cursor);
    }
    return DidMoveALine::No;
}

EditingEngine::DidMoveALine EditingEngine::move_one_down(KeyEvent const& event)
{
    if (m_editor->cursor().line() < (m_editor->line_count() - 1) || m_editor->is_wrapping_enabled()) {
        if (event.ctrl() && event.shift()) {
            if (MoveLineUpOrDownCommand::valid_operation(*this, VerticalDirection::Down)) {
                m_editor->execute<MoveLineUpOrDownCommand>(Badge<EditingEngine> {}, event, *this);
                return DidMoveALine::Yes;
            }
            return DidMoveALine::No;
        }
        auto position_below = m_editor->cursor_content_rect().location().translated(0, m_editor->line_height());
        TextPosition new_cursor = m_editor->text_position_at_content_position(position_below);
        m_editor->set_cursor(new_cursor);
    }
    return DidMoveALine::No;
}

void EditingEngine::move_up(double page_height_factor)
{
    if (m_editor->cursor().line() > 0 || m_editor->is_wrapping_enabled()) {
        int pixels = (int)(m_editor->visible_content_rect().height() * page_height_factor);
        auto position_above = m_editor->cursor_content_rect().location().translated(0, -pixels);
        TextPosition new_cursor = m_editor->text_position_at_content_position(position_above);
        m_editor->set_cursor(new_cursor);
    }
}

void EditingEngine::move_down(double page_height_factor)
{
    if (m_editor->cursor().line() < (m_editor->line_count() - 1) || m_editor->is_wrapping_enabled()) {
        int pixels = (int)(m_editor->visible_content_rect().height() * page_height_factor);
        auto position_below = m_editor->cursor_content_rect().location().translated(0, pixels);
        TextPosition new_cursor = m_editor->text_position_at_content_position(position_below);
        m_editor->set_cursor(new_cursor);
    };
}

void EditingEngine::move_page_up()
{
    move_up(1);
}

void EditingEngine::move_page_down()
{
    move_down(1);
}

void EditingEngine::move_to_first_line()
{
    m_editor->set_cursor(0, 0);
}

void EditingEngine::move_to_last_line()
{
    m_editor->set_cursor(m_editor->line_count() - 1, m_editor->lines()[m_editor->line_count() - 1]->length());
}

void EditingEngine::get_selection_line_boundaries(Badge<MoveLineUpOrDownCommand>, size_t& first_line, size_t& last_line)
{
    get_selection_line_boundaries(first_line, last_line);
}

void EditingEngine::get_selection_line_boundaries(size_t& first_line, size_t& last_line)
{
    auto selection = m_editor->normalized_selection();
    if (!selection.is_valid()) {
        first_line = m_editor->cursor().line();
        last_line = m_editor->cursor().line();
        return;
    }
    first_line = selection.start().line();
    last_line = selection.end().line();
    if (first_line != last_line && selection.end().column() == 0)
        last_line -= 1;
}

void EditingEngine::delete_char()
{
    if (!m_editor->is_editable())
        return;
    m_editor->do_delete();
}

void EditingEngine::delete_line()
{
    if (!m_editor->is_editable())
        return;
    m_editor->delete_current_line();
}

MoveLineUpOrDownCommand::MoveLineUpOrDownCommand(TextDocument& document, KeyEvent event, EditingEngine& engine)
    : TextDocumentUndoCommand(document)
    , m_event(move(event))
    , m_direction(key_code_to_vertical_direction(m_event.key()))
    , m_engine(engine)
    , m_selection(m_engine.editor().selection())
    , m_cursor(m_engine.editor().cursor())
{
}

void MoveLineUpOrDownCommand::redo()
{
    move_lines(m_direction);
}

void MoveLineUpOrDownCommand::undo()
{
    move_lines(!m_direction);
}

bool MoveLineUpOrDownCommand::merge_with(GUI::Command const&)
{
    return false;
}

ByteString MoveLineUpOrDownCommand::action_text() const
{
    return "Move a line";
}

bool MoveLineUpOrDownCommand::valid_operation(EditingEngine& engine, VerticalDirection direction)
{

    VERIFY(engine.editor().line_count() != 0);

    auto const& selection = engine.editor().selection().normalized();
    if (selection.is_valid()) {
        if ((direction == VerticalDirection::Up && selection.start().line() == 0) || (direction == VerticalDirection::Down && selection.end().line() >= engine.editor().line_count() - 1))
            return false;
    } else {
        size_t first_line;
        size_t last_line;
        engine.get_selection_line_boundaries(Badge<MoveLineUpOrDownCommand> {}, first_line, last_line);

        if ((direction == VerticalDirection::Up && first_line == 0) || (direction == VerticalDirection::Down && last_line >= engine.editor().line_count() - 1))
            return false;
    }
    return true;
}

TextRange MoveLineUpOrDownCommand::retrieve_selection(VerticalDirection direction)
{
    if (direction == m_direction)
        return m_selection;

    auto const offset_selection = [this](auto const offset) {
        auto tmp = m_selection;
        tmp.start().set_line(tmp.start().line() + offset);
        tmp.end().set_line(tmp.end().line() + offset);

        return tmp;
    };

    if (direction == VerticalDirection::Up)
        return offset_selection(1);
    if (direction == VerticalDirection::Down)
        return offset_selection(-1);
    VERIFY_NOT_REACHED();
}

void MoveLineUpOrDownCommand::move_lines(VerticalDirection direction)
{
    if (m_event.shift() && m_selection.is_valid()) {
        m_engine.editor().set_selection(retrieve_selection(direction));
        m_engine.editor().did_update_selection();
    }

    if (!m_engine.editor().is_editable())
        return;

    size_t first_line;
    size_t last_line;
    m_engine.get_selection_line_boundaries(Badge<MoveLineUpOrDownCommand> {}, first_line, last_line);

    auto const offset = direction == VerticalDirection::Up ? -1 : 1;
    auto const insertion_index = direction == VerticalDirection::Up ? last_line : first_line;
    auto const moved_line_index = offset + (direction != VerticalDirection::Up ? last_line : first_line);

    auto moved_line = m_document.take_line(moved_line_index);
    m_document.insert_line(insertion_index, move(moved_line));

    m_engine.editor().set_cursor({ m_engine.editor().cursor().line() + offset, m_engine.editor().cursor().column() });
    if (m_engine.editor().has_selection()) {
        m_engine.editor().selection().start().set_line(m_engine.editor().selection().start().line() + offset);
        m_engine.editor().selection().end().set_line(m_engine.editor().selection().end().line() + offset);
    }

    m_engine.editor().did_change();
    m_engine.editor().update();
}

}
