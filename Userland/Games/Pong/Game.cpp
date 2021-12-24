/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Game.h"
#include <AK/Random.h>

namespace Pong {

Game::Game()
{
    start_timer(16);
    reset();
}

Game::~Game()
{
}

void Game::reset_paddles()
{
    if (m_cursor_paddle_target_y.has_value())
        update(cursor_paddle_target_rect());
    m_cursor_paddle_target_y.clear();

    update(enclosing_int_rect(m_player1_paddle.rect));
    m_player1_paddle.moving_up = m_up_key_held;
    m_player1_paddle.moving_down = m_down_key_held;
    m_player1_paddle.rect = { game_width - 12, game_height / 2 - 40, m_player1_paddle.width, m_player1_paddle.height };
    update(enclosing_int_rect(m_player1_paddle.rect));

    update(enclosing_int_rect(m_player2_paddle.rect));
    m_player2_paddle.moving_up = false;
    m_player2_paddle.moving_down = false;
    m_player2_paddle.rect = { 4, game_height / 2 - 40, m_player2_paddle.width, m_player2_paddle.height };
    update(enclosing_int_rect(m_player2_paddle.rect));
}

void Game::reset()
{
    reset_ball(1);
    reset_paddles();
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
    painter.fill_rect(enclosing_int_rect(m_net.rect()), m_net.color);

    painter.fill_ellipse(enclosing_int_rect(m_ball.rect()), Color::Red);

    painter.fill_rect(enclosing_int_rect(m_player1_paddle.rect), m_player1_paddle.color);
    painter.fill_rect(enclosing_int_rect(m_player2_paddle.rect), m_player2_paddle.color);

    if (m_cursor_paddle_target_y.has_value())
        painter.fill_ellipse(cursor_paddle_target_rect(), Color::Blue);

    painter.draw_text(player_1_score_rect(), String::formatted("{}", m_player_1_score), Gfx::TextAlignment::TopLeft, Color::White);
    painter.draw_text(player_2_score_rect(), String::formatted("{}", m_player_2_score), Gfx::TextAlignment::TopLeft, Color::White);
}

void Game::keyup_event(GUI::KeyEvent& event)
{
    switch (event.key()) {
    case Key_W:
    case Key_Up:
        m_up_key_held = false;
        m_player1_paddle.moving_up = false;
        break;
    case Key_S:
    case Key_Down:
        m_down_key_held = false;
        m_player1_paddle.moving_down = false;
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
    case Key_W:
    case Key_Up:
        m_up_key_held = true;
        m_player1_paddle.moving_up = true;
        m_player1_paddle.moving_down = false;
        m_cursor_paddle_target_y.clear();
        break;
    case Key_S:
    case Key_Down:
        m_down_key_held = true;
        m_player1_paddle.moving_up = false;
        m_player1_paddle.moving_down = true;
        m_cursor_paddle_target_y.clear();
        break;
    default:
        break;
    }
}

void Game::track_mouse_move(Gfx::IntPoint const& point)
{
    if (m_up_key_held || m_down_key_held) {
        // We're using the keyboard to move the paddle, the cursor is doing something else
        return;
    }

    if (m_cursor_paddle_target_y.has_value())
        update(cursor_paddle_target_rect());

    auto relative_point = point - window()->position();
    m_cursor_paddle_target_y = clamp(relative_point.y() - m_player1_paddle.rect.height() / 2, 0.f, game_height - m_player1_paddle.rect.height());
    if (m_player1_paddle.rect.y() > *m_cursor_paddle_target_y) {
        m_player1_paddle.moving_up = true;
        m_player1_paddle.moving_down = false;
    } else if (m_player1_paddle.rect.y() < *m_cursor_paddle_target_y) {
        m_player1_paddle.moving_up = false;
        m_player1_paddle.moving_down = true;
    }
    update(cursor_paddle_target_rect());
}

void Game::reset_ball(int serve_to_player)
{
    int position_y_min = (game_width / 2) - 50;
    int position_y_max = (game_width / 2) + 50;
    int position_y = get_random<u32>() % (position_y_max - position_y_min + 1) + position_y_min;
    int position_x = (game_height / 2);
    int velocity_y = get_random<u32>() % 3 + 1;
    int velocity_x = 4 + (5 - velocity_y);
    if (get_random<u32>() % 2)
        velocity_y = velocity_y * -1;
    if (serve_to_player == 2)
        velocity_x = velocity_x * -1;

    m_ball = {};
    m_ball.position = { position_x, position_y };
    m_ball.velocity = { velocity_x, velocity_y };
}

void Game::game_over(int winner)
{
    GUI::MessageBox::show(window(), String::formatted("Player {} wins!", winner), "Pong", GUI::MessageBox::Type::Warning, GUI::MessageBox::InputType::OK);
}

void Game::round_over(int winner)
{
    stop_timer();
    if (winner == 1) {
        update(player_1_score_rect());
        m_player_1_score++;
        update(player_1_score_rect());
    }

    if (winner == 2) {
        update(player_2_score_rect());
        m_player_2_score++;
        update(player_2_score_rect());
    }

    if (m_player_1_score == m_score_to_win || m_player_2_score == m_score_to_win) {
        game_over(winner);
        return;
    }

    reset_ball(winner);
    reset_paddles();
    start_timer(16);
}

void Game::calculate_move()
{
    int player_2_paddle_top = m_player2_paddle.rect.top();
    int player_2_paddle_bottom = m_player2_paddle.rect.bottom();

    if (m_ball.velocity.x() > 0 || m_ball.x() > game_width / 2) {
        // The ball is in the opponent's court, relax.
        m_player2_paddle.moving_up = false;
        m_player2_paddle.moving_down = false;
        return;
    }

    int ball_position = m_ball.y() + m_ball.radius;

    // AI paddle begins moving when the ball crosses the begin_trigger,
    // but stops only if it crosses the end_trigger. end_trigger forces
    // overcorrection, so that the paddle moves more smoothly.
    int begin_trigger = m_player2_paddle.rect.height() / 4;
    int end_trigger = m_player2_paddle.rect.height() / 2;

    if (m_player2_paddle.moving_up) {
        if (player_2_paddle_top + end_trigger < ball_position)
            m_player2_paddle.moving_up = false;
    } else {
        if (player_2_paddle_top + begin_trigger > ball_position)
            m_player2_paddle.moving_up = true;
    }

    if (m_player2_paddle.moving_down) {
        if (player_2_paddle_bottom - end_trigger > ball_position)
            m_player2_paddle.moving_down = false;
    } else {
        if (player_2_paddle_bottom - begin_trigger < ball_position)
            m_player2_paddle.moving_down = true;
    }
}

void Game::tick()
{
    auto new_ball = m_ball;
    new_ball.position += new_ball.velocity;

    update(enclosing_int_rect(m_ball.rect()));

    if (new_ball.y() < new_ball.radius || new_ball.y() > game_height - new_ball.radius) {
        new_ball.position.set_y(m_ball.y());
        new_ball.velocity.set_y(new_ball.velocity.y() * -1);
    }

    if (new_ball.x() < new_ball.radius) {
        round_over(1);
        return;
    }

    if (new_ball.x() > (game_width - new_ball.radius)) {
        round_over(2);
        return;
    }

    update(enclosing_int_rect(new_ball.rect()));

    if (new_ball.rect().intersects(m_player1_paddle.rect)) {
        new_ball.position.set_x(m_ball.x());
        new_ball.velocity.set_x(new_ball.velocity.x() * -1);

        float distance_to_middle_of_paddle = new_ball.y() - m_player1_paddle.rect.center().y();
        float relative_impact_point = distance_to_middle_of_paddle / m_player1_paddle.rect.height();
        new_ball.velocity.set_y(relative_impact_point * 7);
    }

    if (new_ball.rect().intersects(m_player2_paddle.rect)) {
        new_ball.position.set_x(m_ball.x());
        new_ball.velocity.set_x(new_ball.velocity.x() * -1);

        float distance_to_middle_of_paddle = new_ball.y() - m_player2_paddle.rect.center().y();
        float relative_impact_point = distance_to_middle_of_paddle / m_player2_paddle.rect.height();
        new_ball.velocity.set_y(relative_impact_point * 7);
    }

    if (m_player1_paddle.moving_up) {
        update(enclosing_int_rect(m_player1_paddle.rect));
        m_player1_paddle.rect.set_y(max(0.0f, m_player1_paddle.rect.y() - m_player1_paddle.speed));
        if (m_cursor_paddle_target_y.has_value() && m_player1_paddle.rect.y() <= *m_cursor_paddle_target_y) {
            m_cursor_paddle_target_y.clear();
            m_player1_paddle.moving_up = false;
        }
        update(enclosing_int_rect(m_player1_paddle.rect));
    }
    if (m_player1_paddle.moving_down) {
        update(enclosing_int_rect(m_player1_paddle.rect));
        m_player1_paddle.rect.set_y(min(game_height - m_player1_paddle.rect.height(), m_player1_paddle.rect.y() + m_player1_paddle.speed));
        if (m_cursor_paddle_target_y.has_value() && m_player1_paddle.rect.y() >= *m_cursor_paddle_target_y) {
            m_cursor_paddle_target_y.clear();
            m_player1_paddle.moving_down = false;
        }
        update(enclosing_int_rect(m_player1_paddle.rect));
    }

    calculate_move();

    if (m_player2_paddle.moving_up) {
        update(enclosing_int_rect(m_player2_paddle.rect));
        m_player2_paddle.rect.set_y(max(0.0f, m_player2_paddle.rect.y() - m_player2_paddle.speed));
        update(enclosing_int_rect(m_player2_paddle.rect));
    }
    if (m_player2_paddle.moving_down) {
        update(enclosing_int_rect(m_player2_paddle.rect));
        m_player2_paddle.rect.set_y(min(game_height - m_player2_paddle.rect.height(), m_player2_paddle.rect.y() + m_player2_paddle.speed));
        update(enclosing_int_rect(m_player2_paddle.rect));
    }

    m_ball = new_ball;
}

}
