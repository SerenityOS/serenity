/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibVT/Line.h>

namespace VT {

Line::Line(size_t length)
{
    set_length(length, nullptr, nullptr);
}

Line::~Line()
{
}

void Line::set_length(size_t new_length, Line* next_line, CursorPosition* cursor, bool cursor_is_on_next_line)
{
    size_t old_length = length();
    if (old_length == new_length)
        return;

    // Drop the empty cells
    if (m_terminated_at.has_value())
        m_cells.remove(m_terminated_at.value(), m_cells.size() - m_terminated_at.value());

    if (!next_line)
        return m_cells.resize(new_length);

    if (old_length < new_length)
        take_cells_from_next_line(old_length, new_length, next_line, cursor_is_on_next_line, cursor);
    else
        push_cells_into_next_line(old_length, new_length, next_line, cursor_is_on_next_line, cursor);

    m_cells.resize(max(new_length, static_cast<size_t>(m_terminated_at.value_or(new_length))));
}

void Line::push_cells_into_next_line(size_t old_length, size_t new_length, Line* next_line, bool cursor_is_on_next_line, CursorPosition* cursor)
{
    // Push as many cells as _wouldn't_ fit into the next line.
    auto cells_to_preserve = !next_line->m_terminated_at.has_value() && next_line->is_empty() ? 0 : m_terminated_at.value_or(0);
    auto cells_to_push_into_next_line = min(old_length - new_length, length() - cells_to_preserve);
    if (!cells_to_push_into_next_line)
        return;

    auto preserved_cells = length() - cells_to_push_into_next_line;
    if (next_line->m_terminated_at.has_value())
        next_line->m_terminated_at = next_line->m_terminated_at.value() + cells_to_push_into_next_line;

    if (m_terminated_at.has_value() && cells_to_preserve == 0) {
        m_terminated_at.clear();
        if (!next_line->m_terminated_at.has_value())
            next_line->m_terminated_at = cells_to_push_into_next_line;
    }

    if (cursor) {
        if (cursor_is_on_next_line) {
            cursor->column += cells_to_push_into_next_line;
        } else if (cursor->column >= preserved_cells) {
            cursor->row++;
            cursor->column = cursor->column - preserved_cells;
        }
    }

    next_line->m_cells.prepend(m_cells.span().slice_from_end(cells_to_push_into_next_line).data(), cells_to_push_into_next_line);
    m_cells.remove(m_cells.size() - cells_to_push_into_next_line, cells_to_push_into_next_line);
    if (m_terminated_at.has_value())
        m_terminated_at = m_terminated_at.value() - cells_to_push_into_next_line;
}

void Line::take_cells_from_next_line(size_t old_length, size_t new_length, Line* next_line, bool cursor_is_on_next_line, CursorPosition* cursor)
{
    // Take as many cells as would fit from the next line
    if (m_terminated_at.has_value())
        return;

    auto cells_to_grab_from_next_line = min(new_length - old_length, next_line->length());
    auto clear_next_line = false;
    if (next_line->m_terminated_at.has_value()) {
        cells_to_grab_from_next_line = min(cells_to_grab_from_next_line, static_cast<size_t>(next_line->m_terminated_at.value()));
        if (cells_to_grab_from_next_line == *next_line->m_terminated_at) {
            m_terminated_at = old_length + *next_line->m_terminated_at;
            next_line->m_terminated_at.clear();
            clear_next_line = true;
        } else {
            next_line->m_terminated_at = next_line->m_terminated_at.value() - cells_to_grab_from_next_line;
        }
    }

    if (cells_to_grab_from_next_line) {
        if (cursor && cursor_is_on_next_line) {
            if (cursor->column <= cells_to_grab_from_next_line) {
                cursor->row--;
                cursor->column += m_cells.size();
            } else {
                cursor->column -= cells_to_grab_from_next_line;
            }
        }
        m_cells.append(next_line->m_cells.data(), cells_to_grab_from_next_line);
        next_line->m_cells.remove(0, cells_to_grab_from_next_line);
    }

    if (clear_next_line)
        next_line->m_cells.clear();
}

void Line::clear_range(size_t first_column, size_t last_column, const Attribute& attribute)
{
    VERIFY(first_column <= last_column);
    VERIFY(last_column < m_cells.size());
    for (size_t i = first_column; i <= last_column; ++i) {
        auto& cell = m_cells[i];
        if (!m_dirty)
            m_dirty = cell.code_point != ' ' || cell.attribute != attribute;
        cell = Cell { .code_point = ' ', .attribute = attribute };
    }
}

bool Line::has_only_one_background_color() const
{
    if (!length())
        return true;
    // FIXME: Cache this result?
    auto color = attribute_at(0).effective_background_color();
    for (size_t i = 1; i < length(); ++i) {
        if (attribute_at(i).effective_background_color() != color)
            return false;
    }
    return true;
}

}
