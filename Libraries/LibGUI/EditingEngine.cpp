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

#include <LibGUI/EditingEngine.h>
#include <LibGUI/Event.h>
#include <LibGUI/TextEditor.h>

namespace GUI {

EditingEngine::~EditingEngine()
{
}

void EditingEngine::attach(TextEditor& editor)
{
    ASSERT(!m_editor);
    m_editor = editor;
}

void EditingEngine::detach()
{
    ASSERT(m_editor);
    m_editor = nullptr;
}

bool EditingEngine::on_key(const KeyEvent& event)
{
    if (event.key() == KeyCode::Key_Left) {
        if (!event.shift() && m_editor->selection()->is_valid()) {
            m_editor->set_cursor(m_editor->selection()->normalized().start());
            m_editor->selection()->clear();
            m_editor->did_update_selection();
            if (!event.ctrl()) {
                m_editor->update();
                return true;
            }
        }
        if (event.ctrl()) {
            move_to_previous_span(event);
            if (event.shift() && m_editor->selection()->start().is_valid()) {
                m_editor->selection()->set_end(m_editor->cursor());
                m_editor->did_update_selection();
            }
            return true;
        }
        move_one_left(event);
        if (event.shift() && m_editor->selection()->start().is_valid()) {
            m_editor->selection()->set_end(m_editor->cursor());
            m_editor->did_update_selection();
        }
        return true;
    }

    if (event.key() == KeyCode::Key_Right) {
        if (!event.shift() && m_editor->selection()->is_valid()) {
            m_editor->set_cursor(m_editor->selection()->normalized().end());
            m_editor->selection()->clear();
            m_editor->did_update_selection();
            if (!event.ctrl()) {
                m_editor->update();
                return true;
            }
        }
        if (event.ctrl()) {
            move_to_next_span(event);
            return true;
        }
        move_one_right(event);
        if (event.shift() && m_editor->selection()->start().is_valid()) {
            m_editor->selection()->set_end(m_editor->cursor());
            m_editor->did_update_selection();
        }
        return true;
    }

    if (event.key() == KeyCode::Key_Up) {
        move_one_up(event);
        if (event.shift() && m_editor->selection()->start().is_valid()) {
            m_editor->selection()->set_end(m_editor->cursor());
            m_editor->did_update_selection();
        }
        return true;
    }

    if (event.key() == KeyCode::Key_Down) {
        move_one_down(event);
        if (event.shift() && m_editor->selection()->start().is_valid()) {
            m_editor->selection()->set_end(m_editor->cursor());
            m_editor->did_update_selection();
        }
        return true;
    }

    if (event.key() == KeyCode::Key_Home) {
        if (event.ctrl()) {
            m_editor->toggle_selection_if_needed_for_event(event.shift());
            move_to_first_line();
            if (event.shift() && m_editor->selection()->start().is_valid()) {
                m_editor->selection()->set_end(m_editor->cursor());
                m_editor->did_update_selection();
            }
        } else {
            move_to_line_beginning(event);
            if (event.shift() && m_editor->selection()->start().is_valid()) {
                m_editor->selection()->set_end(m_editor->cursor());
                m_editor->did_update_selection();
            }
        }
        return true;
    }

    if (event.key() == KeyCode::Key_End) {
        if (event.ctrl()) {
            m_editor->toggle_selection_if_needed_for_event(event.shift());
            move_to_last_line();
            if (event.shift() && m_editor->selection()->start().is_valid()) {
                m_editor->selection()->set_end(m_editor->cursor());
                m_editor->did_update_selection();
            }
        } else {
            move_to_line_end(event);
            if (event.shift() && m_editor->selection()->start().is_valid()) {
                m_editor->selection()->set_end(m_editor->cursor());
                m_editor->did_update_selection();
            }
        }
        return true;
    }

    if (event.key() == KeyCode::Key_PageUp) {
        move_page_up(event);
        if (event.shift() && m_editor->selection()->start().is_valid()) {
            m_editor->selection()->set_end(m_editor->cursor());
            m_editor->did_update_selection();
        }
        return true;
    }

    if (event.key() == KeyCode::Key_PageDown) {
        move_page_down(event);
        if (event.shift() && m_editor->selection()->start().is_valid()) {
            m_editor->selection()->set_end(m_editor->cursor());
            m_editor->did_update_selection();
        }
        return true;
    }

    return false;
}

void EditingEngine::move_one_left(const KeyEvent& event)
{
    if (m_editor->cursor().column() > 0) {
        int new_column = m_editor->cursor().column() - 1;
        m_editor->toggle_selection_if_needed_for_event(event.shift());
        m_editor->set_cursor(m_editor->cursor().line(), new_column);
    } else if (m_editor->cursor().line() > 0) {
        int new_line = m_editor->cursor().line() - 1;
        int new_column = m_editor->lines()[new_line].length();
        m_editor->toggle_selection_if_needed_for_event(event.shift());
        m_editor->set_cursor(new_line, new_column);
    }
}

void EditingEngine::move_one_right(const KeyEvent& event)
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
    m_editor->toggle_selection_if_needed_for_event(event.shift());
    m_editor->set_cursor(new_line, new_column);
}

