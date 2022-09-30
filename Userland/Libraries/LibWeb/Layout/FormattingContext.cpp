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
#include <LibWeb/Layout/GridFormattingContext.h>
#include <LibWeb/Layout/ReplacedBox.h>
#include <LibWeb/Layout/SVGFormattingContext.h>
#include <LibWeb/Layout/SVGSVGBox.h>
#include <LibWeb/Layout/TableBox.h>
#include <LibWeb/Layout/TableCellBox.h>
#include <LibWeb/Layout/TableFormattingContext.h>

namespace Web::Layout {

FormattingContext::FormattingContext(Type type, LayoutState& state, Box const& context_box, FormattingContext* parent)
    : m_type(type)
    , m_parent(parent)
    , m_context_box(context_box)
    , m_state(state)
{
}

FormattingContext::~FormattingContext() = default;

void FormattingContext::run_intrinsic_sizing(Box const& box)
{
    auto& box_state = m_state.get_mutable(box);

    auto to_available_size = [&](SizeConstraint constraint) {
        if (constraint == SizeConstraint::MinContent)
            return AvailableSize::make_min_content();
        if (constraint == SizeConstraint::MaxContent)
            return AvailableSize::make_max_content();
        return AvailableSize::make_indefinite();
    };

    auto available_width = to_available_size(box_state.width_constraint);
    auto available_height = to_available_size(box_state.height_constraint);

    run(box, LayoutMode::IntrinsicSizing, AvailableSpace(available_width, available_height));
}

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
    if (box.computed_values().display().is_flex_inside())
        return false;

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
        if (parent_display.is_grid_inside()) {
            if (!display.is_grid_inside()) {
                return true;
            }
        }
    }

    // FIXME: table-caption
    // FIXME: anonymous table cells
    // FIXME: Elements with contain: layout, content, or paint.
    // FIXME: multicol
    // FIXME: column-span: all
    return false;
}

OwnPtr<FormattingContext> FormattingContext::create_independent_formatting_context_if_needed(LayoutState& state, Box const& child_box)
{
    if (child_box.is_replaced_box() && !child_box.can_have_children()) {
        // NOTE: This is a bit strange.
        //       Basically, we create a pretend formatting context for replaced elements that does nothing.
        //       This allows other formatting contexts to treat them like elements that actually need inside layout
        //       without having separate code to handle replaced elements.
        // FIXME: Find a better abstraction for this.
        struct ReplacedFormattingContext : public FormattingContext {
            ReplacedFormattingContext(LayoutState& state, Box const& box)
                : FormattingContext(Type::Block, state, box)
            {
            }
            virtual float automatic_content_height() const override { return 0; };
            virtual void run(Box const&, LayoutMode, AvailableSpace const&) override { }
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

    if (child_display.is_grid_inside()) {
        return make<GridFormattingContext>(state, verify_cast<BlockContainer>(child_box), this);
    }

    VERIFY(is_block_formatting_context());
    VERIFY(!child_box.children_are_inline());

    // The child box is a block container that doesn't create its own BFC.
    // It will be formatted by this BFC.
    if (!child_display.is_flow_inside()) {
        dbgln("FIXME: Child box doesn't create BFC, but inside is also not flow! display={}", child_display.to_string());
        // HACK: Instead of crashing, create a dummy formatting context that does nothing.
        // FIXME: Remove this once it's no longer needed. It currently swallows problem with standalone
        //        table-related boxes that don't get fixed up by CSS anonymous table box generation.
        struct DummyFormattingContext : public FormattingContext {
            DummyFormattingContext(LayoutState& state, Box const& box)
                : FormattingContext(Type::Block, state, box)
            {
            }
            virtual float automatic_content_height() const override { return 0; };
            virtual void run(Box const&, LayoutMode, AvailableSpace const&) override { }
        };
        return make<DummyFormattingContext>(state, child_box);
    }
    VERIFY(child_box.is_block_container());
    VERIFY(child_display.is_flow_inside());
    return {};
}

OwnPtr<FormattingContext> FormattingContext::layout_inside(Box const& child_box, LayoutMode layout_mode, AvailableSpace const& available_space)
{
    {
        // OPTIMIZATION: If we're doing intrinsic sizing and `child_box` has definite size in both axes,
        //               we don't need to layout its insides. The size is resolvable without learning
        //               the metrics of whatever's inside the box.
        auto const& used_values = m_state.get(child_box);
        if (layout_mode == LayoutMode::IntrinsicSizing
            && used_values.width_constraint == SizeConstraint::None
            && used_values.height_constraint == SizeConstraint::None
            && used_values.has_definite_width()
            && used_values.has_definite_height()) {
            return nullptr;
        }
    }

    if (!child_box.can_have_children())
        return {};

    auto independent_formatting_context = create_independent_formatting_context_if_needed(m_state, child_box);
    if (independent_formatting_context)
        independent_formatting_context->run(child_box, layout_mode, available_space);
    else
        run(child_box, layout_mode, available_space);

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
        box.for_each_child_of_type<Box>([&](Box const& child) {
            if (!child.is_absolutely_positioned())
                max_width = max(max_width, m_state.get(child).border_box_width());
        });
    }
    return max_width;
}

FormattingContext::ShrinkToFitResult FormattingContext::calculate_shrink_to_fit_widths(Box const& box)
{
    return {
        .preferred_width = calculate_max_content_width(box),
        .preferred_minimum_width = calculate_min_content_width(box),
    };
}

static Gfx::FloatSize solve_replaced_size_constraint(LayoutState const& state, float w, float h, ReplacedBox const& box)
{
    // 10.4 Minimum and maximum widths: 'min-width' and 'max-width'

    auto const& containing_block = *box.containing_block();
    auto const& containing_block_state = state.get(containing_block);
    auto width_of_containing_block = CSS::Length::make_px(containing_block_state.content_width());
    auto height_of_containing_block = CSS::Length::make_px(containing_block_state.content_height());

    auto specified_min_width = box.computed_values().min_width().is_auto() ? 0 : box.computed_values().min_width().resolved(box, width_of_containing_block).to_px(box);
    auto specified_max_width = box.computed_values().max_width().is_none() ? w : box.computed_values().max_width().resolved(box, width_of_containing_block).to_px(box);
    auto specified_min_height = box.computed_values().min_height().is_auto() ? 0 : box.computed_values().min_height().resolved(box, height_of_containing_block).to_px(box);
    auto specified_max_height = box.computed_values().max_height().is_none() ? h : box.computed_values().max_height().resolved(box, height_of_containing_block).to_px(box);

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

float FormattingContext::compute_auto_height_for_block_level_element(Box const& box) const
{
    if (creates_block_formatting_context(box))
        return compute_auto_height_for_block_formatting_context_root(verify_cast<BlockContainer>(box));

    auto const& box_state = m_state.get(box);

    auto display = box.computed_values().display();
    if (display.is_flex_inside()) {
        // https://drafts.csswg.org/css-flexbox-1/#algo-main-container
        // NOTE: The automatic block size of a block-level flex container is its max-content size.
        return calculate_max_content_height(box);
    }

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

            auto const& child_box_state = m_state.get(*child_box);

            // Ignore anonymous block containers with no lines. These don't count as in-flow block boxes.
            if (child_box->is_anonymous() && child_box->is_block_container() && child_box_state.line_boxes.is_empty())
                continue;

            // FIXME: Handle margin collapsing.
            return max(0.0f, child_box_state.offset.y() + child_box_state.content_height() + child_box_state.margin_box_bottom());
        }
    }

    // 4. zero, otherwise
    return 0;
}

