/*
 * Copyright (c) 2021, Mim Hufford <mim@hotmail.co.uk>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Game.h"

namespace FlappyBug {

Game::Game(Bug bug, Cloud cloud)
    : m_bug(move(bug))
    , m_cloud(move(cloud))
{
    set_override_cursor(Gfx::StandardCursor::Hidden);
    start_timer(16);
    reset();
}

void Game::reset()
{
    m_active = false;
    m_last_score = m_difficulty;
    m_difficulty = 1;
    m_restart_cooldown = 3;
    m_bug.reset();
    m_obstacle.reset();
}

void Game::game_over()
{
    if (on_game_end)
        m_high_score = on_game_end(get_final_score(m_difficulty));

    reset();
}

bool Game::ready_to_start() const
{
    if (!m_high_score.has_value()) {
        return true;
    }

    if (m_restart_cooldown <= 0) {
        return true;
    }

    return false;
}

void Game::timer_event(Core::TimerEvent&)
{
    tick();
}

void Game::paint_event(GUI::PaintEvent& event)
{
    GUI::Frame::paint_event(event);

    GUI::Painter painter(*this);
    painter.add_clip_rect(frame_inner_rect());
    painter.add_clip_rect(event.rect());

    painter.draw_tiled_bitmap(frame_inner_rect(), *m_background_bitmap);

    painter.draw_scaled_bitmap(m_cloud.rect(), *m_cloud.bitmap(), m_cloud.bitmap()->rect(), 0.2f);

    painter.fill_rect(enclosing_int_rect(m_obstacle.top_rect()), m_obstacle.color);
    painter.fill_rect(enclosing_int_rect(m_obstacle.bottom_rect()), m_obstacle.color);

    painter.draw_scaled_bitmap(enclosing_int_rect(m_bug.rect()), *m_bug.current_bitmap(), m_bug.flapping_bitmap->rect());

    if (m_active) {
        painter.draw_text(m_score_rect, String::formatted("{:.0}", m_difficulty).release_value_but_fixme_should_propagate_errors(), Gfx::TextAlignment::TopLeft, Color::White);
    } else if (m_high_score.has_value()) {
        auto message = String::formatted("Your score: {}\nHigh score: {}\n\n{}", get_final_score(m_last_score), m_high_score.value(), m_restart_cooldown < 0 ? "Press any key to play again" : " ").release_value_but_fixme_should_propagate_errors();
        painter.draw_text(m_text_rect, message, Gfx::TextAlignment::Center, Color::White);
    } else {
        painter.draw_text(m_text_rect, "Press any key to start"sv, Gfx::TextAlignment::Center, Color::White);
    }
}

void Game::keydown_event(GUI::KeyEvent& event)
{
    if (event.modifiers() || event.key() == Key_F1 || event.key() == Key_F11) {
        event.ignore();
        return;
    }
    switch (event.key()) {
    case Key_Escape:
        GUI::Application::the()->quit();
        break;
    default:
        player_input();
        break;
    }
}

void Game::mousedown_event(GUI::MouseEvent&)
{
    player_input();
}

void Game::player_input()
{
    if (ready_to_start()) {
        m_active = true;
    }
    if (m_active) {
        m_bug.flap();
    }
}

void Game::tick()
{
    auto queue_update = [&]() {
        update(m_score_rect);
        update(m_text_rect);
        update(enclosing_int_rect(m_bug.rect()));
        update(enclosing_int_rect(m_obstacle.top_rect()));
        update(enclosing_int_rect(m_obstacle.bottom_rect()));
        update(m_cloud.rect());
    };

    if (m_active) {
        queue_update();

        m_difficulty += 1.0f / 16.0f;

        m_bug.fall();
        m_bug.apply_velocity();
        m_obstacle.x -= 4 + m_difficulty / 16.0f;
        m_cloud.x -= m_difficulty / 16.0f;

        if (m_bug.y > game_height || m_bug.y < 0) {
            game_over();
        }

        if (m_bug.rect().intersects(m_obstacle.top_rect()) || m_bug.rect().intersects(m_obstacle.bottom_rect())) {
            game_over();
        }

        if (m_obstacle.x < 0) {
            m_obstacle.reset();
        }

        if (m_cloud.x < 0) {
            m_cloud.reset();
        }
    }

    m_restart_cooldown -= 1.0f / 16.0f;

    queue_update();
}

u32 Game::get_final_score(float score)
{
    return static_cast<u32>(roundf(score));
}

}
