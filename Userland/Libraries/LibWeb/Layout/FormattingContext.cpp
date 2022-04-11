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

FormattingContext::~FormattingContext() = default;

bool FormattingContext::creates_block_formatting_context(Box const& box)
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

OwnPtr<FormattingContext> FormattingContext::create_independent_formatting_context_if_needed(FormattingState& state, Box const& child_box)
{
    if (child_box.is_replaced_box() && !child_box.can_have_children()) {
        // NOTE: This is a bit strange.
        //       Basically, we create a pretend formatting context for replaced elements that does nothing.
        //       This allows other formatting contexts to treat them like elements that actually need inside layout
        //       without having separate code to handle replaced elements.
        // FIXME: Find a better abstraction for this.
        struct ReplacedFormattingContext : public FormattingContext {
            ReplacedFormattingContext(FormattingState& state, Box const& box)
                : FormattingContext(Type::Block, state, box)
            {
            }
            virtual void run(Box const&, LayoutMode) override { }
        };
        return make<ReplacedFormattingContext>(state, child_box);
    }

    if (!child_box.can_have_children())
        return {};

    auto child_display = child_box.computed_values().display();

    if (is<SVGSVGBox>(child_box))
        return make<SVGFormattingContext>(state, child_box, this);

    if (child_display.is_flex_inside())
        return make<FlexFormattingContext>(state, child_box, this);

    if (creates_block_formatting_context(child_box))
        return make<BlockFormattingContext>(state, verify_cast<BlockContainer>(child_box), this);

    if (child_display.is_table_inside())
        return make<TableFormattingContext>(state, verify_cast<TableBox>(child_box), this);

    VERIFY(is_block_formatting_context());
    if (child_box.children_are_inline())
        return make<InlineFormattingContext>(state, verify_cast<BlockContainer>(child_box), static_cast<BlockFormattingContext&>(*this));

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
        return make<DummyFormattingContext>(state, child_box);
    }
    VERIFY(child_box.is_block_container());
    VERIFY(child_display.is_flow_inside());
    return {};
}

OwnPtr<FormattingContext> FormattingContext::layout_inside(Box const& child_box, LayoutMode layout_mode)
{
    if (!child_box.can_have_children())
        return {};

    auto independent_formatting_context = create_independent_formatting_context_if_needed(m_state, child_box);
    if (independent_formatting_context)
        independent_formatting_context->run(child_box, layout_mode);
    else
        run(child_box, layout_mode);

    return independent_formatting_context;
}

float FormattingContext::greatest_child_width(Box const& box)
{
    float max_width = 0;
    if (box.children_are_inline()) {
        for (auto& line_box : m_state.get(verify_cast<BlockContainer>(box)).line_boxes) {
            max_width = max(max_width, line_box.width());
        }
    } else {
        box.for_each_child_of_type<Box>([&](auto& child) {
            max_width = max(max_width, m_state.get(child).border_box_width());
        });
    }
    return max_width;
}

