/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, networkException <networkexception@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 * Copyright (c) 2023, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/AbstractView.h>
#include <LibGUI/Button.h>
#include <LibGUI/HeaderView.h>

namespace GUI {

class TableCellPaintingDelegate {
public:
    virtual ~TableCellPaintingDelegate() = default;

    virtual bool should_paint(ModelIndex const&) { return true; }
    virtual void paint(Painter&, Gfx::IntRect const&, Gfx::Palette const&, ModelIndex const&) = 0;
};

class AbstractTableView : public AbstractView {
public:
    int row_height() const { return font().pixel_size_rounded_up() + vertical_padding(); }

    virtual int horizontal_padding() const { return m_horizontal_padding; }
    void set_horizontal_padding(int padding) { m_horizontal_padding = padding; }
    virtual int vertical_padding() const { return m_vertical_padding; }
    void set_vertical_padding(int padding) { m_vertical_padding = padding; }

    bool alternating_row_colors() const { return m_alternating_row_colors; }
    void set_alternating_row_colors(bool b) { m_alternating_row_colors = b; }
    bool highlight_selected_rows() const { return m_highlight_selected_rows; }
    void set_highlight_selected_rows(bool b) { m_highlight_selected_rows = b; }

    bool column_headers_visible() const;
    void set_column_headers_visible(bool);

    void set_column_visible(int, bool);
    // These return/accept a comma-separated list of column ids, for storing in a config file.
    ErrorOr<String> get_visible_columns() const;
    void set_visible_columns(StringView column_ids);
    Function<void()> on_visible_columns_changed;

    int column_width(int column) const;
    void set_column_width(int column, int width);
    void set_default_column_width(int column, int width);
    virtual int minimum_column_width(int column);
    virtual int minimum_row_height(int row);

    Gfx::TextAlignment column_header_alignment(int column) const;
    void set_column_header_alignment(int column, Gfx::TextAlignment);

    void set_column_painting_delegate(int column, OwnPtr<TableCellPaintingDelegate>);

    Gfx::IntPoint adjusted_position(Gfx::IntPoint) const;

    virtual Gfx::IntRect content_rect(ModelIndex const&) const override;
    Gfx::IntRect content_rect_minus_scrollbars(ModelIndex const&) const;
    Gfx::IntRect content_rect(int row, int column) const;
    Gfx::IntRect row_rect(int item_index) const;

    virtual Gfx::IntRect paint_invalidation_rect(ModelIndex const& index) const override;

    virtual void scroll_into_view(ModelIndex const&, bool scroll_horizontally = true, bool scroll_vertically = true) override;
    void scroll_into_view(ModelIndex const& index, Orientation orientation)
    {
        scroll_into_view(index, orientation == Gfx::Orientation::Horizontal, orientation == Gfx::Orientation::Vertical);
    }

    virtual ModelIndex index_at_event_position(Gfx::IntPoint, bool& is_toggle) const;
    virtual ModelIndex index_at_event_position(Gfx::IntPoint) const override;

    virtual void select_all() override;

    void header_did_change_section_visibility(Badge<HeaderView>, Gfx::Orientation, int section, bool visible);
    void header_did_change_section_size(Badge<HeaderView>, Gfx::Orientation, int section, int size);

    virtual void did_scroll() override;

    HeaderView& column_header() { return *m_column_header; }
    HeaderView const& column_header() const { return *m_column_header; }

    HeaderView& row_header() { return *m_row_header; }
    HeaderView const& row_header() const { return *m_row_header; }

    virtual void model_did_update(unsigned flags) override;

protected:
    virtual ~AbstractTableView() override = default;
    AbstractTableView();

    virtual void mousedown_event(MouseEvent&) override;
    virtual void context_menu_event(ContextMenuEvent&) override;
    virtual void keydown_event(KeyEvent&) override;
    virtual void resize_event(ResizeEvent&) override;

    virtual void toggle_index(ModelIndex const&) { }

    void update_content_size();
    virtual void auto_resize_column(int column);
    virtual void update_column_sizes();
    virtual void update_row_sizes();
    virtual int item_count() const;

    TableCellPaintingDelegate* column_painting_delegate(int column) const;

    void move_cursor_relative(int vertical_steps, int horizontal_steps, SelectionUpdate);

    virtual Gfx::IntPoint automatic_scroll_delta_from_position(Gfx::IntPoint pos) const override;

private:
    void layout_headers();
    bool is_navigation(GUI::KeyEvent&);

    RefPtr<HeaderView> m_column_header;
    RefPtr<HeaderView> m_row_header;
    RefPtr<Button> m_corner_button;

    HashMap<int, OwnPtr<TableCellPaintingDelegate>> m_column_painting_delegate;

    bool m_alternating_row_colors { true };
    bool m_highlight_selected_rows { true };

    int m_vertical_padding { 8 };
    int m_horizontal_padding { font().pixel_size_rounded_up() / 2 };
    int m_tab_moves { 0 };
};

}
