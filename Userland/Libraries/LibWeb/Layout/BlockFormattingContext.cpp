/*
 * Copyright (c) 2020-2021, Andreas Kling <kling@serenityos.org>
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

BlockFormattingContext::BlockFormattingContext(BlockContainer& root, FormattingContext* parent)
    : FormattingContext(Type::Block, root, parent)
{
}

BlockFormattingContext::~BlockFormattingContext()
{
}

bool BlockFormattingContext::is_initial() const
{
    return is<InitialContainingBlock>(root());
}

void BlockFormattingContext::run(Box&, LayoutMode layout_mode)
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
    for (auto& box : m_absolutely_positioned_boxes)
        layout_absolutely_positioned_element(box);

    apply_transformations_to_children(root());
}

void BlockFormattingContext::apply_transformations_to_children(Box& box)
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
                            transform_y_offset += value.resolved_or_zero(child_box).to_px(child_box);
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

        auto untransformed_offset = child_box.effective_offset();
        child_box.set_offset(untransformed_offset.x(), untransformed_offset.y() + transform_y_offset);
    });
}

void BlockFormattingContext::compute_width(Box& box)
{
    if (box.is_absolutely_positioned()) {
        compute_width_for_absolutely_positioned_element(box);
        return;
    }

    if (is<ReplacedBox>(box)) {
        // FIXME: This should not be done *by* ReplacedBox
        auto& replaced = verify_cast<ReplacedBox>(box);
        replaced.prepare_for_replaced_layout();
        compute_width_for_block_level_replaced_element_in_normal_flow(replaced);
        return;
    }

    if (box.is_floating()) {
        compute_width_for_floating_box(box);
        return;
    }

    auto& computed_values = box.computed_values();
    float width_of_containing_block = box.containing_block()->content_width();
    auto width_of_containing_block_as_length = CSS::Length::make_px(width_of_containing_block);

    auto zero_value = CSS::Length::make_px(0);

    auto margin_left = CSS::Length::make_auto();
    auto margin_right = CSS::Length::make_auto();
    const auto padding_left = computed_values.padding().left.resolved(box, width_of_containing_block_as_length).resolved_or_zero(box);
    const auto padding_right = computed_values.padding().right.resolved(box, width_of_containing_block_as_length).resolved_or_zero(box);

    auto try_compute_width = [&](const auto& a_width) {
        CSS::Length width = a_width;
        margin_left = computed_values.margin().left.resolved(box, width_of_containing_block_as_length).resolved_or_zero(box);
        margin_right = computed_values.margin().right.resolved(box, width_of_containing_block_as_length).resolved_or_zero(box);

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

    auto specified_width = computed_values.width().resolved(box, width_of_containing_block_as_length).resolved_or_auto(box);

    // 1. The tentative used width is calculated (without 'min-width' and 'max-width')
    auto used_width = try_compute_width(specified_width);

    // 2. The tentative used width is greater than 'max-width', the rules above are applied again,
    //    but this time using the computed value of 'max-width' as the computed value for 'width'.
    auto specified_max_width = computed_values.max_width().resolved(box, width_of_containing_block_as_length).resolved_or_auto(box);
    if (!specified_max_width.is_auto()) {
        if (used_width.to_px(box) > specified_max_width.to_px(box)) {
            used_width = try_compute_width(specified_max_width);
        }
    }

    // 3. If the resulting width is smaller than 'min-width', the rules above are applied again,
    //    but this time using the value of 'min-width' as the computed value for 'width'.
    auto specified_min_width = computed_values.min_width().resolved(box, width_of_containing_block_as_length).resolved_or_auto(box);
    if (!specified_min_width.is_auto()) {
        if (used_width.to_px(box) < specified_min_width.to_px(box)) {
            used_width = try_compute_width(specified_min_width);
        }
    }

    box.set_content_width(used_width.to_px(box));
    box.box_model().margin.left = margin_left.to_px(box);
    box.box_model().margin.right = margin_right.to_px(box);
    box.box_model().border.left = computed_values.border_left().width;
    box.box_model().border.right = computed_values.border_right().width;
    box.box_model().padding.left = padding_left.to_px(box);
    box.box_model().padding.right = padding_right.to_px(box);
}

void BlockFormattingContext::compute_width_for_floating_box(Box& box)
{
    // 10.3.5 Floating, non-replaced elements
    auto& computed_values = box.computed_values();
    float width_of_containing_block = box.containing_block()->content_width();
    auto width_of_containing_block_as_length = CSS::Length::make_px(width_of_containing_block);
    auto zero_value = CSS::Length::make_px(0);

    auto margin_left = computed_values.margin().left.resolved(box, width_of_containing_block_as_length).resolved_or_zero(box);
    auto margin_right = computed_values.margin().right.resolved(box, width_of_containing_block_as_length).resolved_or_zero(box);
    const auto padding_left = computed_values.padding().left.resolved(box, width_of_containing_block_as_length).resolved_or_zero(box);
    const auto padding_right = computed_values.padding().right.resolved(box, width_of_containing_block_as_length).resolved_or_zero(box);

    // If 'margin-left', or 'margin-right' are computed as 'auto', their used value is '0'.
    if (margin_left.is_auto())
        margin_left = zero_value;
    if (margin_right.is_auto())
        margin_right = zero_value;

    auto width = computed_values.width().resolved(box, width_of_containing_block_as_length).resolved_or_auto(box);

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

    float final_width = width.resolved_or_zero(box).to_px(box);
    box.set_content_width(final_width);
    box.box_model().margin.left = margin_left.to_px(box);
    box.box_model().margin.right = margin_right.to_px(box);
    box.box_model().border.left = computed_values.border_left().width;
    box.box_model().border.right = computed_values.border_right().width;
    box.box_model().padding.left = padding_left.to_px(box);
    box.box_model().padding.right = padding_right.to_px(box);
}

void BlockFormattingContext::compute_width_for_block_level_replaced_element_in_normal_flow(ReplacedBox& box)
{
    box.set_content_width(compute_width_for_replaced_element(box));
}

float BlockFormattingContext::compute_theoretical_height(Box const& box)
{
    auto& computed_values = box.computed_values();
    auto& containing_block = *box.containing_block();
    auto containing_block_height = CSS::Length::make_px(containing_block.content_height());

    auto is_absolute = [](CSS::LengthPercentage const& length_percentage) {
        return length_percentage.is_length() && length_percentage.length().is_absolute();
    };

    // Then work out what the height is, based on box type and CSS properties.
    float height = 0;
    if (is<ReplacedBox>(box)) {
        height = compute_height_for_replaced_element(verify_cast<ReplacedBox>(box));
    } else {
        if ((box.computed_values().height().is_length() && box.computed_values().height().length().is_undefined_or_auto())
            || (computed_values.height().is_percentage() && !is_absolute(containing_block.computed_values().height()))) {
            height = compute_auto_height_for_block_level_element(box, ConsiderFloats::No);
        } else {
            height = computed_values.height().resolved(box, containing_block_height).resolved_or_auto(box).to_px(box);
        }
    }

    auto specified_max_height = computed_values.max_height().resolved(box, containing_block_height).resolved_or_auto(box);
    if (!specified_max_height.is_auto()
        && !(computed_values.max_height().is_percentage() && !is_absolute(containing_block.computed_values().height())))
        height = min(height, specified_max_height.to_px(box));
    auto specified_min_height = computed_values.min_height().resolved(box, containing_block_height).resolved_or_auto(box);
    if (!specified_min_height.is_auto()
        && !(computed_values.min_height().is_percentage() && !is_absolute(containing_block.computed_values().height())))
        height = max(height, specified_min_height.to_px(box));
    return height;
}

void BlockFormattingContext::compute_height(Box& box)
{
    auto& computed_values = box.computed_values();
    auto& containing_block = *box.containing_block();
    auto width_of_containing_block_as_length = CSS::Length::make_px(containing_block.content_width());

    // First, resolve the top/bottom parts of the surrounding box model.

    // FIXME: While negative values are generally allowed for margins, for now just ignore those for height calculation
    box.box_model().margin.top = max(computed_values.margin().top.resolved(box, width_of_containing_block_as_length).resolved_or_zero(box).to_px(box), 0);
    box.box_model().margin.bottom = max(computed_values.margin().bottom.resolved(box, width_of_containing_block_as_length).resolved_or_zero(box).to_px(box), 0);

    box.box_model().border.top = computed_values.border_top().width;
    box.box_model().border.bottom = computed_values.border_bottom().width;
    box.box_model().padding.top = computed_values.padding().top.resolved(box, width_of_containing_block_as_length).resolved_or_zero(box).to_px(box);
    box.box_model().padding.bottom = computed_values.padding().bottom.resolved(box, width_of_containing_block_as_length).resolved_or_zero(box).to_px(box);

    auto height = compute_theoretical_height(box);
    box.set_content_height(height);
}

void BlockFormattingContext::compute_position(Box& box)
{
    // 9.4.3 Relative positioning
    // Once a box has been laid out according to the normal flow or floated, it may be shifted relative to this position.

    auto& box_model = box.box_model();
    auto& computed_values = box.computed_values();
    float width_of_containing_block = box.containing_block()->content_width();
    auto width_of_containing_block_as_length = CSS::Length::make_px(width_of_containing_block);

    auto specified_left = computed_values.offset().left.resolved(box, width_of_containing_block_as_length).resolved_or_zero(box);
    auto specified_right = computed_values.offset().right.resolved(box, width_of_containing_block_as_length).resolved_or_zero(box);

    if (specified_left.is_auto() && specified_right.is_auto()) {
        // If both 'left' and 'right' are 'auto' (their initial values), the used values are '0' (i.e., the boxes stay in their original position).
        box_model.offset.left = 0;
        box_model.offset.right = 0;
    } else if (specified_left.is_auto()) {
        // If 'left' is 'auto', its used value is minus the value of 'right' (i.e., the boxes move to the left by the value of 'right').
        box_model.offset.right = specified_right.to_px(box);
        box_model.offset.left = 0 - box_model.offset.right;
    } else if (specified_right.is_auto()) {
        // If 'right' is specified as 'auto', its used value is minus the value of 'left'.
        box_model.offset.left = specified_left.to_px(box);
        box_model.offset.right = 0 - box_model.offset.left;
    } else {
        // If neither 'left' nor 'right' is 'auto', the position is over-constrained, and one of them has to be ignored.
        // If the 'direction' property of the containing block is 'ltr', the value of 'left' wins and 'right' becomes -'left'.
        // If 'direction' of the containing block is 'rtl', 'right' wins and 'left' is ignored.
        // FIXME: Check direction (assuming 'ltr' for now).
        box_model.offset.left = specified_left.to_px(box);
        box_model.offset.right = 0 - box_model.offset.left;
    }
}

void BlockFormattingContext::layout_inline_children(BlockContainer& block_container, LayoutMode layout_mode)
{
    VERIFY(block_container.children_are_inline());

    InlineFormattingContext context(block_container, *this);
    context.run(block_container, layout_mode);
}

void BlockFormattingContext::layout_block_level_children(BlockContainer& block_container, LayoutMode layout_mode)
{
    VERIFY(!block_container.children_are_inline());

    float content_height = 0;
    float content_width = 0;

    block_container.for_each_child_of_type<Box>([&](Box& child_box) {
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
            compute_height(child_box);
        }

        OwnPtr<FormattingContext> independent_formatting_context;
        if (child_box.can_have_children()) {
            independent_formatting_context = create_independent_formatting_context_if_needed(child_box);
            if (independent_formatting_context)
                independent_formatting_context->run(child_box, layout_mode);
            else
                layout_block_level_children(verify_cast<BlockContainer>(child_box), layout_mode);
        }

        compute_height(child_box);

        if (child_box.computed_values().position() == CSS::Position::Relative)
            compute_position(child_box);

        if (is<ReplacedBox>(child_box) || is<BlockContainer>(child_box))
            place_block_level_element_in_normal_flow_horizontally(child_box, block_container);

        // FIXME: This should be factored differently. It's uncool that we mutate the tree *during* layout!
        //        Instead, we should generate the marker box during the tree build.
        if (is<ListItemBox>(child_box))
            verify_cast<ListItemBox>(child_box).layout_marker();

        content_height = max(content_height, child_box.effective_offset().y() + child_box.content_height() + child_box.box_model().margin_box().bottom);
        content_width = max(content_width, child_box.content_width());

        if (independent_formatting_context)
            independent_formatting_context->parent_context_did_dimension_child_root_box();

        return IterationDecision::Continue;
    });

    if (layout_mode != LayoutMode::Default) {
        if (block_container.computed_values().width().is_length() && block_container.computed_values().width().length().is_undefined_or_auto())
            block_container.set_content_width(content_width);
    }
}

void BlockFormattingContext::compute_vertical_box_model_metrics(Box& child_box, BlockContainer const& containing_block)
{
    auto& box_model = child_box.box_model();
    auto const& computed_values = child_box.computed_values();
    auto width_of_containing_block = CSS::Length::make_px(containing_block.content_width());

    box_model.margin.top = computed_values.margin().top.resolved(child_box, width_of_containing_block).resolved_or_zero(containing_block).to_px(child_box);
    box_model.margin.bottom = computed_values.margin().bottom.resolved(child_box, width_of_containing_block).resolved_or_zero(containing_block).to_px(child_box);
    box_model.border.top = computed_values.border_top().width;
    box_model.border.bottom = computed_values.border_bottom().width;
    box_model.padding.top = computed_values.padding().top.resolved(child_box, width_of_containing_block).resolved_or_zero(containing_block).to_px(child_box);
    box_model.padding.bottom = computed_values.padding().bottom.resolved(child_box, width_of_containing_block).resolved_or_zero(containing_block).to_px(child_box);
}

void BlockFormattingContext::place_block_level_element_in_normal_flow_vertically(Box& child_box, BlockContainer const& containing_block)
{
    auto& box_model = child_box.box_model();
    auto const& computed_values = child_box.computed_values();

    compute_vertical_box_model_metrics(child_box, containing_block);

    float y = box_model.margin_box().top
        + box_model.offset.top;

    // NOTE: Empty (0-height) preceding siblings have their margins collapsed with *their* preceding sibling, etc.
    float collapsed_bottom_margin_of_preceding_siblings = 0;

    auto* relevant_sibling = child_box.previous_sibling_of_type<Layout::BlockContainer>();
    while (relevant_sibling != nullptr) {
        if (!relevant_sibling->is_absolutely_positioned() && !relevant_sibling->is_floating()) {
            collapsed_bottom_margin_of_preceding_siblings = max(collapsed_bottom_margin_of_preceding_siblings, relevant_sibling->box_model().margin.bottom);
            if (relevant_sibling->border_box_height() > 0)
                break;
        }
        relevant_sibling = relevant_sibling->previous_sibling();
    }

    if (relevant_sibling) {
        y += relevant_sibling->effective_offset().y()
            + relevant_sibling->content_height()
            + relevant_sibling->box_model().border_box().bottom;

        // Collapse top margin with bottom margin of preceding siblings if needed
        float my_margin_top = box_model.margin.top;

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
            for (auto& floating_box : float_side.boxes) {
                clearance_y = max(clearance_y, floating_box.effective_offset().y() + floating_box.border_box_height());
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

    child_box.set_offset(child_box.effective_offset().x(), y);
}

void BlockFormattingContext::place_block_level_element_in_normal_flow_horizontally(Box& child_box, BlockContainer const& containing_block)
{
    auto& box_model = child_box.box_model();

    float x = 0;
    if (containing_block.computed_values().text_align() == CSS::TextAlign::LibwebCenter) {
        x = (containing_block.content_width() / 2) - child_box.content_width() / 2;
    } else {
        x = box_model.margin_box().left + box_model.offset.left;
    }

    child_box.set_offset(x, child_box.effective_offset().y());
}

void BlockFormattingContext::layout_initial_containing_block(LayoutMode layout_mode)
{
    auto viewport_rect = root().browsing_context().viewport_rect();

    auto& icb = verify_cast<Layout::InitialContainingBlock>(root());

    VERIFY(!icb.children_are_inline());
    layout_block_level_children(root(), layout_mode);

    // Compute scrollable overflow.
    float bottom_edge = 0;
    float right_edge = 0;
    icb.for_each_in_subtree_of_type<Box>([&](Box& child) {
        auto child_rect = child.absolute_border_box_rect();
        bottom_edge = max(bottom_edge, child_rect.bottom());
        right_edge = max(right_edge, child_rect.right());
        return IterationDecision::Continue;
    });

    if (bottom_edge >= viewport_rect.height() || right_edge >= viewport_rect.width()) {
        auto& overflow_data = icb.ensure_overflow_data();
        overflow_data.scrollable_overflow_rect = viewport_rect.to_type<float>();
        // NOTE: The edges are *within* the rectangle, so we add 1 to get the width and height.
        overflow_data.scrollable_overflow_rect.set_size(right_edge + 1, bottom_edge + 1);
    } else {
        icb.clear_overflow_data();
    }
}

void BlockFormattingContext::layout_floating_child(Box& box, BlockContainer const& containing_block)
{
    VERIFY(box.is_floating());

    compute_width(box);
    (void)layout_inside(box, LayoutMode::Default);
    compute_height(box);

    // First we place the box normally (to get the right y coordinate.)
    place_block_level_element_in_normal_flow_vertically(box, containing_block);
    place_block_level_element_in_normal_flow_horizontally(box, containing_block);

    auto float_box = [&](FloatSide side, FloatSideData& side_data) {
        auto first_edge = [&](PixelBox const& thing) { return side == FloatSide::Left ? thing.left : thing.right; };
        auto second_edge = [&](PixelBox const& thing) { return side == FloatSide::Right ? thing.left : thing.right; };

        // Then we float it to the left or right.
        float x = box.effective_offset().x();

        auto box_in_root_rect = box.margin_box_rect_in_ancestor_coordinate_space(root());
        float y_in_root = box_in_root_rect.y();

        float y = box.effective_offset().y();

        if (side_data.boxes.is_empty()) {
            // This is the first floating box on this side. Go all the way to the edge.
            if (side == FloatSide::Left)
                x = box.box_model().margin_box().left;
            else
                x = containing_block.content_width() - box.box_model().margin_box().right - box.content_width();
            side_data.y_offset = 0;
        } else {
            auto& previous_box = side_data.boxes.last();
            auto previous_rect = previous_box.margin_box_rect_in_ancestor_coordinate_space(root());

            auto margin_collapsed_with_previous = max(
                second_edge(previous_box.box_model().margin),
                first_edge(box.box_model().margin));

            float wanted_x = 0;
            bool fits_on_line = false;

            if (side == FloatSide::Left) {
                auto previous_right_border_edge = previous_box.effective_offset().x()
                    + previous_box.content_width()
                    + previous_box.box_model().padding.right
                    + previous_box.box_model().border.right
                    + margin_collapsed_with_previous;

                wanted_x = previous_right_border_edge + box.box_model().border.left + box.box_model().padding.left;
                fits_on_line = (wanted_x + box.content_width() + box.box_model().padding.right + box.box_model().border.right + box.box_model().margin.right) <= containing_block.content_width();
            } else {
                auto previous_left_border_edge = previous_box.effective_offset().x()
                    - previous_box.content_width()
                    - previous_box.box_model().padding.left
                    - previous_box.box_model().border.left
                    - margin_collapsed_with_previous;

                wanted_x = previous_left_border_edge - box.box_model().border.right - box.box_model().padding.right - box.content_width();
                fits_on_line = (wanted_x - box.box_model().padding.left - box.box_model().border.left - box.box_model().margin.left) >= 0;
            }

            if (fits_on_line) {
                if (previous_rect.contains_vertically(y_in_root + side_data.y_offset)) {
                    // This box touches another already floating box. Stack after others.
                    x = wanted_x;
                } else {
                    // This box does not touch another floating box, go all the way to the first edge.
                    x = first_edge(box.box_model().margin_box());

                    // Also, forget all previous boxes floated to this side while since they're no longer relevant.
                    side_data.boxes.clear();
                }
            } else {
                // We ran out of horizontal space on this "float line", and need to break.
                x = first_edge(box.box_model().margin_box());
                float lowest_border_edge = 0;
                for (auto const& box : side_data.boxes)
                    lowest_border_edge = max(lowest_border_edge, box.border_box_height());

                side_data.y_offset += lowest_border_edge;

                // Also, forget all previous boxes floated to this side while since they're no longer relevant.
                side_data.boxes.clear();
            }
        }
        y += side_data.y_offset;
        side_data.boxes.append(box);

        box.set_offset(x, y);
    };

    // Next, float to the left and/or right
    if (box.computed_values().float_() == CSS::Float::Left) {
        float_box(FloatSide::Left, m_left_floats);
    } else if (box.computed_values().float_() == CSS::Float::Right) {
        float_box(FloatSide::Right, m_right_floats);
    }
}
}
