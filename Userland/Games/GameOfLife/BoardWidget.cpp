/*
 * Copyright (c) 2021, Andres Crucitti <dasc495@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "BoardWidget.h"
#include <LibGUI/Painter.h>

BoardWidget::BoardWidget(size_t rows, size_t columns)
{
    m_timer = add<Core::Timer>();
    m_timer->stop();
    m_timer->on_timeout = [this] {
        run_generation();
    };
    m_timer->set_interval(m_running_timer_interval);

    update_board(rows, columns);
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

void BoardWidget::update_board(size_t rows, size_t columns)
{
    set_running(false);

    m_last_cell_toggled = columns * rows;

    if (m_board) {
        if (columns == m_board->columns() && rows == m_board->rows()) {
            return;
        }
    }

    m_board = make<Board>(rows, columns);
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

void BoardWidget::toggle_cell(size_t index)
{
    if (m_running || !m_toggling_cells || m_last_cell_toggled == index)
        return;

    m_last_cell_toggled = index;
    m_board->toggle_cell(index);

    if (on_cell_toggled)
        on_cell_toggled(m_board, index);

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
    size_t index = get_index_for_point(event.x(), event.y());
    set_toggling_cells(true);
    toggle_cell(index);
}

void BoardWidget::mousemove_event(GUI::MouseEvent& event)
{
    size_t index = get_index_for_point(event.x(), event.y());
    if (is_toggling()) {
        if (last_toggled() != index)
            toggle_cell(index);
    }
}

void BoardWidget::mouseup_event(GUI::MouseEvent&)
{
    set_toggling_cells(false);
}

size_t BoardWidget::get_index_for_point(int x, int y) const
{
    int cell_size = get_cell_size();
    Gfx::IntSize board_offset = get_board_offset();
    return m_board->columns() * ((y - board_offset.height()) / cell_size) + (x - board_offset.width()) / cell_size;
}
