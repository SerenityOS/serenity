/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/SignalSlot.h>
#include <AK/FixedArray.h>
#include <AK/OwnPtr.h>
#include <AK/Tuple.h>
#include <AK/SinglyLinkedList.h>
#include "CellWidget.h"

class BoardWidget final : public GUI::Widget {
    C_OBJECT(BoardWidget);

public:
    // Signals
    AK::Signal<Color> on_cell_color_changed, on_cell_text_color_changed;
    AK::Signal<i32> on_cell_size_changed;
    AK::Signal<i32, i32> on_solved;
    AK::Signal<> on_cell_moved;

    virtual ~BoardWidget() override;

    int rows() const { return m_rows; }
    int columns() const { return m_columns; }
    int cell_size() const { return m_cell_size; }
    Gfx::Color get_background_color_for_cell() const { return background_color_for_cell; }
    Gfx::Color get_text_color_for_cell() const { return text_color_for_cell; }

    void generate_cells();

private:
    explicit BoardWidget(int rows, int columns, int cell_size, Gfx::Color cell_color, Gfx::Color cell_text_color);

    virtual void keydown_event(GUI::KeyEvent&) override;

    void shuffle_cells();
    void ensure_puzzle_is_solvable();

    Color background_color_for_cell;
    Color text_color_for_cell;

    int m_empty_cell_index { 0 };
    int m_rows { 3 }, m_columns { 3 }, m_cell_size { 32 };

    OwnPtr<AK::FixedArray<CellWidget*>> m_cells;
    AK::ConnectionBag cons;
};
