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

    if (event.key() == KeyCode::Key_Up) {
        if (m_editor->cursor().line() > 0 || m_editor->is_wrapping_enabled()) {
            m_editor->update_selection(event.shift());
        }
        move_one_up(event);
        if (event.shift() && m_editor->selection().start().is_valid()) {
            m_editor->selection().set_end(m_editor->cursor());
            m_editor->did_update_selection();
        }
        return true;
    }

    if (event.key() == KeyCode::Key_Down) {
        if (m_editor->cursor().line() < (m_editor->line_count() - 1) || m_editor->is_wrapping_enabled()) {
            m_editor->update_selection(event.shift());
        }
        move_one_down(event);
        if (event.shift() && m_editor->selection().start().is_valid()) {
            m_editor->selection().set_end(m_editor->cursor());
            m_editor->did_update_selection();
        }
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
        int new_column = m_editor->cursor().column() - 1;
        m_editor->set_cursor(m_editor->cursor().line(), new_column);
    } else if (m_editor->cursor().line() > 0) {
        int new_line = m_editor->cursor().line() - 1;
        int new_column = m_editor->lines()[new_line].length();
        m_editor->set_cursor(new_line, new_column);
    }
}

void EditingEngine::move_one_right()
{
    int new_line = m_editor->cursor().line();
    int new_column = m_editor->cursor().column();
    if (m_editor->cursor().column() < m_editor->current_line().length()) {
        new_line = m_editor->cursor().line();
        new_column = m_editor->cursor().column() + 1;
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
        // FIXME: Replicate the first_nonspace_column behavior in wrapping mode.
        auto home_position = m_editor->cursor_content_rect().location().translated(-m_editor->width(), 0);
        m_editor->set_cursor(m_editor->text_position_at_content_position(home_position));
    } else {
        move_to_logical_line_beginning();
    }
}

void EditingEngine::move_to_line_end()
{
    if (m_editor->is_wrapping_enabled()) {
        auto end_position = m_editor->cursor_content_rect().location().translated(m_editor->width(), 0);
        m_editor->set_cursor(m_editor->text_position_at_content_position(end_position));
    } else {
        move_to_logical_line_end();
    }
}

void EditingEngine::move_to_logical_line_end()
{
    m_editor->set_cursor({ m_editor->cursor().line(), m_editor->current_line().length() });
}

void EditingEngine::move_one_up(KeyEvent const& event)
{
    if (m_editor->cursor().line() > 0 || m_editor->is_wrapping_enabled()) {
        if (event.ctrl() && event.shift()) {
            move_selected_lines_up();
            return;
        }
        TextPosition new_cursor;
        if (m_editor->is_wrapping_enabled()) {
            auto position_above = m_editor->cursor_content_rect().location().translated(0, -m_editor->line_height());
            new_cursor = m_editor->text_position_at_content_position(position_above);
        } else {
            size_t new_line = m_editor->cursor().line() - 1;
            size_t new_column = min(m_editor->cursor().column(), m_editor->line(new_line).length());
            new_cursor = { new_line, new_column };
        }
        m_editor->set_cursor(new_cursor);
    }
};

void EditingEngine::move_one_down(KeyEvent const& event)
{
    if (m_editor->cursor().line() < (m_editor->line_count() - 1) || m_editor->is_wrapping_enabled()) {
        if (event.ctrl() && event.shift()) {
            move_selected_lines_down();
            return;
        }
        TextPosition new_cursor;
        if (m_editor->is_wrapping_enabled()) {
            auto position_below = m_editor->cursor_content_rect().location().translated(0, m_editor->line_height());
            new_cursor = m_editor->text_position_at_content_position(position_below);
        } else {
            size_t new_line = m_editor->cursor().line() + 1;
            size_t new_column = min(m_editor->cursor().column(), m_editor->line(new_line).length());
            new_cursor = { new_line, new_column };
        }
        m_editor->set_cursor(new_cursor);
    }
};

