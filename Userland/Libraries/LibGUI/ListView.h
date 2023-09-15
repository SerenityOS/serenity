/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/AbstractView.h>

namespace GUI {

class ListView : public AbstractView {
    C_OBJECT(ListView)
public:
    virtual ~ListView() override;

    int item_count() const;

    bool alternating_row_colors() const { return m_alternating_row_colors; }
    void set_alternating_row_colors(bool b) { m_alternating_row_colors = b; }

    bool hover_highlighting() const { return m_hover_highlighting; }
    void set_hover_highlighting(bool b) { m_hover_highlighting = b; }

    int item_height() const { return m_item_height.value_or(font().preferred_line_height() + vertical_padding()); }
    void set_item_height(int item_height) { m_item_height = item_height; }
    int horizontal_padding() const { return m_horizontal_padding; }
    void set_horizontal_padding(int horizontal_padding) { m_horizontal_padding = horizontal_padding; }
    int vertical_padding() const { return m_vertical_padding; }
    void set_vertical_padding(int vertical_padding) { m_vertical_padding = vertical_padding; }

    virtual void scroll_into_view(ModelIndex const& index, bool scroll_horizontally, bool scroll_vertically) override;

    Gfx::IntPoint adjusted_position(Gfx::IntPoint) const;

    virtual ModelIndex index_at_event_position(Gfx::IntPoint) const override;
    virtual Gfx::IntRect content_rect(ModelIndex const&) const override;

    int model_column() const { return m_model_column; }
    void set_model_column(int column) { m_model_column = column; }

    virtual void select_all() override;

    Function<void()> on_escape_pressed;

    virtual void move_cursor(CursorMovement, SelectionUpdate) override;
    void move_cursor_relative(int steps, SelectionUpdate);

protected:
    ListView();
    virtual void paint_list_item(Painter&, int row_index, int painted_item_index);

private:
    virtual void model_did_update(unsigned flags) override;
    virtual void paint_event(PaintEvent&) override;
    virtual void keydown_event(KeyEvent&) override;
    virtual void resize_event(ResizeEvent&) override;
    virtual void mousemove_event(MouseEvent&) override;
    virtual void layout_relevant_change_occurred() override;

    Gfx::IntRect content_rect(int row) const;
    void update_content_size();

    Optional<int> m_item_height {};
    int m_horizontal_padding { 2 };
    int m_vertical_padding { 2 };
    int m_model_column { 0 };
    bool m_alternating_row_colors { true };
    bool m_hover_highlighting { false };
};

}
