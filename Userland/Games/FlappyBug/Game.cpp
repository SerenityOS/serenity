/*
 * Copyright (c) 2021, Mim Hufford <mim@hotmail.co.uk>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Game.h"
#include <AK/Random.h>

namespace FlappyBug {

Game::Game()
{
    set_override_cursor(Gfx::StandardCursor::Hidden);
    start_timer(16);
    reset();
}

void Game::reset()
{
    m_active = false;
    m_difficulty = 1;
    m_bug.reset();
    m_obstacle.reset();
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

    painter.fill_rect(enclosing_int_rect(m_obstacle.top_rect()), Color::White);
    painter.fill_rect(enclosing_int_rect(m_obstacle.bottom_rect()), Color::White);
    painter.fill_ellipse(enclosing_int_rect(m_bug.rect()), Color::Red);

    painter.draw_text({ 10, 10, 100, 100 }, String::formatted("{}", m_difficulty), Gfx::TextAlignment::TopLeft, Color::Green);
}

void Game::keydown_event(GUI::KeyEvent& event)
{
    switch (event.key()) {
    case Key_Escape:
        GUI::Application::the()->quit();
        break;
    default:
        m_active = true;
        m_bug.flap();
        break;
    }
}

void Game::tick()
{
    if (m_active) {
        m_difficulty += 0.0001f;

        m_bug.fall();
        m_bug.apply_velocity();
        m_obstacle.x -= 4 + m_difficulty;

        if (m_bug.y > game_height || m_bug.y < 0) {
            reset();
        }

        if (m_bug.rect().intersects(m_obstacle.top_rect()) || m_bug.rect().intersects(m_obstacle.bottom_rect())) {
            reset();
        }

        if (m_obstacle.x < 0) {
            m_obstacle.reset();
        }
    }

    update();
}

}