void EditingEngine::move_to_previous_span(const KeyEvent& event)
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
    m_editor->toggle_selection_if_needed_for_event(event.shift());
    m_editor->set_cursor(new_cursor);
}

void EditingEngine::move_to_next_span(const KeyEvent& event)
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
    m_editor->toggle_selection_if_needed_for_event(event.shift());
    m_editor->set_cursor(new_cursor);
    if (event.shift() && m_editor->selection()->start().is_valid()) {
        m_editor->selection()->set_end(m_editor->cursor());
        m_editor->did_update_selection();
    }
}

void EditingEngine::move_to_line_beginning(const KeyEvent& event)
{
    TextPosition new_cursor;
    m_editor->toggle_selection_if_needed_for_event(event.shift());
    if (m_editor->is_line_wrapping_enabled()) {
        // FIXME: Replicate the first_nonspace_column behavior in wrapping mode.
        auto home_position = m_editor->cursor_content_rect().location().translated(-m_editor->width(), 0);
        new_cursor = m_editor->text_position_at_content_position(home_position);
    } else {
        size_t first_nonspace_column = m_editor->current_line().first_non_whitespace_column();
        if (m_editor->cursor().column() == first_nonspace_column) {
            new_cursor = { m_editor->cursor().line(), 0 };
        } else {
            new_cursor = { m_editor->cursor().line(), first_nonspace_column };
        }
    }
    m_editor->set_cursor(new_cursor);
}

void EditingEngine::move_to_line_end(const KeyEvent& event)
{
    TextPosition new_cursor;
    if (m_editor->is_line_wrapping_enabled()) {
        auto end_position = m_editor->cursor_content_rect().location().translated(m_editor->width(), 0);
        new_cursor = m_editor->text_position_at_content_position(end_position);
    } else {
        new_cursor = { m_editor->cursor().line(), m_editor->current_line().length() };
    }
    m_editor->toggle_selection_if_needed_for_event(event.shift());
    m_editor->set_cursor(new_cursor);
}

void EditingEngine::move_one_up(const KeyEvent& event)
{
    if (m_editor->cursor().line() > 0 || m_editor->is_line_wrapping_enabled()) {
        if (event.ctrl() && event.shift()) {
            move_selected_lines_up();
            return;
        }
        TextPosition new_cursor;
        if (m_editor->is_line_wrapping_enabled()) {
            auto position_above = m_editor->cursor_content_rect().location().translated(0, -m_editor->line_height());
            new_cursor = m_editor->text_position_at_content_position(position_above);
        } else {
            size_t new_line = m_editor->cursor().line() - 1;
            size_t new_column = min(m_editor->cursor().column(), m_editor->line(new_line).length());
            new_cursor = { new_line, new_column };
        }
        m_editor->toggle_selection_if_needed_for_event(event.shift());
        m_editor->set_cursor(new_cursor);
    }
};

