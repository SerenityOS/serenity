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
#include <LibGUI/Application.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/Painter.h>
#include <LibGfx/StandardCursor.h>

namespace Breakout {

Game::Game()
{
    set_override_cursor(Gfx::StandardCursor::Hidden);
    start_timer(16);
    reset();
}

Game::~Game()
{
}

void Game::reset_paddle()
{
    m_paddle.rect = { game_width / 2 - 40, game_height - 20, 80, 16 };
}

void Game::reset()
{
    reset_ball();
    reset_paddle();
    generate_bricks();
}

void Game::generate_bricks()
{
    Gfx::Color colors[] = {
        Gfx::Color::Red,
        Gfx::Color::Green,
        Gfx::Color::Blue,
        Gfx::Color::Yellow,
        Gfx::Color::Magenta,
        Gfx::Color::Cyan,
        Gfx::Color::LightGray,
    };

    int brick_width = 40;
    int brick_height = 12;
    int brick_spacing = 3;
    int field_left_offset = 30;
    int field_top_offset = 30;

    for (int row = 0; row < 7; ++row) {
        for (int column = 0; column < 10; ++column) {
            Brick brick;
            brick.rect = {
                field_left_offset + (column * brick_width) + (column * brick_spacing),
                field_top_offset + (row * brick_height) + (row * brick_spacing),
                brick_width,
                brick_height
            };
            brick.color = colors[row];
            m_bricks.append(brick);
        }
    }
}

void Game::timer_event(Core::TimerEvent&)
{
    tick();
}

void Game::paint_event(GUI::PaintEvent& event)
{
    GUI::Painter painter(*this);
    painter.add_clip_rect(event.rect());

    painter.fill_rect(rect(), Color::Black);

    painter.fill_ellipse(m_ball.rect(), Color::Red);

    painter.fill_rect(m_paddle.rect, Color::White);

    for (auto& brick : m_bricks) {
        if (!brick.dead)
            painter.fill_rect(brick.rect, brick.color);
    }
}

void Game::keyup_event(GUI::KeyEvent& event)
{
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
    int new_paddle_x = event.x() - m_paddle.rect.width() / 2;
    new_paddle_x = max(0, new_paddle_x);
    new_paddle_x = min(game_width - m_paddle.rect.width(), new_paddle_x);
    m_paddle.rect.set_x(new_paddle_x);
}

void Game::reset_ball()
{
    m_ball = {};
    m_ball.x = 150;
    m_ball.y = 200;
    m_ball.vx = 3;
    m_ball.vy = 3;
}

void Game::hurt()
{
    stop_timer();
    GUI::MessageBox::show(window(), "Ouch!", "Breakout", GUI::MessageBox::Type::Warning, GUI::MessageBox::InputType::OK);
    reset_ball();
    reset_paddle();
    start_timer(16);
}

void Game::win()
{
    stop_timer();
    GUI::MessageBox::show(window(), "You win!", "Breakout", GUI::MessageBox::Type::Information, GUI::MessageBox::InputType::OK);
    reset();
    start_timer(16);
}

void Game::tick()
{
    auto new_ball = m_ball;
    new_ball.x += new_ball.vx;
    new_ball.y += new_ball.vy;

    if (new_ball.x < new_ball.radius || new_ball.x > game_width - new_ball.radius) {
        new_ball.x = m_ball.x;
        new_ball.vx *= -1;
    }

    if (new_ball.y < new_ball.radius) {
        new_ball.y = m_ball.y;
        new_ball.vy *= -1;
    }

    if (new_ball.y > game_height - new_ball.radius) {
        hurt();
        return;
    }

    if (new_ball.rect().intersects(m_paddle.rect)) {
        new_ball.y = m_ball.y;
        new_ball.vy *= -1;
    }

    for (auto& brick : m_bricks) {
        if (brick.dead)
            continue;
        if (new_ball.rect().intersects(brick.rect)) {
            brick.dead = true;

            auto overlap = new_ball.rect().intersected(brick.rect);
            if (overlap.width() < overlap.height()) {
                new_ball.x = m_ball.x;
                new_ball.vx *= -1;
            } else {
                new_ball.y = m_ball.y;
                new_ball.vy *= -1;
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
        m_paddle.rect.set_x(max(0, m_paddle.rect.x() - m_paddle.speed));
    }
    if (m_paddle.moving_right) {
        m_paddle.rect.set_x(min(game_width - m_paddle.rect.width(), m_paddle.rect.x() + m_paddle.speed));
    }

    m_ball = new_ball;

    update();
}

}
