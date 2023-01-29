/*
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "BoardWidget.h"

#include <LibGUI/Menu.h>
#include <LibGUI/Painter.h>
#include <LibGfx/Palette.h>

BoardWidget::BoardWidget(size_t rows, size_t columns)
    : m_board(make<Board>(rows, columns))
{
    update_color_scheme();
}

void BoardWidget::update_color_scheme()
{
    auto const& palette = GUI::Widget::palette();
    m_board->set_color_scheme({
        palette.bright_black(),
        palette.bright_red(),
        palette.bright_green(),
        palette.bright_yellow(),
        palette.bright_blue(),
        palette.bright_magenta(),
        palette.bright_cyan(),
        palette.bright_white(),
    });
    m_background_color = palette.background();
}

void BoardWidget::resize_board(size_t rows, size_t columns)
{
    if (columns == m_board->columns() && rows == m_board->rows())
        return;
    m_board->resize(rows, columns);
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

void BoardWidget::event(Core::Event& event)
{
    if (event.type() == GUI::Event::ThemeChange)
        update_color_scheme();
    GUI::Frame::event(event);
}

void BoardWidget::paint_event(GUI::PaintEvent& event)
{
    GUI::Widget::paint_event(event);

    GUI::Painter painter(*this);
    painter.add_clip_rect(event.rect());
    painter.fill_rect(event.rect(), m_background_color);

    int cell_size = get_cell_size();
    Gfx::IntSize board_offset = get_board_offset();

    for (size_t row = 0; row < m_board->rows(); ++row) {
        for (size_t column = 0; column < m_board->columns(); ++column) {
            int cell_x = column * cell_size + board_offset.width();
            int cell_y = row * cell_size + board_offset.height();

            Gfx::Rect cell_rect(cell_x, cell_y, cell_size, cell_size);
            Color fill_color = m_board->get_color_scheme()[m_board->cell(row, column)];
            painter.fill_rect(cell_rect, fill_color);
        }
    }
}

void BoardWidget::mousedown_event(GUI::MouseEvent& event)
{
    if (event.button() == GUI::MouseButton::Primary && on_move) {
        auto row_and_column = get_row_and_column_for_point(event.x(), event.y());
        if (!row_and_column.has_value())
            return;
        on_move(row_and_column.value());
    }
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
