/*
 * Copyright (c) 2021, Andres Crucitti <dasc495@gmail.com>
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 * Copyright (c) 2021, Ryan Wilson <ryan@rdwilson.xyz>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "BoardWidget.h"
#include <LibGUI/Painter.h>

BoardWidget::BoardWidget(size_t rows, size_t columns)
    : m_board(make<Board>(rows, columns))
{
    m_timer = add<Core::Timer>();
    m_timer->stop();
    m_timer->on_timeout = [this] {
        run_generation();
    };
    m_timer->set_interval(m_running_timer_interval);
}

void BoardWidget::run_generation()
{
    m_board->run_generation();
    update();
    if (m_board->is_stalled()) {
        if (on_stall)
            on_stall();
        update();
    };
}

void BoardWidget::resize_board(size_t rows, size_t columns)
{
    if (columns == m_board->columns() && rows == m_board->rows())
        return;
    m_board->resize(rows, columns);
    m_last_cell_toggled = { rows, columns };
}

void BoardWidget::set_running_timer_interval(int interval)
{
    if (is_running())
        return;

    m_running_timer_interval = interval;
    m_timer->set_interval(m_running_timer_interval);

    if (on_running_state_change)
        on_running_state_change();
}

void BoardWidget::set_running(bool running)
{
    if (running == m_running)
        return;

    m_running = running;

    if (m_running) {
        m_timer->start();
    } else {
        m_timer->stop();
    }

    if (on_running_state_change)
        on_running_state_change();

    update();
}

void BoardWidget::toggle_cell(size_t row, size_t column)
{
    if (m_running || !m_toggling_cells || (m_last_cell_toggled.row == row && m_last_cell_toggled.column == column))
        return;

    m_last_cell_toggled = { row, column };
    m_board->toggle_cell(row, column);

    if (on_cell_toggled)
        on_cell_toggled(m_board, row, column);

    update();
}

int BoardWidget::get_cell_size() const
{
    int width = rect().width() / m_board->columns();
    int height = rect().height() / m_board->rows();

    return min(width, height);
}

Gfx::IntSize BoardWidget::get_board_offset() const
{
    int cell_size = get_cell_size();
    return {
        (width() - cell_size * m_board->columns()) / 2,
        (height() - cell_size * m_board->rows()) / 2,
    };
}

void BoardWidget::paint_event(GUI::PaintEvent& event)
{
    GUI::Widget::paint_event(event);

    GUI::Painter painter(*this);
    painter.add_clip_rect(event.rect());
    painter.fill_rect(event.rect(), Color::Black);

    int cell_size = get_cell_size();
    Gfx::IntSize board_offset = get_board_offset();

    for (size_t row = 0; row < m_board->rows(); ++row) {
        for (size_t column = 0; column < m_board->columns(); ++column) {
            int cell_x = column * cell_size + board_offset.width();
            int cell_y = row * cell_size + board_offset.height();

            Gfx::Rect cell_rect(cell_x, cell_y, cell_size, cell_size);

            Color border_color = Color::DarkGray;
            Color fill_color;

            bool on = m_board->cell(row, column);
            if (on) {
                fill_color = Color::from_rgb(Gfx::make_rgb(220, 220, 80));
            } else {
                fill_color = Color::MidGray;
            }

            painter.fill_rect(cell_rect, fill_color);
            if (cell_size > 4) {
                painter.draw_rect(cell_rect, border_color);
            }
        }
    }
}

void BoardWidget::mousedown_event(GUI::MouseEvent& event)
{
    set_toggling_cells(true);
    auto row_and_column = get_row_and_column_for_point(event.x(), event.y());
    if (!row_and_column.has_value())
        return;
    auto [row, column] = row_and_column.value();
    if (m_active_pattern == nullptr) {
        toggle_cell(row, column);
    } else {
        place_pattern(row, column);
    }
}

void BoardWidget::mousemove_event(GUI::MouseEvent& event)
{
    auto row_and_column = get_row_and_column_for_point(event.x(), event.y());
    if (!row_and_column.has_value())
        return;
    auto [row, column] = row_and_column.value();
    if (m_toggling_cells) {
        if (m_last_cell_toggled.row != row || m_last_cell_toggled.column != column)
            toggle_cell(row, column);
    }
}

void BoardWidget::mouseup_event(GUI::MouseEvent&)
{
    set_toggling_cells(false);
}

Optional<Board::RowAndColumn> BoardWidget::get_row_and_column_for_point(int x, int y) const
{
    auto board_offset = get_board_offset();
    auto cell_size = get_cell_size();
    auto board_width = m_board->columns() * cell_size;
    auto board_height = m_board->rows() * cell_size;
    if (x <= board_offset.width() || static_cast<size_t>(x) >= board_offset.width() + board_width)
        return {};
    if (y <= board_offset.height() || static_cast<size_t>(y) >= board_offset.height() + board_height)
        return {};
    return { {
        .row = static_cast<size_t>((y - board_offset.height()) / cell_size),
        .column = static_cast<size_t>((x - board_offset.width()) / cell_size),
    } };
}

void BoardWidget::place_pattern(size_t row, size_t column)
{
    int y_offset = 0;
    for (auto line : m_active_pattern->m_pattern) {
        int x_offset = 0;
        for (auto c : line) {
            if (c == 'O' && (row + y_offset) < m_board->rows() && (column + x_offset) < m_board->columns())
                toggle_cell(row + y_offset, column + x_offset);
            x_offset++;
        }
        y_offset++;
    }
}

void BoardWidget::set_active_pattern(Pattern* pattern)
{
    if (m_active_pattern == pattern)
        return;

    m_active_pattern = pattern;
}
