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

#include <LibGUI/GAbstractView.h>

class GPainter;

// FIXME: Rename this to something without "table cell" in the name.
class GTableCellPaintingDelegate {
public:
    virtual ~GTableCellPaintingDelegate() {}

    virtual void paint(GPainter&, const Rect&, const Palette&, const GModel&, const GModelIndex&) = 0;
};

class GAbstractColumnView : public GAbstractView {
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

    void set_cell_painting_delegate(int column, OwnPtr<GTableCellPaintingDelegate>&&);

    int horizontal_padding() const { return m_horizontal_padding; }

    Point adjusted_position(const Point&) const;

    virtual Rect content_rect(const GModelIndex&) const override;
    Rect content_rect(int row, int column) const;
    Rect row_rect(int item_index) const;

    void scroll_into_view(const GModelIndex&, Orientation);

protected:
    virtual ~GAbstractColumnView() override;
    explicit GAbstractColumnView(GWidget* parent);

    virtual void did_update_model() override;
    virtual void mouseup_event(GMouseEvent&) override;
    virtual void mousedown_event(GMouseEvent&) override;
    virtual void mousemove_event(GMouseEvent&) override;
    virtual void doubleclick_event(GMouseEvent&) override;
    virtual void keydown_event(GKeyEvent&) override;
    virtual void leave_event(CEvent&) override;
    virtual void context_menu_event(GContextMenuEvent&) override;

    virtual GModelIndex index_at_event_position(const Point&, bool& is_toggle) const;
    virtual void toggle_index(const GModelIndex&) {}

    void paint_headers(GPainter&);
    Rect header_rect(int column) const;

    static const Font& header_font();
    void update_headers();
    void set_hovered_header_index(int);

    struct ColumnData {
        int width { 0 };
        bool has_initialized_width { false };
        bool visibility { true };
        RefPtr<GAction> visibility_action;
        OwnPtr<GTableCellPaintingDelegate> cell_painting_delegate;
    };
    ColumnData& column_data(int column) const;

    mutable Vector<ColumnData> m_column_data;

    GMenu& ensure_header_context_menu();
    RefPtr<GMenu> m_header_context_menu;

    Rect column_resize_grabbable_rect(int) const;
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
    Point m_column_resize_origin;
    int m_column_resize_original_width { 0 };
    int m_resizing_column { -1 };
    int m_pressed_column_header_index { -1 };
    bool m_pressed_column_header_is_pressed { false };
    int m_hovered_column_header_index { -1 };
};
