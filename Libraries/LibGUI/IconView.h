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

#include <AK/IterationDecision.h>
#include <LibGUI/AbstractView.h>
#include <LibGUI/Forward.h>
#include <LibGUI/Variant.h>

namespace GUI {

class IconView : public AbstractView {
    C_OBJECT(IconView)
public:
    virtual ~IconView() override;

    int content_width() const;
    int horizontal_padding() const { return m_horizontal_padding; }

    virtual void scroll_into_view(const ModelIndex&, bool scroll_horizontally = true, bool scroll_vertically = true) override;

    Gfx::IntSize effective_item_size() const { return m_effective_item_size; }

    int model_column() const { return m_model_column; }
    void set_model_column(int column) { m_model_column = column; }

    virtual ModelIndex index_at_event_position(const Gfx::IntPoint&) const override;
    virtual Gfx::IntRect content_rect(const ModelIndex&) const override;

    virtual void select_all() override;

private:
    IconView();

    virtual void did_update_model(unsigned flags) override;
    virtual void paint_event(PaintEvent&) override;
    virtual void second_paint_event(PaintEvent&) override;
    virtual void resize_event(ResizeEvent&) override;
    virtual void mousedown_event(MouseEvent&) override;
    virtual void mousemove_event(MouseEvent&) override;
    virtual void mouseup_event(MouseEvent&) override;
    virtual void drag_move_event(DragEvent&) override;

    virtual void move_cursor(CursorMovement, SelectionUpdate) override;

    struct ItemData {
        Gfx::IntRect text_rect;
        Gfx::IntRect icon_rect;
        int icon_offset_y;
        int text_offset_y;
        Variant data;
        ModelIndex index;
        bool valid { false };
        bool selected { false }; // always valid
        bool selection_toggled;  // only used as a temporary marker

        bool is_valid() const { return valid; }
        void invalidate()
        {
            valid = false;
            data.clear();
        }

        bool is_intersecting(const Gfx::IntRect& rect) const
        {
            ASSERT(valid);
            return icon_rect.intersects(rect) || text_rect.intersects(rect);
        }

        bool is_containing(const Gfx::IntPoint& point) const
        {
            ASSERT(valid);
            return icon_rect.contains(point) || text_rect.contains(point);
        }
    };

    template<typename Function>
    IterationDecision for_each_item_intersecting_rect(const Gfx::IntRect& rect, Function f) const
    {
        ASSERT(model());
        if (rect.is_empty())
            return IterationDecision::Continue;
        int begin_row, begin_column;
        column_row_from_content_position(rect.top_left(), begin_row, begin_column);
        int end_row, end_column;
        column_row_from_content_position(rect.bottom_right(), end_row, end_column);
        int items_per_column = end_column - begin_column + 1;
        int item_index = max(0, begin_row * m_visual_column_count + begin_column);
        int last_index = min(item_count(), end_row * m_visual_column_count + end_column + 1);
        while (item_index < last_index) {
            for (int i = item_index; i < min(item_index + items_per_column, last_index); i++) {
                auto& item_data = get_item_data(i);
                if (item_data.is_intersecting(rect)) {
                    auto decision = f(item_data);
                    if (decision != IterationDecision::Continue)
                        return decision;
                }
            }
            item_index += m_visual_column_count;
        };
        return IterationDecision::Continue;
    }

    template<typename Function>
    IterationDecision for_each_item_intersecting_rects(const Vector<Gfx::IntRect>& rects, Function f) const
    {
        for (auto& rect : rects) {
            auto decision = for_each_item_intersecting_rect(rect, f);
            if (decision != IterationDecision::Continue)
                return decision;
        }
        return IterationDecision::Continue;
    }

    void column_row_from_content_position(const Gfx::IntPoint& content_position, int& row, int& column) const
    {
        row = max(0, min(m_visual_row_count - 1, content_position.y() / effective_item_size().height()));
        column = max(0, min(m_visual_column_count - 1, content_position.x() / effective_item_size().width()));
    }

    int item_count() const;
    Gfx::IntRect item_rect(int item_index) const;
    void update_content_size();
    void update_item_rects(int item_index, ItemData& item_data) const;
    void get_item_rects(int item_index, ItemData& item_data, const Gfx::Font&) const;
    bool update_rubber_banding(const Gfx::IntPoint&);
    void scroll_out_of_view_timer_fired();

    void reinit_item_cache() const;
    int model_index_to_item_index(const ModelIndex& model_index) const
    {
        ASSERT(model_index.row() < item_count());
        return model_index.row();
    }

    virtual void did_update_selection() override;
    virtual void clear_selection() override;
    virtual void add_selection(const ModelIndex& new_index) override;
    virtual void set_selection(const ModelIndex& new_index) override;
    virtual void toggle_selection(const ModelIndex& new_index) override;

    ItemData& get_item_data(int) const;
    ItemData* item_data_from_content_position(const Gfx::IntPoint&) const;
    void do_clear_selection();
    bool do_add_selection(ItemData&);
    void add_selection(ItemData&);
    void remove_selection(ItemData&);
    void toggle_selection(ItemData&);

    int m_horizontal_padding { 5 };
    int m_model_column { 0 };
    int m_visual_column_count { 0 };
    int m_visual_row_count { 0 };

    Gfx::IntSize m_effective_item_size { 80, 80 };

    bool m_rubber_banding { false };
    bool m_rubber_banding_store_selection { false };
    RefPtr<Core::Timer> m_out_of_view_timer;
    Gfx::IntPoint m_out_of_view_position;
    Gfx::IntPoint m_rubber_band_origin;
    Gfx::IntPoint m_rubber_band_current;

    ModelIndex m_drop_candidate_index;

    mutable Vector<ItemData> m_item_data_cache;
    mutable int m_selected_count_cache { 0 };
    mutable int m_first_selected_hint { 0 };
    mutable bool m_item_data_cache_valid { false };

    bool m_changing_selection { false };
};

}
