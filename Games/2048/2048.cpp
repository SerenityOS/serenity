/*
 * Copyright (c) 2020, the SerenityOS developers.
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

#include "2048.h"
#include <LibCore/ConfigFile.h>
#include <LibGUI/FontDatabase.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/Painter.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/Font.h>
#include <LibGfx/Palette.h>
#include <stdlib.h>
#include <time.h>

TwentyFortyEightGame::TwentyFortyEightGame()
{
    set_font(GUI::FontDatabase::the().get_by_name("Liza Regular"));
    srand(time(nullptr));
    reset();
}

TwentyFortyEightGame::~TwentyFortyEightGame()
{
}

template<typename Board>
void TwentyFortyEightGame::add_tile(Board& board, int max_tile_value)
{
    int row;
    int column;
    do {
        row = rand() % m_rows;
        column = rand() % m_columns;
    } while (board[row][column] != 0);

    int value = rand() % max_tile_value;
    value = round_up_to_power_of_two(value, max_tile_value);
    board[row][column] = max(2, value);
}

void TwentyFortyEightGame::reset()
{
    auto initial_state = [&]() -> State {
        State state;
        state.board.resize(m_columns);
        auto& board = state.board;
        for (auto& row : board) {
            row.resize(m_rows);
            for (auto& j : row)
                j = 0;
        }

        add_tile(state.board, m_starting_tile);
        add_tile(state.board, m_starting_tile);

        return state;
    };

    m_states.clear();
    m_states.append(initial_state());

    m_current_turn = 0;
    m_states.last().score_text = "Score: 0";

    update();
}

Gfx::IntRect TwentyFortyEightGame::score_rect() const
{
    int score_width = font().width(m_states.last().score_text);
    return { 0, 2, score_width, font().glyph_height() };
}

static Vector<Vector<u32>> transpose(const Vector<Vector<u32>>& board)
{
    Vector<Vector<u32>> new_board;
    auto result_row_count = board[0].size();
    auto result_column_count = board.size();

    new_board.resize(result_row_count);

    for (size_t i = 0; i < board.size(); ++i) {
        auto& row = new_board[i];
        row.clear_with_capacity();
        row.ensure_capacity(result_column_count);
        for (auto& entry : board) {
            row.append(entry[i]);
        }
    }

    return new_board;
}

static Vector<Vector<u32>> reverse(const Vector<Vector<u32>>& board)
{
    auto new_board = board;
    for (auto& row : new_board) {
        for (size_t i = 0; i < row.size() / 2; ++i)
            swap(row[i], row[row.size() - i - 1]);
    }

    return new_board;
}

static Vector<u32> slide_row(const Vector<u32>& row, size_t& successful_merge_score)
{
    if (row.size() < 2)
        return row;

    auto x = row[0];
    auto y = row[1];

    auto result = row;
    result.take_first();

    if (x == 0) {
        result = slide_row(result, successful_merge_score);
        result.append(0);
        return result;
    }

    if (y == 0) {
        result[0] = x;
        result = slide_row(result, successful_merge_score);
        result.append(0);
        return result;
    }

    if (x == y) {
        result.take_first();
        result = slide_row(result, successful_merge_score);
        result.append(0);
        result.prepend(x + x);
        successful_merge_score += x * 2;
        return result;
    }

    result = slide_row(result, successful_merge_score);
    result.prepend(x);
    return result;
}

static Vector<Vector<u32>> slide_left(const Vector<Vector<u32>>& board, size_t& successful_merge_score)
{
    Vector<Vector<u32>> new_board;
    for (auto& row : board)
        new_board.append(slide_row(row, successful_merge_score));

    return new_board;
}

static bool is_complete(const TwentyFortyEightGame::State& state)
{
    for (auto& row : state.board) {
        if (row.contains_slow(2048))
            return true;
    }

    return false;
}

static bool has_no_neighbors(const Span<const u32>& row)
{
    if (row.size() < 2)
        return true;

    auto x = row[0];
    auto y = row[1];

    if (x == y)
        return false;

    return has_no_neighbors(row.slice(1, row.size() - 1));
};

static bool is_stalled(const TwentyFortyEightGame::State& state)
{
    static auto stalled = [](auto& row) {
        return !row.contains_slow(0) && has_no_neighbors(row.span());
    };

    for (auto& row : state.board)
        if (!stalled(row))
            return false;

    for (auto& row : transpose(state.board))
        if (!stalled(row))
            return false;

    return true;
}

void TwentyFortyEightGame::keydown_event(GUI::KeyEvent& event)
{
    auto& previous_state = m_states.last();
    State new_state;
    size_t successful_merge_score = 0;
    switch (event.key()) {
    case KeyCode::Key_A:
    case KeyCode::Key_Left:
        new_state.board = transpose(slide_left(transpose(previous_state.board), successful_merge_score));
        break;
    case KeyCode::Key_D:
    case KeyCode::Key_Right:
        new_state.board = transpose(reverse(slide_left(reverse(transpose(previous_state.board)), successful_merge_score)));
        break;
    case KeyCode::Key_W:
    case KeyCode::Key_Up:
        new_state.board = slide_left(previous_state.board, successful_merge_score);
        break;
    case KeyCode::Key_S:
    case KeyCode::Key_Down:
        new_state.board = reverse(slide_left(reverse(previous_state.board), successful_merge_score));
        break;
    case KeyCode::Key_U:
    case KeyCode::Key_Backspace:
        if (m_states.size() > 1) {
            m_states.take_last();
            update();
        } else {
            return;
        }
    default:
        return;
    }

    if (new_state.board != previous_state.board) {
        ++m_current_turn;
        add_tile(new_state.board, m_starting_tile * 2);
        auto last_score = m_states.last().score;
        if (m_states.size() == 16)
            m_states.take_first();
        m_states.append(move(new_state));

        m_states.last().score = last_score + successful_merge_score;
        m_states.last().score_text = String::format("Score: %d", score());

        update();
    }

    if (is_complete(m_states.last())) {
        // You won!
        GUI::MessageBox::show(window(),
            String::format("Score = %d in %zu turns", score(), m_current_turn),
            "You won!",
            GUI::MessageBox::Type::Information);
        return game_over();
    }

    if (is_stalled(m_states.last())) {
        // Game over!
        GUI::MessageBox::show(window(),
            String::format("Score = %d in %zu turns", score(), m_current_turn),
            "You lost!",
            GUI::MessageBox::Type::Information);
        return game_over();
    }
}

void TwentyFortyEightGame::paint_event(GUI::PaintEvent&)
{
    static auto color_for_entry = [&](u32 entry) -> Color {
        constexpr static u8 blend_alpha = 128;
        switch (entry) {
        case 0:
            return palette().base().lightened();
        case 2:
            return palette().base().blend(Color(Color::LightGray).with_alpha(blend_alpha));
        case 4:
            return palette().base().blend(Color(Color::WarmGray).with_alpha(blend_alpha));
        case 8:
            return palette().base().blend(Color(Color::MidMagenta).with_alpha(blend_alpha));
        case 16:
            return palette().base().blend(Color(Color::Magenta).with_alpha(blend_alpha));
        case 32:
            return palette().base().blend(Color(Color::Cyan).with_alpha(blend_alpha));
        case 64:
            return palette().base().blend(Color(Color::DarkCyan).with_alpha(blend_alpha));
        case 128:
            return palette().base().blend(Color(Color::MidBlue).with_alpha(blend_alpha));
        case 256:
            return palette().base().blend(Color(Color::Blue).with_alpha(blend_alpha));
        case 512:
            return palette().base().blend(Color(Color::DarkBlue).with_alpha(blend_alpha));
        case 1024:
            return palette().base().blend(Color(Color::Yellow).with_alpha(blend_alpha));
        case 2048:
            return palette().base().blend(Color(Color::Green).with_alpha(blend_alpha));
        default:
            ASSERT_NOT_REACHED();
        }
    };

    GUI::Painter painter(*this);

    painter.draw_text(score_rect(), m_states.last().score_text, font(), Gfx::TextAlignment::TopLeft, palette().color(ColorRole::BaseText));

    painter.translate(0, font().glyph_height() + 2);

    constexpr size_t column_padding = 2, row_padding = 2;
    size_t column_offset = column_padding, row_offset = row_padding;
    float column_size = (height() - font().glyph_height() - 2 - column_padding) / m_columns, row_size = (width() - 2 * row_padding) / m_rows;

    for (auto column = 0; column < m_columns; ++column) {
        for (auto row = 0; row < m_rows; ++row) {
            auto rect = Gfx::IntRect(column_offset, row_offset, column_size - column_padding, row_size - row_padding);
            painter.draw_rect(rect, Color::White);
            auto entry = m_states.last().board[row][column];
            painter.fill_rect(rect.shrunken(1, 1), color_for_entry(entry));
            if (entry > 0)
                painter.draw_text(rect, String::number(entry), font(), Gfx::TextAlignment::Center, palette().color(ColorRole::BaseText));

            column_offset += column_size;
        }
        column_offset = column_padding;
        row_offset += row_size;
    }
}

void TwentyFortyEightGame::game_over()
{
    reset();
}

int TwentyFortyEightGame::score() const
{
    return m_states.last().score;
}