void EditingEngine::move_up(double page_height_factor)
{
    if (m_editor->cursor().line() > 0 || m_editor->is_wrapping_enabled()) {
        int pixels = (int)(m_editor->visible_content_rect().height() * page_height_factor);

        TextPosition new_cursor;
        if (m_editor->is_wrapping_enabled()) {
            auto position_above = m_editor->cursor_content_rect().location().translated(0, -pixels);
            new_cursor = m_editor->text_position_at_content_position(position_above);
        } else {
            size_t page_step = (size_t)pixels / (size_t)m_editor->line_height();
            size_t new_line = m_editor->cursor().line() < page_step ? 0 : m_editor->cursor().line() - page_step;
            size_t new_column = min(m_editor->cursor().column(), m_editor->line(new_line).length());
            new_cursor = { new_line, new_column };
        }
        m_editor->set_cursor(new_cursor);
    }
};

void EditingEngine::move_down(double page_height_factor)
{
    if (m_editor->cursor().line() < (m_editor->line_count() - 1) || m_editor->is_wrapping_enabled()) {
        int pixels = (int)(m_editor->visible_content_rect().height() * page_height_factor);
        TextPosition new_cursor;
        if (m_editor->is_wrapping_enabled()) {
            auto position_below = m_editor->cursor_content_rect().location().translated(0, pixels);
            new_cursor = m_editor->text_position_at_content_position(position_below);
        } else {
            size_t new_line = min(m_editor->line_count() - 1, m_editor->cursor().line() + pixels / m_editor->line_height());
            size_t new_column = min(m_editor->cursor().column(), m_editor->lines()[new_line].length());
            new_cursor = { new_line, new_column };
        }
        m_editor->set_cursor(new_cursor);
    };
}

void EditingEngine::move_page_up()
{
    move_up(1);
};

void EditingEngine::move_page_down()
{
    move_down(1);
};

void EditingEngine::move_to_first_line()
{
    m_editor->set_cursor(0, 0);
};

void EditingEngine::move_to_last_line()
{
    m_editor->set_cursor(m_editor->line_count() - 1, m_editor->lines()[m_editor->line_count() - 1].length());
};

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

void EditingEngine::move_selected_lines_up()
{
    if (!m_editor->is_editable())
        return;
    size_t first_line;
    size_t last_line;
    get_selection_line_boundaries(first_line, last_line);

    if (first_line == 0)
        return;

    auto& lines = m_editor->document().lines();
    lines.insert((int)last_line, lines.take((int)first_line - 1));
    m_editor->set_cursor({ first_line - 1, 0 });

    if (m_editor->has_selection()) {
        m_editor->selection().set_start({ first_line - 1, 0 });
        m_editor->selection().set_end({ last_line - 1, m_editor->line(last_line - 1).length() });
    }

    m_editor->did_change();
    m_editor->update();
}

void EditingEngine::move_selected_lines_down()
{
    if (!m_editor->is_editable())
        return;
    size_t first_line;
    size_t last_line;
    get_selection_line_boundaries(first_line, last_line);

    auto& lines = m_editor->document().lines();
    VERIFY(lines.size() != 0);
    if (last_line >= lines.size() - 1)
        return;

    lines.insert((int)first_line, lines.take((int)last_line + 1));
    m_editor->set_cursor({ first_line + 1, 0 });

    if (m_editor->has_selection()) {
        m_editor->selection().set_start({ first_line + 1, 0 });
        m_editor->selection().set_end({ last_line + 1, m_editor->line(last_line + 1).length() });
    }

    m_editor->did_change();
    m_editor->update();
}

void EditingEngine::delete_char()
{
    if (!m_editor->is_editable())
        return;
    m_editor->do_delete();
};

void EditingEngine::delete_line()
{
    if (!m_editor->is_editable())
        return;
    m_editor->delete_current_line();
};

}
