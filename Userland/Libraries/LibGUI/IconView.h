/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
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
    virtual ~IconView() override = default;

    enum class FlowDirection {
        LeftToRight,
        TopToBottom,
    };

    FlowDirection flow_direction() const { return m_flow_direction; }
    void set_flow_direction(FlowDirection);

    int horizontal_padding() const { return m_horizontal_padding; }

    virtual void scroll_into_view(ModelIndex const&, bool scroll_horizontally = true, bool scroll_vertically = true) override;

    Gfx::IntSize effective_item_size() const { return m_effective_item_size; }

    bool always_wrap_item_labels() const { return m_always_wrap_item_labels; }
    void set_always_wrap_item_labels(bool value) { m_always_wrap_item_labels = value; }

    int model_column() const { return m_model_column; }
    void set_model_column(int column) { m_model_column = column; }

    virtual ModelIndex index_at_event_position(Gfx::IntPoint) const override;
    virtual Gfx::IntRect content_rect(ModelIndex const&) const override;
    virtual Gfx::IntRect editing_rect(ModelIndex const&) const override;
    virtual Gfx::IntRect paint_invalidation_rect(ModelIndex const&) const override;

    virtual void select_all() override;

protected:
    virtual void did_change_font() override;

private:
    IconView();

    virtual void model_did_update(unsigned flags) override;
    virtual void paint_event(PaintEvent&) override;
    virtual void second_paint_event(PaintEvent&) override;
    virtual void resize_event(ResizeEvent&) override;
    virtual void mousedown_event(MouseEvent&) override;
    virtual void mousemove_event(MouseEvent&) override;
    virtual void mouseup_event(MouseEvent&) override;
    virtual void did_change_hovered_index(ModelIndex const& old_index, ModelIndex const& new_index) override;
    virtual void did_change_cursor_index(ModelIndex const& old_index, ModelIndex const& new_index) override;
    virtual void editing_widget_did_change(ModelIndex const& index) override;

    virtual void move_cursor(CursorMovement, SelectionUpdate) override;

    virtual void automatic_scrolling_timer_did_fire() override;

    struct ItemData {
        Gfx::IntRect text_rect;
        Optional<Gfx::IntRect> text_rect_wrapped;
        Gfx::IntRect icon_rect;
        int icon_offset_y;
        int text_offset_y;
        ByteString text;
        Vector<StringView> wrapped_text_lines;
        ModelIndex index;
        bool valid { false };
        bool selected { false }; // always valid
        bool selection_toggled;  // only used as a temporary marker

        bool is_valid() const { return valid; }
        void invalidate()
        {
            valid = false;
            text = {};
        }

        Gfx::IntRect hot_icon_rect() const { return icon_rect.inflated(10, 10); }
        Gfx::IntRect hot_text_rect() const { return text_rect.inflated(2, 2); }

        bool is_intersecting(Gfx::IntRect const& rect) const
        {
            VERIFY(valid);
            return hot_icon_rect().intersects(rect) || hot_text_rect().intersects(rect);
        }

        bool is_containing(Gfx::IntPoint point) const
        {
            VERIFY(valid);
            return hot_icon_rect().contains(point) || hot_text_rect().contains(point);
        }

        Gfx::IntRect rect(bool wrapped = false) const
        {
            if (wrapped && text_rect_wrapped.has_value())
                return text_rect_wrapped->united(icon_rect);
            return text_rect.united(icon_rect);
        }
    };

    template<typename Function>
    IterationDecision for_each_item_intersecting_rect(Gfx::IntRect const&, Function) const;

    template<typename Function>
    IterationDecision for_each_item_intersecting_rects(Vector<Gfx::IntRect> const&, Function) const;

    void column_row_from_content_position(Gfx::IntPoint content_position, int& row, int& column) const
    {
        row = max(0, min(m_visual_row_count - 1, content_position.y() / effective_item_size().height()));
        column = max(0, min(m_visual_column_count - 1, content_position.x() / effective_item_size().width()));
    }

    int item_count() const;
    Gfx::IntRect item_rect(int item_index) const;
    void update_content_size();
    void update_item_rects(int item_index, ItemData& item_data) const;
    void get_item_rects(int item_index, ItemData& item_data, Gfx::Font const&) const;
    bool update_rubber_banding(Gfx::IntPoint);
    int items_per_page() const;

    void rebuild_item_cache() const;
    int model_index_to_item_index(ModelIndex const& model_index) const
    {
        VERIFY(model_index.row() < item_count());
        return model_index.row();
    }

    virtual void did_update_selection() override;
    virtual void clear_selection() override;
    virtual void add_selection(ModelIndex const& new_index) override;
    virtual void set_selection(ModelIndex const& new_index) override;
    virtual void toggle_selection(ModelIndex const& new_index) override;

    ItemData& get_item_data(int) const;
    ItemData* item_data_from_content_position(Gfx::IntPoint) const;
    void do_clear_selection();
    bool do_add_selection(ItemData&);
    void add_selection(ItemData&);
    void remove_item_selection(ItemData&);
    void toggle_selection(ItemData&);

    int m_horizontal_padding { 5 };
    int m_model_column { 0 };
    int m_visual_column_count { 0 };
    int m_visual_row_count { 0 };

    Gfx::IntSize m_effective_item_size { 80, 80 };

    bool m_always_wrap_item_labels { false };

    bool m_rubber_banding { false };
    Gfx::IntPoint m_out_of_view_position;
    Gfx::IntPoint m_rubber_band_origin;
    Gfx::IntPoint m_rubber_band_current;
    Gfx::IntPoint m_rubber_band_scroll_delta;

    FlowDirection m_flow_direction { FlowDirection::LeftToRight };

    mutable Vector<ItemData> m_item_data_cache;
    mutable int m_selected_count_cache { 0 };
    mutable int m_first_selected_hint { 0 };
    mutable bool m_item_data_cache_valid { false };

    bool m_changing_selection { false };

    bool m_had_valid_size { false };
};

}
