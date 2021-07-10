/*
 * Copyright (c) 2020, Sergey Bugaev <bugaevc@serenityos.org>
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

    virtual ModelIndex index_at_event_position(const Gfx::IntPoint&) const override;
    virtual Gfx::IntRect content_rect(const ModelIndex&) const override;
    virtual Gfx::IntRect paint_invalidation_rect(ModelIndex const&) const override;

private:
    ColumnsView();
    virtual ~ColumnsView() override;
    void push_column(const ModelIndex& parent_index);
    void update_column_sizes();

    int item_height() const { return 18; }
    int icon_size() const { return 16; }
    int icon_spacing() const { return 2; }
    int text_padding() const { return 2; }

    virtual void model_did_update(unsigned flags) override;
    virtual void paint_event(PaintEvent&) override;
    virtual void mousedown_event(MouseEvent& event) override;

    void move_cursor(CursorMovement, SelectionUpdate) override;

    virtual void select_all() override;
    struct Column {
        ModelIndex parent_index;
        int width;
        // TODO: per-column vertical scroll?
    };

    Vector<Column> m_columns;
    int m_model_column { 0 };
};

}
