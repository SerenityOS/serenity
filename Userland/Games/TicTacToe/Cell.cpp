/*
 * Copyright (c) 2021-2022, Leonardo Nicolas <leonicolas@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Cell.h"
#include "Game.h"
#include <LibGfx/Palette.h>

REGISTER_WIDGET(TicTacToe, Cell);

namespace TicTacToe {

#define HIGHLIGHT_COLOR 0xfffca6

Cell::Cell()
{
    REGISTER_INT_PROPERTY("index", index, set_index);

    m_backgroud_color = palette().color(background_role());
    initialize_highlight_fade_timer();
}

void Cell::initialize_highlight_fade_timer()
{
    auto highlight_color = Gfx::Color::from_rgb(HIGHLIGHT_COLOR);
    m_highlight_timer = Core::Timer::construct();
    m_highlight_timer->on_timeout = [this, highlight_color]() {
        auto final_color = m_backgroud_color.interpolate(highlight_color, m_highlight_step);
        m_highlight_step += 0.1f * (m_highlight_steps_count % 2 ? 1 : -1);

        auto __palette = palette();
        __palette.set_color(background_role(), final_color);
        set_palette(__palette);
        update();

        if (m_highlight_step < 0 || m_highlight_step >= 1) {
            if (m_highlight_steps_count++ > 2)
                m_highlight_timer->stop();
        }
    };
}

bool Cell::is_empty()
{
    return m_content == Content::Empty;
}

void Cell::set_content(Content content)
{
    m_content = content;
    if (content == Content::Empty)
        reset_background();
    else
        update();
}

void Cell::highlight()
{
    m_highlight_steps_count = 0;
    m_highlight_step = 0.0f;
    if (m_highlight_timer->is_active())
        m_highlight_timer->stop();
    m_highlight_timer->start(30);
}

void Cell::reset_background()
{
    if (m_highlight_timer->is_active())
        m_highlight_timer->stop();
    auto __palette = palette();
    __palette.set_color(background_role(), m_backgroud_color);
    set_palette(__palette);
    update();
}

void Cell::paint_event(GUI::PaintEvent&)
{
    GUI::Painter painter(*this);

    if (m_content == Content::X)
        draw_x(painter);
    else if (m_content == Content::O)
        draw_o(painter);
}

void Cell::mousedown_event(GUI::MouseEvent& event)
{
    if (event.button() != GUI::MouseButton::Primary)
        return;
    Game::the().do_move(index());
}

void Cell::draw_x(GUI::Painter& painter)
{
    painter.draw_line({ 20, 20 }, { 80, 80 }, Color::DarkRed, 8);
    painter.draw_line({ 20, 80 }, { 80, 20 }, Color::DarkRed, 8);
}

void Cell::draw_o(GUI::Painter& painter)
{
    painter.fill_ellipse({ 12, 12, 78, 78 }, Color::DarkBlue);
    painter.fill_ellipse({ 22, 22, 58, 58 }, palette().color(background_role()));
}

}
