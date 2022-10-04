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
    FlexFormattingContext(LayoutState&, Box const& flex_container, FormattingContext* parent);
    ~FlexFormattingContext();

    virtual bool inhibits_floating() const override { return true; }

    virtual void run(Box const&, LayoutMode, AvailableSpace const&) override;
    virtual float automatic_content_height() const override;

    Box const& flex_container() const { return context_box(); }

    virtual bool can_determine_size_of_child() const override;
    virtual void determine_width_of_child(Box const&, AvailableSpace const&) override;
    virtual void determine_height_of_child(Box const&, AvailableSpace const&) override;

private:
    void dump_items() const;

    struct DirectionAgnosticMargins {
        float main_before { 0 };
        float main_after { 0 };
        float cross_before { 0 };
        float cross_after { 0 };

        bool main_before_is_auto { false };
        bool main_after_is_auto { false };
        bool cross_before_is_auto { false };
        bool cross_after_is_auto { false };
    };

    struct FlexItem {
        Box& box;
        CSS::FlexBasisData used_flex_basis {};
        bool used_flex_basis_is_definite { false };
        float flex_base_size { 0 };
        float hypothetical_main_size { 0 };
        float hypothetical_cross_size { 0 };
        float hypothetical_cross_size_with_margins() { return hypothetical_cross_size + margins.cross_before + margins.cross_after + borders.cross_after + borders.cross_before + padding.cross_after + padding.cross_before; }
        float target_main_size { 0 };
        bool frozen { false };
        Optional<float> flex_factor {};
        float scaled_flex_shrink_factor { 0 };
        float desired_flex_fraction { 0 };
        float main_size { 0 };
        float cross_size { 0 };
        float main_offset { 0 };
        float cross_offset { 0 };
        DirectionAgnosticMargins margins {};
        DirectionAgnosticMargins borders {};
        DirectionAgnosticMargins padding {};
        bool is_min_violation { false };
        bool is_max_violation { false };

        float add_main_margin_box_sizes(float content_size) const
        {
            return content_size + margins.main_before + margins.main_after + borders.main_before + borders.main_after + padding.main_before + padding.main_after;
        }

        float add_cross_margin_box_sizes(float content_size) const
        {
            return content_size + margins.cross_before + margins.cross_after + borders.cross_before + borders.cross_after + padding.cross_before + padding.cross_after;
        }
    };

    struct FlexLine {
        Vector<FlexItem*> items;
        float cross_size { 0 };
        float remaining_free_space { 0 };
        float chosen_flex_fraction { 0 };
    };

    bool has_definite_main_size(Box const&) const;
    bool has_definite_cross_size(Box const&) const;
    float specified_main_size(Box const&) const;
    float specified_cross_size(Box const&) const;
    float resolved_definite_main_size(FlexItem const&) const;
    float resolved_definite_cross_size(FlexItem const&) const;
    bool has_main_min_size(Box const&) const;
    bool has_cross_min_size(Box const&) const;
    float specified_main_max_size(Box const&) const;
    float specified_cross_max_size(Box const&) const;
    bool is_cross_auto(Box const&) const;
    float specified_main_min_size(Box const&) const;
    float specified_cross_min_size(Box const&) const;
    bool has_main_max_size(Box const&) const;
    bool has_cross_max_size(Box const&) const;
    float automatic_minimum_size(FlexItem const&) const;
    float content_based_minimum_size(FlexItem const&) const;
    Optional<float> specified_size_suggestion(FlexItem const&) const;
    Optional<float> transferred_size_suggestion(FlexItem const&) const;
    float content_size_suggestion(FlexItem const&) const;
    CSS::Size const& computed_main_size(Box const&) const;
    CSS::Size const& computed_main_min_size(Box const&) const;
    CSS::Size const& computed_main_max_size(Box const&) const;
    CSS::Size const& computed_cross_size(Box const&) const;
    CSS::Size const& computed_cross_min_size(Box const&) const;
    CSS::Size const& computed_cross_max_size(Box const&) const;

    float get_pixel_width(Box const& box, Optional<CSS::Size> const& length_percentage) const;
    float get_pixel_height(Box const& box, Optional<CSS::Size> const& length_percentage) const;

    bool flex_item_is_stretched(FlexItem const&) const;

    void set_main_size(Box const&, float size);
    void set_cross_size(Box const&, float size);
    void set_offset(Box const&, float main_offset, float cross_offset);
    void set_main_axis_first_margin(FlexItem&, float margin);
    void set_main_axis_second_margin(FlexItem&, float margin);

    void copy_dimensions_from_flex_items_to_boxes();

    void generate_anonymous_flex_items();

    void determine_available_space_for_items(AvailableSpace const&);

    float calculate_indefinite_main_size(FlexItem const&);
    void determine_flex_base_size_and_hypothetical_main_size(FlexItem&);

    void determine_main_size_of_flex_container();

    void collect_flex_items_into_flex_lines();

    void resolve_flexible_lengths();

    void resolve_cross_axis_auto_margins();

    void determine_hypothetical_cross_size_of_item(FlexItem&, bool resolve_percentage_min_max_sizes);

    void calculate_cross_size_of_each_flex_line(float cross_min_size, float cross_max_size);

    CSS::AlignItems alignment_for_item(FlexItem const&) const;

    void determine_used_cross_size_of_each_flex_item();

    void distribute_any_remaining_free_space();

    void align_all_flex_items_along_the_cross_axis();

    void determine_flex_container_used_cross_size(float cross_min_size, float cross_max_size);

    void align_all_flex_lines();

    bool is_row_layout() const { return m_flex_direction == CSS::FlexDirection::Row || m_flex_direction == CSS::FlexDirection::RowReverse; }
    bool is_single_line() const { return flex_container().computed_values().flex_wrap() == CSS::FlexWrap::Nowrap; }
    bool is_direction_reverse() const { return m_flex_direction == CSS::FlexDirection::ColumnReverse || m_flex_direction == CSS::FlexDirection::RowReverse; }
    void populate_specified_margins(FlexItem&, CSS::FlexDirection) const;

    void determine_intrinsic_size_of_flex_container();
    [[nodiscard]] float calculate_intrinsic_main_size_of_flex_container();
    [[nodiscard]] float calculate_intrinsic_cross_size_of_flex_container();

    [[nodiscard]] float calculate_cross_min_content_contribution(FlexItem const&, bool resolve_percentage_min_max_sizes) const;
    [[nodiscard]] float calculate_cross_max_content_contribution(FlexItem const&, bool resolve_percentage_min_max_sizes) const;
    [[nodiscard]] float calculate_main_min_content_contribution(FlexItem const&) const;
    [[nodiscard]] float calculate_main_max_content_contribution(FlexItem const&) const;

    [[nodiscard]] float calculate_min_content_main_size(FlexItem const&) const;
    [[nodiscard]] float calculate_max_content_main_size(FlexItem const&) const;
    [[nodiscard]] float calculate_min_content_cross_size(FlexItem const&) const;
    [[nodiscard]] float calculate_max_content_cross_size(FlexItem const&) const;

    [[nodiscard]] float calculate_fit_content_main_size(FlexItem const&) const;
    [[nodiscard]] float calculate_fit_content_cross_size(FlexItem const&) const;

    virtual void parent_context_did_dimension_child_root_box() override;

    CSS::FlexBasisData used_flex_basis_for_item(FlexItem const&) const;

    LayoutState::UsedValues& m_flex_container_state;

    Vector<FlexLine> m_flex_lines;
    Vector<FlexItem> m_flex_items;
    CSS::FlexDirection m_flex_direction {};

    struct AxisAgnosticAvailableSpace {
        AvailableSize main;
        AvailableSize cross;
        AvailableSpace space;
    };
    Optional<AxisAgnosticAvailableSpace> m_available_space_for_items;
    Optional<AxisAgnosticAvailableSpace> m_available_space_for_flex_container;
};

}
