/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Mustafa Quraish <mustafa@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 * Copyright (c) 2023, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Game.h"
#include <AK/Random.h>
#include <LibConfig/Client.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/Painter.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/Font/Font.h>
#include <LibGfx/Font/FontDatabase.h>

namespace Snake {

ErrorOr<NonnullRefPtr<Game>> Game::try_create()
{
    static constexpr auto food_bitmaps_files = Array {
        "/res/emoji/U+1F41F.png"sv,
        "/res/emoji/U+1F95A.png"sv,
        "/res/emoji/U+1F99C.png"sv,
        "/res/emoji/U+1F986.png"sv,
        "/res/emoji/U+1FAB2.png"sv,
        "/res/emoji/U+1F426.png"sv,
        "/res/emoji/U+1F424.png"sv,
        "/res/emoji/U+1F40D.png"sv,
        "/res/emoji/U+1F989.png"sv,
        "/res/emoji/U+1F54A.png"sv,
        "/res/emoji/U+1F408.png"sv,
        "/res/emoji/U+1F420.png"sv,
        "/res/emoji/U+1F415.png"sv,
        "/res/emoji/U+1F429.png"sv,
        "/res/emoji/U+1F98C.png"sv,
        "/res/emoji/U+1F416.png"sv,
        "/res/emoji/U+1F401.png"sv,
        "/res/emoji/U+1F400.png"sv,
        "/res/emoji/U+1F407.png"sv,
        "/res/emoji/U+1F43F.png"sv,
        "/res/emoji/U+1F9A5.png"sv,
        "/res/emoji/U+1F423.png"sv,
        "/res/emoji/U+1F425.png"sv,
        "/res/emoji/U+1F98E.png"sv,
        "/res/emoji/U+1F997.png"sv,
        "/res/emoji/U+1FAB3.png"sv,
        "/res/emoji/U+1F413.png"sv,
        "/res/emoji/U+1FAB0.png"sv,
        "/res/emoji/U+1FAB1.png"sv,
    };

    Vector<NonnullRefPtr<Gfx::Bitmap>> food_bitmaps;
    TRY(food_bitmaps.try_ensure_capacity(food_bitmaps_files.size()));

    for (auto file : food_bitmaps_files) {
        auto bitmap = Gfx::Bitmap::load_from_file(file);
        if (bitmap.is_error()) {
            dbgln("\033[31;1mCould not load bitmap file\033[0m '{}': {}", file, bitmap.error());
            return bitmap.release_error();
        }

        food_bitmaps.unchecked_append(bitmap.release_value());
    }

    auto color = Color::from_argb(Config::read_u32("Snake"sv, "Snake"sv, "BaseColor"sv, Color(Color::Green).value()));
    auto skin_name = TRY(String::from_byte_string(Config::read_string("Snake"sv, "Snake"sv, "SnakeSkin"sv, "Snake"sv)));
    auto skin = TRY(SnakeSkin::create(skin_name, color));

    return adopt_nonnull_ref_or_enomem(new (nothrow) Game(move(food_bitmaps), color, skin_name, move(skin)));
}

Game::Game(Vector<NonnullRefPtr<Gfx::Bitmap>> food_bitmaps, Color snake_color, String snake_skin_name, NonnullOwnPtr<SnakeSkin> skin)
    : m_food_bitmaps(move(food_bitmaps))
    , m_snake_color(move(snake_color))
    , m_snake_skin_name(move(snake_skin_name))
    , m_snake_skin(move(skin))
{
    set_font(Gfx::FontDatabase::default_fixed_width_font().bold_variant());
    reset();
}

void Game::pause()
{
    stop_timer();
}

void Game::start()
{
    static constexpr int timer_ms = 100;
    start_timer(timer_ms);
}

void Game::reset()
{
    m_head = { m_rows / 2, m_columns / 2 };
    m_tail.clear_with_capacity();
    m_length = 2;
    m_score = 0;
    m_is_new_high_score = false;
    m_velocity_queue.clear();

    if (on_score_update)
        on_score_update(m_score);

    pause();
    start();
    spawn_fruit();
    update();
}

bool Game::is_available(Coordinate const& coord)
{
    for (size_t i = 0; i < m_tail.size(); ++i) {
        if (m_tail[i] == coord)
            return false;
    }
    if (m_head == coord)
        return false;
    if (m_fruit == coord)
        return false;
    return true;
}

void Game::spawn_fruit()
{
    Coordinate coord;
    for (;;) {
        coord.row = get_random_uniform(m_rows);
        coord.column = get_random_uniform(m_columns);
        if (is_available(coord))
            break;
    }
    m_fruit = coord;
    m_fruit_type = get_random_uniform(m_food_bitmaps.size());
}

void Game::timer_event(Core::TimerEvent&)
{
    Vector<Coordinate> dirty_cells;

    m_tail.prepend(m_head);

    if (m_tail.size() > m_length) {
        dirty_cells.append(m_tail.last());
        m_tail.take_last();
    }

    if (!m_velocity_queue.is_empty())
        m_velocity = m_velocity_queue.dequeue();

    dirty_cells.append(m_head);
    dirty_cells.append(m_tail.last());

    m_head.row += m_velocity.vertical;
    m_head.column += m_velocity.horizontal;

    m_last_velocity = m_velocity;

    if (m_head.row >= m_rows)
        m_head.row = 0;
    if (m_head.row < 0)
        m_head.row = m_rows - 1;
    if (m_head.column >= m_columns)
        m_head.column = 0;
    if (m_head.column < 0)
        m_head.column = m_columns - 1;

    dirty_cells.append(m_head);

    for (size_t i = 0; i < m_tail.size(); ++i) {
        if (m_head == m_tail[i]) {
            game_over();
            return;
        }
    }

    if (m_head == m_fruit) {
        ++m_length;
        ++m_score;

        if (on_score_update)
            m_is_new_high_score = on_score_update(m_score);

        dirty_cells.append(m_fruit);
        spawn_fruit();
        dirty_cells.append(m_fruit);
    }

    for (auto& coord : dirty_cells) {
        update(cell_rect(coord));
    }
}

void Game::keydown_event(GUI::KeyEvent& event)
{
    switch (event.key()) {
    case KeyCode::Key_A:
    case KeyCode::Key_Left:
        if (last_velocity().horizontal == 1)
            break;
        queue_velocity(0, -1);
        break;
    case KeyCode::Key_D:
    case KeyCode::Key_Right:
        if (last_velocity().horizontal == -1)
            break;
        queue_velocity(0, 1);
        break;
    case KeyCode::Key_W:
    case KeyCode::Key_Up:
        if (last_velocity().vertical == 1)
            break;
        queue_velocity(-1, 0);
        break;
    case KeyCode::Key_S:
    case KeyCode::Key_Down:
        if (last_velocity().vertical == -1)
            break;
        queue_velocity(1, 0);
        break;
    default:
        event.ignore();
        break;
    }
}

Gfx::IntRect Game::cell_rect(Coordinate const& coord) const
{
    auto game_rect = frame_inner_rect();
    auto cell_size = Gfx::IntSize(game_rect.width() / m_columns, game_rect.height() / m_rows);
    return {
        game_rect.x() + coord.column * cell_size.width(),
        game_rect.y() + coord.row * cell_size.height(),
        cell_size.width(),
        cell_size.height()
    };
}

void Game::paint_event(GUI::PaintEvent& event)
{
    GUI::Frame::paint_event(event);
    GUI::Painter painter(*this);
    painter.add_clip_rect(frame_inner_rect());
    painter.add_clip_rect(event.rect());
    painter.fill_rect(event.rect(), Color::Black);

    auto head_rect = cell_rect(m_head);
    m_snake_skin->draw_head(painter, head_rect, m_last_velocity.as_direction());

    for (size_t i = 0; i < m_tail.size(); i++) {
        auto previous_position = i > 0 ? m_tail[i - 1] : m_head;
        auto rect = cell_rect(m_tail[i]);

        if (i == m_tail.size() - 1) {
            m_snake_skin->draw_tail(painter, rect, direction_to_position(m_tail[i], previous_position));
            continue;
        }

        m_snake_skin->draw_body(painter, rect, direction_to_position(m_tail[i], previous_position), direction_to_position(m_tail[i], m_tail[i + 1]));
    }

    painter.draw_scaled_bitmap(cell_rect(m_fruit), m_food_bitmaps[m_fruit_type], m_food_bitmaps[m_fruit_type]->rect());
}

void Game::game_over()
{
    stop_timer();

    StringBuilder text;
    text.appendff("Your score was {}", m_score);
    if (m_is_new_high_score) {
        text.append("\nThat's a new high score!"sv);
    }
    GUI::MessageBox::show(window(),
        text.to_byte_string(),
        "Game Over"sv,
        GUI::MessageBox::Type::Information);

    reset();
}

void Game::queue_velocity(int v, int h)
{
    if (last_velocity().vertical == v && last_velocity().horizontal == h)
        return;
    m_velocity_queue.enqueue({ v, h });
}

Velocity const& Game::last_velocity() const
{
    if (!m_velocity_queue.is_empty())
        return m_velocity_queue.last();

    return m_last_velocity;
}

Direction Game::direction_to_position(Snake::Coordinate const& from, Snake::Coordinate const& to) const
{
    auto x_difference = to.column - from.column;
    auto y_difference = to.row - from.row;

    if (y_difference == 1)
        return Direction::Down;
    if (y_difference == -1)
        return Direction::Up;
    if (y_difference != 0) {
        // We wrapped around the screen, so invert the direction.
        return (y_difference > 0) ? Direction::Up : Direction::Down;
    }

    if (x_difference == 1)
        return Direction::Right;
    if (x_difference == -1)
        return Direction::Left;
    if (x_difference != 0) {
        // We wrapped around the screen, so invert the direction.
        return (x_difference > 0) ? Direction::Left : Direction::Right;
    }

    VERIFY_NOT_REACHED();
}

void Game::config_string_did_change(StringView domain, StringView group, StringView key, StringView value)
{
    if (domain == "Snake"sv && group == "Snake"sv && key == "SnakeSkin"sv) {
        set_skin_name(String::from_utf8(value).release_value_but_fixme_should_propagate_errors());
        return;
    }
}

void Game::config_u32_did_change(StringView domain, StringView group, StringView key, u32 value)
{
    if (domain == "Snake"sv && group == "Snake"sv && key == "BaseColor"sv) {
        set_skin_color(Color::from_argb(value));
        return;
    }
}

void Game::set_skin_color(Gfx::Color color)
{
    if (m_snake_color != color) {
        m_snake_color = color;
        set_skin(SnakeSkin::create(m_snake_skin_name, m_snake_color).release_value_but_fixme_should_propagate_errors());
    }
}

void Game::set_skin_name(String name)
{
    if (m_snake_skin_name != name) {
        m_snake_skin_name = name;
        set_skin(SnakeSkin::create(m_snake_skin_name, m_snake_color).release_value_but_fixme_should_propagate_errors());
    }
}

void Game::set_skin(NonnullOwnPtr<Snake::SnakeSkin> skin)
{
    m_snake_skin = move(skin);
    update();
}

}
