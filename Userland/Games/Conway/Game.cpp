/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Game.h"
#include <AK/Random.h>
#include <AK/StringBuilder.h>
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
    clear_universe();
    seed_universe();
    start_timer(m_sleep);
    update();
}

void Game::seed_universe()
{
    auto set = [&](int x, int y, String s) {
        int p = 0;
        for (auto c : s) {
            m_universe[y][x + p] = c == 'O' ? 1 : 0;
            p++;
        }
    };

    switch (m_pattern) {
    case Pattern::Random:
        for (int y = 0; y < m_rows; y++)
            for (int x = 0; x < m_columns; x++)
                m_universe[y][x] = (get_random<u32>() % 2) ? 1 : 0;
        break;
    case Pattern::GosperGliderGun:
        set(20, 25, "........................O............");
        set(20, 26, "......................O.O............");
        set(20, 27, "............OO......OO............OO.");
        set(20, 28, "...........O...O....OO............OO.");
        set(20, 29, "OO........O.....O...OO...............");
        set(20, 30, "OO........O...O.OO....O.O............");
        set(20, 31, "..........O.....O.......O............");
        set(20, 32, "...........O...O.....................");
        set(20, 33, "............OO.......................");
        break;
    case Pattern::SimkinGliderGun:
        set(20, 25, "OO.....OO........................");
        set(20, 26, "OO.....OO........................");
        set(20, 27, ".................................");
        set(20, 28, "....OO...........................");
        set(20, 29, "....OO...........................");
        set(20, 30, ".................................");
        set(20, 31, ".................................");
        set(20, 32, ".................................");
        set(20, 33, ".................................");
        set(20, 34, "......................OO.OO......");
        set(20, 35, ".....................O.....O.....");
        set(20, 36, ".....................O......O..OO");
        set(20, 37, ".....................OOO...O...OO");
        set(20, 38, "..........................O......");
        set(20, 39, ".................................");
        set(20, 40, ".................................");
        set(20, 41, ".................................");
        set(20, 42, "....................OO...........");
        set(20, 43, "....................O............");
        set(20, 44, ".....................OOO.........");
        set(20, 45, ".......................O.........");
        break;
    case Pattern::Infinite1:
        set(20, 80, "OOOOOOOO.OOOOO...OOO......OOOOOOO.OOOOO");
        break;
    case Pattern::Infinite2:
        set(27, 80, "......O.");
        set(27, 81, "....O.OO");
        set(27, 82, "....O.O.");
        set(27, 83, "....O...");
        set(27, 84, "..O.....");
        set(27, 85, "O.O.....");
        break;
    case Pattern::Infinite3:
        set(20, 85, "OOO.O");
        set(20, 86, "O....");
        set(20, 87, "...OO");
        set(20, 88, ".OO.O");
        set(20, 89, "O.O.O");
        break;
    }
}

void Game::clear_universe()
{
    for (int y = 0; y < m_rows; y++)
        for (int x = 0; x < m_columns; x++)
            m_universe[y][x] = 0;
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