FormattingContext::ShrinkToFitResult FormattingContext::calculate_shrink_to_fit_widths(Box const& box)
{
    auto [min_content, max_content] = calculate_intrinsic_sizes(box);
    return {
        .preferred_width = max_content.width(),
        .preferred_minimum_width = min_content.width(),
    };
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

float FormattingContext::compute_auto_height_for_block_level_element(FormattingState const& state, Box const& box)
{
    if (creates_block_formatting_context(box))
        return compute_auto_height_for_block_formatting_context_root(state, verify_cast<BlockContainer>(box));

    auto const& box_state = state.get(box);

    auto display = box.computed_values().display();
    if (display.is_flex_inside())
        return box_state.content_height;

    // https://www.w3.org/TR/CSS22/visudet.html#normal-block
    // 10.6.3 Block-level non-replaced elements in normal flow when 'overflow' computes to 'visible'

    // The element's height is the distance from its top content edge to the first applicable of the following:

    // 1. the bottom edge of the last line box, if the box establishes a inline formatting context with one or more lines
    if (box.children_are_inline() && !box_state.line_boxes.is_empty())
        return box_state.line_boxes.last().bottom();

    // 2. the bottom edge of the bottom (possibly collapsed) margin of its last in-flow child, if the child's bottom margin does not collapse with the element's bottom margin
    // FIXME: 3. the bottom border edge of the last in-flow child whose top margin doesn't collapse with the element's bottom margin
    if (!box.children_are_inline()) {
        for (auto* child_box = box.last_child_of_type<Box>(); child_box; child_box = child_box->previous_sibling_of_type<Box>()) {
            if (child_box->is_absolutely_positioned() || child_box->is_floating())
                continue;

            // FIXME: This is hack. If the last child is a list-item marker box, we ignore it for purposes of height calculation.
            //        Perhaps markers should not be considered in-flow(?) Perhaps they should always be the first child of the list-item
            //        box instead of the last child.
            if (child_box->is_list_item_marker_box())
                continue;

            auto const& child_box_state = state.get(*child_box);

            // Ignore anonymous block containers with no lines. These don't count as in-flow block boxes.
            if (child_box->is_anonymous() && child_box->is_block_container() && child_box_state.line_boxes.is_empty())
                continue;

            // FIXME: Handle margin collapsing.
            return max(0, child_box_state.offset.y() + child_box_state.content_height + child_box_state.margin_box_bottom());
        }
    }

    // 4. zero, otherwise
    return 0;
}

// https://www.w3.org/TR/CSS22/visudet.html#root-height
float FormattingContext::compute_auto_height_for_block_formatting_context_root(FormattingState const& state, BlockContainer const& root)
{
    // 10.6.7 'Auto' heights for block formatting context roots
    Optional<float> top;
    Optional<float> bottom;

    if (root.children_are_inline()) {
        // If it only has inline-level children, the height is the distance between
        // the top content edge and the bottom of the bottommost line box.
        auto const& line_boxes = state.get(root).line_boxes;
        top = 0;
        if (!line_boxes.is_empty())
            bottom = line_boxes.last().bottom();
    } else {
        // If it has block-level children, the height is the distance between
        // the top margin-edge of the topmost block-level child box
        // and the bottom margin-edge of the bottommost block-level child box.
        root.for_each_child_of_type<Box>([&](Layout::Box& child_box) {
            // Absolutely positioned children are ignored,
            // and relatively positioned boxes are considered without their offset.
            // Note that the child box may be an anonymous block box.
            if (child_box.is_absolutely_positioned())
                return IterationDecision::Continue;

            // FIXME: This doesn't look right.
            if ((root.computed_values().overflow_y() == CSS::Overflow::Visible) && child_box.is_floating())
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
    }

    // In addition, if the element has any floating descendants
    // whose bottom margin edge is below the element's bottom content edge,
    // then the height is increased to include those edges.
    root.for_each_child_of_type<Box>([&](Layout::Box& child_box) {
        if (!child_box.is_floating())
            return IterationDecision::Continue;

        auto const& child_box_state = state.get(child_box);
        float child_box_bottom = child_box_state.offset.y() + child_box_state.content_height + child_box_state.margin_box_bottom();

        if (!bottom.has_value() || child_box_bottom > bottom.value())
            bottom = child_box_bottom;

        return IterationDecision::Continue;
    });

    return max(0, bottom.value_or(0) - top.value_or(0));
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
    auto const border_left = computed_values.border_left().width;
    auto const border_right = computed_values.border_right().width;
    auto const padding_left = computed_values.padding().left.resolved(box, width_of_containing_block).to_px(box);
    auto const padding_right = computed_values.padding().right.resolved(box, width_of_containing_block).to_px(box);

    auto try_compute_width = [&](auto const& a_width) {
        margin_left = computed_values.margin().left.resolved(box, width_of_containing_block).resolved(box);
        margin_right = computed_values.margin().right.resolved(box, width_of_containing_block).resolved(box);

        auto left = computed_values.inset().left.resolved(box, width_of_containing_block).resolved(box);
        auto right = computed_values.inset().right.resolved(box, width_of_containing_block).resolved(box);
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

// https://www.w3.org/TR/CSS22/visudet.html#abs-non-replaced-height
void FormattingContext::compute_height_for_absolutely_positioned_non_replaced_element(Box const& box)
{
    // 10.6.4 Absolutely positioned, non-replaced elements

    // FIXME: The section below is partly on-spec, partly ad-hoc.
    auto& computed_values = box.computed_values();
    auto const& containing_block = *box.containing_block();
    auto const& containing_block_state = m_state.get(containing_block);
    auto& box_state = m_state.get_mutable(box);
    auto width_of_containing_block = CSS::Length::make_px(containing_block_state.content_width);
    auto height_of_containing_block = CSS::Length::make_px(containing_block_state.content_height);

    CSS::Length specified_top = computed_values.inset().top.resolved(box, height_of_containing_block).resolved(box);
    CSS::Length specified_bottom = computed_values.inset().bottom.resolved(box, height_of_containing_block).resolved(box);
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

    if (specified_height.is_auto() && specified_top.is_auto() && specified_bottom.is_auto()) {
        specified_height = CSS::Length(compute_auto_height_for_block_level_element(m_state, box), CSS::Length::Type::Px);
    }

    else if (specified_height.is_auto() && !specified_top.is_auto() && specified_bottom.is_auto()) {
        specified_height = CSS::Length(compute_auto_height_for_block_level_element(m_state, box), CSS::Length::Type::Px);
        box_state.inset_bottom = containing_block_state.content_height - specified_height.to_px(box) - specified_top.to_px(box) - box_state.margin_top - box_state.padding_top - box_state.border_top - box_state.margin_bottom - box_state.padding_bottom - box_state.border_bottom;
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
    auto independent_formatting_context = layout_inside(box, LayoutMode::Normal);
    compute_height_for_absolutely_positioned_element(box);

    box_state.margin_left = box.computed_values().margin().left.resolved(box, width_of_containing_block).to_px(box);
    box_state.margin_top = box.computed_values().margin().top.resolved(box, height_of_containing_block).to_px(box);
    box_state.margin_right = box.computed_values().margin().right.resolved(box, width_of_containing_block).to_px(box);
    box_state.margin_bottom = box.computed_values().margin().bottom.resolved(box, height_of_containing_block).to_px(box);

    box_state.border_left = box.computed_values().border_left().width;
    box_state.border_right = box.computed_values().border_right().width;
    box_state.border_top = box.computed_values().border_top().width;
    box_state.border_bottom = box.computed_values().border_bottom().width;

    box_state.inset_left = box.computed_values().inset().left.resolved(box, width_of_containing_block).to_px(box);
    box_state.inset_top = box.computed_values().inset().top.resolved(box, height_of_containing_block).to_px(box);
    box_state.inset_right = box.computed_values().inset().right.resolved(box, width_of_containing_block).to_px(box);
    box_state.inset_bottom = box.computed_values().inset().bottom.resolved(box, height_of_containing_block).to_px(box);

    auto is_auto = [](auto const& length_percentage) {
        return length_percentage.is_length() && length_percentage.length().is_auto();
    };

    if (is_auto(box.computed_values().inset().left) && specified_width.is_auto() && is_auto(box.computed_values().inset().right)) {
        if (is_auto(box.computed_values().margin().left))
            box_state.margin_left = 0;
        if (is_auto(box.computed_values().margin().right))
            box_state.margin_right = 0;
    }

    Gfx::FloatPoint used_offset;

    if (!is_auto(box.computed_values().inset().left)) {
        float x_offset = box_state.inset_left
            + box_state.border_box_left();
        used_offset.set_x(x_offset + box_state.margin_left);
    } else if (!is_auto(box.computed_values().inset().right)) {
        float x_offset = 0
            - box_state.inset_right
            - box_state.border_box_right();
        used_offset.set_x(containing_block_state.content_width + x_offset - box_state.content_width - box_state.margin_right);
    } else {
        float x_offset = box_state.margin_box_left();
        used_offset.set_x(x_offset);
    }

    if (!is_auto(box.computed_values().inset().top)) {
        float y_offset = box_state.inset_top
            + box_state.border_box_top();
        used_offset.set_y(y_offset + box_state.margin_top);
    } else if (!is_auto(box.computed_values().inset().bottom)) {
        float y_offset = 0
            - box_state.inset_bottom
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

// https://www.w3.org/TR/css-position-3/#relpos-insets
void FormattingContext::compute_inset(Box const& box)
{
    if (box.computed_values().position() != CSS::Position::Relative)
        return;

    auto resolve_two_opposing_insets = [&](CSS::LengthPercentage const& computed_start, CSS::LengthPercentage const& computed_end, float& used_start, float& used_end, float reference_for_percentage) {
        auto resolved_first = computed_start.resolved(box, CSS::Length::make_px(reference_for_percentage)).resolved(box);
        auto resolved_second = computed_end.resolved(box, CSS::Length::make_px(reference_for_percentage)).resolved(box);

        if (resolved_first.is_auto() && resolved_second.is_auto()) {
            // If opposing inset properties in an axis both compute to auto (their initial values),
            // their used values are zero (i.e., the boxes stay in their original position in that axis).
            used_start = 0;
            used_end = 0;
        } else if (resolved_first.is_auto() || resolved_second.is_auto()) {
            // If only one is auto, its used value becomes the negation of the other, and the box is shifted by the specified amount.
            if (resolved_first.is_auto()) {
                used_end = resolved_second.to_px(box);
                used_start = 0 - used_end;
            } else {
                used_start = resolved_first.to_px(box);
                used_end = 0 - used_start;
            }
        } else {
            // If neither is auto, the position is over-constrained; (with respect to the writing mode of its containing block)
            // the computed end side value is ignored, and its used value becomes the negation of the start side.
            used_start = resolved_first.to_px(box);
            used_end = 0 - used_start;
        }
    };

    auto& box_state = m_state.get_mutable(box);
    auto const& computed_values = box.computed_values();
    auto const& containing_block_state = m_state.get(*box.containing_block());

    // FIXME: Respect the containing block's writing-mode.
    resolve_two_opposing_insets(computed_values.inset().left, computed_values.inset().right, box_state.inset_left, box_state.inset_right, containing_block_state.content_width);
    resolve_two_opposing_insets(computed_values.inset().top, computed_values.inset().bottom, box_state.inset_top, box_state.inset_bottom, containing_block_state.content_height);
}

FormattingState::IntrinsicSizes FormattingContext::calculate_intrinsic_sizes(Layout::Box const& box) const
{
    // FIXME: This should handle replaced elements with "native" intrinsic size properly!

    if (box.has_intrinsic_width() && box.has_intrinsic_height()) {
        auto const& replaced_box = static_cast<ReplacedBox const&>(box);
        Gfx::FloatSize size { replaced_box.intrinsic_width().value_or(0), replaced_box.intrinsic_height().value_or(0) };
        return FormattingState::IntrinsicSizes {
            .min_content_size = size,
            .max_content_size = size,
        };
    }

    auto& root_state = m_state.m_root;

    // If we have cached intrinsic sizes for this box, use them.
    auto it = root_state.intrinsic_sizes.find(&box);
    if (it != root_state.intrinsic_sizes.end())
        return it->value;

    // Nothing cached, perform two throwaway layouts to determine the intrinsic sizes.

    FormattingState::IntrinsicSizes cached_box_sizes;
    auto const& containing_block = *box.containing_block();
    {
        FormattingState throwaway_state(&m_state);
        auto& containing_block_state = throwaway_state.get_mutable(containing_block);
        containing_block_state.content_width = INFINITY;
        containing_block_state.content_height = INFINITY;
        auto independent_formatting_context = const_cast<FormattingContext*>(this)->create_independent_formatting_context_if_needed(throwaway_state, box);
        VERIFY(independent_formatting_context);

        independent_formatting_context->run(box, LayoutMode::MaxContent);

        if (independent_formatting_context->type() == FormattingContext::Type::Flex) {
            auto const& box_state = throwaway_state.get(box);
            cached_box_sizes.max_content_size = { box_state.content_width, box_state.content_height };
        } else {
            cached_box_sizes.max_content_size.set_width(independent_formatting_context->greatest_child_width(box));
            cached_box_sizes.max_content_size.set_height(calculate_auto_height(throwaway_state, box));
        }
    }

    {
        FormattingState throwaway_state(&m_state);
        auto& containing_block_state = throwaway_state.get_mutable(containing_block);
        containing_block_state.content_width = 0;
        containing_block_state.content_height = 0;
        auto independent_formatting_context = const_cast<FormattingContext*>(this)->create_independent_formatting_context_if_needed(throwaway_state, box);
        VERIFY(independent_formatting_context);
        independent_formatting_context->run(box, LayoutMode::MinContent);
        if (independent_formatting_context->type() == FormattingContext::Type::Flex) {
            auto const& box_state = throwaway_state.get(box);
            cached_box_sizes.min_content_size = { box_state.content_width, box_state.content_height };
        } else {
            cached_box_sizes.min_content_size.set_width(independent_formatting_context->greatest_child_width(box));
            cached_box_sizes.min_content_size.set_height(calculate_auto_height(throwaway_state, box));
        }
    }

    if (cached_box_sizes.min_content_size.width() > cached_box_sizes.max_content_size.width()) {
        float tmp = cached_box_sizes.min_content_size.width();
        cached_box_sizes.min_content_size.set_width(cached_box_sizes.max_content_size.width());
        cached_box_sizes.max_content_size.set_width(tmp);
    }

    if (cached_box_sizes.min_content_size.height() > cached_box_sizes.max_content_size.height()) {
        float tmp = cached_box_sizes.min_content_size.height();
        cached_box_sizes.min_content_size.set_height(cached_box_sizes.max_content_size.height());
        cached_box_sizes.max_content_size.set_height(tmp);
    }

    root_state.intrinsic_sizes.set(&box, cached_box_sizes);
    return cached_box_sizes;
}

FormattingContext::MinAndMaxContentSize FormattingContext::calculate_min_and_max_content_width(Layout::Box const& box) const
{
    auto const& sizes = calculate_intrinsic_sizes(box);
    return { sizes.min_content_size.width(), sizes.max_content_size.width() };
}

FormattingContext::MinAndMaxContentSize FormattingContext::calculate_min_and_max_content_height(Layout::Box const& box) const
{
    auto const& sizes = calculate_intrinsic_sizes(box);
    return { sizes.min_content_size.height(), sizes.max_content_size.height() };
}

float FormattingContext::calculate_fit_content_size(float min_content_size, float max_content_size, Optional<float> available_space) const
{
    // If the available space in a given axis is definite, equal to clamp(min-content size, stretch-fit size, max-content size)
    // (i.e. max(min-content size, min(max-content size, stretch-fit size))).
    if (available_space.has_value()) {
        // FIXME: Compute the real stretch-fit size.
        auto stretch_fit_size = *available_space;
        auto s = max(min_content_size, min(max_content_size, stretch_fit_size));
        return s;
    }

    // FIXME: When sizing under a min-content constraint, equal to the min-content size.

    // Otherwise, equal to the max-content size in that axis.
    return max_content_size;
}

float FormattingContext::calculate_fit_content_width(Layout::Box const& box, Optional<float> available_space) const
{
    auto [min_content_size, max_content_size] = calculate_min_and_max_content_width(box);
    return calculate_fit_content_size(min_content_size, max_content_size, available_space);
}

float FormattingContext::calculate_fit_content_height(Layout::Box const& box, Optional<float> available_space) const
{
    auto [min_content_size, max_content_size] = calculate_min_and_max_content_height(box);
    return calculate_fit_content_size(min_content_size, max_content_size, available_space);
}

float FormattingContext::calculate_auto_height(FormattingState const& state, Box const& box)
{
    if (is<ReplacedBox>(box)) {
        return compute_height_for_replaced_element(state, verify_cast<ReplacedBox>(box));
    }

    return compute_auto_height_for_block_level_element(state, box);
}
}
