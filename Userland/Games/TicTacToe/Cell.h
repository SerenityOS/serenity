/*
 * Copyright (c) 2021, Leonardo Nicolas <leonicolas@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/Painter.h>
#include <LibGUI/Widget.h>

namespace TicTacToe {

class Cell final : public GUI::Widget {
    C_OBJECT(Cell);

public:
    enum Content {
        X, O, Empty
    };

    int index() const { return m_index; }
    void set_index(int index) { m_index = index; }

    bool is_empty();
    void set_content(Cell::Content content);

protected:
    Cell();

    virtual void paint_event(GUI::PaintEvent&) override;
    virtual void mousedown_event(GUI::MouseEvent&) override;

private:
    void draw_x(GUI::Painter& painter);
    void draw_o(GUI::Painter& painter);

    Content m_content { Content::Empty };
    int m_index { 0 };
};

}
