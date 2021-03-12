/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
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

#include "Game.h"
#include "LevelSelectDialog.h"
#include <LibGUI/Application.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/Painter.h>
#include <LibGfx/Font.h>
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

Game::~Game()
{
}

void Game::reset_paddle()
{
    m_paddle.moving_left = false;
    m_paddle.moving_right = false;
    m_paddle.rect = { game_width / 2 - 40, game_height - 20, 80, 16 };
}

void Game::reset()
{
    m_lives = 3;
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
    } else {
        // Rainbow
        for (int row = 0; row < 7; ++row) {
            for (int column = 0; column < 10; ++column) {
                Brick brick(row, column, colors[row]);
                m_bricks.append(brick);
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

    update();
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

    int msg_width = font().width(String::formatted("Lives: {}", m_lives));
    int msg_height = font().glyph_height();
    painter.draw_text({ (game_width - msg_width - 2), 2, msg_width, msg_height }, String::formatted("Lives: {}", m_lives), Gfx::TextAlignment::Center, Color::White);

    if (m_paused) {
        const char* msg = m_cheater ? "C H E A T E R" : "P A U S E D";
        int msg_width = font().width(msg);
        int msg_height = font().glyph_height();
        painter.draw_text({ (game_width / 2) - (msg_width / 2), (game_height / 2) - (msg_height / 2), msg_width, msg_height }, msg, Gfx::TextAlignment::Center, Color::White);
    }
}

void Game::keyup_event(GUI::KeyEvent& event)
{
    if (m_paused)
        return;
    switch (event.key()) {
    case Key_Left:
        m_paddle.moving_left = false;
        break;
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
    case Key_Left:
        m_paddle.moving_left = true;
        break;
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
    float new_paddle_x = event.x() - m_paddle.rect.width() / 2;
    new_paddle_x = max(0.0f, new_paddle_x);
    new_paddle_x = min(game_width - m_paddle.rect.width(), new_paddle_x);
    m_paddle.rect.set_x(new_paddle_x);
}

void Game::reset_ball()
{
    int position_x_min = (game_width / 2) - 50;
    int position_x_max = (game_width / 2) + 50;
    int position_x = arc4random() % (position_x_max - position_x_min + 1) + position_x_min;
    int position_y = 200;
    int velocity_x = arc4random() % 3 + 1;
    int velocity_y = 3 + (3 - velocity_x);
    if (arc4random() % 2)
        velocity_x = velocity_x * -1;

    m_ball = {};
    m_ball.position = { position_x, position_y };
    m_ball.velocity = { velocity_x, velocity_y };
}

void Game::hurt()
{
    stop_timer();
    m_lives--;
    if (m_lives <= 0) {
        update();
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

    if (new_ball.rect().intersects(m_paddle.rect)) {
        new_ball.position.set_y(m_ball.y());
        new_ball.velocity.set_y(new_ball.velocity.y() * -1);

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
        m_paddle.rect.set_x(max(0.0f, m_paddle.rect.x() - m_paddle.speed));
    }
    if (m_paddle.moving_right) {
        m_paddle.rect.set_x(min(game_width - m_paddle.rect.width(), m_paddle.rect.x() + m_paddle.speed));
    }

    m_ball = new_ball;

    if (m_pause_count > 50)
        m_cheater = true;

    update();
}

}
