/*
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "SudokuWidget.h"
#include "Board.h"
#include <Kernel/API/KeyCode.h>
#include <LibGUI/Painter.h>
#include <LibGUI/Window.h>
#include <LibGfx/Color.h>
#include <LibGfx/Font.h>
#include <LibGfx/FontDatabase.h>

SudokuWidget::SudokuWidget() { }

void SudokuWidget::paint_event(GUI::PaintEvent& event)
{
    auto const min_size = min(width(), height());
    auto const widget_offset_x = (window()->width() - min_size) / 2;
    auto const widget_offset_y = (window()->height() - min_size) / 2;

    GUI::Frame::paint_event(event);

    GUI::Painter painter(*this);
    painter.add_clip_rect(event.rect());

    painter.fill_rect({ 0, 0, width(), height() }, Color::Black);

    painter.translate(frame_thickness() + widget_offset_x,
        frame_thickness() + widget_offset_y);

    if (m_board) {

        auto board_dimension = m_board->dimension();

        auto tile_size = Gfx::IntSize { m_cell_size * 0.97f, m_cell_size * 0.97f };
        for (size_t x = 0; x < board_dimension; x++) {
            for (size_t y = 0; y < board_dimension; y++) {

                // Create subsquare borders
                if (x % 3 == 0 && y % 3 == 0) {
                    auto sub_square_size = Gfx::IntSize { m_cell_size * 2.97f, m_cell_size * 2.97f };
                    Gfx::IntPoint top_left_corner_of_sub_square = { m_cell_size * x,
                        m_cell_size * y };
                    Gfx::IntRect sub_square_rect = { top_left_corner_of_sub_square, sub_square_size };
                    painter.fill_rect(sub_square_rect, Color::LightGray);
                }

                // Paint tiles
                // FIXME: Colors should be changeable. Also text isn't very visible
                // against the active tile shading
                Gfx::IntPoint tile_location = { m_cell_size * x, m_cell_size * y };
                Gfx::IntRect tile_rect = { tile_location, tile_size };
                Square* square = m_board->get_square(x, y);
                Color square_color = Color::White;
                if (square == m_active_square)
                    square_color = Color::LightGray;
                painter.fill_rect(tile_rect, square_color);

                Color text_color = Color::WarmGray;
                if (square->get_value()) {
                    if (square->is_fixed())
                        text_color = Color::Black;
                    painter.draw_text(tile_rect, String::number(square->get_value()),
                        font(), Gfx::TextAlignment::Center, text_color);
                }
            }
        };
    };
}

void SudokuWidget::resize_event(GUI::ResizeEvent&)
{
    if (m_board) {
        auto board_dimension = m_board->dimension();
        m_cell_size = min(width() / (board_dimension), height() / (board_dimension));
        pick_font();
    }
}

void SudokuWidget::mousedown_event(GUI::MouseEvent& event)
{
    if (!frame_inner_rect().contains(event.position()))
        return;

    Square* square = mouse_to_square(event);
    if (m_active_square == square)
        m_active_square = nullptr;
    else
        m_active_square = square;

    update();
}

void SudokuWidget::keydown_event(GUI::KeyEvent& event)
{
    if (event.code_point() >= '1' && event.code_point() <= '9') {
        u32 digit = event.code_point() - '0';
        m_active_square->set_value(digit);
        if (m_board->is_board_solved())
            on_win();

        update();
        return;
    }

    switch (event.key()) {
    case KeyCode::Key_Delete:
    case KeyCode::Key_Backspace:
        m_active_square->set_value(0);
        update();
        break;
    case KeyCode::Key_A:
    case KeyCode::Key_Left:
        move_active_square(-1, 0);
        update();
        break;
    case KeyCode::Key_D:
    case KeyCode::Key_Right:
        move_active_square(1, 0);
        update();
        break;
    case KeyCode::Key_W:
    case KeyCode::Key_Up:
        move_active_square(0, -1);
        update();
        break;
    case KeyCode::Key_S:
    case KeyCode::Key_Down:
        move_active_square(0, 1);
        update();
        break;
    default:
        break;
    }
}

void SudokuWidget::pick_font()
{
    String best_font_name;
    int best_font_size = -1;
    auto& font_database = Gfx::FontDatabase::the();
    font_database.for_each_font([&](Gfx::Font const& font) {
        if (font.family() != "Liza" || font.weight() != 700)
            return;
        auto size = font.glyph_height();
        if (size * 2 <= m_cell_size && size > best_font_size) {
            best_font_name = font.qualified_name();
            best_font_size = size;
        }
    });

    auto font = font_database.get_by_name(best_font_name);
    set_font(font);

    m_min_cell_size = best_font_size;
}

void SudokuWidget::set_board(Board* board) { m_board = board; }

void SudokuWidget::new_game()
{
    m_board->new_game();
    repaint();
}

Square* SudokuWidget::mouse_to_square(GUI::MouseEvent& event)
{
    auto min_size = min(width(), height());
    auto widget_offset_x = (window()->width() - min_size) / 2;
    auto widget_offset_y = (window()->height() - min_size) / 2;

    auto x = ((event.x() - widget_offset_x) / m_cell_size);
    auto y = ((event.y() - widget_offset_y) / m_cell_size);
    auto dimension = (int)m_board->dimension();

    if (x >= dimension || x < 0 || y >= dimension || y < 0)
        return nullptr;

    return m_board->get_square(x, y);
}

void SudokuWidget::move_active_square(int x, int y)
{
    if (!m_active_square) {
        m_active_square = m_board->get_square(0, 0);
        return;
    }
    auto current_x = m_active_square->get_x();
    auto current_y = m_active_square->get_y();

    auto new_x = current_x + x;
    auto new_y = current_y + y;
    auto dimension = (int)m_board->dimension();

    if (new_x < 0 || new_y < 0 || new_x >= dimension || new_y >= dimension)
        return;

    m_active_square = m_board->get_square(new_x, new_y);
}
