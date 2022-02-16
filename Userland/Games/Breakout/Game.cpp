/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Game.h"
#include "LevelSelectDialog.h"
#include <AK/Random.h>
#include <LibGUI/Application.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/Painter.h>
#include <LibGfx/StandardCursor.h>
#include <unistd.h>

namespace Breakout {

Game::Game()
{
    set_override_cursor(Gfx::StandardCursor::Hidden);
    auto level_dialog = LevelSelectDialog::show(m_board, window());
    if (level_dialog != GUI::Dialog::ExecOK)
        m_board = -1;
    set_paused(false);
    start_timer(16);
    reset();
}

void Game::reset_paddle()
{
    update(enclosing_int_rect(m_paddle.rect));
    m_paddle.moving_left = false;
    m_paddle.moving_right = false;
    m_paddle.rect = { game_width / 2 - 40, game_height - 20, 80, 16 };
    update(enclosing_int_rect(m_paddle.rect));
}

void Game::reset()
{
    update(lives_left_rect());
    m_lives = 3;
    update(lives_left_rect());

    m_pause_count = 0;
    m_cheater = false;
    reset_ball();
    reset_paddle();
    generate_bricks();
}

void Game::generate_bricks()
{
    m_bricks = {};

    Gfx::Color colors[] = {
        Gfx::Color::Red,
        Gfx::Color::Green,
        Gfx::Color::Blue,
        Gfx::Color::Yellow,
        Gfx::Color::Magenta,
        Gfx::Color::Cyan,
        Gfx::Color::LightGray,
    };

    Vector<Brick> boards[] = {
        // :^)
        Vector({
            Brick(0, 0, colors[3], 40, 12, 100),
            Brick(0, 4, colors[3], 40, 12, 100),
            Brick(1, 2, colors[3], 40, 12, 100),
            Brick(1, 5, colors[3], 40, 12, 100),
            Brick(2, 1, colors[3], 40, 12, 100),
            Brick(2, 3, colors[3], 40, 12, 100),
            Brick(2, 6, colors[3], 40, 12, 100),
            Brick(3, 6, colors[3], 40, 12, 100),
            Brick(4, 0, colors[3], 40, 12, 100),
            Brick(4, 6, colors[3], 40, 12, 100),
            Brick(5, 6, colors[3], 40, 12, 100),
            Brick(6, 5, colors[3], 40, 12, 100),
            Brick(7, 4, colors[3], 40, 12, 100),
        })
    };

    if (m_board != -1) {
        m_bricks = boards[m_board];

        for (auto& brick : m_bricks)
            update(enclosing_int_rect(brick.rect));
    } else {
        // Rainbow
        for (int row = 0; row < 7; ++row) {
            for (int column = 0; column < 10; ++column) {
                Brick brick(row, column, colors[row]);
                m_bricks.append(brick);
                update(enclosing_int_rect(brick.rect));
            }
        }
    }
}

void Game::set_paused(bool paused)
{
    m_paused = paused;

    if (m_paused) {
        set_override_cursor(Gfx::StandardCursor::None);
        m_pause_count++;
    } else {
        set_override_cursor(Gfx::StandardCursor::Hidden);
    }

    update(pause_rect());
}

void Game::timer_event(Core::TimerEvent&)
{
    if (m_paused)
        return;
    tick();
}

void Game::paint_event(GUI::PaintEvent& event)
{
    GUI::Painter painter(*this);
    painter.add_clip_rect(event.rect());

    painter.fill_rect(rect(), Color::Black);

    painter.fill_ellipse(enclosing_int_rect(m_ball.rect()), Color::Red);

    painter.fill_rect(enclosing_int_rect(m_paddle.rect), Color::White);

    for (auto& brick : m_bricks) {
        if (!brick.dead)
            painter.fill_rect(enclosing_int_rect(brick.rect), brick.color);
    }

    painter.draw_text(lives_left_rect(), String::formatted("Lives: {}", m_lives), Gfx::TextAlignment::Center, Color::White);

    if (m_paused) {
        const char* msg = m_cheater ? "C H E A T E R" : "P A U S E D";
        painter.draw_text(pause_rect(), msg, Gfx::TextAlignment::Center, Color::White);
    }
}

void Game::keyup_event(GUI::KeyEvent& event)
{
    if (m_paused)
        return;
    switch (event.key()) {
    case Key_A:
        [[fallthrough]];
    case Key_Left:
        m_paddle.moving_left = false;
        break;
    case Key_D:
        [[fallthrough]];
    case Key_Right:
        m_paddle.moving_right = false;
        break;
    default:
        break;
    }
}

void Game::keydown_event(GUI::KeyEvent& event)
{
    if (m_paused)
        return;
    switch (event.key()) {
    case Key_Escape:
        GUI::Application::the()->quit();
        break;
    case Key_A:
        [[fallthrough]];
    case Key_Left:
        m_paddle.moving_left = true;
        break;
    case Key_D:
        [[fallthrough]];
    case Key_Right:
        m_paddle.moving_right = true;
        break;
    default:
        break;
    }
}

void Game::mousemove_event(GUI::MouseEvent& event)
{
    if (m_paused)
        return;
    update(enclosing_int_rect(m_paddle.rect));
    float new_paddle_x = event.x() - m_paddle.rect.width() / 2;
    new_paddle_x = max(0.0f, new_paddle_x);
    new_paddle_x = min(game_width - m_paddle.rect.width(), new_paddle_x);
    m_paddle.rect.set_x(new_paddle_x);
    update(enclosing_int_rect(m_paddle.rect));
}

void Game::reset_ball()
{
    int position_x_min = (game_width / 2) - 50;
    int position_x_max = (game_width / 2) + 50;
    int position_x = get_random<u32>() % (position_x_max - position_x_min + 1) + position_x_min;
    int position_y = 200;
    int velocity_x = get_random<u32>() % 3 + 1;
    int velocity_y = 3 + (3 - velocity_x);
    if (get_random<u32>() % 2)
        velocity_x = velocity_x * -1;

    update(enclosing_int_rect(m_ball.rect()));
    m_ball = {};
    m_ball.position = { position_x, position_y };
    m_ball.velocity = { velocity_x, velocity_y };
    update(enclosing_int_rect(m_ball.rect()));
}

void Game::hurt()
{
    stop_timer();
    update(lives_left_rect());
    m_lives--;
    update(lives_left_rect());
    if (m_lives <= 0) {
        GUI::MessageBox::show(window(), "You lose!", "Breakout", GUI::MessageBox::Type::Information, GUI::MessageBox::InputType::OK);
        reset();
    }
    sleep(1);
    reset_ball();
    reset_paddle();
    start_timer(16);
}

void Game::win()
{
    stop_timer();
    update();
    if (m_cheater) {
        GUI::MessageBox::show(window(), "You cheated not only the game, but yourself.", "Breakout", GUI::MessageBox::Type::Information, GUI::MessageBox::InputType::OK);
    } else {
        GUI::MessageBox::show(window(), "You win!", "Breakout", GUI::MessageBox::Type::Information, GUI::MessageBox::InputType::OK);
    }
    reset();
    start_timer(16);
}

void Game::tick()
{
    auto new_ball = m_ball;
    new_ball.position += new_ball.velocity;

    update(enclosing_int_rect(m_ball.rect()));

    if (new_ball.x() < new_ball.radius || new_ball.x() > game_width - new_ball.radius) {
        new_ball.position.set_x(m_ball.x());
        new_ball.velocity.set_x(new_ball.velocity.x() * -1);
    }

    if (new_ball.y() < new_ball.radius) {
        new_ball.position.set_y(m_ball.y());
        new_ball.velocity.set_y(new_ball.velocity.y() * -1);
    }

    if (new_ball.y() > game_height - new_ball.radius) {
        hurt();
        return;
    }

    update(enclosing_int_rect(new_ball.rect()));

    if (new_ball.rect().intersects(m_paddle.rect)) {
        if (m_ball.y() < new_ball.y()) {
            new_ball.position.set_y(m_ball.y());
        }
        new_ball.velocity.set_y(fabs(new_ball.velocity.y()) * -1);

        float distance_to_middle_of_paddle = new_ball.x() - m_paddle.rect.center().x();
        float relative_impact_point = distance_to_middle_of_paddle / m_paddle.rect.width();
        new_ball.velocity.set_x(relative_impact_point * 7);
    }

    for (auto& brick : m_bricks) {
        if (brick.dead)
            continue;
        if (new_ball.rect().intersects(brick.rect)) {
            brick.dead = true;

            auto overlap = new_ball.rect().intersected(brick.rect);
            if (overlap.width() < overlap.height()) {
                new_ball.position.set_x(m_ball.x());
                new_ball.velocity.set_x(new_ball.velocity.x() * -1);
            } else {
                new_ball.position.set_y(m_ball.y());
                new_ball.velocity.set_y(new_ball.velocity.y() * -1);
            }
            update(enclosing_int_rect(brick.rect));
            break;
        }
    }

    bool has_live_bricks = false;
    for (auto& brick : m_bricks) {
        if (!brick.dead) {
            has_live_bricks = true;
            break;
        }
    }

    if (!has_live_bricks) {
        win();
        return;
    }

    if (m_paddle.moving_left) {
        update(enclosing_int_rect(m_paddle.rect));
        m_paddle.rect.set_x(max(0.0f, m_paddle.rect.x() - m_paddle.speed));
        update(enclosing_int_rect(m_paddle.rect));
    }
    if (m_paddle.moving_right) {
        update(enclosing_int_rect(m_paddle.rect));
        m_paddle.rect.set_x(min(game_width - m_paddle.rect.width(), m_paddle.rect.x() + m_paddle.speed));
        update(enclosing_int_rect(m_paddle.rect));
    }

    m_ball = new_ball;

    if (m_pause_count > 50)
        m_cheater = true;
}

}
