/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#pragma once

#include <LibGUI/AbstractView.h>

namespace GUI {

class TableCellPaintingDelegate {
public:
    virtual ~TableCellPaintingDelegate() { }

    virtual void paint(Painter&, const Gfx::IntRect&, const Gfx::Palette&, const ModelIndex&) = 0;
};

class AbstractTableView : public AbstractView {
public:
    int row_height() const { return m_row_height; }
    void set_row_height(int);

    bool alternating_row_colors() const { return m_alternating_row_colors; }
    void set_alternating_row_colors(bool b) { m_alternating_row_colors = b; }
    bool highlight_selected_rows() const { return m_highlight_selected_rows; }
    void set_highlight_selected_rows(bool b) { m_highlight_selected_rows = b; }

    bool column_headers_visible() const;
    void set_column_headers_visible(bool);

    void set_column_hidden(int, bool);

    int column_width(int column) const;
    void set_column_width(int column, int width);

    Gfx::TextAlignment column_header_alignment(int column) const;
    void set_column_header_alignment(int column, Gfx::TextAlignment);

    void set_column_painting_delegate(int column, OwnPtr<TableCellPaintingDelegate>);

    int horizontal_padding() const { return m_horizontal_padding; }

    Gfx::IntPoint adjusted_position(const Gfx::IntPoint&) const;

    virtual Gfx::IntRect content_rect(const ModelIndex&) const override;
    Gfx::IntRect content_rect(int row, int column) const;
    Gfx::IntRect row_rect(int item_index) const;

    virtual void scroll_into_view(const ModelIndex&, bool scroll_horizontally = true, bool scroll_vertically = true) override;
    void scroll_into_view(const ModelIndex& index, Orientation orientation)
    {
        scroll_into_view(index, orientation == Gfx::Orientation::Horizontal, orientation == Gfx::Orientation::Vertical);
    }

    virtual ModelIndex index_at_event_position(const Gfx::IntPoint&, bool& is_toggle) const;
    virtual ModelIndex index_at_event_position(const Gfx::IntPoint&) const override;

    virtual void select_all() override;

    void header_did_change_section_visibility(Badge<HeaderView>, Gfx::Orientation, int section, bool visible);
    void header_did_change_section_size(Badge<HeaderView>, Gfx::Orientation, int section, int size);

    virtual void did_scroll() override;

    HeaderView& column_header() { return *m_column_header; }
    const HeaderView& column_header() const { return *m_column_header; }

    HeaderView& row_header() { return *m_row_header; }
    const HeaderView& row_header() const { return *m_row_header; }

protected:
    virtual ~AbstractTableView() override;
    AbstractTableView();

    virtual void mousedown_event(MouseEvent&) override;
    virtual void doubleclick_event(MouseEvent&) override;
    virtual void context_menu_event(ContextMenuEvent&) override;
    virtual void keydown_event(KeyEvent&) override;
    virtual void resize_event(ResizeEvent&) override;

    virtual void did_update_model(unsigned flags) override;
    virtual void toggle_index(const ModelIndex&) { }

    void update_content_size();
    virtual void update_column_sizes();
    virtual void update_row_sizes();
    virtual int item_count() const;

    TableCellPaintingDelegate* column_painting_delegate(int column) const;

    void move_cursor_relative(int vertical_steps, int horizontal_steps, SelectionUpdate);

private:
    void layout_headers();

    RefPtr<HeaderView> m_column_header;
    RefPtr<HeaderView> m_row_header;
    RefPtr<Button> m_corner_button;

    HashMap<int, OwnPtr<TableCellPaintingDelegate>> m_column_painting_delegate;

    bool m_alternating_row_colors { true };
    bool m_highlight_selected_rows { true };
    int m_horizontal_padding { 5 };
    int m_row_height { 16 };
};

}