void EditingEngine::move_one_down(const KeyEvent& event)
{
    if (m_editor->cursor().line() < (m_editor->line_count() - 1) || m_editor->is_line_wrapping_enabled()) {
        if (event.ctrl() && event.shift()) {
            move_selected_lines_down();
            return;
        }
        TextPosition new_cursor;
        if (m_editor->is_line_wrapping_enabled()) {
            new_cursor = m_editor->text_position_at_content_position(m_editor->cursor_content_rect().location().translated(0, m_editor->line_height()));
            auto position_below = m_editor->cursor_content_rect().location().translated(0, m_editor->line_height());
            new_cursor = m_editor->text_position_at_content_position(position_below);
        } else {
            size_t new_line = m_editor->cursor().line() + 1;
            size_t new_column = min(m_editor->cursor().column(), m_editor->line(new_line).length());
            new_cursor = { new_line, new_column };
        }
        m_editor->toggle_selection_if_needed_for_event(event.shift());
        m_editor->set_cursor(new_cursor);
    }
};

void EditingEngine::move_up(const KeyEvent& event, double page_height_factor)
{
    if (m_editor->cursor().line() > 0 || m_editor->is_line_wrapping_enabled()) {
        int pixels = (int)(m_editor->visible_content_rect().height() * page_height_factor);

        TextPosition new_cursor;
        if (m_editor->is_line_wrapping_enabled()) {
            auto position_above = m_editor->cursor_content_rect().location().translated(0, -pixels);
            new_cursor = m_editor->text_position_at_content_position(position_above);
        } else {
            size_t page_step = (size_t)pixels / (size_t)m_editor->line_height();
            size_t new_line = m_editor->cursor().line() < page_step ? 0 : m_editor->cursor().line() - page_step;
            size_t new_column = min(m_editor->cursor().column(), m_editor->line(new_line).length());
            new_cursor = { new_line, new_column };
        }
        m_editor->toggle_selection_if_needed_for_event(event.shift());
        m_editor->set_cursor(new_cursor);
    }
};

void EditingEngine::move_down(const KeyEvent& event, double page_height_factor)
{
    if (m_editor->cursor().line() < (m_editor->line_count() - 1) || m_editor->is_line_wrapping_enabled()) {
        int pixels = (int)(m_editor->visible_content_rect().height() * page_height_factor);
        TextPosition new_cursor;
        if (m_editor->is_line_wrapping_enabled()) {
            auto position_below = m_editor->cursor_content_rect().location().translated(0, pixels);
            new_cursor = m_editor->text_position_at_content_position(position_below);
        } else {
            size_t new_line = min(m_editor->line_count() - 1, m_editor->cursor().line() + pixels / m_editor->line_height());
            size_t new_column = min(m_editor->cursor().column(), m_editor->lines()[new_line].length());
            new_cursor = { new_line, new_column };
        }
        m_editor->toggle_selection_if_needed_for_event(event.shift());
        m_editor->set_cursor(new_cursor);
    };
}

void EditingEngine::move_page_up(const KeyEvent& event)
{
    move_up(event, 1);
};

void EditingEngine::move_page_down(const KeyEvent& event)
{
    move_down(event, 1);
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
        m_editor->selection()->set_start({ first_line - 1, 0 });
        m_editor->selection()->set_end({ last_line - 1, m_editor->line(last_line - 1).length() });
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
    ASSERT(lines.size() != 0);
    if (last_line >= lines.size() - 1)
        return;

    lines.insert((int)first_line, lines.take((int)last_line + 1));
    m_editor->set_cursor({ first_line + 1, 0 });

    if (m_editor->has_selection()) {
        m_editor->selection()->set_start({ first_line + 1, 0 });
        m_editor->selection()->set_end({ last_line + 1, m_editor->line(last_line + 1).length() });
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
