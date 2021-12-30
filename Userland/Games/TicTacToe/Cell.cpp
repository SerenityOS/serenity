/*
 * Copyright (c) 2021, Leonardo Nicolas <leonicolas@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Cell.h"
#include "Game.h"

REGISTER_WIDGET(TicTacToe, Cell);

namespace TicTacToe {

Cell::Cell() {
    REGISTER_INT_PROPERTY("index", index, set_index);
}

bool Cell::is_empty() {
    return m_content == Content::Empty;
}

void Cell::set_content(Content content) {
    m_content = content;
    update();
}

void Cell::paint_event(GUI::PaintEvent&)
{
    GUI::Painter painter(*this);

    if(m_content == Content::X)
        draw_x(painter);
    else if(m_content == Content::O)
        draw_o(painter);
}

void Cell::mousedown_event(GUI::MouseEvent& event)
{
    if (event.button() != GUI::MouseButton::Primary)
        return;
    Game::the().make_move(index());
}

void Cell::draw_x(GUI::Painter& painter) {
    painter.draw_line({ 20, 20 }, { 80, 80 }, Color::DarkRed, 8);
    painter.draw_line({ 20, 80 }, { 80, 20 }, Color::DarkRed, 8);
}

void Cell::draw_o(GUI::Painter& painter) {
    painter.fill_ellipse({ 12, 12, 78, 78 }, Color::DarkBlue);
    painter.fill_ellipse({ 22, 22, 58, 58 }, Color::White);
}

}
