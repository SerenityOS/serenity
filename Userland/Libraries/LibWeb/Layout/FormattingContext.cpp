/*
 * Copyright (c) 2020-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Dump.h>
#include <LibWeb/Layout/BlockFormattingContext.h>
#include <LibWeb/Layout/Box.h>
#include <LibWeb/Layout/FlexFormattingContext.h>
#include <LibWeb/Layout/FormattingContext.h>
#include <LibWeb/Layout/InlineFormattingContext.h>
#include <LibWeb/Layout/ReplacedBox.h>
#include <LibWeb/Layout/SVGFormattingContext.h>
#include <LibWeb/Layout/SVGSVGBox.h>
#include <LibWeb/Layout/TableBox.h>
#include <LibWeb/Layout/TableCellBox.h>
#include <LibWeb/Layout/TableFormattingContext.h>

namespace Web::Layout {

FormattingContext::FormattingContext(Type type, Box& context_box, FormattingContext* parent)
    : m_type(type)
    , m_parent(parent)
    , m_context_box(context_box)
{
}

FormattingContext::~FormattingContext()
{
}

bool FormattingContext::creates_block_formatting_context(const Box& box)
{
    if (box.is_root_element())
        return true;
    if (box.is_floating())
        return true;
    if (box.is_absolutely_positioned())
        return true;
    if (box.is_inline_block())
        return true;
    if (is<TableCellBox>(box))
        return true;

    CSS::Overflow overflow_x = box.computed_values().overflow_x();
    if ((overflow_x != CSS::Overflow::Visible) && (overflow_x != CSS::Overflow::Clip))
        return true;

    CSS::Overflow overflow_y = box.computed_values().overflow_y();
    if ((overflow_y != CSS::Overflow::Visible) && (overflow_y != CSS::Overflow::Clip))
        return true;

    auto display = box.computed_values().display();

    if (display.is_flow_root_inside())
        return true;

    if (box.parent()) {
        auto parent_display = box.parent()->computed_values().display();
        if (parent_display.is_flex_inside()) {
            // FIXME: Flex items (direct children of the element with display: flex or inline-flex) if they are neither flex nor grid nor table containers themselves.
            if (!display.is_flex_inside())
                return true;
        }
    }

    // FIXME: table-caption
    // FIXME: anonymous table cells
    // FIXME: Elements with contain: layout, content, or paint.
    // FIXME: grid
    // FIXME: multicol
    // FIXME: column-span: all
    return false;
}

OwnPtr<FormattingContext> FormattingContext::create_independent_formatting_context_if_needed(Box& child_box)
{
    if (!child_box.can_have_children())
        return {};

    auto child_display = child_box.computed_values().display();

    if (is<SVGSVGBox>(child_box))
        return make<SVGFormattingContext>(child_box, this);

    if (child_display.is_flex_inside())
        return make<FlexFormattingContext>(child_box, this);

    if (creates_block_formatting_context(child_box))
        return make<BlockFormattingContext>(verify_cast<BlockContainer>(child_box), this);

    if (child_display.is_table_inside())
        return make<TableFormattingContext>(verify_cast<TableBox>(child_box), this);

    VERIFY(is_block_formatting_context());
    if (child_box.children_are_inline())
        return make<InlineFormattingContext>(verify_cast<BlockContainer>(child_box), static_cast<BlockFormattingContext&>(*this));

    // The child box is a block container that doesn't create its own BFC.
    // It will be formatted by this BFC.
    VERIFY(child_display.is_flow_inside());
    VERIFY(child_box.is_block_container());
    return {};
}

OwnPtr<FormattingContext> FormattingContext::layout_inside(Box& child_box, LayoutMode layout_mode)
{
    if (!child_box.can_have_children())
        return {};

    auto independent_formatting_context = create_independent_formatting_context_if_needed(child_box);
    if (independent_formatting_context)
        independent_formatting_context->run(child_box, layout_mode);
    else
        run(child_box, layout_mode);

    return independent_formatting_context;
}

static float greatest_child_width(Box const& box)
{
    float max_width = 0;
    if (box.children_are_inline()) {
        for (auto& child : verify_cast<BlockContainer>(box).line_boxes()) {
            max_width = max(max_width, child.width());
        }
    } else {
        box.for_each_child_of_type<Box>([&](auto& child) {
            max_width = max(max_width, child.border_box_width());
        });
    }
    return max_width;
}

FormattingContext::ShrinkToFitResult FormattingContext::calculate_shrink_to_fit_widths(Box& box)
{
    // Calculate the preferred width by formatting the content without breaking lines
    // other than where explicit line breaks occur.
    (void)layout_inside(box, LayoutMode::OnlyRequiredLineBreaks);
    float preferred_width = greatest_child_width(box);

    // Also calculate the preferred minimum width, e.g., by trying all possible line breaks.
    // CSS 2.2 does not define the exact algorithm.

    (void)layout_inside(box, LayoutMode::AllPossibleLineBreaks);
    float preferred_minimum_width = greatest_child_width(box);

    return { preferred_width, preferred_minimum_width };
}

static Gfx::FloatSize solve_replaced_size_constraint(float w, float h, const ReplacedBox& box)
{
    // 10.4 Minimum and maximum widths: 'min-width' and 'max-width'

    auto& containing_block = *box.containing_block();
    auto width_of_containing_block = CSS::Length::make_px(containing_block.content_width());
    auto height_of_containing_block = CSS::Length::make_px(containing_block.content_height());

    auto specified_min_width = box.computed_values().min_width().resolved(box, width_of_containing_block).resolved_or_zero(box).to_px(box);
    auto specified_max_width = box.computed_values().max_width().resolved(box, width_of_containing_block).resolved(CSS::Length::make_px(w), box).to_px(box);
    auto specified_min_height = box.computed_values().min_height().resolved(box, height_of_containing_block).resolved_or_auto(box).to_px(box);
    auto specified_max_height = box.computed_values().max_height().resolved(box, height_of_containing_block).resolved(CSS::Length::make_px(h), box).to_px(box);

    auto min_width = min(specified_min_width, specified_max_width);
    auto max_width = max(specified_min_width, specified_max_width);
    auto min_height = min(specified_min_height, specified_max_height);
    auto max_height = max(specified_min_height, specified_max_height);

    if (w > max_width)
        return { w, max(max_width * h / w, min_height) };
    if (w < min_width)
        return { max_width, min(min_width * h / w, max_height) };
    if (h > max_height)
        return { max(max_height * w / h, min_width), max_height };
    if (h < min_height)
        return { min(min_height * w / h, max_width), min_height };
    if ((w > max_width && h > max_height) && (max_width / w < max_height / h))
        return { max_width, max(min_height, max_width * h / w) };
    if ((w > max_width && h > max_height) && (max_width / w > max_height / h))
        return { max(min_width, max_height * w / h), max_height };
    if ((w < min_width && h < min_height) && (min_width / w < min_height / h))
        return { min(max_width, min_height * w / h), min_height };
    if ((w < min_width && h < min_height) && (min_width / w > min_height / h))
        return { min_width, min(max_height, min_width * h / w) };
    if (w < min_width && h > max_height)
        return { min_width, max_height };
    if (w > max_width && h < min_height)
        return { max_width, min_height };
    return { w, h };
}

float FormattingContext::compute_auto_height_for_block_level_element(Box const& box, ConsiderFloats consider_floats)
{
    Optional<float> top;
    Optional<float> bottom;

    if (box.children_are_inline()) {
        // If it only has inline-level children, the height is the distance between
        // the top content edge and the bottom of the bottommost line box.
        auto& block_container = verify_cast<BlockContainer>(box);
        top = 0;
        if (!block_container.line_boxes().is_empty()) {
            for (auto& fragment : block_container.line_boxes().last().fragments()) {
                if (!bottom.has_value() || (fragment.offset().y() + fragment.height()) > bottom.value())
                    bottom = fragment.offset().y() + fragment.height();
            }
        }
    } else {
        // If it has block-level children, the height is the distance between
        // the top margin-edge of the topmost block-level child box
        // and the bottom margin-edge of the bottommost block-level child box.
        box.for_each_child_of_type<Box>([&](Layout::Box& child_box) {
            if (child_box.is_absolutely_positioned())
                return IterationDecision::Continue;
            if ((box.computed_values().overflow_y() == CSS::Overflow::Visible) && child_box.is_floating())
                return IterationDecision::Continue;

            float child_box_top = child_box.effective_offset().y() - child_box.box_model().margin_box().top;
            float child_box_bottom = child_box.effective_offset().y() + child_box.content_height() + child_box.box_model().margin_box().bottom;

            if (!top.has_value() || child_box_top < top.value())
                top = child_box_top;

            if (!bottom.has_value() || child_box_bottom > bottom.value())
                bottom = child_box_bottom;

            return IterationDecision::Continue;
        });
        if (consider_floats == ConsiderFloats::Yes) {
            // In addition, if the element has any floating descendants
            // whose bottom margin edge is below the element's bottom content edge,
            // then the height is increased to include those edges.
            box.for_each_child_of_type<Box>([&](Layout::Box& child_box) {
                if (!child_box.is_floating())
                    return IterationDecision::Continue;

                float child_box_bottom = child_box.effective_offset().y() + child_box.content_height();

                if (!bottom.has_value() || child_box_bottom > bottom.value())
                    bottom = child_box_bottom;

                return IterationDecision::Continue;
            });
        }
    }
    return bottom.value_or(0) - top.value_or(0);
}

// 10.3.2 Inline, replaced elements, https://www.w3.org/TR/CSS22/visudet.html#inline-replaced-width
float FormattingContext::tentative_width_for_replaced_element(ReplacedBox const& box, CSS::Length const& computed_width)
{
    auto& containing_block = *box.containing_block();
    auto height_of_containing_block = CSS::Length::make_px(containing_block.content_height());
    auto computed_height = box.computed_values().height().resolved(box, height_of_containing_block).resolved_or_auto(box);

    float used_width = computed_width.to_px(box);

    // If 'height' and 'width' both have computed values of 'auto' and the element also has an intrinsic width,
    // then that intrinsic width is the used value of 'width'.
    if (computed_height.is_auto() && computed_width.is_auto() && box.has_intrinsic_width())
        return box.intrinsic_width().value();

    // If 'height' and 'width' both have computed values of 'auto' and the element has no intrinsic width,
    // but does have an intrinsic height and intrinsic ratio;
    // or if 'width' has a computed value of 'auto',
    // 'height' has some other computed value, and the element does have an intrinsic ratio; then the used value of 'width' is:
    //
    //     (used height) * (intrinsic ratio)
    if ((computed_height.is_auto() && computed_width.is_auto() && !box.has_intrinsic_width() && box.has_intrinsic_height() && box.has_intrinsic_aspect_ratio())
        || (computed_width.is_auto() && box.has_intrinsic_aspect_ratio())) {
        return compute_height_for_replaced_element(box) * box.intrinsic_aspect_ratio().value();
    }

    // If 'height' and 'width' both have computed values of 'auto' and the element has an intrinsic ratio but no intrinsic height or width,
    // then the used value of 'width' is undefined in CSS 2.2. However, it is suggested that, if the containing block's width does not itself
    // depend on the replaced element's width, then the used value of 'width' is calculated from the constraint equation used for block-level,
    // non-replaced elements in normal flow.

    // Otherwise, if 'width' has a computed value of 'auto', and the element has an intrinsic width, then that intrinsic width is the used value of 'width'.
    if (computed_width.is_auto() && box.has_intrinsic_width())
        return box.intrinsic_width().value();

    // Otherwise, if 'width' has a computed value of 'auto', but none of the conditions above are met, then the used value of 'width' becomes 300px.
    // If 300px is too wide to fit the device, UAs should use the width of the largest rectangle that has a 2:1 ratio and fits the device instead.
    if (computed_width.is_auto())
        return 300;

    return used_width;
}

void FormattingContext::compute_width_for_absolutely_positioned_element(Box& box)
{
    if (is<ReplacedBox>(box))
        compute_width_for_absolutely_positioned_replaced_element(verify_cast<ReplacedBox>(box));
    else
        compute_width_for_absolutely_positioned_non_replaced_element(box);
}

void FormattingContext::compute_height_for_absolutely_positioned_element(Box& box)
{
    if (is<ReplacedBox>(box))
        compute_height_for_absolutely_positioned_replaced_element(verify_cast<ReplacedBox>(box));
    else
        compute_height_for_absolutely_positioned_non_replaced_element(box);
}

float FormattingContext::compute_width_for_replaced_element(const ReplacedBox& box)
{
    // 10.3.4 Block-level, replaced elements in normal flow...
    // 10.3.2 Inline, replaced elements

    auto zero_value = CSS::Length::make_px(0);
    auto& containing_block = *box.containing_block();
    auto width_of_containing_block = CSS::Length::make_px(containing_block.content_width());

    auto margin_left = box.computed_values().margin().left.resolved(box, width_of_containing_block).resolved_or_zero(box);
    auto margin_right = box.computed_values().margin().right.resolved(box, width_of_containing_block).resolved_or_zero(box);

    // A computed value of 'auto' for 'margin-left' or 'margin-right' becomes a used value of '0'.
    if (margin_left.is_auto())
        margin_left = zero_value;
    if (margin_right.is_auto())
        margin_right = zero_value;

    auto specified_width = box.computed_values().width().resolved(box, width_of_containing_block).resolved_or_auto(box);

    // 1. The tentative used width is calculated (without 'min-width' and 'max-width')
    auto used_width = tentative_width_for_replaced_element(box, specified_width);

    // 2. The tentative used width is greater than 'max-width', the rules above are applied again,
    //    but this time using the computed value of 'max-width' as the computed value for 'width'.
    auto specified_max_width = box.computed_values().max_width().resolved(box, width_of_containing_block).resolved_or_auto(box);
    if (!specified_max_width.is_auto()) {
        if (used_width > specified_max_width.to_px(box)) {
            used_width = tentative_width_for_replaced_element(box, specified_max_width);
        }
    }

    // 3. If the resulting width is smaller than 'min-width', the rules above are applied again,
    //    but this time using the value of 'min-width' as the computed value for 'width'.
    auto specified_min_width = box.computed_values().min_width().resolved(box, width_of_containing_block).resolved_or_auto(box);
    if (!specified_min_width.is_auto()) {
        if (used_width < specified_min_width.to_px(box)) {
            used_width = tentative_width_for_replaced_element(box, specified_min_width);
        }
    }

    return used_width;
}

// 10.6.2 Inline replaced elements, block-level replaced elements in normal flow, 'inline-block' replaced elements in normal flow and floating replaced elements
// https://www.w3.org/TR/CSS22/visudet.html#inline-replaced-height
float FormattingContext::tentative_height_for_replaced_element(ReplacedBox const& box, CSS::Length const& computed_height)
{
    auto& containing_block = *box.containing_block();
    auto width_of_containing_block = CSS::Length::make_px(containing_block.content_width());
    auto computed_width = box.computed_values().width().resolved(box, width_of_containing_block).resolved_or_auto(box);

    // If 'height' and 'width' both have computed values of 'auto' and the element also has
    // an intrinsic height, then that intrinsic height is the used value of 'height'.
    if (computed_width.is_auto() && computed_height.is_auto() && box.has_intrinsic_height())
        return box.intrinsic_height().value();

    // Otherwise, if 'height' has a computed value of 'auto', and the element has an intrinsic ratio then the used value of 'height' is:
    //
    //     (used width) / (intrinsic ratio)
    if (computed_height.is_auto() && box.has_intrinsic_aspect_ratio())
        return compute_width_for_replaced_element(box) / box.intrinsic_aspect_ratio().value();

    // Otherwise, if 'height' has a computed value of 'auto', and the element has an intrinsic height, then that intrinsic height is the used value of 'height'.
    if (computed_height.is_auto() && box.has_intrinsic_height())
        return box.intrinsic_height().value();

    // Otherwise, if 'height' has a computed value of 'auto', but none of the conditions above are met,
    // then the used value of 'height' must be set to the height of the largest rectangle that has a 2:1 ratio, has a height not greater than 150px,
    // and has a width not greater than the device width.
    if (computed_height.is_auto())
        return 150;

    return computed_height.to_px(box);
}

float FormattingContext::compute_height_for_replaced_element(const ReplacedBox& box)
{
    // 10.6.2 Inline replaced elements, block-level replaced elements in normal flow,
    // 'inline-block' replaced elements in normal flow and floating replaced elements

    auto& containing_block = *box.containing_block();
    auto width_of_containing_block = CSS::Length::make_px(containing_block.content_width());
    auto height_of_containing_block = CSS::Length::make_px(containing_block.content_height());
    auto specified_width = box.computed_values().width().resolved(box, width_of_containing_block).resolved_or_auto(box);
    auto specified_height = box.computed_values().height().resolved(box, height_of_containing_block).resolved_or_auto(box);

    float used_height = tentative_height_for_replaced_element(box, specified_height);

    if (specified_width.is_auto() && specified_height.is_auto() && box.has_intrinsic_aspect_ratio()) {
        float w = tentative_width_for_replaced_element(box, specified_width);
        float h = used_height;
        used_height = solve_replaced_size_constraint(w, h, box).height();
    }

    return used_height;
}

void FormattingContext::compute_width_for_absolutely_positioned_non_replaced_element(Box& box)
{
    auto& containing_block = *box.containing_block();
    auto width_of_containing_block = CSS::Length::make_px(containing_block.content_width());
    auto& computed_values = box.computed_values();
    auto zero_value = CSS::Length::make_px(0);

    auto margin_left = CSS::Length::make_auto();
    auto margin_right = CSS::Length::make_auto();
    const auto border_left = computed_values.border_left().width;
    const auto border_right = computed_values.border_right().width;
    const auto padding_left = computed_values.padding().left.resolved(box, width_of_containing_block).resolved_or_zero(box);
    const auto padding_right = computed_values.padding().right.resolved(box, width_of_containing_block).resolved_or_zero(box);

    auto try_compute_width = [&](const auto& a_width) {
        margin_left = computed_values.margin().left.resolved(box, width_of_containing_block).resolved_or_zero(box);
        margin_right = computed_values.margin().right.resolved(box, width_of_containing_block).resolved_or_zero(box);

        auto left = computed_values.offset().left.resolved(box, width_of_containing_block).resolved_or_auto(box);
        auto right = computed_values.offset().right.resolved(box, width_of_containing_block).resolved_or_auto(box);
        auto width = a_width;

        auto solve_for_left = [&] {
            return CSS::Length(containing_block.content_width() - margin_left.to_px(box) - border_left - padding_left.to_px(box) - width.to_px(box) - padding_right.to_px(box) - border_right - margin_right.to_px(box) - right.to_px(box), CSS::Length::Type::Px);
        };

        auto solve_for_width = [&] {
            return CSS::Length(containing_block.content_width() - left.to_px(box) - margin_left.to_px(box) - border_left - padding_left.to_px(box) - padding_right.to_px(box) - border_right - margin_right.to_px(box) - right.to_px(box), CSS::Length::Type::Px);
        };

        auto solve_for_right = [&] {
            return CSS::Length(containing_block.content_width() - left.to_px(box) - margin_left.to_px(box) - border_left - padding_left.to_px(box) - width.to_px(box) - padding_right.to_px(box) - border_right - margin_right.to_px(box), CSS::Length::Type::Px);
        };

        // If all three of 'left', 'width', and 'right' are 'auto':
        if (left.is_auto() && width.is_auto() && right.is_auto()) {
            // First set any 'auto' values for 'margin-left' and 'margin-right' to 0.
            if (margin_left.is_auto())
                margin_left = CSS::Length::make_px(0);
            if (margin_right.is_auto())
                margin_right = CSS::Length::make_px(0);
            // Then, if the 'direction' property of the element establishing the static-position containing block
            // is 'ltr' set 'left' to the static position and apply rule number three below;
            // otherwise, set 'right' to the static position and apply rule number one below.
            // FIXME: This is very hackish.
            left = CSS::Length::make_px(0);
            goto Rule3;
        }

        if (!left.is_auto() && !width.is_auto() && !right.is_auto()) {
            // FIXME: This should be solved in a more complicated way.
            return width;
        }

        if (margin_left.is_auto())
            margin_left = CSS::Length::make_px(0);
        if (margin_right.is_auto())
            margin_right = CSS::Length::make_px(0);

        // 1. 'left' and 'width' are 'auto' and 'right' is not 'auto',
        //    then the width is shrink-to-fit. Then solve for 'left'
        if (left.is_auto() && width.is_auto() && !right.is_auto()) {
            auto result = calculate_shrink_to_fit_widths(box);
            solve_for_left();
            auto available_width = solve_for_width();
            width = CSS::Length(min(max(result.preferred_minimum_width, available_width.to_px(box)), result.preferred_width), CSS::Length::Type::Px);
        }

        // 2. 'left' and 'right' are 'auto' and 'width' is not 'auto',
        //    then if the 'direction' property of the element establishing
        //    the static-position containing block is 'ltr' set 'left'
        //    to the static position, otherwise set 'right' to the static position.
        //    Then solve for 'left' (if 'direction is 'rtl') or 'right' (if 'direction' is 'ltr').
        else if (left.is_auto() && right.is_auto() && !width.is_auto()) {
            // FIXME: Check direction
            // FIXME: Use the static-position containing block
            left = zero_value;
            right = solve_for_right();
        }

        // 3. 'width' and 'right' are 'auto' and 'left' is not 'auto',
        //    then the width is shrink-to-fit. Then solve for 'right'
        else if (width.is_auto() && right.is_auto() && !left.is_auto()) {
        Rule3:
            auto result = calculate_shrink_to_fit_widths(box);
            auto available_width = solve_for_width();
            width = CSS::Length(min(max(result.preferred_minimum_width, available_width.to_px(box)), result.preferred_width), CSS::Length::Type::Px);
            right = solve_for_right();
        }

        // 4. 'left' is 'auto', 'width' and 'right' are not 'auto', then solve for 'left'
        else if (left.is_auto() && !width.is_auto() && !right.is_auto()) {
            left = solve_for_left();
        }

        // 5. 'width' is 'auto', 'left' and 'right' are not 'auto', then solve for 'width'
        else if (width.is_auto() && !left.is_auto() && !right.is_auto()) {
            width = solve_for_width();
        }

        // 6. 'right' is 'auto', 'left' and 'width' are not 'auto', then solve for 'right'
        else if (right.is_auto() && !left.is_auto() && !width.is_auto()) {
            right = solve_for_right();
        }

        return width;
    };

    auto specified_width = computed_values.width().resolved(box, width_of_containing_block).resolved_or_auto(box);

    // 1. The tentative used width is calculated (without 'min-width' and 'max-width')
    auto used_width = try_compute_width(specified_width);

    // 2. The tentative used width is greater than 'max-width', the rules above are applied again,
    //    but this time using the computed value of 'max-width' as the computed value for 'width'.
    auto specified_max_width = computed_values.max_width().resolved(box, width_of_containing_block).resolved_or_auto(box);
    if (!specified_max_width.is_auto()) {
        if (used_width.to_px(box) > specified_max_width.to_px(box)) {
            used_width = try_compute_width(specified_max_width);
        }
    }

    // 3. If the resulting width is smaller than 'min-width', the rules above are applied again,
    //    but this time using the value of 'min-width' as the computed value for 'width'.
    auto specified_min_width = computed_values.min_width().resolved(box, width_of_containing_block).resolved_or_auto(box);
    if (!specified_min_width.is_auto()) {
        if (used_width.to_px(box) < specified_min_width.to_px(box)) {
            used_width = try_compute_width(specified_min_width);
        }
    }

    box.set_content_width(used_width.to_px(box));

    box.box_model().margin.left = margin_left.to_px(box);
    box.box_model().margin.right = margin_right.to_px(box);
    box.box_model().border.left = border_left;
    box.box_model().border.right = border_right;
    box.box_model().padding.left = padding_left.to_px(box);
    box.box_model().padding.right = padding_right.to_px(box);
}

void FormattingContext::compute_width_for_absolutely_positioned_replaced_element(ReplacedBox& box)
{
    // 10.3.8 Absolutely positioned, replaced elements
    // The used value of 'width' is determined as for inline replaced elements.
    box.prepare_for_replaced_layout();
    box.set_content_width(compute_width_for_replaced_element(box));
}

void FormattingContext::compute_height_for_absolutely_positioned_non_replaced_element(Box& box)
{
    auto& computed_values = box.computed_values();
    auto& containing_block = *box.containing_block();
    auto width_of_containing_block = CSS::Length::make_px(containing_block.content_width());
    auto height_of_containing_block = CSS::Length::make_px(containing_block.content_height());

    CSS::Length specified_top = computed_values.offset().top.resolved(box, height_of_containing_block).resolved_or_auto(box);
    CSS::Length specified_bottom = computed_values.offset().bottom.resolved(box, height_of_containing_block).resolved_or_auto(box);
    CSS::Length specified_height;

    if (computed_values.height().is_percentage() && !(containing_block.computed_values().height().is_length() && containing_block.computed_values().height().length().is_absolute())) {
        specified_height = CSS::Length::make_auto();
    } else {
        specified_height = computed_values.height().resolved(box, height_of_containing_block).resolved_or_auto(box);
    }

    auto specified_max_height = computed_values.max_height().resolved(box, height_of_containing_block).resolved_or_auto(box);
    auto specified_min_height = computed_values.min_height().resolved(box, height_of_containing_block).resolved_or_auto(box);

    box.box_model().margin.top = computed_values.margin().top.resolved(box, width_of_containing_block).resolved_or_zero(box).to_px(box);
    box.box_model().margin.bottom = computed_values.margin().bottom.resolved(box, width_of_containing_block).resolved_or_zero(box).to_px(box);
    box.box_model().border.top = computed_values.border_top().width;
    box.box_model().border.bottom = computed_values.border_bottom().width;
    box.box_model().padding.top = computed_values.padding().top.resolved(box, width_of_containing_block).resolved_or_zero(box).to_px(box);
    box.box_model().padding.bottom = computed_values.padding().bottom.resolved(box, width_of_containing_block).resolved_or_zero(box).to_px(box);

    if (specified_height.is_auto() && !specified_top.is_auto() && specified_bottom.is_auto()) {
        const auto& margin = box.box_model().margin;
        const auto& padding = box.box_model().padding;
        const auto& border = box.box_model().border;

        specified_height = CSS::Length(compute_auto_height_for_block_level_element(box), CSS::Length::Type::Px);
        box.box_model().offset.bottom = containing_block.content_height() - specified_height.to_px(box) - specified_top.to_px(box) - margin.top - padding.top - border.top - margin.bottom - padding.bottom - border.bottom;
    }

    else if (specified_height.is_auto() && !specified_top.is_auto() && !specified_bottom.is_auto()) {
        const auto& margin = box.box_model().margin;
        const auto& padding = box.box_model().padding;
        const auto& border = box.box_model().border;

        specified_height = CSS::Length(containing_block.content_height() - specified_top.to_px(box) - margin.top - padding.top - border.top - specified_bottom.to_px(box) - margin.bottom - padding.bottom - border.bottom, CSS::Length::Type::Px);
    }

    if (!specified_height.is_auto()) {
        float used_height = specified_height.to_px(box);
        if (!specified_max_height.is_auto())
            used_height = min(used_height, specified_max_height.to_px(box));
        if (!specified_min_height.is_auto())
            used_height = max(used_height, specified_min_height.to_px(box));
        box.set_content_height(used_height);
    }
}

void FormattingContext::layout_absolutely_positioned_element(Box& box)
{
    auto& containing_block = *box.containing_block();
    auto width_of_containing_block = CSS::Length::make_px(containing_block.content_width());
    auto height_of_containing_block = CSS::Length::make_px(containing_block.content_height());
    auto& box_model = box.box_model();

    auto specified_width = box.computed_values().width().resolved(box, width_of_containing_block).resolved_or_auto(box);

    compute_width_for_absolutely_positioned_element(box);
    auto independent_formatting_context = layout_inside(box, LayoutMode::Default);
    compute_height_for_absolutely_positioned_element(box);

    box_model.margin.left = box.computed_values().margin().left.resolved(box, width_of_containing_block).resolved_or_auto(box).to_px(box);
    box_model.margin.top = box.computed_values().margin().top.resolved(box, height_of_containing_block).resolved_or_auto(box).to_px(box);
    box_model.margin.right = box.computed_values().margin().right.resolved(box, width_of_containing_block).resolved_or_auto(box).to_px(box);
    box_model.margin.bottom = box.computed_values().margin().bottom.resolved(box, height_of_containing_block).resolved_or_auto(box).to_px(box);

    box_model.border.left = box.computed_values().border_left().width;
    box_model.border.right = box.computed_values().border_right().width;
    box_model.border.top = box.computed_values().border_top().width;
    box_model.border.bottom = box.computed_values().border_bottom().width;

    box_model.offset.left = box.computed_values().offset().left.resolved(box, width_of_containing_block).resolved_or_auto(box).to_px(box);
    box_model.offset.top = box.computed_values().offset().top.resolved(box, height_of_containing_block).resolved_or_auto(box).to_px(box);
    box_model.offset.right = box.computed_values().offset().right.resolved(box, width_of_containing_block).resolved_or_auto(box).to_px(box);
    box_model.offset.bottom = box.computed_values().offset().bottom.resolved(box, height_of_containing_block).resolved_or_auto(box).to_px(box);

    auto is_auto = [](auto const& length_percentage) {
        return length_percentage.is_length() && length_percentage.length().is_auto();
    };

    if (is_auto(box.computed_values().offset().left) && specified_width.is_auto() && is_auto(box.computed_values().offset().right)) {
        if (is_auto(box.computed_values().margin().left))
            box_model.margin.left = 0;
        if (is_auto(box.computed_values().margin().right))
            box_model.margin.right = 0;
    }

    Gfx::FloatPoint used_offset;

    if (!is_auto(box.computed_values().offset().left)) {
        float x_offset = box_model.offset.left
            + box_model.border_box().left;
        used_offset.set_x(x_offset + box_model.margin.left);
    } else if (!is_auto(box.computed_values().offset().right)) {
        float x_offset = 0
            - box_model.offset.right
            - box_model.border_box().right;
        used_offset.set_x(containing_block.content_width() + x_offset - box.content_width() - box_model.margin.right);
    } else {
        float x_offset = box_model.margin_box().left;
        used_offset.set_x(x_offset);
    }

    if (!is_auto(box.computed_values().offset().top)) {
        float y_offset = box_model.offset.top
            + box_model.border_box().top;
        used_offset.set_y(y_offset + box_model.margin.top);
    } else if (!is_auto(box.computed_values().offset().bottom)) {
        float y_offset = 0
            - box_model.offset.bottom
            - box_model.border_box().bottom;
        used_offset.set_y(containing_block.content_height() + y_offset - box.content_height() - box_model.margin.bottom);
    } else {
        float y_offset = box_model.margin_box().top;
        used_offset.set_y(y_offset);
    }

    box.set_offset(used_offset);

    if (independent_formatting_context)
        independent_formatting_context->parent_context_did_dimension_child_root_box();
}

void FormattingContext::compute_height_for_absolutely_positioned_replaced_element(ReplacedBox& box)
{
    // 10.6.5 Absolutely positioned, replaced elements
    // The used value of 'height' is determined as for inline replaced elements.
    box.set_content_height(compute_height_for_replaced_element(box));
}

}
