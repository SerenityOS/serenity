/*
 * Copyright (c) 2021, the SerenityOS developers.
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
#include <LibGUI/Painter.h>
#include <stdlib.h>
#include <time.h>

Game::Game()
{
    reset();
}

Game::~Game()
{
}

void Game::reset()
{
    stop_timer();
    seed_universe();
    start_timer(m_sleep);
    update();
}

void Game::seed_universe()
{
    for (int y = 0; y < m_rows; y++) {
        for (int x = 0; x < m_columns; x++) {
            m_universe[y][x] = (arc4random() % 2) ? 1 : 0;
        }
    }
}

void Game::update_universe()
{
    bool new_universe[m_rows][m_columns];

    for (int y = 0; y < m_rows; y++) {
        for (int x = 0; x < m_columns; x++) {
            int n = 0;
            auto cell = m_universe[y][x];

            for (int y1 = y - 1; y1 <= y + 1; y1++) {
                for (int x1 = x - 1; x1 <= x + 1; x1++) {
                    if (m_universe[(y1 + m_rows) % m_rows][(x1 + m_columns) % m_columns]) {
                        n++;
                    }
                }
            }

            if (cell)
                n--;

            if (n == 3 || (n == 2 && cell))
                new_universe[y][x] = true;
            else
                new_universe[y][x] = false;
        }
    }

    for (int y = 0; y < m_rows; y++) {
        for (int x = 0; x < m_columns; x++) {
            m_universe[y][x] = new_universe[y][x];
        }
    }
}

void Game::timer_event(Core::TimerEvent&)
{
    update_universe();
    update();
}

Gfx::IntRect Game::first_cell_rect() const
{
    auto game_rect = rect();
    auto cell_size = Gfx::IntSize(game_rect.width() / m_columns, game_rect.height() / m_rows);
    auto x_margin = (game_rect.width() - (cell_size.width() * m_columns)) / 2;
    auto y_margin = (game_rect.height() - (cell_size.height() * m_rows)) / 2;
    return { x_margin, y_margin, cell_size.width(), cell_size.height() };
}

void Game::paint_event(GUI::PaintEvent& event)
{
    GUI::Painter painter(*this);
    painter.add_clip_rect(event.rect());
    painter.fill_rect(event.rect(), m_dead_color);
    auto first_rect = first_cell_rect();

    for (int y = 0; y < m_rows; y++) {
        for (int x = 0; x < m_columns; x++) {
            Gfx::IntRect rect {
                x * first_rect.width() + first_rect.left(),
                y * first_rect.height() + first_rect.top(),
                first_rect.width(),
                first_rect.height()
            };
            painter.fill_rect(rect, m_universe[y][x] ? m_alive_color : m_dead_color);
        }
    }
}

void Game::mousedown_event(GUI::MouseEvent& event)
{
    switch (event.button()) {
    case GUI::MouseButton::Left:
    case GUI::MouseButton::Right:
        m_last_button = event.button();
        break;
    default:
        return;
    }
    interact_at(event.position());
}

void Game::mouseup_event(GUI::MouseEvent& event)
{
    if (event.button() == m_last_button)
        m_last_button = GUI::MouseButton::None;
}

void Game::mousemove_event(GUI::MouseEvent& event)
{
    interact_at(event.position());
}

void Game::interact_at(const Gfx::IntPoint& point)
{
    if (m_last_button == GUI::MouseButton::None)
        return;

    auto first_rect = first_cell_rect();
    // Too tiny window, we don't actually display anything.
    if (first_rect.width() == 0 || first_rect.height() == 0)
        return;

    // Too far left/up.
    if (point.x() < first_rect.left() || point.y() < first_rect.top())
        return;

    int cell_x = (point.x() - first_rect.left()) / first_rect.width();
    int cell_y = (point.y() - first_rect.top()) / first_rect.height();

    // Too far right/down.
    if (cell_x >= m_columns || cell_y >= m_rows)
        return;

    switch (m_last_button) {
    case GUI::MouseButton::Left:
        m_universe[cell_y][cell_x] = true;
        break;
    case GUI::MouseButton::Right:
        m_universe[cell_y][cell_x] = false;
        break;
    default:
        VERIFY_NOT_REACHED();
    }
}
