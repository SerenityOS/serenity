/*
 * Copyright (c) 2020, the SerenityOS developers.
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "BoardView.h"
#include <LibGUI/Painter.h>
#include <LibGfx/Font/Font.h>
#include <LibGfx/Font/FontDatabase.h>
#include <LibGfx/Palette.h>

BoardView::BoardView(Game::Board const* board)
    : m_board(board)
{
}

void BoardView::set_board(Game::Board const* board)
{
    if (has_timer())
        stop_timer();

    slide_animation_frame = 0;
    pop_in_animation_frame = 0;
    start_timer(frame_duration_ms);

    if (m_board == board)
        return;

    if (!board) {
        m_board = nullptr;
        return;
    }

    bool must_resize = !m_board || m_board->tiles().size() != board->tiles().size();

    m_board = board;

    if (must_resize)
        resize();

    update();
}

void BoardView::pick_font()
{
    String best_font_name;
    int best_font_size = -1;
    auto& font_database = Gfx::FontDatabase::the();
    font_database.for_each_font([&](Gfx::Font const& font) {
        if (font.family() != "Liza" || font.weight() != 700)
            return;
        auto size = font.pixel_size_rounded_up();
        if (size * 2 <= m_cell_size && size > best_font_size) {
            best_font_name = font.qualified_name();
            best_font_size = size;
        }
    });

    if (best_font_name.is_empty()) {
        dbgln("Failed to find a good font for size {}, using the default font", m_cell_size / 2);
        best_font_name = font_database.default_font().qualified_name();
    }
    auto const font = font_database.get_by_name(best_font_name);
    set_font(font);

    m_min_cell_size = font->pixel_size_rounded_up();
}

size_t BoardView::rows() const
{
    if (!m_board)
        return 0;
    return m_board->tiles().size();
}

size_t BoardView::columns() const
{
    if (!m_board)
        return 0;
    if (m_board->tiles().is_empty())
        return 0;
    return m_board->tiles()[0].size();
}

void BoardView::resize_event(GUI::ResizeEvent&)
{
    resize();
}

void BoardView::resize()
{
    constexpr float padding_ratio = 7;
    m_padding = min(
        width() / (columns() * (padding_ratio + 1) + 1),
        height() / (rows() * (padding_ratio + 1) + 1));
    m_cell_size = m_padding * padding_ratio;

    pick_font();
}

void BoardView::keydown_event(GUI::KeyEvent& event)
{
    if (!on_move) {
        event.ignore();
        return;
    }

    switch (event.key()) {
    case KeyCode::Key_A:
    case KeyCode::Key_Left:
        on_move(Game::Direction::Left);
        return;
    case KeyCode::Key_D:
    case KeyCode::Key_Right:
        on_move(Game::Direction::Right);
        return;
    case KeyCode::Key_W:
    case KeyCode::Key_Up:
        on_move(Game::Direction::Up);
        return;
    case KeyCode::Key_S:
    case KeyCode::Key_Down:
        on_move(Game::Direction::Down);
        return;
    default:
        break;
    }

    event.ignore();
}

Gfx::Color BoardView::background_color_for_cell(u32 value)
{
    switch (value) {
    case 0:
        return Color::from_rgb(0xcdc1b4);
    case 2:
        return Color::from_rgb(0xeee4da);
    case 4:
        return Color::from_rgb(0xede0c8);
    case 8:
        return Color::from_rgb(0xf2b179);
    case 16:
        return Color::from_rgb(0xf59563);
    case 32:
        return Color::from_rgb(0xf67c5f);
    case 64:
        return Color::from_rgb(0xf65e3b);
    case 128:
        return Color::from_rgb(0xedcf72);
    case 256:
        return Color::from_rgb(0xedcc61);
    case 512:
        return Color::from_rgb(0xedc850);
    case 1024:
        return Color::from_rgb(0xedc53f);
    case 2048:
        return Color::from_rgb(0xedc22e);
    default:
        VERIFY(value > 2048);
        return Color::from_rgb(0x3c3a32);
    }
}

Gfx::Color BoardView::text_color_for_cell(u32 value)
{
    if (value <= 4)
        return Color::from_rgb(0x776e65);
    return Color::from_rgb(0xf9f6f2);
}

void BoardView::timer_event(Core::TimerEvent&)
{
    if (slide_animation_frame < animation_duration) {
        slide_animation_frame++;
        update();
    } else if (pop_in_animation_frame < animation_duration) {
        pop_in_animation_frame++;
        update();
        if (pop_in_animation_frame == animation_duration)
            stop_timer();
    }
}

void BoardView::paint_event(GUI::PaintEvent& event)
{
    Frame::paint_event(event);

    Color background_color = Color::from_rgb(0xbbada0);

    GUI::Painter painter(*this);
    painter.add_clip_rect(event.rect());
    painter.add_clip_rect(frame_inner_rect());
    painter.translate(frame_thickness(), frame_thickness());

    if (!m_board) {
        painter.fill_rect(rect(), background_color);
        return;
    }
    auto& tiles = m_board->tiles();

    Gfx::IntRect field_rect {
        0,
        0,
        static_cast<int>(m_padding + (m_cell_size + m_padding) * columns()),
        static_cast<int>(m_padding + (m_cell_size + m_padding) * rows())
    };
    field_rect.center_within(rect());
    painter.fill_rect(field_rect, background_color);

    auto tile_center = [&](size_t row, size_t column) {
        return Gfx::IntPoint {
            field_rect.x() + m_padding + (m_cell_size + m_padding) * column + m_cell_size / 2,
            field_rect.y() + m_padding + (m_cell_size + m_padding) * row + m_cell_size / 2,
        };
    };

    if (slide_animation_frame < animation_duration) {
        // background
        for (size_t column = 0; column < columns(); ++column) {
            for (size_t row = 0; row < rows(); ++row) {
                auto center = tile_center(row, column);
                auto tile_size = Gfx::IntSize { m_cell_size, m_cell_size };
                auto rect = Gfx::IntRect::centered_on(center, tile_size);
                painter.fill_rect(rect, background_color_for_cell(0));
            }
        }

        for (auto& sliding_tile : m_board->sliding_tiles()) {
            auto center_from = tile_center(sliding_tile.row_from, sliding_tile.column_from);
            auto center_to = tile_center(sliding_tile.row_to, sliding_tile.column_to);
            auto offset = Gfx::FloatPoint(center_to - center_from);
            auto center = center_from + Gfx::IntPoint(offset * (slide_animation_frame / (float)animation_duration));

            auto tile_size = Gfx::IntSize { m_cell_size, m_cell_size };
            auto rect = Gfx::IntRect::centered_on(center, tile_size);

            painter.fill_rect(rect, background_color_for_cell(sliding_tile.value_from));
            painter.draw_text(rect, String::number(sliding_tile.value_from), font(), Gfx::TextAlignment::Center, text_color_for_cell(sliding_tile.value_from));
        }
    } else {
        for (size_t column = 0; column < columns(); ++column) {
            for (size_t row = 0; row < rows(); ++row) {
                auto center = tile_center(row, column);
                auto tile_size = Gfx::IntSize { m_cell_size, m_cell_size };
                if (pop_in_animation_frame < animation_duration && Game::Board::Position { row, column } == m_board->last_added_position()) {
                    float pop_in_size = m_min_cell_size + (m_cell_size - m_min_cell_size) * (pop_in_animation_frame / (float)animation_duration);
                    tile_size = Gfx::IntSize { pop_in_size, pop_in_size };
                }
                auto rect = Gfx::IntRect::centered_on(center, tile_size);
                auto entry = tiles[row][column];
                painter.fill_rect(rect, background_color_for_cell(entry));
                if (entry > 0)
                    painter.draw_text(rect, String::number(entry), font(), Gfx::TextAlignment::Center, text_color_for_cell(entry));
            }
        }
    }
}
