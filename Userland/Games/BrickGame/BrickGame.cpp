/*
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "BrickGame.h"
#include <AK/Random.h>
#include <LibConfig/Client.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/Painter.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/Font/FontDatabase.h>
#include <LibGfx/Point.h>

using Position = Gfx::Point<int>;

class Well final {
public:
    using Row = u32;

    Well()
    {
        reset();
    }

    [[nodiscard]] static constexpr size_t number_of_columns() { return 10; }

    [[nodiscard]] static constexpr size_t number_of_rows() { return top_margin() + 18 + bottom_margin(); }

    [[nodiscard]] static constexpr size_t left_margin() { return margin_left; }

    [[nodiscard]] static constexpr size_t top_margin() { return margin_top; }

    [[nodiscard]] static constexpr size_t bottom_margin() { return margin_bottom; }

    [[nodiscard]] Row operator[](size_t i) const { return m_rows[i]; }

    Row& operator[](size_t i) { return m_rows[i]; }

    [[nodiscard]] bool operator[](Position pos) const
    {
        return (m_rows[pos.y()] & (1 << (31 - pos.x()))) != 0;
    }

    void reset()
    {
        auto const rows = number_of_rows();
        for (size_t row_index = 0; row_index < rows; ++row_index)
            m_rows[row_index] = s_empty_row;

        for (size_t row_index = rows - margin_bottom; row_index < rows; ++row_index)
            m_rows[row_index] = s_full_row;
    }

    size_t check_and_remove_full_rows()
    {
        size_t number_of_removed_rows {};
        auto current = int(number_of_rows() - bottom_margin());
        for (auto row { current - 1 }; row >= 0; --row) {
            if (m_rows[row] == s_full_row) {
                number_of_removed_rows += 1;
                continue;
            }
            m_rows[--current] = m_rows[row];
        }
        for (; current >= 0; --current)
            m_rows[current] = s_empty_row;
        return number_of_removed_rows;
    }

private:
    static constexpr size_t column_count = 10;
    static constexpr size_t row_count = 18;

    static constexpr size_t margin_left = 4;
    static constexpr size_t margin_top = 1;
    static constexpr size_t margin_right = 32 - margin_left - column_count;
    static constexpr size_t margin_bottom = 4;

    static constexpr size_t total_row_count = row_count + margin_top + margin_bottom;

    // empty row looks like 0b1111'0000'0000'0011'1111'1111'1111'1111
    // note, we have margin on both sides to implement collision checking for
    // block shapes.
    static constexpr Row s_empty_row = ~((~(~0u << column_count)) << margin_right);

    // full row looks like 0b1111'1111'1111'1111'1111'1111'1111'1111
    static constexpr Row s_full_row = ~0;

    // A well is an array of rows, each row has 32 columns, each column is represented as a bit in the u32.
    // First column has index 0, it is the most significant bit in the u32.
    // An empty cell in the row is represented as a zero bit.
    // For convenience of testing of block-wall collisions the well starts at the non-zero margin
    // from top, left, right and bottom, i.e. it is surrounded with walls of specified width/height (margin).
    // Note, that block-well collision testing is a simple and fast 'and' bit operation of well row bit contents and
    // the shape row bits contents.
    Array<Row, total_row_count> m_rows {}; // 0 is the topmost row in the well
};

class Block final {
public:
    static constexpr size_t shape_size = 4;

    Block() = default;
    Block(Block const&) = default;
    Block& operator=(Block const&) = default;

    Block& rotate_left()
    {
        m_rotation = m_rotation == 0 ? number_of_rotations - 1 : m_rotation - 1;
        return *this;
    }

    Block& rotate_right()
    {
        m_rotation = m_rotation == number_of_rotations - 1 ? 0 : m_rotation + 1;
        return *this;
    }

    Block& move_left()
    {
        m_position = m_position.moved_left(1);
        return *this;
    }

    Block& move_right()
    {
        m_position = m_position.moved_right(1);
        return *this;
    }

    Block& move_down()
    {
        m_position = m_position.moved_down(1);
        return *this;
    }

    Block& move_to(Position pos)
    {
        m_position = pos;
        return *this;
    }

    Block& random_shape()
    {
        m_shape = get_random_uniform(number_of_shapes);
        m_rotation = 0;
        m_position = { 6, 0 };
        return *this;
    }

    [[nodiscard]] bool operator[](Position pos) const
    {
        return (block_row(pos.y() - m_position.y()) & (1 << (31 - pos.x()))) != 0;
    }

    [[nodiscard]] bool has_collision(Well const& well) const
    {
        for (size_t start_row = 0; start_row < shape_size; ++start_row) {
            auto const row_index = start_row + m_position.y();
            if (row_index >= Well::number_of_rows() || (well[row_index] & block_row(start_row)) != 0)
                return true;
        }
        return false;
    }

    void place_into(Well& well)
    {
        for (size_t row_index = 0; row_index < shape_size; ++row_index)
            well[m_position.y() + row_index] |= block_row(row_index);
    }

    [[nodiscard]] bool dot_at(Position position) const
    {
        return ((shape_data_at(position.y()) & (1 << (3 - position.x()))) != 0);
    }

private:
    static constexpr u8 number_of_shapes = 7;
    static constexpr u8 number_of_rotations = 4;

    using Shape = u16;
    using Row = u32;

    // Each shape is stored in one u16, each nibble is representing one shape row, highest nibble being the first row.
    // Every shape has 4x4 dimension and there are 4 possible shape rotations.
    static constexpr Shape s_shapes[number_of_shapes][number_of_rotations] = {
        // Shape: I
        { 0b0000'1111'0000'0000, 0b0010'0010'0010'0010, 0b0000'1111'0000'0000,
            0b0010'0010'0010'0010 },

        // Shape: J
        { 0b0000'0111'0001'0000, 0b0001'0001'0011'0000, 0b0000'0100'0111'0000,
            0b0011'0010'0010'0000 },

        // Shape: L
        { 0b0000'0111'0100'0000, 0b0110'0010'0010'0000, 0b0000'0001'0111'0000,
            0b0010'0010'0011'0000 },

        // Shape: O
        { 0b0000'0110'0110'0000, 0b0000'0110'0110'0000, 0b0000'0110'0110'0000,
            0b0000'0110'0110'0000 },

        // Shape: S
        { 0b0000'0011'0110'0000, 0b0100'0110'0010'0000, 0b0000'0011'0110'0000,
            0b0100'0110'0010'0000 },

        // Shape: T
        { 0b0000'0111'0010'0000, 0b0001'0011'0001'0000, 0b0000'0010'0111'0000,
            0b0100'0110'0100'0000 },

        // Shape: Z
        { 0b0000'0110'0011'0000, 0b0001'0011'0010'0000, 0b0000'0110'0011'0000,
            0b0001'0011'0010'0000 }
    };

    Position m_position {};
    u8 m_rotation {};
    u8 m_shape {};

    [[nodiscard]] Row block_row(size_t row) const
    {
        return shape_data_at(row) << (32 - m_position.x() - shape_size);
    }

    [[nodiscard]] Row shape_data_at(size_t row) const
    {
        switch (row) {
        case 0:
            return Row((s_shapes[m_shape][m_rotation] >> 12) & 0xf);
        case 1:
            return Row((s_shapes[m_shape][m_rotation] >> 8) & 0xf);
        case 2:
            return Row((s_shapes[m_shape][m_rotation] >> 4) & 0xf);
        case 3:
            return Row(s_shapes[m_shape][m_rotation] & 0xf);
        default:
            return Row {};
        }
    }
};

class Bricks final {

public:
    enum class GameState {
        Active,
        Paused,
        GameOver
    };

    // Game will request a UI update when any of these events occur:
    // - score changes
    // - level changes
    // - current block position or rotation changes
    // - any well row(s) state change
    enum class RenderRequest {
        SkipRender,
        RequestUpdate
    };

    Bricks() { reset(); }

    [[nodiscard]] unsigned score() const { return m_score; }

    [[nodiscard]] unsigned level() const { return m_level; }

    [[nodiscard]] GameState state() const { return m_state; }

    void add_new_block()
    {
        m_block = m_next_block;
        m_next_block.random_shape();
        m_state = m_block.has_collision(m_well) ? GameState::GameOver : GameState::Active;
    }

    [[nodiscard]] Block const& next_block() const { return m_next_block; }

    [[nodiscard]] BrickGame::BoardSpace operator[](Position pos) const
    {
        if (m_well[pos] || m_block[pos])
            return BrickGame::BoardSpace::FullyOn;
        if (m_shadow_hint_block[pos])
            return BrickGame::BoardSpace::ShadowHint;
        return BrickGame::BoardSpace::Off;
    }

    [[nodiscard]] RenderRequest rotate_left() { return set_current_block(Block(m_block).rotate_left()); }

    [[nodiscard]] RenderRequest rotate_right() { return set_current_block(Block(m_block).rotate_right()); }

    [[nodiscard]] RenderRequest move_left() { return set_current_block(Block(m_block).move_left()); }

    [[nodiscard]] RenderRequest move_right() { return set_current_block(Block(m_block).move_right()); }

    [[nodiscard]] RenderRequest move_down()
    {
        auto const block = Block(m_block).move_down();
        if (block.has_collision(m_well)) {
            m_block.place_into(m_well);
            check_and_remove_full_rows();
            add_new_block();
            update_shadow_hint_block();
            return RenderRequest::RequestUpdate;
        }
        m_block = block;
        update_shadow_hint_block();
        return RenderRequest::RequestUpdate;
    }

    [[nodiscard]] RenderRequest move_down_fast()
    {
        for (auto block = m_block;; block.move_down()) {
            if (block.has_collision(m_well)) {
                m_block.place_into(m_well);
                check_and_remove_full_rows();
                add_new_block();
                break;
            }
            m_block = block;
        }
        update_shadow_hint_block();
        return RenderRequest::RequestUpdate;
    }

    void update_shadow_hint_block()
    {
        for (auto block = m_block;; block.move_down()) {
            if (block.has_collision(m_well))
                return;
            m_shadow_hint_block = block;
        }
    }

    void toggle_pause()
    {
        switch (m_state) {
        case GameState::Active:
            m_state = GameState::Paused;
            break;
        case GameState::Paused:
            m_state = GameState::Active;
            break;
        case GameState::GameOver:
            break;
        }
    }

    [[nodiscard]] RenderRequest update()
    {
        auto const current_level { m_level };
        for (size_t i = 0; i < s_level_map.size(); i++) {
            if (m_score < s_level_map[i].m_score)
                break;
            m_level = i;
        }
        auto const now { UnixDateTime::now() };
        auto const delay = s_level_map[m_level].m_delay;
        if (now - m_last_update > delay) {
            m_last_update = now;
            return move_down();
        }
        return current_level == m_level ? RenderRequest::SkipRender : RenderRequest::RequestUpdate;
    }

    void reset()
    {
        m_level = 0;
        m_score = 0;
        m_well.reset();
        m_block.random_shape();
        m_next_block.random_shape();
        update_shadow_hint_block();
        m_last_update = UnixDateTime::now();
        m_state = GameState::Active;
    }

private:
    Well m_well {};
    Block m_block {};
    Block m_next_block {};
    Block m_shadow_hint_block {};
    unsigned m_level {};
    unsigned m_score {};
    GameState m_state { GameState::GameOver };
    // FIXME: Should probably use a monotonic clock instead.
    UnixDateTime m_last_update {};

    struct LevelMap final {
        unsigned const m_score;
        Duration const m_delay;
    };

    static constexpr Array<LevelMap, 14> s_level_map = {
        LevelMap { 0, Duration::from_milliseconds(38000 / 60) },
        LevelMap { 1000, Duration::from_milliseconds(34000 / 60) },
        LevelMap { 2000, Duration::from_milliseconds(29000 / 60) },
        LevelMap { 3000, Duration::from_milliseconds(25000 / 60) },
        LevelMap { 4000, Duration::from_milliseconds(22000 / 60) },
        LevelMap { 5000, Duration::from_milliseconds(18000 / 60) },
        LevelMap { 6000, Duration::from_milliseconds(15000 / 60) },
        LevelMap { 7000, Duration::from_milliseconds(11000 / 60) },
        LevelMap { 8000, Duration::from_milliseconds(7000 / 60) },
        LevelMap { 9000, Duration::from_milliseconds(5000 / 60) },
        LevelMap { 10000, Duration::from_milliseconds(4000 / 60) },
        LevelMap { 20000, Duration::from_milliseconds(3000 / 60) },
        LevelMap { 30000, Duration::from_milliseconds(2000 / 60) },
        LevelMap { 10000000, Duration::from_milliseconds(1000 / 60) }
    };

    [[nodiscard]] RenderRequest set_current_block(Block const& block)
    {
        if (!block.has_collision(m_well)) {
            m_block = block;
            update_shadow_hint_block();
            return RenderRequest::RequestUpdate;
        }
        return RenderRequest::SkipRender;
    }

    RenderRequest check_and_remove_full_rows()
    {
        auto const number_of_removed_rows { m_well.check_and_remove_full_rows() };
        switch (number_of_removed_rows) {
        case 0:
            return RenderRequest::SkipRender;
        case 1:
            m_score += 40 * (m_level + 1);
            break;
        case 2:
            m_score += 100 * (m_level + 1);
            break;
        case 3:
            m_score += 300 * (m_level + 1);
            break;
        case 4:
            m_score += 1200 * (m_level + 1);
            break;
        default:
            VERIFY_NOT_REACHED();
        }
        return RenderRequest::RequestUpdate;
    }
};

BrickGame::BrickGame(StringView app_name)
    : m_app_name { app_name }
    , m_state { GameState::Idle }
    , m_brick_game(make<Bricks>())
{
    set_font(Gfx::FontDatabase::default_fixed_width_font().bold_variant());
    m_high_score = Config::read_i32(m_app_name, m_app_name, "HighScore"sv, 0);
    reset();
}

void BrickGame::reset()
{
    m_state = GameState::Active;
    m_brick_game->reset();
    stop_timer();
    start_timer(15); // 66.6ms
    m_brick_game->add_new_block();
    // A new game must always succeed to start, otherwise it is not fun to play
    VERIFY(m_brick_game->state() == Bricks::GameState::Active);
    update();
}

void BrickGame::toggle_pause()
{
    m_brick_game->toggle_pause();
    update();
}

void BrickGame::set_show_shadow_hint(bool should_show)
{
    m_show_shadow_hint = should_show;
    repaint();
}

bool BrickGame::show_shadow_hint()
{
    return m_show_shadow_hint;
}

void BrickGame::timer_event(Core::TimerEvent&)
{
    switch (m_brick_game->state()) {
    case Bricks::GameState::GameOver:
        game_over();
        break;
    case Bricks::GameState::Active:
        if (m_brick_game->update() == Bricks::RenderRequest::RequestUpdate)
            update();
        break;
    case Bricks::GameState::Paused:
        break;
    }
}

void BrickGame::keydown_event(GUI::KeyEvent& event)
{
    switch (event.key()) {
    case KeyCode::Key_Escape:
    case KeyCode::Key_P:
        toggle_pause();
        return;
    default:
        break;
    }

    if (m_brick_game->state() == Bricks::GameState::Paused) {
        event.ignore();
        return;
    }

    Bricks::RenderRequest render_request { Bricks::RenderRequest::SkipRender };
    switch (event.key()) {
    case KeyCode::Key_A:
    case KeyCode::Key_H:
    case KeyCode::Key_Left:
        render_request = m_brick_game->move_left();
        break;
    case KeyCode::Key_D:
    case KeyCode::Key_L:
    case KeyCode::Key_Right:
        render_request = m_brick_game->move_right();
        break;
    case KeyCode::Key_W:
    case KeyCode::Key_K:
    case KeyCode::Key_Up:
        render_request = m_brick_game->rotate_right();
        break;
    case KeyCode::Key_E:
    case KeyCode::Key_Z:
        render_request = m_brick_game->rotate_left();
        break;
    case KeyCode::Key_S:
    case KeyCode::Key_Down:
        render_request = m_brick_game->move_down();
        break;
    case KeyCode::Key_Space:
        render_request = m_brick_game->move_down_fast();
        break;
    default:
        event.ignore();
        break;
    }
    if (render_request == Bricks::RenderRequest::RequestUpdate)
        update();
}

void BrickGame::paint_cell(GUI::Painter& painter, Gfx::IntRect rect, BrickGame::BoardSpace space)
{
    Color inside_color;
    Color outside_color;

    switch (space) {
    case BrickGame::BoardSpace::FullyOn:
        inside_color = m_front_color;
        outside_color = m_front_color;
        break;
    case BrickGame::BoardSpace::ShadowHint:
        inside_color = m_shadow_color;
        outside_color = m_show_shadow_hint ? m_hint_block_color : m_shadow_color;
        break;
    case BrickGame::BoardSpace::Off:
        inside_color = m_shadow_color;
        outside_color = m_shadow_color;
        break;
    }

    painter.draw_rect(rect, m_back_color);
    rect.inflate(-1, -1, -1, -1);
    painter.draw_rect(rect, outside_color);
    painter.set_pixel(rect.top_left(), m_back_color);
    painter.set_pixel(rect.bottom_left().moved_up(1), m_back_color);
    painter.set_pixel(rect.top_right().moved_left(1), m_back_color);
    painter.set_pixel(rect.bottom_right().translated(-1), m_back_color);
    rect.inflate(-2, -2);
    painter.draw_rect(rect, outside_color);
    rect.inflate(-2, -2);
    painter.draw_rect(rect, m_back_color);
    rect.inflate(-2, -2);
    painter.draw_rect(rect, m_back_color);
    rect.inflate(-2, -2);
    painter.fill_rect(rect, inside_color);
}

void BrickGame::paint_sidebar_text(GUI::Painter& painter, int row, StringView text)
{
    auto const text_width = font().width_rounded_up(text);
    auto const entire_area_rect { frame_inner_rect() };
    auto const margin = 4;
    auto const rect { Gfx::IntRect { entire_area_rect.x() + entire_area_rect.width() - 116,
        2 * margin + entire_area_rect.y() + (font().pixel_size_rounded_up() + margin) * row,
        text_width, font().pixel_size_rounded_up() } };
    painter.draw_text(rect, text, Gfx::TextAlignment::TopLeft, Color::Black);
}

void BrickGame::paint_paused_text(GUI::Painter& painter)
{
    auto const paused_text = "Paused"sv;
    auto const paused_text_width = font().width_rounded_up(paused_text);
    auto const more_or_less_font_height = static_cast<int>(font().pixel_size_rounded_up());
    auto const entire_area_rect { frame_inner_rect() };
    auto const margin = more_or_less_font_height * 2;

    auto pause_text_box = Gfx::IntRect({}, { paused_text_width + margin, more_or_less_font_height + margin }).centered_within(entire_area_rect);
    painter.fill_rect(pause_text_box, m_front_color);

    pause_text_box.inflate(-2, -2);
    painter.fill_rect(pause_text_box, m_back_color);

    painter.draw_text(frame_inner_rect(), paused_text, Gfx::TextAlignment::Center, Color::Black);
}

ErrorOr<void> BrickGame::paint_game(GUI::Painter& painter, Gfx::IntRect const& rect)
{
    painter.fill_rect(rect, m_back_color);
    if (m_state == GameState::Active) {
        // TODO: optimize repainting
        painter.draw_rect(rect.inflated(-4, -4), m_front_color);

        auto const entire_area_rect { frame_inner_rect() };
        Gfx::IntRect well_rect { entire_area_rect };
        well_rect.inflate(0, -120, 0, 0);
        well_rect.inflate(-4, -4);
        painter.draw_rect(well_rect, m_front_color);
        well_rect.inflate(-4, -4);

        auto const cell_size = Gfx::IntSize(well_rect.width() / Well::number_of_columns(), well_rect.height() / (Well::number_of_rows() - Well::top_margin() - Well::bottom_margin()));
        auto cell_rect = [&](Position pos) {
            return Gfx::IntRect {
                well_rect.x() + pos.x() * cell_size.width(),
                well_rect.y() + pos.y() * cell_size.height(),
                cell_size.width() - 1,
                cell_size.height() - 1
            };
        };

        auto const number_of_columns = int(Well::number_of_columns());
        auto const number_of_rows = int(Well::number_of_rows() - Well::top_margin() - Well::bottom_margin());
        for (int row = 0; row < number_of_rows; ++row)
            for (int col = 0; col < number_of_columns; ++col) {
                auto const position = Position { col, row };
                auto const board_position = position.translated(int(Well::left_margin()), int(Well::top_margin()));
                paint_cell(painter, cell_rect(position), (*m_brick_game)[board_position]);
            }

        paint_sidebar_text(painter, 0, TRY(String::formatted("Score: {}", m_brick_game->score())));
        paint_sidebar_text(painter, 1, TRY(String::formatted("Level: {}", m_brick_game->level())));
        paint_sidebar_text(painter, 4, TRY(String::formatted("Hi-Score: {}", m_high_score)));
        paint_sidebar_text(painter, 12, "Next:"sv);

        auto const hint_rect = Gfx::IntRect {
            frame_inner_rect().x() + frame_inner_rect().width() - 105,
            frame_inner_rect().y() + 200,
            int(cell_size.width() * Block::shape_size),
            int(cell_size.height() * Block::shape_size)
        };

        painter.draw_rect(hint_rect.inflated(4, 4), m_front_color);

        auto const dot_rect = Gfx::IntRect { hint_rect.x(), hint_rect.y(), cell_size.width() - 1, cell_size.height() - 1 };
        for (size_t y = 0; y < Block::shape_size; ++y)
            for (size_t x = 0; x < Block::shape_size; ++x)
                paint_cell(painter,
                    dot_rect.translated(int(x * cell_size.width()), int(y * cell_size.height())),
                    m_brick_game->next_block().dot_at({ x, y }) ? BrickGame::BoardSpace::FullyOn : BrickGame::BoardSpace::Off);

        if (m_brick_game->state() == Bricks::GameState::Paused)
            paint_paused_text(painter);
    }

    return {};
}

void BrickGame::paint_event(GUI::PaintEvent& event)
{
    GUI::Frame::paint_event(event);
    GUI::Painter painter(*this);
    painter.add_clip_rect(frame_inner_rect());
    painter.add_clip_rect(event.rect());
    paint_game(painter, frame_inner_rect()).release_value_but_fixme_should_propagate_errors();
}

void BrickGame::game_over()
{
    stop_timer();
    StringBuilder text;
    auto const current_score = m_brick_game->score();
    text.appendff("Your score was {}", current_score);
    if (current_score > m_high_score) {
        text.append("\nThat's a new high score!"sv);
        Config::write_i32(m_app_name, m_app_name, "HighScore"sv, int(m_high_score = current_score));
    }
    GUI::MessageBox::show(window(),
        text.string_view(),
        "Game Over"sv,
        GUI::MessageBox::Type::Information);

    reset();
}
