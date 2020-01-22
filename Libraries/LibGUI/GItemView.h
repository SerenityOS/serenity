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

#include <AK/Function.h>
#include <AK/HashMap.h>
#include <LibGUI/GAbstractView.h>
#include <LibGUI/GModel.h>

class GScrollBar;
class Painter;

class GItemView : public GAbstractView {
    C_OBJECT(GItemView)
public:
    virtual ~GItemView() override;

    int content_width() const;
    int horizontal_padding() const { return m_horizontal_padding; }

    void scroll_into_view(const GModelIndex&, Orientation);
    Size effective_item_size() const { return m_effective_item_size; }

    int model_column() const { return m_model_column; }
    void set_model_column(int column) { m_model_column = column; }

private:
    explicit GItemView(GWidget* parent);

    virtual void did_update_model() override;
    virtual void paint_event(GPaintEvent&) override;
    virtual void second_paint_event(GPaintEvent&) override;
    virtual void resize_event(GResizeEvent&) override;
    virtual void mousedown_event(GMouseEvent&) override;
    virtual void mousemove_event(GMouseEvent&) override;
    virtual void mouseup_event(GMouseEvent&) override;
    virtual void keydown_event(GKeyEvent&) override;
    virtual void doubleclick_event(GMouseEvent&) override;
    virtual void context_menu_event(GContextMenuEvent&) override;

    int item_count() const;
    Rect item_rect(int item_index) const;
    GModelIndex index_at_event_position(const Point&) const;
    Vector<int> items_intersecting_rect(const Rect&) const;
    void update_content_size();
    void get_item_rects(int item_index, const Font&, const GVariant& item_text, Rect& item_rect, Rect& icon_rect, Rect& text_rect) const;

    int m_horizontal_padding { 5 };
    int m_model_column { 0 };
    int m_visual_column_count { 0 };
    int m_visual_row_count { 0 };

    bool m_might_drag { false };

    Point m_left_mousedown_position;

    Size m_effective_item_size { 80, 80 };

    bool m_rubber_banding { false };
    Point m_rubber_band_origin;
    Point m_rubber_band_current;
    Vector<GModelIndex> m_rubber_band_remembered_selection;
};
