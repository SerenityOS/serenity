/*
 * Copyright (c) 2020-2022, Andreas Kling <kling@serenityos.org>
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

FormattingContext::FormattingContext(Type type, FormattingState& state, Box const& context_box, FormattingContext* parent)
    : m_type(type)
    , m_parent(parent)
    , m_context_box(context_box)
    , m_state(state)
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

OwnPtr<FormattingContext> FormattingContext::create_independent_formatting_context_if_needed(Box const& child_box)
{
    if (!child_box.can_have_children())
        return {};

    auto child_display = child_box.computed_values().display();

    if (is<SVGSVGBox>(child_box))
        return make<SVGFormattingContext>(m_state, child_box, this);

    if (child_display.is_flex_inside())
        return make<FlexFormattingContext>(m_state, child_box, this);

    if (creates_block_formatting_context(child_box))
        return make<BlockFormattingContext>(m_state, verify_cast<BlockContainer>(child_box), this);

    if (child_display.is_table_inside())
        return make<TableFormattingContext>(m_state, verify_cast<TableBox>(child_box), this);

    VERIFY(is_block_formatting_context());
    if (child_box.children_are_inline())
        return make<InlineFormattingContext>(m_state, verify_cast<BlockContainer>(child_box), static_cast<BlockFormattingContext&>(*this));

    // The child box is a block container that doesn't create its own BFC.
    // It will be formatted by this BFC.
    if (!child_display.is_flow_inside()) {
        dbgln("FIXME: Child box doesn't create BFC, but inside is also not flow! display={}", child_display.to_string());
        // HACK: Instead of crashing, create a dummy formatting context that does nothing.
        // FIXME: Remove this once it's no longer needed. It currently swallows problem with standalone
        //        table-related boxes that don't get fixed up by CSS anonymous table box generation.
        struct DummyFormattingContext : public FormattingContext {
            DummyFormattingContext(FormattingState& state, Box const& box)
                : FormattingContext(Type::Block, state, box)
            {
            }
            virtual void run(Box const&, LayoutMode) override { }
        };
        return make<DummyFormattingContext>(m_state, child_box);
    }
    VERIFY(child_box.is_block_container());
    VERIFY(child_display.is_flow_inside());
    return {};
}

OwnPtr<FormattingContext> FormattingContext::layout_inside(Box const& child_box, LayoutMode layout_mode)
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

static float greatest_child_width(FormattingState const& state, Box const& box)
{
    float max_width = 0;
    if (box.children_are_inline()) {
        for (auto& child : state.get(verify_cast<BlockContainer>(box)).line_boxes) {
            max_width = max(max_width, child.width());
        }
    } else {
        box.for_each_child_of_type<Box>([&](auto& child) {
            max_width = max(max_width, state.get(child).border_box_width());
        });
    }
    return max_width;
}

FormattingContext::ShrinkToFitResult FormattingContext::calculate_shrink_to_fit_widths(Box const& box)
{
    // Calculate the preferred width by formatting the content without breaking lines
    // other than where explicit line breaks occur.
    (void)layout_inside(box, LayoutMode::OnlyRequiredLineBreaks);
    float preferred_width = greatest_child_width(m_state, box);

    // Also calculate the preferred minimum width, e.g., by trying all possible line breaks.
    // CSS 2.2 does not define the exact algorithm.

    (void)layout_inside(box, LayoutMode::AllPossibleLineBreaks);
    float preferred_minimum_width = greatest_child_width(m_state, box);

    return { preferred_width, preferred_minimum_width };
}

static Gfx::FloatSize solve_replaced_size_constraint(FormattingState const& state, float w, float h, ReplacedBox const& box)
{
    // 10.4 Minimum and maximum widths: 'min-width' and 'max-width'

    auto const& containing_block = *box.containing_block();
    auto const& containing_block_state = state.get(containing_block);
    auto width_of_containing_block = CSS::Length::make_px(containing_block_state.content_width);
    auto height_of_containing_block = CSS::Length::make_px(containing_block_state.content_height);

    auto specified_min_width = box.computed_values().min_width().has_value() ? box.computed_values().min_width()->resolved(box, width_of_containing_block).to_px(box) : 0;
    auto specified_max_width = box.computed_values().max_width().has_value() ? box.computed_values().max_width()->resolved(box, width_of_containing_block).to_px(box) : w;
    auto specified_min_height = box.computed_values().min_height().has_value() ? box.computed_values().min_height()->resolved(box, height_of_containing_block).to_px(box) : 0;
    auto specified_max_height = box.computed_values().max_height().has_value() ? box.computed_values().max_height()->resolved(box, height_of_containing_block).to_px(box) : h;

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

float FormattingContext::compute_auto_height_for_block_level_element(FormattingState const& state, Box const& box, ConsiderFloats consider_floats)
{
    Optional<float> top;
    Optional<float> bottom;

    if (box.children_are_inline()) {
        // If it only has inline-level children, the height is the distance between
        // the top content edge and the bottom of the bottommost line box.
        auto const& block_container = verify_cast<BlockContainer>(box);
        auto const& line_boxes = state.get(block_container).line_boxes;
        top = 0;
        if (!line_boxes.is_empty()) {
            for (auto& fragment : line_boxes.last().fragments()) {
                float fragment_top = fragment.offset().y() - fragment.border_box_top();
                if (!top.has_value() || fragment_top < *top)
                    top = fragment_top;
                float fragment_bottom = fragment.offset().y() + fragment.height() + fragment.border_box_bottom();
                if (!bottom.has_value() || fragment_bottom > *bottom)
                    bottom = fragment_bottom;
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

            auto const& child_box_state = state.get(child_box);

            float child_box_top = child_box_state.offset.y() - child_box_state.margin_box_top();
            float child_box_bottom = child_box_state.offset.y() + child_box_state.content_height + child_box_state.margin_box_bottom();

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

                auto const& child_box_state = state.get(child_box);

                float child_box_bottom = child_box_state.offset.y() + child_box_state.content_height;

                if (!bottom.has_value() || child_box_bottom > bottom.value())
                    bottom = child_box_bottom;

                return IterationDecision::Continue;
            });
        }
    }
    return bottom.value_or(0) - top.value_or(0);
}

// 10.3.2 Inline, replaced elements, https://www.w3.org/TR/CSS22/visudet.html#inline-replaced-width
float FormattingContext::tentative_width_for_replaced_element(FormattingState const& state, ReplacedBox const& box, CSS::Length const& computed_width)
{
    auto const& containing_block = *box.containing_block();
    auto height_of_containing_block = CSS::Length::make_px(state.get(containing_block).content_height);
    auto computed_height = box.computed_values().height().has_value() ? box.computed_values().height()->resolved(box, height_of_containing_block).resolved(box) : CSS::Length::make_auto();

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
        return compute_height_for_replaced_element(state, box) * box.intrinsic_aspect_ratio().value();
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

void FormattingContext::compute_width_for_absolutely_positioned_element(Box const& box)
{
    if (is<ReplacedBox>(box))
        compute_width_for_absolutely_positioned_replaced_element(verify_cast<ReplacedBox>(box));
    else
        compute_width_for_absolutely_positioned_non_replaced_element(box);
}

void FormattingContext::compute_height_for_absolutely_positioned_element(Box const& box)
{
    if (is<ReplacedBox>(box))
        compute_height_for_absolutely_positioned_replaced_element(verify_cast<ReplacedBox>(box));
    else
        compute_height_for_absolutely_positioned_non_replaced_element(box);
}

float FormattingContext::compute_width_for_replaced_element(FormattingState const& state, ReplacedBox const& box)
{
    // 10.3.4 Block-level, replaced elements in normal flow...
    // 10.3.2 Inline, replaced elements

    auto zero_value = CSS::Length::make_px(0);
    auto const& containing_block = *box.containing_block();
    auto width_of_containing_block = CSS::Length::make_px(state.get(containing_block).content_width);

    auto margin_left = box.computed_values().margin().left.resolved(box, width_of_containing_block).resolved(box);
    auto margin_right = box.computed_values().margin().right.resolved(box, width_of_containing_block).resolved(box);

    // A computed value of 'auto' for 'margin-left' or 'margin-right' becomes a used value of '0'.
    if (margin_left.is_auto())
        margin_left = zero_value;
    if (margin_right.is_auto())
        margin_right = zero_value;

    auto specified_width = box.computed_values().width().has_value() ? box.computed_values().width()->resolved(box, width_of_containing_block).resolved(box) : CSS::Length::make_auto();

    // 1. The tentative used width is calculated (without 'min-width' and 'max-width')
    auto used_width = tentative_width_for_replaced_element(state, box, specified_width);

    // 2. The tentative used width is greater than 'max-width', the rules above are applied again,
    //    but this time using the computed value of 'max-width' as the computed value for 'width'.
    auto specified_max_width = box.computed_values().max_width().has_value() ? box.computed_values().max_width()->resolved(box, width_of_containing_block).resolved(box) : CSS::Length::make_auto();
    if (!specified_max_width.is_auto()) {
        if (used_width > specified_max_width.to_px(box)) {
            used_width = tentative_width_for_replaced_element(state, box, specified_max_width);
        }
    }

    // 3. If the resulting width is smaller than 'min-width', the rules above are applied again,
    //    but this time using the value of 'min-width' as the computed value for 'width'.
    auto specified_min_width = box.computed_values().min_width().has_value() ? box.computed_values().min_width()->resolved(box, width_of_containing_block).resolved(box) : CSS::Length::make_auto();
    if (!specified_min_width.is_auto()) {
        if (used_width < specified_min_width.to_px(box)) {
            used_width = tentative_width_for_replaced_element(state, box, specified_min_width);
        }
    }

    return used_width;
}

// 10.6.2 Inline replaced elements, block-level replaced elements in normal flow, 'inline-block' replaced elements in normal flow and floating replaced elements
// https://www.w3.org/TR/CSS22/visudet.html#inline-replaced-height
float FormattingContext::tentative_height_for_replaced_element(FormattingState const& state, ReplacedBox const& box, CSS::Length const& computed_height)
{
    auto const& containing_block = *box.containing_block();
    auto width_of_containing_block = CSS::Length::make_px(state.get(containing_block).content_width);
    auto computed_width = box.computed_values().width().has_value() ? box.computed_values().width()->resolved(box, width_of_containing_block).resolved(box) : CSS::Length::make_auto();

    // If 'height' and 'width' both have computed values of 'auto' and the element also has
    // an intrinsic height, then that intrinsic height is the used value of 'height'.
    if (computed_width.is_auto() && computed_height.is_auto() && box.has_intrinsic_height())
        return box.intrinsic_height().value();

    // Otherwise, if 'height' has a computed value of 'auto', and the element has an intrinsic ratio then the used value of 'height' is:
    //
    //     (used width) / (intrinsic ratio)
    if (computed_height.is_auto() && box.has_intrinsic_aspect_ratio())
        return compute_width_for_replaced_element(state, box) / box.intrinsic_aspect_ratio().value();

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

float FormattingContext::compute_height_for_replaced_element(FormattingState const& state, ReplacedBox const& box)
{
    // 10.6.2 Inline replaced elements, block-level replaced elements in normal flow,
    // 'inline-block' replaced elements in normal flow and floating replaced elements

    auto const& containing_block = *box.containing_block();
    auto const& containing_block_state = state.get(containing_block);
    auto width_of_containing_block = CSS::Length::make_px(containing_block_state.content_width);
    auto height_of_containing_block = CSS::Length::make_px(containing_block_state.content_height);
    auto specified_width = box.computed_values().width().has_value() ? box.computed_values().width()->resolved(box, width_of_containing_block).resolved(box) : CSS::Length::make_auto();
    auto specified_height = box.computed_values().height().has_value() ? box.computed_values().height()->resolved(box, height_of_containing_block).resolved(box) : CSS::Length::make_auto();

    float used_height = tentative_height_for_replaced_element(state, box, specified_height);

    if (specified_width.is_auto() && specified_height.is_auto() && box.has_intrinsic_aspect_ratio()) {
        float w = tentative_width_for_replaced_element(state, box, specified_width);
        float h = used_height;
        used_height = solve_replaced_size_constraint(state, w, h, box).height();
    }

    return used_height;
}

void FormattingContext::compute_width_for_absolutely_positioned_non_replaced_element(Box const& box)
{
    auto& containing_block_state = m_state.get(*box.containing_block());
    auto& box_state = m_state.get_mutable(box);

    auto width_of_containing_block = CSS::Length::make_px(containing_block_state.content_width);
    auto& computed_values = box.computed_values();
    auto zero_value = CSS::Length::make_px(0);

    auto margin_left = CSS::Length::make_auto();
    auto margin_right = CSS::Length::make_auto();
    const auto border_left = computed_values.border_left().width;
    const auto border_right = computed_values.border_right().width;
    const auto padding_left = computed_values.padding().left.resolved(box, width_of_containing_block).to_px(box);
    const auto padding_right = computed_values.padding().right.resolved(box, width_of_containing_block).to_px(box);

    auto try_compute_width = [&](const auto& a_width) {
        margin_left = computed_values.margin().left.resolved(box, width_of_containing_block).resolved(box);
        margin_right = computed_values.margin().right.resolved(box, width_of_containing_block).resolved(box);

        auto left = computed_values.offset().left.resolved(box, width_of_containing_block).resolved(box);
        auto right = computed_values.offset().right.resolved(box, width_of_containing_block).resolved(box);
        auto width = a_width;

        auto solve_for_left = [&] {
            return CSS::Length(containing_block_state.content_width - margin_left.to_px(box) - border_left - padding_left - width.to_px(box) - padding_right - border_right - margin_right.to_px(box) - right.to_px(box), CSS::Length::Type::Px);
        };

        auto solve_for_width = [&] {
            return CSS::Length(containing_block_state.content_width - left.to_px(box) - margin_left.to_px(box) - border_left - padding_left - padding_right - border_right - margin_right.to_px(box) - right.to_px(box), CSS::Length::Type::Px);
        };

        auto solve_for_right = [&] {
            return CSS::Length(containing_block_state.content_width - left.to_px(box) - margin_left.to_px(box) - border_left - padding_left - width.to_px(box) - padding_right - border_right - margin_right.to_px(box), CSS::Length::Type::Px);
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

    auto specified_width = computed_values.width().has_value() ? computed_values.width()->resolved(box, width_of_containing_block).resolved(box) : CSS::Length::make_auto();

    // 1. The tentative used width is calculated (without 'min-width' and 'max-width')
    auto used_width = try_compute_width(specified_width);

    // 2. The tentative used width is greater than 'max-width', the rules above are applied again,
    //    but this time using the computed value of 'max-width' as the computed value for 'width'.
    auto specified_max_width = computed_values.max_width().has_value() ? computed_values.max_width()->resolved(box, width_of_containing_block).resolved(box) : CSS::Length::make_auto();
    if (!specified_max_width.is_auto()) {
        if (used_width.to_px(box) > specified_max_width.to_px(box)) {
            used_width = try_compute_width(specified_max_width);
        }
    }

    // 3. If the resulting width is smaller than 'min-width', the rules above are applied again,
    //    but this time using the value of 'min-width' as the computed value for 'width'.
    auto specified_min_width = computed_values.min_width().has_value() ? computed_values.min_width()->resolved(box, width_of_containing_block).resolved(box) : CSS::Length::make_auto();
    if (!specified_min_width.is_auto()) {
        if (used_width.to_px(box) < specified_min_width.to_px(box)) {
            used_width = try_compute_width(specified_min_width);
        }
    }

    box_state.content_width = used_width.to_px(box);

    box_state.margin_left = margin_left.to_px(box);
    box_state.margin_right = margin_right.to_px(box);
    box_state.border_left = border_left;
    box_state.border_right = border_right;
    box_state.padding_left = padding_left;
    box_state.padding_right = padding_right;
}

void FormattingContext::compute_width_for_absolutely_positioned_replaced_element(ReplacedBox const& box)
{
    // 10.3.8 Absolutely positioned, replaced elements
    // The used value of 'width' is determined as for inline replaced elements.
    // FIXME: This const_cast is gross.
    const_cast<ReplacedBox&>(box).prepare_for_replaced_layout();
    m_state.get_mutable(box).content_width = compute_width_for_replaced_element(m_state, box);
}

void FormattingContext::compute_height_for_absolutely_positioned_non_replaced_element(Box const& box)
{
    auto& computed_values = box.computed_values();
    auto const& containing_block = *box.containing_block();
    auto const& containing_block_state = m_state.get(containing_block);
    auto& box_state = m_state.get_mutable(box);
    auto width_of_containing_block = CSS::Length::make_px(containing_block_state.content_width);
    auto height_of_containing_block = CSS::Length::make_px(containing_block_state.content_height);

    CSS::Length specified_top = computed_values.offset().top.resolved(box, height_of_containing_block).resolved(box);
    CSS::Length specified_bottom = computed_values.offset().bottom.resolved(box, height_of_containing_block).resolved(box);
    CSS::Length specified_height = CSS::Length::make_auto();

    if (computed_values.height().has_value() && computed_values.height()->is_percentage()
        && !(containing_block.computed_values().height().has_value() && containing_block.computed_values().height()->is_length() && containing_block.computed_values().height()->length().is_absolute())) {
        // specified_height is already auto
    } else {
        specified_height = computed_values.height().has_value() ? computed_values.height()->resolved(box, height_of_containing_block).resolved(box) : CSS::Length::make_auto();
    }

    auto specified_max_height = computed_values.max_height().has_value() ? computed_values.max_height()->resolved(box, height_of_containing_block).resolved(box) : CSS::Length::make_auto();
    auto specified_min_height = computed_values.min_height().has_value() ? computed_values.min_height()->resolved(box, height_of_containing_block).resolved(box) : CSS::Length::make_auto();

    box_state.margin_top = computed_values.margin().top.resolved(box, width_of_containing_block).to_px(box);
    box_state.margin_bottom = computed_values.margin().bottom.resolved(box, width_of_containing_block).to_px(box);
    box_state.border_top = computed_values.border_top().width;
    box_state.border_bottom = computed_values.border_bottom().width;
    box_state.padding_top = computed_values.padding().top.resolved(box, width_of_containing_block).to_px(box);
    box_state.padding_bottom = computed_values.padding().bottom.resolved(box, width_of_containing_block).to_px(box);

    if (specified_height.is_auto() && !specified_top.is_auto() && specified_bottom.is_auto()) {
        specified_height = CSS::Length(compute_auto_height_for_block_level_element(m_state, box), CSS::Length::Type::Px);
        box_state.offset_bottom = containing_block_state.content_height - specified_height.to_px(box) - specified_top.to_px(box) - box_state.margin_top - box_state.padding_top - box_state.border_top - box_state.margin_bottom - box_state.padding_bottom - box_state.border_bottom;
    }

    else if (specified_height.is_auto() && !specified_top.is_auto() && !specified_bottom.is_auto()) {
        specified_height = CSS::Length(containing_block_state.content_height - specified_top.to_px(box) - box_state.margin_top - box_state.padding_top - box_state.border_top - specified_bottom.to_px(box) - box_state.margin_bottom - box_state.padding_bottom - box_state.border_bottom, CSS::Length::Type::Px);
    }

    if (!specified_height.is_auto()) {
        float used_height = specified_height.to_px(box);
        if (!specified_max_height.is_auto())
            used_height = min(used_height, specified_max_height.to_px(box));
        if (!specified_min_height.is_auto())
            used_height = max(used_height, specified_min_height.to_px(box));
        box_state.content_height = used_height;
    }
}

void FormattingContext::layout_absolutely_positioned_element(Box const& box)
{
    auto const& containing_block_state = m_state.get(*box.containing_block());
    auto width_of_containing_block = CSS::Length::make_px(containing_block_state.content_width);
    auto height_of_containing_block = CSS::Length::make_px(containing_block_state.content_height);
    auto& box_state = m_state.get_mutable(box);

    auto specified_width = box.computed_values().width().has_value() ? box.computed_values().width()->resolved(box, width_of_containing_block).resolved(box) : CSS::Length::make_auto();

    compute_width_for_absolutely_positioned_element(box);
    auto independent_formatting_context = layout_inside(box, LayoutMode::Default);
    compute_height_for_absolutely_positioned_element(box);

    box_state.margin_left = box.computed_values().margin().left.resolved(box, width_of_containing_block).to_px(box);
    box_state.margin_top = box.computed_values().margin().top.resolved(box, height_of_containing_block).to_px(box);
    box_state.margin_right = box.computed_values().margin().right.resolved(box, width_of_containing_block).to_px(box);
    box_state.margin_bottom = box.computed_values().margin().bottom.resolved(box, height_of_containing_block).to_px(box);

    box_state.border_left = box.computed_values().border_left().width;
    box_state.border_right = box.computed_values().border_right().width;
    box_state.border_top = box.computed_values().border_top().width;
    box_state.border_bottom = box.computed_values().border_bottom().width;

    box_state.offset_left = box.computed_values().offset().left.resolved(box, width_of_containing_block).to_px(box);
    box_state.offset_top = box.computed_values().offset().top.resolved(box, height_of_containing_block).to_px(box);
    box_state.offset_right = box.computed_values().offset().right.resolved(box, width_of_containing_block).to_px(box);
    box_state.offset_bottom = box.computed_values().offset().bottom.resolved(box, height_of_containing_block).to_px(box);

    auto is_auto = [](auto const& length_percentage) {
        return length_percentage.is_length() && length_percentage.length().is_auto();
    };

    if (is_auto(box.computed_values().offset().left) && specified_width.is_auto() && is_auto(box.computed_values().offset().right)) {
        if (is_auto(box.computed_values().margin().left))
            box_state.margin_left = 0;
        if (is_auto(box.computed_values().margin().right))
            box_state.margin_right = 0;
    }

    Gfx::FloatPoint used_offset;

    if (!is_auto(box.computed_values().offset().left)) {
        float x_offset = box_state.offset_left
            + box_state.border_box_left();
        used_offset.set_x(x_offset + box_state.margin_left);
    } else if (!is_auto(box.computed_values().offset().right)) {
        float x_offset = 0
            - box_state.offset_right
            - box_state.border_box_right();
        used_offset.set_x(containing_block_state.content_width + x_offset - box_state.content_width - box_state.margin_right);
    } else {
        float x_offset = box_state.margin_box_left();
        used_offset.set_x(x_offset);
    }

    if (!is_auto(box.computed_values().offset().top)) {
        float y_offset = box_state.offset_top
            + box_state.border_box_top();
        used_offset.set_y(y_offset + box_state.margin_top);
    } else if (!is_auto(box.computed_values().offset().bottom)) {
        float y_offset = 0
            - box_state.offset_bottom
            - box_state.border_box_bottom();
        used_offset.set_y(containing_block_state.content_height + y_offset - box_state.content_height - box_state.margin_bottom);
    } else {
        float y_offset = box_state.margin_box_top();
        used_offset.set_y(y_offset);
    }

    box_state.offset = used_offset;

    if (independent_formatting_context)
        independent_formatting_context->parent_context_did_dimension_child_root_box();
}

void FormattingContext::compute_height_for_absolutely_positioned_replaced_element(ReplacedBox const& box)
{
    // 10.6.5 Absolutely positioned, replaced elements
    // The used value of 'height' is determined as for inline replaced elements.
    m_state.get_mutable(box).content_height = compute_height_for_replaced_element(m_state, box);
}

void FormattingContext::compute_position(Box const& box)
{
    // 9.4.3 Relative positioning
    // Once a box has been laid out according to the normal flow or floated, it may be shifted relative to this position.

    if (box.computed_values().position() != CSS::Position::Relative)
        return;

    auto& box_state = m_state.get_mutable(box);
    auto const& computed_values = box.computed_values();
    float width_of_containing_block = m_state.get(*box.containing_block()).content_width;
    auto width_of_containing_block_as_length = CSS::Length::make_px(width_of_containing_block);

    auto specified_left = computed_values.offset().left.resolved(box, width_of_containing_block_as_length).resolved(box);
    auto specified_right = computed_values.offset().right.resolved(box, width_of_containing_block_as_length).resolved(box);

    if (specified_left.is_auto() && specified_right.is_auto()) {
        // If both 'left' and 'right' are 'auto' (their initial values), the used values are '0' (i.e., the boxes stay in their original position).
        box_state.offset_left = 0;
        box_state.offset_right = 0;
    } else if (specified_left.is_auto()) {
        // If 'left' is 'auto', its used value is minus the value of 'right' (i.e., the boxes move to the left by the value of 'right').
        box_state.offset_right = specified_right.to_px(box);
        box_state.offset_left = 0 - box_state.offset_right;
    } else if (specified_right.is_auto()) {
        // If 'right' is specified as 'auto', its used value is minus the value of 'left'.
        box_state.offset_left = specified_left.to_px(box);
        box_state.offset_right = 0 - box_state.offset_left;
    } else {
        // If neither 'left' nor 'right' is 'auto', the position is over-constrained, and one of them has to be ignored.
        // If the 'direction' property of the containing block is 'ltr', the value of 'left' wins and 'right' becomes -'left'.
        // If 'direction' of the containing block is 'rtl', 'right' wins and 'left' is ignored.
        // FIXME: Check direction (assuming 'ltr' for now).
        box_state.offset_left = specified_left.to_px(box);
        box_state.offset_right = 0 - box_state.offset_left;
    }
}

}