// https://www.w3.org/TR/CSS22/visudet.html#root-height
float FormattingContext::compute_auto_height_for_block_formatting_context_root(BlockContainer const& root) const
{
    // 10.6.7 'Auto' heights for block formatting context roots
    Optional<float> top;
    Optional<float> bottom;

    if (root.children_are_inline()) {
        // If it only has inline-level children, the height is the distance between
        // the top content edge and the bottom of the bottommost line box.
        auto const& line_boxes = m_state.get(root).line_boxes;
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

            auto const& child_box_state = m_state.get(child_box);

            float child_box_top = child_box_state.offset.y() - child_box_state.margin_box_top();
            float child_box_bottom = child_box_state.offset.y() + child_box_state.content_height() + child_box_state.margin_box_bottom();

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
    for (auto* floating_box : m_state.get(root).floating_descendants()) {
        // NOTE: Floating box coordinates are relative to their own containing block,
        //       which may or may not be the BFC root.
        auto margin_box = margin_box_rect_in_ancestor_coordinate_space(*floating_box, root, m_state);
        float floating_box_bottom_margin_edge = margin_box.bottom() + 1;
        if (!bottom.has_value() || floating_box_bottom_margin_edge > bottom.value())
            bottom = floating_box_bottom_margin_edge;
    }

    return max(0.0f, bottom.value_or(0) - top.value_or(0));
}

// 10.3.2 Inline, replaced elements, https://www.w3.org/TR/CSS22/visudet.html#inline-replaced-width
float FormattingContext::tentative_width_for_replaced_element(LayoutState const& state, ReplacedBox const& box, CSS::Size const& computed_width, AvailableSpace const& available_space)
{
    // Treat percentages of indefinite containing block widths as 0 (the initial width).
    if (computed_width.is_percentage() && !state.get(*box.containing_block()).has_definite_width())
        return 0;

    auto height_of_containing_block = CSS::Length::make_px(containing_block_height_for(box, state));
    auto const& computed_height = box.computed_values().height();

    float used_width = computed_width.resolved(box, CSS::Length::make_px(available_space.width.to_px())).to_px(box);

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
        || (computed_width.is_auto() && !computed_height.is_auto() && box.has_intrinsic_aspect_ratio())) {
        return compute_height_for_replaced_element(state, box, available_space) * box.intrinsic_aspect_ratio().value();
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

void FormattingContext::compute_width_for_absolutely_positioned_element(Box const& box, AvailableSpace const& available_space)
{
    if (is<ReplacedBox>(box))
        compute_width_for_absolutely_positioned_replaced_element(verify_cast<ReplacedBox>(box), available_space);
    else
        compute_width_for_absolutely_positioned_non_replaced_element(box, available_space);
}

void FormattingContext::compute_height_for_absolutely_positioned_element(Box const& box, AvailableSpace const& available_space)
{
    if (is<ReplacedBox>(box))
        compute_height_for_absolutely_positioned_replaced_element(static_cast<ReplacedBox const&>(box), available_space);
    else
        compute_height_for_absolutely_positioned_non_replaced_element(box, available_space);
}

float FormattingContext::compute_width_for_replaced_element(LayoutState const& state, ReplacedBox const& box, AvailableSpace const& available_space)
{
    // 10.3.4 Block-level, replaced elements in normal flow...
    // 10.3.2 Inline, replaced elements

    auto zero_value = CSS::Length::make_px(0);
    auto width_of_containing_block_as_length = CSS::Length::make_px(available_space.width.to_px());

    auto margin_left = box.computed_values().margin().left().resolved(box, width_of_containing_block_as_length).resolved(box);
    auto margin_right = box.computed_values().margin().right().resolved(box, width_of_containing_block_as_length).resolved(box);

    // A computed value of 'auto' for 'margin-left' or 'margin-right' becomes a used value of '0'.
    if (margin_left.is_auto())
        margin_left = zero_value;
    if (margin_right.is_auto())
        margin_right = zero_value;

    auto computed_width = box.computed_values().width();

    // 1. The tentative used width is calculated (without 'min-width' and 'max-width')
    auto used_width = tentative_width_for_replaced_element(state, box, computed_width, available_space);

    // 2. The tentative used width is greater than 'max-width', the rules above are applied again,
    //    but this time using the computed value of 'max-width' as the computed value for 'width'.
    auto computed_max_width = box.computed_values().max_width();
    if (!computed_max_width.is_none()) {
        if (used_width > computed_max_width.resolved(box, width_of_containing_block_as_length).to_px(box)) {
            used_width = tentative_width_for_replaced_element(state, box, computed_max_width, available_space);
        }
    }

    // 3. If the resulting width is smaller than 'min-width', the rules above are applied again,
    //    but this time using the value of 'min-width' as the computed value for 'width'.
    auto computed_min_width = box.computed_values().min_width();
    if (!computed_min_width.is_auto()) {
        if (used_width < computed_min_width.resolved(box, width_of_containing_block_as_length).to_px(box)) {
            used_width = tentative_width_for_replaced_element(state, box, computed_min_width, available_space);
        }
    }

    return used_width;
}

// 10.6.2 Inline replaced elements, block-level replaced elements in normal flow, 'inline-block' replaced elements in normal flow and floating replaced elements
// https://www.w3.org/TR/CSS22/visudet.html#inline-replaced-height
float FormattingContext::tentative_height_for_replaced_element(LayoutState const& state, ReplacedBox const& box, CSS::Size const& computed_height, AvailableSpace const& available_space)
{
    // Treat percentages of indefinite containing block heights as 0 (the initial height).
    if (computed_height.is_percentage() && !state.get(*box.containing_block()).has_definite_height())
        return 0;

    auto const& computed_width = box.computed_values().width();

    // If 'height' and 'width' both have computed values of 'auto' and the element also has
    // an intrinsic height, then that intrinsic height is the used value of 'height'.
    if (computed_width.is_auto() && computed_height.is_auto() && box.has_intrinsic_height())
        return box.intrinsic_height().value();

    // Otherwise, if 'height' has a computed value of 'auto', and the element has an intrinsic ratio then the used value of 'height' is:
    //
    //     (used width) / (intrinsic ratio)
    if (computed_height.is_auto() && box.has_intrinsic_aspect_ratio())
        return compute_width_for_replaced_element(state, box, available_space) / box.intrinsic_aspect_ratio().value();

    // Otherwise, if 'height' has a computed value of 'auto', and the element has an intrinsic height, then that intrinsic height is the used value of 'height'.
    if (computed_height.is_auto() && box.has_intrinsic_height())
        return box.intrinsic_height().value();

    // Otherwise, if 'height' has a computed value of 'auto', but none of the conditions above are met,
    // then the used value of 'height' must be set to the height of the largest rectangle that has a 2:1 ratio, has a height not greater than 150px,
    // and has a width not greater than the device width.
    if (computed_height.is_auto())
        return 150;

    return computed_height.resolved(box, CSS::Length::make_px(available_space.height.to_px())).to_px(box);
}

float FormattingContext::compute_height_for_replaced_element(LayoutState const& state, ReplacedBox const& box, AvailableSpace const& available_space)
{
    // 10.6.2 Inline replaced elements, block-level replaced elements in normal flow,
    // 'inline-block' replaced elements in normal flow and floating replaced elements

    auto width_of_containing_block_as_length = CSS::Length::make_px(available_space.width.to_px());
    auto height_of_containing_block_as_length = CSS::Length::make_px(available_space.height.to_px());
    auto computed_width = box.computed_values().width();
    auto computed_height = box.computed_values().height();

    float used_height = tentative_height_for_replaced_element(state, box, computed_height, available_space);

    if (computed_width.is_auto() && computed_height.is_auto() && box.has_intrinsic_aspect_ratio()) {
        float w = tentative_width_for_replaced_element(state, box, computed_width, available_space);
        float h = used_height;
        used_height = solve_replaced_size_constraint(state, w, h, box).height();
    }

    return used_height;
}

void FormattingContext::compute_width_for_absolutely_positioned_non_replaced_element(Box const& box, AvailableSpace const& available_space)
{
    auto width_of_containing_block = available_space.width.to_px();
    auto width_of_containing_block_as_length = CSS::Length::make_px(width_of_containing_block);
    auto& computed_values = box.computed_values();
    auto zero_value = CSS::Length::make_px(0);

    auto margin_left = CSS::Length::make_auto();
    auto margin_right = CSS::Length::make_auto();
    auto const border_left = computed_values.border_left().width;
    auto const border_right = computed_values.border_right().width;
    auto const padding_left = computed_values.padding().left().resolved(box, width_of_containing_block_as_length).to_px(box);
    auto const padding_right = computed_values.padding().right().resolved(box, width_of_containing_block_as_length).to_px(box);

    auto try_compute_width = [&](auto const& a_width) {
        margin_left = computed_values.margin().left().resolved(box, width_of_containing_block_as_length).resolved(box);
        margin_right = computed_values.margin().right().resolved(box, width_of_containing_block_as_length).resolved(box);

        auto left = computed_values.inset().left().resolved(box, width_of_containing_block_as_length).resolved(box);
        auto right = computed_values.inset().right().resolved(box, width_of_containing_block_as_length).resolved(box);
        auto width = a_width;

        auto solve_for_left = [&] {
            return CSS::Length(width_of_containing_block - margin_left.to_px(box) - border_left - padding_left - width.to_px(box) - padding_right - border_right - margin_right.to_px(box) - right.to_px(box), CSS::Length::Type::Px);
        };

        auto solve_for_width = [&] {
            return CSS::Length(width_of_containing_block - left.to_px(box) - margin_left.to_px(box) - border_left - padding_left - padding_right - border_right - margin_right.to_px(box) - right.to_px(box), CSS::Length::Type::Px);
        };

        auto solve_for_right = [&] {
            return CSS::Length(width_of_containing_block - left.to_px(box) - margin_left.to_px(box) - border_left - padding_left - width.to_px(box) - padding_right - border_right - margin_right.to_px(box), CSS::Length::Type::Px);
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

    auto specified_width = computed_values.width().resolved(box, width_of_containing_block_as_length).resolved(box);

    // 1. The tentative used width is calculated (without 'min-width' and 'max-width')
    auto used_width = try_compute_width(specified_width);

    // 2. The tentative used width is greater than 'max-width', the rules above are applied again,
    //    but this time using the computed value of 'max-width' as the computed value for 'width'.
    if (!computed_values.max_width().is_none()) {
        auto max_width = computed_values.max_width().resolved(box, width_of_containing_block_as_length).resolved(box);
        if (used_width.to_px(box) > max_width.to_px(box)) {
            used_width = try_compute_width(max_width);
        }
    }

    // 3. If the resulting width is smaller than 'min-width', the rules above are applied again,
    //    but this time using the value of 'min-width' as the computed value for 'width'.
    if (!computed_values.min_width().is_auto()) {
        auto min_width = computed_values.min_width().resolved(box, width_of_containing_block_as_length).resolved(box);
        if (used_width.to_px(box) < min_width.to_px(box)) {
            used_width = try_compute_width(min_width);
        }
    }

    auto& box_state = m_state.get_mutable(box);
    box_state.set_content_width(used_width.to_px(box));

    box_state.margin_left = margin_left.to_px(box);
    box_state.margin_right = margin_right.to_px(box);
    box_state.border_left = border_left;
    box_state.border_right = border_right;
    box_state.padding_left = padding_left;
    box_state.padding_right = padding_right;
}

void FormattingContext::compute_width_for_absolutely_positioned_replaced_element(ReplacedBox const& box, AvailableSpace const& available_space)
{
    // 10.3.8 Absolutely positioned, replaced elements
    // The used value of 'width' is determined as for inline replaced elements.
    // FIXME: This const_cast is gross.
    const_cast<ReplacedBox&>(box).prepare_for_replaced_layout();
    m_state.get_mutable(box).set_content_width(compute_width_for_replaced_element(m_state, box, available_space));
}

// https://www.w3.org/TR/CSS22/visudet.html#abs-non-replaced-height
void FormattingContext::compute_height_for_absolutely_positioned_non_replaced_element(Box const& box, AvailableSpace const& available_space)
{
    // 10.6.4 Absolutely positioned, non-replaced elements

    // FIXME: The section below is partly on-spec, partly ad-hoc.
    auto& computed_values = box.computed_values();

    auto width_of_containing_block = containing_block_width_for(box);
    auto height_of_containing_block = available_space.height.to_px();
    auto width_of_containing_block_as_length = CSS::Length::make_px(width_of_containing_block);
    auto height_of_containing_block_as_length = CSS::Length::make_px(height_of_containing_block);

    auto const& computed_top = computed_values.inset().top();
    auto const& computed_bottom = computed_values.inset().bottom();
    auto const& computed_height = computed_values.height();
    auto const& computed_min_height = computed_values.min_height();
    auto const& computed_max_height = computed_values.max_height();

    auto used_top = computed_top.resolved(box, height_of_containing_block_as_length).resolved(box).to_px(box);
    auto used_bottom = computed_bottom.resolved(box, height_of_containing_block_as_length).resolved(box).to_px(box);
    auto tentative_height = CSS::Length::make_auto();

    if (!computed_height.is_auto())
        tentative_height = computed_values.height().resolved(box, height_of_containing_block_as_length).resolved(box);

    auto& box_state = m_state.get_mutable(box);
    box_state.margin_top = computed_values.margin().top().resolved(box, width_of_containing_block_as_length).to_px(box);
    box_state.margin_bottom = computed_values.margin().bottom().resolved(box, width_of_containing_block_as_length).to_px(box);
    box_state.border_top = computed_values.border_top().width;
    box_state.border_bottom = computed_values.border_bottom().width;
    box_state.padding_top = computed_values.padding().top().resolved(box, width_of_containing_block_as_length).to_px(box);
    box_state.padding_bottom = computed_values.padding().bottom().resolved(box, width_of_containing_block_as_length).to_px(box);

    if (computed_height.is_auto() && computed_top.is_auto() && computed_bottom.is_auto()) {
        tentative_height = CSS::Length(compute_auto_height_for_block_level_element(box), CSS::Length::Type::Px);
    }

    else if (computed_height.is_auto() && !computed_top.is_auto() && computed_bottom.is_auto()) {
        tentative_height = CSS::Length(compute_auto_height_for_block_level_element(box), CSS::Length::Type::Px);
        box_state.inset_bottom = height_of_containing_block - tentative_height.to_px(box) - used_top - box_state.margin_top - box_state.padding_top - box_state.border_top - box_state.margin_bottom - box_state.padding_bottom - box_state.border_bottom;
    }

    else if (computed_height.is_auto() && !computed_top.is_auto() && !computed_bottom.is_auto()) {
        tentative_height = CSS::Length(height_of_containing_block - used_top - box_state.margin_top - box_state.padding_top - box_state.border_top - used_bottom - box_state.margin_bottom - box_state.padding_bottom - box_state.border_bottom, CSS::Length::Type::Px);
    }

    float used_height = tentative_height.to_px(box);
    if (!computed_max_height.is_none())
        used_height = min(used_height, computed_max_height.resolved(box, height_of_containing_block_as_length).resolved(box).to_px(box));
    if (!computed_min_height.is_auto())
        used_height = max(used_height, computed_min_height.resolved(box, height_of_containing_block_as_length).resolved(box).to_px(box));

    box_state.set_content_height(used_height);
}

void FormattingContext::layout_absolutely_positioned_element(Box const& box, AvailableSpace const& available_space)
{
    auto& containing_block_state = m_state.get_mutable(*box.containing_block());
    auto& box_state = m_state.get_mutable(box);

    auto width_of_containing_block = available_space.width.to_px();
    auto height_of_containing_block = available_space.height.to_px();
    auto width_of_containing_block_as_length = CSS::Length::make_px(width_of_containing_block);
    auto height_of_containing_block_as_length = CSS::Length::make_px(height_of_containing_block);

    auto specified_width = box.computed_values().width().resolved(box, width_of_containing_block_as_length).resolved(box);

    compute_width_for_absolutely_positioned_element(box, available_space);
    auto independent_formatting_context = layout_inside(box, LayoutMode::Normal, box_state.available_inner_space_or_constraints_from(available_space));
    compute_height_for_absolutely_positioned_element(box, available_space);

    box_state.margin_left = box.computed_values().margin().left().resolved(box, width_of_containing_block_as_length).to_px(box);
    box_state.margin_top = box.computed_values().margin().top().resolved(box, width_of_containing_block_as_length).to_px(box);
    box_state.margin_right = box.computed_values().margin().right().resolved(box, width_of_containing_block_as_length).to_px(box);
    box_state.margin_bottom = box.computed_values().margin().bottom().resolved(box, width_of_containing_block_as_length).to_px(box);

    box_state.border_left = box.computed_values().border_left().width;
    box_state.border_right = box.computed_values().border_right().width;
    box_state.border_top = box.computed_values().border_top().width;
    box_state.border_bottom = box.computed_values().border_bottom().width;

    box_state.inset_left = box.computed_values().inset().left().resolved(box, width_of_containing_block_as_length).to_px(box);
    box_state.inset_top = box.computed_values().inset().top().resolved(box, height_of_containing_block_as_length).to_px(box);
    box_state.inset_right = box.computed_values().inset().right().resolved(box, width_of_containing_block_as_length).to_px(box);
    box_state.inset_bottom = box.computed_values().inset().bottom().resolved(box, height_of_containing_block_as_length).to_px(box);

    if (box.computed_values().inset().left().is_auto() && box.computed_values().width().is_auto() && box.computed_values().inset().right().is_auto()) {
        if (box.computed_values().margin().left().is_auto())
            box_state.margin_left = 0;
        if (box.computed_values().margin().right().is_auto())
            box_state.margin_right = 0;
    }

    Gfx::FloatPoint used_offset;

    auto* relevant_parent = box.first_ancestor_of_type<Layout::BlockContainer>();
    while (relevant_parent != nullptr) {
        if (!relevant_parent->is_absolutely_positioned() && !relevant_parent->is_floating()) {
            break;
        } else {
            relevant_parent = relevant_parent->first_ancestor_of_type<Layout::BlockContainer>();
        }
    }
    auto parent_location = absolute_content_rect(static_cast<Box const&>(*relevant_parent), m_state);

    if (!box.computed_values().inset().left().is_auto()) {
        float x_offset = box_state.inset_left
            + box_state.border_box_left();
        used_offset.set_x(x_offset + box_state.margin_left);
    } else if (!box.computed_values().inset().right().is_auto()) {
        float x_offset = 0
            - box_state.inset_right
            - box_state.border_box_right();
        used_offset.set_x(width_of_containing_block + x_offset - box_state.content_width() - box_state.margin_right);
    } else {
        float x_offset = box_state.margin_box_left()
            + (relevant_parent->computed_values().position() == CSS::Position::Relative ? 0 : parent_location.x());
        used_offset.set_x(x_offset);
    }

    if (!box.computed_values().inset().top().is_auto()) {
        float y_offset = box_state.inset_top
            + box_state.border_box_top();
        used_offset.set_y(y_offset + box_state.margin_top);
    } else if (!box.computed_values().inset().bottom().is_auto()) {
        float y_offset = 0
            - box_state.inset_bottom
            - box_state.border_box_bottom();
        used_offset.set_y(height_of_containing_block + y_offset - box_state.content_height() - box_state.margin_bottom);
    } else {
        float y_offset = box_state.margin_box_top()
            + compute_box_y_position_with_respect_to_siblings(box, box_state)
            + (relevant_parent->computed_values().position() == CSS::Position::Relative ? 0 : parent_location.y());
        used_offset.set_y(y_offset);
    }

    // NOTE: Absolutely positioned boxes are relative to the *padding edge* of the containing block.
    used_offset.translate_by(-containing_block_state.padding_left, -containing_block_state.padding_top);

    box_state.set_content_offset(used_offset);

    if (independent_formatting_context)
        independent_formatting_context->parent_context_did_dimension_child_root_box();
}

void FormattingContext::compute_height_for_absolutely_positioned_replaced_element(ReplacedBox const& box, AvailableSpace const& available_space)
{
    // 10.6.5 Absolutely positioned, replaced elements
    // The used value of 'height' is determined as for inline replaced elements.
    m_state.get_mutable(box).set_content_height(compute_height_for_replaced_element(m_state, box, available_space));
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

    // FIXME: Respect the containing block's writing-mode.
    resolve_two_opposing_insets(computed_values.inset().left(), computed_values.inset().right(), box_state.inset_left, box_state.inset_right, containing_block_width_for(box));
    resolve_two_opposing_insets(computed_values.inset().top(), computed_values.inset().bottom(), box_state.inset_top, box_state.inset_bottom, containing_block_height_for(box));
}

float FormattingContext::calculate_fit_content_size(float min_content_size, float max_content_size, AvailableSize const& available_size) const
{
    // If the available space in a given axis is definite, equal to clamp(min-content size, stretch-fit size, max-content size)
    // (i.e. max(min-content size, min(max-content size, stretch-fit size))).
    if (available_size.is_definite()) {
        // FIXME: Compute the real stretch-fit size.
        auto stretch_fit_size = available_size.to_px();
        auto s = max(min_content_size, min(max_content_size, stretch_fit_size));
        return s;
    }

    // When sizing under a min-content constraint, equal to the min-content size.
    if (available_size.is_min_content())
        return min_content_size;

    // Otherwise, equal to the max-content size in that axis.
    return max_content_size;
}

float FormattingContext::calculate_fit_content_width(Layout::Box const& box, AvailableSize const& available_size) const
{
    // When sizing under a min-content constraint, equal to the min-content size.
    // NOTE: We check this first, to avoid needlessly calculating the max-content size.
    if (available_size.is_min_content())
        return calculate_min_content_width(box);

    if (available_size.is_max_content())
        return calculate_max_content_width(box);

    return calculate_fit_content_size(calculate_min_content_width(box), calculate_max_content_width(box), available_size);
}

float FormattingContext::calculate_fit_content_height(Layout::Box const& box, AvailableSize const& available_size) const
{
    // When sizing under a min-content constraint, equal to the min-content size.
    // NOTE: We check this first, to avoid needlessly calculating the max-content size.
    if (available_size.is_min_content())
        return calculate_min_content_height(box);

    if (available_size.is_max_content())
        return calculate_max_content_height(box);

    return calculate_fit_content_size(calculate_min_content_height(box), calculate_max_content_height(box), available_size);
}

float FormattingContext::calculate_min_content_width(Layout::Box const& box) const
{
    if (box.has_intrinsic_width())
        return *box.intrinsic_width();

    auto& root_state = m_state.m_root;

    auto& cache = *root_state.intrinsic_sizes.ensure(&box, [] { return adopt_own(*new LayoutState::IntrinsicSizes); });
    if (cache.min_content_width.has_value())
        return *cache.min_content_width;

    LayoutState throwaway_state(&m_state);

    auto& box_state = throwaway_state.get_mutable(box);
    box_state.width_constraint = SizeConstraint::MinContent;

    auto context = const_cast<FormattingContext*>(this)->create_independent_formatting_context_if_needed(throwaway_state, box);
    VERIFY(context);
    context->run_intrinsic_sizing(box);
    if (context->type() == FormattingContext::Type::Flex) {
        cache.min_content_width = box_state.content_width();
    } else {
        cache.min_content_width = context->greatest_child_width(box);
    }

    if (!isfinite(*cache.min_content_width)) {
        // HACK: If layout calculates a non-finite result, something went wrong. Force it to zero and log a little whine.
        dbgln("FIXME: Calculated non-finite min-content width for {}", box.debug_description());
        cache.min_content_width = 0;
    }

    return *cache.min_content_width;
}

float FormattingContext::calculate_max_content_width(Layout::Box const& box) const
{
    if (box.has_intrinsic_width())
        return *box.intrinsic_width();

    auto& root_state = m_state.m_root;

    auto& cache = *root_state.intrinsic_sizes.ensure(&box, [] { return adopt_own(*new LayoutState::IntrinsicSizes); });
    if (cache.max_content_width.has_value())
        return *cache.max_content_width;

    LayoutState throwaway_state(&m_state);

    auto& box_state = throwaway_state.get_mutable(box);
    box_state.width_constraint = SizeConstraint::MaxContent;

    auto context = const_cast<FormattingContext*>(this)->create_independent_formatting_context_if_needed(throwaway_state, box);
    VERIFY(context);
    context->run_intrinsic_sizing(box);
    if (context->type() == FormattingContext::Type::Flex) {
        cache.max_content_width = box_state.content_width();
    } else {
        cache.max_content_width = context->greatest_child_width(box);
    }

    if (!isfinite(*cache.max_content_width)) {
        // HACK: If layout calculates a non-finite result, something went wrong. Force it to zero and log a little whine.
        dbgln("FIXME: Calculated non-finite max-content width for {}", box.debug_description());
        cache.max_content_width = 0;
    }

    return *cache.max_content_width;
}

float FormattingContext::calculate_min_content_height(Layout::Box const& box) const
{
    if (box.has_intrinsic_height())
        return *box.intrinsic_height();

    auto& root_state = m_state.m_root;

    auto& cache = *root_state.intrinsic_sizes.ensure(&box, [] { return adopt_own(*new LayoutState::IntrinsicSizes); });
    if (cache.min_content_height.has_value())
        return *cache.min_content_height;

    LayoutState throwaway_state(&m_state);

    auto& box_state = throwaway_state.get_mutable(box);
    box_state.height_constraint = SizeConstraint::MinContent;

    auto context = const_cast<FormattingContext*>(this)->create_independent_formatting_context_if_needed(throwaway_state, box);
    VERIFY(context);
    context->run_intrinsic_sizing(box);
    cache.min_content_height = context->automatic_content_height();

    if (!isfinite(*cache.min_content_height)) {
        // HACK: If layout calculates a non-finite result, something went wrong. Force it to zero and log a little whine.
        dbgln("FIXME: Calculated non-finite min-content height for {}", box.debug_description());
        cache.min_content_height = 0;
    }

    return *cache.min_content_height;
}

float FormattingContext::calculate_max_content_height(Layout::Box const& box) const
{
    if (box.has_intrinsic_height())
        return *box.intrinsic_height();

    auto& root_state = m_state.m_root;

    auto& cache = *root_state.intrinsic_sizes.ensure(&box, [] { return adopt_own(*new LayoutState::IntrinsicSizes); });
    if (cache.max_content_height.has_value())
        return *cache.max_content_height;

    LayoutState throwaway_state(&m_state);

    auto& box_state = throwaway_state.get_mutable(box);
    box_state.height_constraint = SizeConstraint::MaxContent;

    auto context = const_cast<FormattingContext*>(this)->create_independent_formatting_context_if_needed(throwaway_state, box);
    VERIFY(context);
    context->run_intrinsic_sizing(box);
    cache.max_content_height = context->automatic_content_height();

    if (!isfinite(*cache.max_content_height)) {
        // HACK: If layout calculates a non-finite result, something went wrong. Force it to zero and log a little whine.
        dbgln("FIXME: Calculated non-finite max-content height for {}", box.debug_description());
        cache.max_content_height = 0;
    }

    return *cache.max_content_height;
}

float FormattingContext::containing_block_width_for(Box const& box, LayoutState const& state)
{
    auto& containing_block_state = state.get(*box.containing_block());
    auto& box_state = state.get(box);

    switch (box_state.width_constraint) {
    case SizeConstraint::MinContent:
        return 0;
    case SizeConstraint::MaxContent:
        return INFINITY;
    case SizeConstraint::None:
        return containing_block_state.content_width();
    }
    VERIFY_NOT_REACHED();
}

float FormattingContext::containing_block_height_for(Box const& box, LayoutState const& state)
{
    auto& containing_block_state = state.get(*box.containing_block());
    auto& box_state = state.get(box);

    switch (box_state.height_constraint) {
    case SizeConstraint::MinContent:
        return 0;
    case SizeConstraint::MaxContent:
        return INFINITY;
    case SizeConstraint::None:
        return containing_block_state.content_height();
    }
    VERIFY_NOT_REACHED();
}

static Box const* previous_block_level_sibling(Box const& box)
{
    for (auto* sibling = box.previous_sibling_of_type<Box>(); sibling; sibling = sibling->previous_sibling_of_type<Box>()) {
        if (sibling->computed_values().display().is_block_outside())
            return sibling;
    }
    return nullptr;
}

float FormattingContext::compute_box_y_position_with_respect_to_siblings(Box const& child_box, LayoutState::UsedValues const& box_state)
{
    float y = box_state.border_box_top();

    Vector<float> collapsible_margins;

    auto* relevant_sibling = previous_block_level_sibling(child_box);
    while (relevant_sibling != nullptr) {
        if (!relevant_sibling->is_absolutely_positioned() && !relevant_sibling->is_floating()) {
            auto const& relevant_sibling_state = m_state.get(*relevant_sibling);
            collapsible_margins.append(relevant_sibling_state.margin_bottom);
            // NOTE: Empty (0-height) preceding siblings have their margins collapsed with *their* preceding sibling, etc.
            if (relevant_sibling_state.border_box_height() > 0)
                break;
            collapsible_margins.append(relevant_sibling_state.margin_top);
        }
        relevant_sibling = previous_block_level_sibling(*relevant_sibling);
    }

    if (relevant_sibling) {
        // Collapse top margin with the collapsed margin(s) of preceding siblings.
        collapsible_margins.append(box_state.margin_top);

        float smallest_margin = 0;
        float largest_margin = 0;
        size_t negative_margin_count = 0;
        for (auto margin : collapsible_margins) {
            if (margin < 0)
                ++negative_margin_count;
            largest_margin = max(largest_margin, margin);
            smallest_margin = min(smallest_margin, margin);
        }

        float collapsed_margin = 0;
        if (negative_margin_count == collapsible_margins.size()) {
            // When all margins are negative, the size of the collapsed margin is the smallest (most negative) margin.
            collapsed_margin = smallest_margin;
        } else if (negative_margin_count > 0) {
            // When negative margins are involved, the size of the collapsed margin is the sum of the largest positive margin and the smallest (most negative) negative margin.
            collapsed_margin = largest_margin + smallest_margin;
        } else {
            // Otherwise, collapse all the adjacent margins by using only the largest one.
            collapsed_margin = largest_margin;
        }

        auto const& relevant_sibling_state = m_state.get(*relevant_sibling);
        return y + relevant_sibling_state.offset.y()
            + relevant_sibling_state.content_height()
            + relevant_sibling_state.border_box_bottom()
            + collapsed_margin;
    } else {
        return y + box_state.margin_top;
    }
}

// https://drafts.csswg.org/css-sizing-3/#stretch-fit-size
float FormattingContext::calculate_stretch_fit_width(Box const& box, AvailableSize const& available_width) const
{
    // The size a box would take if its outer size filled the available space in the given axis;
    // in other words, the stretch fit into the available space, if that is definite.
    // Undefined if the available space is indefinite.
    auto const& box_state = m_state.get(box);
    return available_width.to_px()
        - box_state.margin_left
        - box_state.margin_right
        - box_state.padding_left
        - box_state.padding_right
        - box_state.border_left
        - box_state.border_right;
}

}
