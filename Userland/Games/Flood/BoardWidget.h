/*
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Board.h"
#include <AK/Function.h>
#include <AK/NonnullOwnPtr.h>
#include <AK/Optional.h>
#include <AK/RefPtr.h>
#include <LibGUI/Menu.h>
#include <LibGUI/Widget.h>

class BoardWidget final : public GUI::Widget {
    C_OBJECT(BoardWidget);

public:
    Function<void(Board::RowAndColumn)> on_move;

    virtual void paint_event(GUI::PaintEvent&) override;
    virtual void mousedown_event(GUI::MouseEvent&) override;

    void set_background_color(Color const background);

    int get_cell_size() const;
    Gfx::IntSize get_board_offset() const;

    Optional<Board::RowAndColumn> get_row_and_column_for_point(int x, int y) const;

    void resize_board(size_t rows, size_t columns);
    Board* board() { return m_board.ptr(); }

private:
    BoardWidget(size_t rows, size_t columns, Vector<Color> colors, Color background_color);
    NonnullOwnPtr<Board> m_board;

    Color m_background_color;
};
