/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/AbstractTableView.h>

namespace GUI {

class TableView : public AbstractTableView {
    C_OBJECT(TableView)
public:
    virtual ~TableView() override = default;

    enum class GridStyle {
        None,
        Horizontal,
        Vertical,
        Both,
    };

    enum class CursorStyle {
        None,
        Item,
        Row,
    };

    GridStyle grid_style() const { return m_grid_style; }
    void set_grid_style(GridStyle);

    void set_highlight_key_column(bool b) { m_highlight_key_column = b; }
    bool is_key_column_highlighted() const { return m_highlight_key_column; }

    virtual void move_cursor(CursorMovement, SelectionUpdate) override;

protected:
    TableView();

    virtual void keydown_event(KeyEvent&) override;
    virtual void mousedown_event(MouseEvent&) override;
    virtual void mouseup_event(MouseEvent&) override;
    virtual void mousemove_event(MouseEvent&) override;
    virtual void paint_event(PaintEvent&) override;
    virtual void second_paint_event(PaintEvent&) override;

private:
    GridStyle m_grid_style { GridStyle::None };

    bool m_highlight_key_column { true };

    bool m_rubber_banding { false };
    int m_rubber_band_origin { 0 };
    int m_rubber_band_current { 0 };
};

}
