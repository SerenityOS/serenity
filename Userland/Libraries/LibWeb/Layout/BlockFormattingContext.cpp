/*
 * Copyright (c) 2020-2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/CSS/Length.h>
#include <LibWeb/DOM/Node.h>
#include <LibWeb/HTML/BrowsingContext.h>
#include <LibWeb/Layout/BlockContainer.h>
#include <LibWeb/Layout/BlockFormattingContext.h>
#include <LibWeb/Layout/Box.h>
#include <LibWeb/Layout/InitialContainingBlock.h>
#include <LibWeb/Layout/InlineFormattingContext.h>
#include <LibWeb/Layout/ListItemBox.h>
#include <LibWeb/Layout/ListItemMarkerBox.h>
#include <LibWeb/Layout/ReplacedBox.h>

namespace Web::Layout {

BlockFormattingContext::BlockFormattingContext(FormattingState& state, BlockContainer const& root, FormattingContext* parent)
    : FormattingContext(Type::Block, state, root, parent)
{
}

BlockFormattingContext::~BlockFormattingContext()
{
    if (!m_was_notified_after_parent_dimensioned_my_root_box) {
        // HACK: The parent formatting context never notified us after assigning dimensions to our root box.
        //       Pretend that it did anyway, to make sure absolutely positioned children get laid out.
        // FIXME: Get rid of this hack once parent contexts behave properly.
        parent_context_did_dimension_child_root_box();
    }
}

bool BlockFormattingContext::is_initial() const
{
    return is<InitialContainingBlock>(root());
}

void BlockFormattingContext::run(Box const&, LayoutMode layout_mode)
{
    if (is_initial()) {
        layout_initial_containing_block(layout_mode);
        return;
    }

    if (root().children_are_inline())
        layout_inline_children(root(), layout_mode);
    else
        layout_block_level_children(root(), layout_mode);
}

void BlockFormattingContext::parent_context_did_dimension_child_root_box()
{
    m_was_notified_after_parent_dimensioned_my_root_box = true;

    for (auto& box : m_absolutely_positioned_boxes)
        layout_absolutely_positioned_element(box);

    apply_transformations_to_children(root());
}

void BlockFormattingContext::apply_transformations_to_children(Box const& box)
{
    box.for_each_child_of_type<Box>([&](auto& child_box) {
        float transform_y_offset = 0.0f;
        if (!child_box.computed_values().transformations().is_empty()) {
            // FIXME: All transformations can be interpreted as successive 3D-matrix operations on the box, we don't do that yet.
            //        https://drafts.csswg.org/css-transforms/#serialization-of-the-computed-value
            for (auto transformation : child_box.computed_values().transformations()) {
                switch (transformation.function) {
                case CSS::TransformFunction::TranslateY:
                    if (transformation.values.size() != 1)
                        continue;
                    transformation.values.first().visit(
                        [&](CSS::Length& value) {
                            transform_y_offset += value.to_px(child_box);
                        },
                        [&](float value) {
                            transform_y_offset += value;
                        },
                        [&](auto&) {
                            dbgln("FIXME: Implement unsupported transformation function value type!");
                        });
                    break;
                default:
                    dbgln("FIXME: Implement missing transform function!");
                }
            }
        }

        auto& child_box_state = m_state.get_mutable(child_box);
        auto untransformed_offset = child_box_state.offset;
        child_box_state.offset = Gfx::FloatPoint { untransformed_offset.x(), untransformed_offset.y() + transform_y_offset };
    });
}

void BlockFormattingContext::compute_width(Box const& box)
{
    if (box.is_absolutely_positioned()) {
        compute_width_for_absolutely_positioned_element(box);
        return;
    }

    if (is<ReplacedBox>(box)) {
        // FIXME: This should not be done *by* ReplacedBox
        auto& replaced = verify_cast<ReplacedBox>(box);
        // FIXME: This const_cast is gross.
        const_cast<ReplacedBox&>(replaced).prepare_for_replaced_layout();
        compute_width_for_block_level_replaced_element_in_normal_flow(replaced);
        return;
    }

    if (box.is_floating()) {
        compute_width_for_floating_box(box);
        return;
    }

    auto const& computed_values = box.computed_values();
    float width_of_containing_block = m_state.get(*box.containing_block()).content_width;
    auto width_of_containing_block_as_length = CSS::Length::make_px(width_of_containing_block);

    auto zero_value = CSS::Length::make_px(0);

    auto margin_left = CSS::Length::make_auto();
    auto margin_right = CSS::Length::make_auto();
    const auto padding_left = computed_values.padding().left.resolved(box, width_of_containing_block_as_length).resolved(box);
    const auto padding_right = computed_values.padding().right.resolved(box, width_of_containing_block_as_length).resolved(box);

    auto try_compute_width = [&](const auto& a_width) {
        CSS::Length width = a_width;
        margin_left = computed_values.margin().left.resolved(box, width_of_containing_block_as_length).resolved(box);
        margin_right = computed_values.margin().right.resolved(box, width_of_containing_block_as_length).resolved(box);

        float total_px = computed_values.border_left().width + computed_values.border_right().width;
        for (auto& value : { margin_left, padding_left, width, padding_right, margin_right }) {
            total_px += value.to_px(box);
        }

        if (!box.is_inline()) {
            // 10.3.3 Block-level, non-replaced elements in normal flow
            // If 'width' is not 'auto' and 'border-left-width' + 'padding-left' + 'width' + 'padding-right' + 'border-right-width' (plus any of 'margin-left' or 'margin-right' that are not 'auto') is larger than the width of the containing block, then any 'auto' values for 'margin-left' or 'margin-right' are, for the following rules, treated as zero.
            if (width.is_auto() && total_px > width_of_containing_block) {
                if (margin_left.is_auto())
                    margin_left = zero_value;
                if (margin_right.is_auto())
                    margin_right = zero_value;
            }

            // 10.3.3 cont'd.
            auto underflow_px = width_of_containing_block - total_px;

            if (width.is_auto()) {
                if (margin_left.is_auto())
                    margin_left = zero_value;
                if (margin_right.is_auto())
                    margin_right = zero_value;
                if (underflow_px >= 0) {
                    width = CSS::Length(underflow_px, CSS::Length::Type::Px);
                } else {
                    width = zero_value;
                    margin_right = CSS::Length(margin_right.to_px(box) + underflow_px, CSS::Length::Type::Px);
                }
            } else {
                if (!margin_left.is_auto() && !margin_right.is_auto()) {
                    margin_right = CSS::Length(margin_right.to_px(box) + underflow_px, CSS::Length::Type::Px);
                } else if (!margin_left.is_auto() && margin_right.is_auto()) {
                    margin_right = CSS::Length(underflow_px, CSS::Length::Type::Px);
                } else if (margin_left.is_auto() && !margin_right.is_auto()) {
                    margin_left = CSS::Length(underflow_px, CSS::Length::Type::Px);
                } else { // margin_left.is_auto() && margin_right.is_auto()
                    auto half_of_the_underflow = CSS::Length(underflow_px / 2, CSS::Length::Type::Px);
                    margin_left = half_of_the_underflow;
                    margin_right = half_of_the_underflow;
                }
            }
        } else if (box.is_inline_block()) {

            // 10.3.9 'Inline-block', non-replaced elements in normal flow

            // A computed value of 'auto' for 'margin-left' or 'margin-right' becomes a used value of '0'.
            if (margin_left.is_auto())
                margin_left = zero_value;
            if (margin_right.is_auto())
                margin_right = zero_value;

            // If 'width' is 'auto', the used value is the shrink-to-fit width as for floating elements.
            if (width.is_auto()) {

                // Find the available width: in this case, this is the width of the containing
                // block minus the used values of 'margin-left', 'border-left-width', 'padding-left',
                // 'padding-right', 'border-right-width', 'margin-right', and the widths of any relevant scroll bars.
                float available_width = width_of_containing_block
                    - margin_left.to_px(box) - computed_values.border_left().width - padding_left.to_px(box)
                    - padding_right.to_px(box) - computed_values.border_right().width - margin_right.to_px(box);

                auto result = calculate_shrink_to_fit_widths(box);

                // Then the shrink-to-fit width is: min(max(preferred minimum width, available width), preferred width).
                width = CSS::Length(min(max(result.preferred_minimum_width, available_width), result.preferred_width), CSS::Length::Type::Px);
            }
        }

        return width;
    };

    auto specified_width = computed_values.width().has_value() ? computed_values.width()->resolved(box, width_of_containing_block_as_length).resolved(box) : CSS::Length::make_auto();

    // 1. The tentative used width is calculated (without 'min-width' and 'max-width')
    auto used_width = try_compute_width(specified_width);

    // 2. The tentative used width is greater than 'max-width', the rules above are applied again,
    //    but this time using the computed value of 'max-width' as the computed value for 'width'.
    auto specified_max_width = computed_values.max_width().has_value() ? computed_values.max_width()->resolved(box, width_of_containing_block_as_length).resolved(box) : CSS::Length::make_auto();
    if (!specified_max_width.is_auto()) {
        if (used_width.to_px(box) > specified_max_width.to_px(box)) {
            used_width = try_compute_width(specified_max_width);
        }
    }

    // 3. If the resulting width is smaller than 'min-width', the rules above are applied again,
    //    but this time using the value of 'min-width' as the computed value for 'width'.
    auto specified_min_width = computed_values.min_width().has_value() ? computed_values.min_width()->resolved(box, width_of_containing_block_as_length).resolved(box) : CSS::Length::make_auto();
    if (!specified_min_width.is_auto()) {
        if (used_width.to_px(box) < specified_min_width.to_px(box)) {
            used_width = try_compute_width(specified_min_width);
        }
    }

    auto& box_state = m_state.get_mutable(box);
    box_state.content_width = used_width.to_px(box);
    box_state.margin_left = margin_left.to_px(box);
    box_state.margin_right = margin_right.to_px(box);
    box_state.border_left = computed_values.border_left().width;
    box_state.border_right = computed_values.border_right().width;
    box_state.padding_left = padding_left.to_px(box);
    box_state.padding_right = padding_right.to_px(box);
}

void BlockFormattingContext::compute_width_for_floating_box(Box const& box)
{
    // 10.3.5 Floating, non-replaced elements
    auto& computed_values = box.computed_values();
    auto& containing_block = *box.containing_block();
    float width_of_containing_block = m_state.get(containing_block).content_width;
    auto width_of_containing_block_as_length = CSS::Length::make_px(width_of_containing_block);
    auto zero_value = CSS::Length::make_px(0);

    auto margin_left = computed_values.margin().left.resolved(box, width_of_containing_block_as_length).resolved(box);
    auto margin_right = computed_values.margin().right.resolved(box, width_of_containing_block_as_length).resolved(box);
    const auto padding_left = computed_values.padding().left.resolved(box, width_of_containing_block_as_length).resolved(box);
    const auto padding_right = computed_values.padding().right.resolved(box, width_of_containing_block_as_length).resolved(box);

    // If 'margin-left', or 'margin-right' are computed as 'auto', their used value is '0'.
    if (margin_left.is_auto())
        margin_left = zero_value;
    if (margin_right.is_auto())
        margin_right = zero_value;

    auto width = computed_values.width().has_value() ? computed_values.width()->resolved(box, width_of_containing_block_as_length).resolved(box) : CSS::Length::make_auto();

    // If 'width' is computed as 'auto', the used value is the "shrink-to-fit" width.
    if (width.is_auto()) {

        // Find the available width: in this case, this is the width of the containing
        // block minus the used values of 'margin-left', 'border-left-width', 'padding-left',
        // 'padding-right', 'border-right-width', 'margin-right', and the widths of any relevant scroll bars.
        float available_width = width_of_containing_block
            - margin_left.to_px(box) - computed_values.border_left().width - padding_left.to_px(box)
            - padding_right.to_px(box) - computed_values.border_right().width - margin_right.to_px(box);

        auto result = calculate_shrink_to_fit_widths(box);

        // Then the shrink-to-fit width is: min(max(preferred minimum width, available width), preferred width).
        width = CSS::Length(min(max(result.preferred_minimum_width, available_width), result.preferred_width), CSS::Length::Type::Px);
    }

    auto& box_state = m_state.get_mutable(box);
    box_state.content_width = width.to_px(box);
    box_state.margin_left = margin_left.to_px(box);
    box_state.margin_right = margin_right.to_px(box);
    box_state.border_left = computed_values.border_left().width;
    box_state.border_right = computed_values.border_right().width;
    box_state.padding_left = padding_left.to_px(box);
    box_state.padding_right = padding_right.to_px(box);
}

void BlockFormattingContext::compute_width_for_block_level_replaced_element_in_normal_flow(ReplacedBox const& box)
{
    m_state.get_mutable(box).content_width = compute_width_for_replaced_element(m_state, box);
}

float BlockFormattingContext::compute_theoretical_height(FormattingState const& state, Box const& box)
{
    auto const& computed_values = box.computed_values();
    auto const& containing_block = *box.containing_block();
    auto const& containing_block_state = state.get(containing_block);
    auto containing_block_height = CSS::Length::make_px(containing_block_state.content_height);

    auto is_absolute = [](Optional<CSS::LengthPercentage> const& length_percentage) {
        return length_percentage.has_value() && length_percentage->is_length() && length_percentage->length().is_absolute();
    };

    // Then work out what the height is, based on box type and CSS properties.
    float height = 0;
    if (is<ReplacedBox>(box)) {
        height = compute_height_for_replaced_element(state, verify_cast<ReplacedBox>(box));
    } else {
        if (!box.computed_values().height().has_value()
            || (box.computed_values().height()->is_length() && box.computed_values().height()->length().is_auto())
            || (computed_values.height().has_value() && computed_values.height()->is_percentage() && !is_absolute(containing_block.computed_values().height()))) {
            height = compute_auto_height_for_block_level_element(state, box, ConsiderFloats::No);
        } else {
            height = computed_values.height().has_value() ? computed_values.height()->resolved(box, containing_block_height).to_px(box) : 0;
        }
    }

    auto specified_max_height = computed_values.max_height().has_value() ? computed_values.max_height()->resolved(box, containing_block_height).resolved(box) : CSS::Length::make_auto();
    if (!specified_max_height.is_auto()
        && !(computed_values.max_height().has_value() && computed_values.max_height()->is_percentage() && !is_absolute(containing_block.computed_values().height())))
        height = min(height, specified_max_height.to_px(box));
    auto specified_min_height = computed_values.min_height().has_value() ? computed_values.min_height()->resolved(box, containing_block_height).resolved(box) : CSS::Length::make_auto();
    if (!specified_min_height.is_auto()
        && !(computed_values.min_height().has_value() && computed_values.min_height()->is_percentage() && !is_absolute(containing_block.computed_values().height())))
        height = max(height, specified_min_height.to_px(box));
    return height;
}

void BlockFormattingContext::compute_height(Box const& box, FormattingState& state)
{
    auto const& computed_values = box.computed_values();
    auto const& containing_block = *box.containing_block();
    auto width_of_containing_block_as_length = CSS::Length::make_px(state.get(containing_block).content_width);

    // First, resolve the top/bottom parts of the surrounding box model.

    auto& box_state = state.get_mutable(box);

    // FIXME: While negative values are generally allowed for margins, for now just ignore those for height calculation
    box_state.margin_top = max(computed_values.margin().top.resolved(box, width_of_containing_block_as_length).to_px(box), 0);
    box_state.margin_bottom = max(computed_values.margin().bottom.resolved(box, width_of_containing_block_as_length).to_px(box), 0);

    box_state.border_top = computed_values.border_top().width;
    box_state.border_bottom = computed_values.border_bottom().width;
    box_state.padding_top = computed_values.padding().top.resolved(box, width_of_containing_block_as_length).to_px(box);
    box_state.padding_bottom = computed_values.padding().bottom.resolved(box, width_of_containing_block_as_length).to_px(box);

    box_state.content_height = compute_theoretical_height(state, box);
}

void BlockFormattingContext::layout_inline_children(BlockContainer const& block_container, LayoutMode layout_mode)
{
    VERIFY(block_container.children_are_inline());

    InlineFormattingContext context(m_state, block_container, *this);
    context.run(block_container, layout_mode);
}

void BlockFormattingContext::layout_block_level_children(BlockContainer const& block_container, LayoutMode layout_mode)
{
    VERIFY(!block_container.children_are_inline());

    float content_height = 0;
    float content_width = 0;

    block_container.for_each_child_of_type<Box>([&](Box& child_box) {
        auto& box_state = m_state.get_mutable(child_box);

        if (child_box.is_absolutely_positioned()) {
            m_absolutely_positioned_boxes.append(child_box);
            return IterationDecision::Continue;
        }

        // NOTE: ListItemMarkerBoxes are placed by their corresponding ListItemBox.
        if (is<ListItemMarkerBox>(child_box))
            return IterationDecision::Continue;

        if (child_box.is_floating()) {
            layout_floating_child(child_box, block_container);
            return IterationDecision::Continue;
        }

        compute_width(child_box);
        if (is<ReplacedBox>(child_box) || is<BlockContainer>(child_box))
            place_block_level_element_in_normal_flow_vertically(child_box, block_container);

        if (child_box.has_definite_height()) {
            compute_height(child_box, m_state);
        }

        OwnPtr<FormattingContext> independent_formatting_context;
        if (child_box.can_have_children()) {
            independent_formatting_context = create_independent_formatting_context_if_needed(child_box);
            if (independent_formatting_context)
                independent_formatting_context->run(child_box, layout_mode);
            else
                layout_block_level_children(verify_cast<BlockContainer>(child_box), layout_mode);
        }

        compute_height(child_box, m_state);

        compute_position(child_box);

        if (is<ReplacedBox>(child_box) || is<BlockContainer>(child_box))
            place_block_level_element_in_normal_flow_horizontally(child_box, block_container);

        if (is<ListItemBox>(child_box)) {
            layout_list_item_marker(static_cast<ListItemBox const&>(child_box));
        }

        content_height = max(content_height, box_state.offset.y() + box_state.content_height + box_state.margin_box_bottom());
        content_width = max(content_width, box_state.content_width);

        if (independent_formatting_context)
            independent_formatting_context->parent_context_did_dimension_child_root_box();

        return IterationDecision::Continue;
    });

    if (layout_mode != LayoutMode::Default) {
        auto& width = block_container.computed_values().width();
        if (!width.has_value() || (width->is_length() && width->length().is_auto())) {
            auto& block_container_state = m_state.get_mutable(block_container);
            block_container_state.content_width = content_width;
        }
    }
}

void BlockFormattingContext::compute_vertical_box_model_metrics(Box const& box, BlockContainer const& containing_block)
{
    auto& box_state = m_state.get_mutable(box);
    auto const& computed_values = box.computed_values();
    auto width_of_containing_block = CSS::Length::make_px(m_state.get(containing_block).content_width);

    box_state.margin_top = computed_values.margin().top.resolved(box, width_of_containing_block).resolved(containing_block).to_px(box);
    box_state.margin_bottom = computed_values.margin().bottom.resolved(box, width_of_containing_block).resolved(containing_block).to_px(box);
    box_state.border_top = computed_values.border_top().width;
    box_state.border_bottom = computed_values.border_bottom().width;
    box_state.padding_top = computed_values.padding().top.resolved(box, width_of_containing_block).resolved(containing_block).to_px(box);
    box_state.padding_bottom = computed_values.padding().bottom.resolved(box, width_of_containing_block).resolved(containing_block).to_px(box);
}

void BlockFormattingContext::place_block_level_element_in_normal_flow_vertically(Box const& child_box, BlockContainer const& containing_block)
{
    auto& box_state = m_state.get_mutable(child_box);
    auto const& computed_values = child_box.computed_values();

    compute_vertical_box_model_metrics(child_box, containing_block);

    float y = box_state.margin_box_top()
        + box_state.offset_top;

    // NOTE: Empty (0-height) preceding siblings have their margins collapsed with *their* preceding sibling, etc.
    float collapsed_bottom_margin_of_preceding_siblings = 0;

    auto* relevant_sibling = child_box.previous_sibling_of_type<Layout::BlockContainer>();
    while (relevant_sibling != nullptr) {
        if (!relevant_sibling->is_absolutely_positioned() && !relevant_sibling->is_floating()) {
            auto const& relevant_sibling_state = m_state.get(*relevant_sibling);
            collapsed_bottom_margin_of_preceding_siblings = max(collapsed_bottom_margin_of_preceding_siblings, relevant_sibling_state.margin_bottom);
            if (relevant_sibling_state.border_box_height() > 0)
                break;
        }
        relevant_sibling = relevant_sibling->previous_sibling();
    }

    if (relevant_sibling) {
        auto const& relevant_sibling_state = m_state.get(*relevant_sibling);
        y += relevant_sibling_state.offset.y()
            + relevant_sibling_state.content_height
            + relevant_sibling_state.border_box_bottom();

        // Collapse top margin with bottom margin of preceding siblings if needed
        float my_margin_top = box_state.margin_top;

        if (my_margin_top < 0 || collapsed_bottom_margin_of_preceding_siblings < 0) {
            // Negative margins present.
            float largest_negative_margin = -min(my_margin_top, collapsed_bottom_margin_of_preceding_siblings);
            float largest_positive_margin = (my_margin_top < 0 && collapsed_bottom_margin_of_preceding_siblings < 0) ? 0 : max(my_margin_top, collapsed_bottom_margin_of_preceding_siblings);
            float final_margin = largest_positive_margin - largest_negative_margin;
            y += final_margin - my_margin_top;
        } else if (collapsed_bottom_margin_of_preceding_siblings > my_margin_top) {
            // Sibling's margin is larger than mine, adjust so we use sibling's.
            y += collapsed_bottom_margin_of_preceding_siblings - my_margin_top;
        }
    }

    auto clear_floating_boxes = [&](FloatSideData& float_side) {
        if (!float_side.boxes.is_empty()) {
            float clearance_y = 0;
            for (auto const& floating_box : float_side.boxes) {
                auto const& floating_box_state = m_state.get(floating_box);
                clearance_y = max(clearance_y, floating_box_state.offset.y() + floating_box_state.border_box_height());
            }
            y = max(y, clearance_y);
            float_side.boxes.clear();
            float_side.y_offset = 0;
        }
    };

    // Flex-items don't float and also don't clear.
    if ((computed_values.clear() == CSS::Clear::Left || computed_values.clear() == CSS::Clear::Both) && !child_box.is_flex_item())
        clear_floating_boxes(m_left_floats);
    if ((computed_values.clear() == CSS::Clear::Right || computed_values.clear() == CSS::Clear::Both) && !child_box.is_flex_item())
        clear_floating_boxes(m_right_floats);

    box_state.offset = Gfx::FloatPoint { box_state.offset.x(), y };
}

void BlockFormattingContext::place_block_level_element_in_normal_flow_horizontally(Box const& child_box, BlockContainer const& containing_block)
{
    auto& box_state = m_state.get_mutable(child_box);
    auto const& containing_block_state = m_state.get(containing_block);

    float x = 0;
    if (containing_block.computed_values().text_align() == CSS::TextAlign::LibwebCenter) {
        x = (containing_block_state.content_width / 2) - box_state.content_width / 2;
    } else {
        x = box_state.margin_box_left() + box_state.offset_left;
    }

    box_state.offset = Gfx::FloatPoint { x, box_state.offset.y() };
}

void BlockFormattingContext::layout_initial_containing_block(LayoutMode layout_mode)
{
    auto viewport_rect = root().browsing_context().viewport_rect();

    auto& icb = verify_cast<Layout::InitialContainingBlock>(root());
    auto& icb_state = m_state.get_mutable(icb);

    VERIFY(!icb.children_are_inline());
    layout_block_level_children(root(), layout_mode);

    // Compute scrollable overflow.
    float bottom_edge = 0;
    float right_edge = 0;
    icb.for_each_in_subtree_of_type<Box>([&](Box const& child) {
        auto const& child_state = m_state.get(child);
        auto child_rect = absolute_content_rect(child, m_state);
        child_rect.inflate(child_state.border_box_top(), child_state.border_box_right(), child_state.border_box_bottom(), child_state.border_box_left());
        bottom_edge = max(bottom_edge, child_rect.bottom());
        right_edge = max(right_edge, child_rect.right());
        return IterationDecision::Continue;
    });

    if (bottom_edge >= viewport_rect.height() || right_edge >= viewport_rect.width()) {
        // FIXME: Move overflow data to FormattingState!
        auto& overflow_data = icb_state.ensure_overflow_data();
        overflow_data.scrollable_overflow_rect = viewport_rect.to_type<float>();
        // NOTE: The edges are *within* the rectangle, so we add 1 to get the width and height.
        overflow_data.scrollable_overflow_rect.set_size(right_edge + 1, bottom_edge + 1);
    }
}

void BlockFormattingContext::layout_floating_child(Box const& box, BlockContainer const& containing_block)
{
    VERIFY(box.is_floating());

    auto& box_state = m_state.get_mutable(box);
    auto containing_block_content_width = m_state.get(containing_block).content_width;

    compute_width(box);
    (void)layout_inside(box, LayoutMode::Default);
    compute_height(box, m_state);

    // First we place the box normally (to get the right y coordinate.)
    place_block_level_element_in_normal_flow_vertically(box, containing_block);
    place_block_level_element_in_normal_flow_horizontally(box, containing_block);

    auto float_box = [&](FloatSide side, FloatSideData& side_data) {
        auto first_edge = [&](FormattingState::NodeState const& thing) { return side == FloatSide::Left ? thing.margin_left : thing.margin_right; };
        auto second_edge = [&](FormattingState::NodeState const& thing) { return side == FloatSide::Right ? thing.margin_left : thing.margin_right; };
        auto edge_of_containing_block = [&] {
            if (side == FloatSide::Left)
                return box_state.margin_box_left();
            return containing_block_content_width - box_state.margin_box_right() - box_state.content_width;
        };

        // Then we float it to the left or right.

        auto box_in_root_rect = margin_box_rect_in_ancestor_coordinate_space(box, root(), m_state);
        float y_in_root = box_in_root_rect.y();

        float x = 0;
        float y = box_state.offset.y();

        if (side_data.boxes.is_empty()) {
            // This is the first floating box on this side. Go all the way to the edge.
            x = edge_of_containing_block();
            side_data.y_offset = 0;
        } else {
            auto& previous_box = side_data.boxes.last();
            auto const& previous_box_state = m_state.get(previous_box);
            auto previous_rect = margin_box_rect_in_ancestor_coordinate_space(previous_box, root(), m_state);

            auto margin_collapsed_with_previous = max(
                second_edge(previous_box_state),
                first_edge(box_state));

            float wanted_x = 0;
            bool fits_on_line = false;

            if (side == FloatSide::Left) {
                auto previous_right_border_edge = previous_box_state.offset.x()
                    + previous_box_state.content_width
                    + previous_box_state.padding_right
                    + previous_box_state.border_right
                    + margin_collapsed_with_previous;

                wanted_x = previous_right_border_edge + box_state.border_left + box_state.padding_left;
                fits_on_line = (wanted_x + box_state.content_width + box_state.padding_right + box_state.border_right + box_state.margin_right) <= containing_block_content_width;
            } else {
                auto previous_left_border_edge = previous_box_state.offset.x()
                    - previous_box_state.padding_left
                    - previous_box_state.border_left
                    - margin_collapsed_with_previous;

                wanted_x = previous_left_border_edge - box_state.border_right - box_state.padding_right - box_state.content_width;
                fits_on_line = (wanted_x - box_state.padding_left - box_state.border_left - box_state.margin_left) >= 0;
            }

            if (fits_on_line) {
                if (previous_rect.contains_vertically(y_in_root + side_data.y_offset)) {
                    // This box touches another already floating box. Stack after others.
                    x = wanted_x;
                } else {
                    // This box does not touch another floating box, go all the way to the edge.
                    x = edge_of_containing_block();

                    // Also, forget all previous boxes floated to this side while since they're no longer relevant.
                    side_data.boxes.clear();
                }
            } else {
                // We ran out of horizontal space on this "float line", and need to break.
                x = edge_of_containing_block();
                float lowest_border_edge = 0;
                for (auto const& box : side_data.boxes) {
                    auto const& box_state = m_state.get(box);
                    lowest_border_edge = max(lowest_border_edge, box_state.border_box_height());
                }

                side_data.y_offset += lowest_border_edge;

                // Also, forget all previous boxes floated to this side while since they're no longer relevant.
                side_data.boxes.clear();
            }
        }
        y += side_data.y_offset;
        side_data.boxes.append(box);

        box_state.offset = Gfx::FloatPoint { x, y };
    };

    // Next, float to the left and/or right
    if (box.computed_values().float_() == CSS::Float::Left) {
        float_box(FloatSide::Left, m_left_floats);
    } else if (box.computed_values().float_() == CSS::Float::Right) {
        float_box(FloatSide::Right, m_right_floats);
    }
}

void BlockFormattingContext::layout_list_item_marker(ListItemBox const& list_item_box)
{
    if (!list_item_box.marker())
        return;

    auto& marker = *list_item_box.marker();
    auto& marker_state = m_state.get_mutable(marker);
    auto& list_item_state = m_state.get_mutable(list_item_box);

    int image_width = 0;
    int image_height = 0;
    if (auto const* list_style_image = marker.list_style_image_bitmap()) {
        image_width = list_style_image->rect().width();
        image_height = list_style_image->rect().height();
    }

    if (marker.text().is_empty()) {
        marker_state.content_width = image_width + 4;
    } else {
        auto text_width = marker.font().width(marker.text());
        marker_state.content_width = image_width + text_width;
    }

    marker_state.content_height = max(image_height, marker.line_height());

    marker_state.offset = { -(marker_state.content_width + 4), 0 };

    if (marker_state.content_height > list_item_state.content_height)
        list_item_state.content_height = marker_state.content_height;
}

}
