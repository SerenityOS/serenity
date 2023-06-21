/*
 * Copyright (c) 2020, Sergey Bugaev <bugaevc@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Vector.h>
#include <LibGUI/AbstractView.h>

namespace GUI {

class ColumnsView : public AbstractView {
    C_OBJECT(ColumnsView)
public:
    int model_column() const { return m_model_column; }
    void set_model_column(int column) { m_model_column = column; }

    virtual ModelIndex index_at_event_position(Gfx::IntPoint) const override;
    virtual Gfx::IntRect content_rect(ModelIndex const&) const override;
    virtual Gfx::IntRect paint_invalidation_rect(ModelIndex const&) const override;

    virtual void scroll_into_view(ModelIndex const&, bool scroll_horizontally, bool scroll_vertically) override;

private:
    ColumnsView();
    virtual ~ColumnsView() override = default;
    void push_column(ModelIndex const& parent_index);
    void update_column_sizes();

    int item_height() const { return 18; }
    int icon_size() const { return 16; }
    int icon_spacing() const { return 2; }
    int text_padding() const { return 2; }
    int column_separator_width() const { return 1; }

    virtual void model_did_update(unsigned flags) override;
    virtual void second_paint_event(PaintEvent&) override;
    virtual void paint_event(PaintEvent&) override;
    virtual void mousedown_event(MouseEvent& event) override;
    virtual void mousemove_event(MouseEvent&) override;
    virtual void mouseup_event(MouseEvent&) override;

    virtual void select_range(ModelIndex const&) override;

    void move_cursor(CursorMovement, SelectionUpdate) override;

    virtual void select_all() override;
    struct Column {
        ModelIndex parent_index;
        int width;
        // TODO: per-column vertical scroll?
    };

    Optional<Column> column_at_event_position(Gfx::IntPoint) const;
    ModelIndex index_at_event_position_in_column(Gfx::IntPoint, Column const&) const;
    Gfx::IntRect index_content_rect(ModelIndex const&);

    bool m_rubber_banding { false };
    int m_rubber_band_origin { 0 };
    Column m_rubber_band_origin_column;
    int m_rubber_band_current { 0 };

    Vector<Column> m_columns;
    int m_model_column { 0 };
};

}
