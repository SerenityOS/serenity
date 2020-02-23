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
    virtual ~TableCellPaintingDelegate() {}

    virtual void paint(Painter&, const Gfx::Rect&, const Gfx::Palette&, const Model&, const ModelIndex&) = 0;
};

class AbstractTableView : public AbstractView {
public:
    int item_height() const { return 16; }

    bool alternating_row_colors() const { return m_alternating_row_colors; }
    void set_alternating_row_colors(bool b) { m_alternating_row_colors = b; }

    int header_height() const { return m_headers_visible ? 16 : 0; }

    bool headers_visible() const { return m_headers_visible; }
    void set_headers_visible(bool headers_visible) { m_headers_visible = headers_visible; }

    bool is_column_hidden(int) const;
    void set_column_hidden(int, bool);

    void set_size_columns_to_fit_content(bool b) { m_size_columns_to_fit_content = b; }
    bool size_columns_to_fit_content() const { return m_size_columns_to_fit_content; }

    void set_cell_painting_delegate(int column, OwnPtr<TableCellPaintingDelegate>&&);

    int horizontal_padding() const { return m_horizontal_padding; }

    Gfx::Point adjusted_position(const Gfx::Point&) const;

    virtual Gfx::Rect content_rect(const ModelIndex&) const override;
    Gfx::Rect content_rect(int row, int column) const;
    Gfx::Rect row_rect(int item_index) const;

    void scroll_into_view(const ModelIndex&, Orientation);

    virtual ModelIndex index_at_event_position(const Gfx::Point&, bool& is_toggle) const;
    virtual ModelIndex index_at_event_position(const Gfx::Point&) const override;

protected:
    virtual ~AbstractTableView() override;
    explicit AbstractTableView(Widget* parent = nullptr);

    virtual void did_update_model() override;
    virtual void mouseup_event(MouseEvent&) override;
    virtual void mousedown_event(MouseEvent&) override;
    virtual void mousemove_event(MouseEvent&) override;
    virtual void doubleclick_event(MouseEvent&) override;
    virtual void keydown_event(KeyEvent&) override;
    virtual void leave_event(Core::Event&) override;
    virtual void context_menu_event(ContextMenuEvent&) override;

    virtual void toggle_index(const ModelIndex&) {}

    void paint_headers(Painter&);
    Gfx::Rect header_rect(int column) const;

    static const Gfx::Font& header_font();
    void update_headers();
    void set_hovered_header_index(int);

    struct ColumnData {
        int width { 0 };
        bool has_initialized_width { false };
        bool visibility { true };
        RefPtr<Action> visibility_action;
        OwnPtr<TableCellPaintingDelegate> cell_painting_delegate;
    };
    ColumnData& column_data(int column) const;

    mutable Vector<ColumnData> m_column_data;

    Menu& ensure_header_context_menu();
    RefPtr<Menu> m_header_context_menu;

    Gfx::Rect column_resize_grabbable_rect(int) const;
    int column_width(int) const;
    void update_content_size();
    virtual void update_column_sizes();
    virtual int item_count() const;

private:
    bool m_headers_visible { true };
    bool m_size_columns_to_fit_content { false };
    bool m_in_column_resize { false };
    bool m_alternating_row_colors { true };
    int m_horizontal_padding { 5 };
    Gfx::Point m_column_resize_origin;
    int m_column_resize_original_width { 0 };
    int m_resizing_column { -1 };
    int m_pressed_column_header_index { -1 };
    bool m_pressed_column_header_is_pressed { false };
    int m_hovered_column_header_index { -1 };
};

}
