/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/Layout/Box.h>
#include <LibWeb/Layout/FormattingContext.h>

namespace Web::Layout {

class FlexFormattingContext final : public FormattingContext {
public:
    FlexFormattingContext(FormattingState&, Box const& flex_container, FormattingContext* parent);
    ~FlexFormattingContext();

    virtual bool inhibits_floating() const override { return true; }

    virtual void run(Box const&, LayoutMode) override;

    Box const& flex_container() const { return context_box(); }

private:
    struct DirectionAgnosticMargins {
        float main_before { 0 };
        float main_after { 0 };
        float cross_before { 0 };
        float cross_after { 0 };
    };

    struct FlexItem {
        Box& box;
        float flex_base_size { 0 };
        float hypothetical_main_size { 0 };
        float hypothetical_cross_size { 0 };
        float hypothetical_cross_size_with_margins() { return hypothetical_cross_size + margins.cross_before + margins.cross_after; }
        float target_main_size { 0 };
        bool frozen { false };
        Optional<float> flex_factor {};
        float scaled_flex_shrink_factor { 0 };
        float max_content_flex_fraction { 0 };
        float main_size { 0 };
        float cross_size { 0 };
        float main_offset { 0 };
        float cross_offset { 0 };
        DirectionAgnosticMargins margins {};
        bool is_min_violation { false };
        bool is_max_violation { false };
    };

    struct FlexLine {
        Vector<FlexItem*> items;
        float cross_size { 0 };
    };

    bool has_definite_main_size(Box const&) const;
    bool has_definite_cross_size(Box const&) const;
    float specified_main_size(Box const&) const;
    float specified_cross_size(Box const&) const;
    bool has_main_min_size(Box const&) const;
    bool has_cross_min_size(Box const&) const;
    bool cross_size_is_absolute_or_resolved_nicely(NodeWithStyle const&) const;
    float specified_main_max_size(Box const&) const;
    float specified_cross_max_size(Box const&) const;
    float calculated_main_size(Box const&) const;
    bool is_cross_auto(Box const&) const;
    bool is_main_axis_margin_first_auto(Box const&) const;
    bool is_main_axis_margin_second_auto(Box const&) const;
    float specified_main_size_of_child_box(Box const& child_box) const;
    float specified_main_min_size(Box const&) const;
    float specified_cross_min_size(Box const&) const;
    bool has_main_max_size(Box const&) const;
    bool has_cross_max_size(Box const&) const;
    float sum_of_margin_padding_border_in_main_axis(Box const&) const;

    void set_main_size(Box const&, float size);
    void set_cross_size(Box const&, float size);
    void set_offset(Box const&, float main_offset, float cross_offset);
    void set_main_axis_first_margin(Box const&, float margin);
    void set_main_axis_second_margin(Box const&, float margin);

    void generate_anonymous_flex_items();

    struct AvailableSpace {
        float main { 0 };
        float cross { 0 };
    };
    AvailableSpace determine_available_main_and_cross_space(bool& main_size_is_infinite, bool& main_is_constrained, bool& cross_is_constrained, float& main_min_size, float& main_max_size, float& cross_min_size, float& cross_max_size) const;

    float layout_for_maximum_main_size(Box const&);
    void determine_flex_base_size_and_hypothetical_main_size(FlexItem&);

    void determine_main_size_of_flex_container(bool main_is_constrained, bool main_size_is_infinite, float& main_available_size, float main_min_size, float main_max_size);

    void collect_flex_items_into_flex_lines(float main_available_size);

    void resolve_flexible_lengths(float main_available_size);

    float determine_hypothetical_cross_size_of_item(Box const&);

    void calculate_cross_size_of_each_flex_line(float cross_min_size, float cross_max_size);

    void determine_used_cross_size_of_each_flex_item();

    void distribute_any_remaining_free_space(float main_available_size);

    void align_all_flex_items_along_the_cross_axis();

    void determine_flex_container_used_cross_size(float cross_min_size, float cross_max_size);

    void align_all_flex_lines();

    bool is_row_layout() const { return m_flex_direction == CSS::FlexDirection::Row || m_flex_direction == CSS::FlexDirection::RowReverse; }
    bool is_single_line() const { return flex_container().computed_values().flex_wrap() == CSS::FlexWrap::Nowrap; }

    void populate_specified_margins(FlexItem&, CSS::FlexDirection) const;

    FormattingState::NodeState& m_flex_container_state;

    Vector<FlexLine> m_flex_lines;
    Vector<FlexItem> m_flex_items;
    CSS::FlexDirection m_flex_direction {};
};

}
