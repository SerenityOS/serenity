/*
 * Copyright (c) 2020-2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/CSS/Length.h>
#include <LibWeb/DOM/Node.h>
#include <LibWeb/Dump.h>
#include <LibWeb/Layout/BlockContainer.h>
#include <LibWeb/Layout/BlockFormattingContext.h>
#include <LibWeb/Layout/Box.h>
#include <LibWeb/Layout/InlineFormattingContext.h>
#include <LibWeb/Layout/InlineLevelIterator.h>
#include <LibWeb/Layout/LineBuilder.h>
#include <LibWeb/Layout/ReplacedBox.h>
#include <LibWeb/Layout/SVGSVGBox.h>

namespace Web::Layout {

InlineFormattingContext::InlineFormattingContext(LayoutState& state, BlockContainer const& containing_block, BlockFormattingContext& parent)
    : FormattingContext(Type::Inline, state, containing_block, &parent)
    , m_containing_block_state(state.get(containing_block))
{
}

InlineFormattingContext::~InlineFormattingContext() = default;

BlockFormattingContext& InlineFormattingContext::parent()
{
    return static_cast<BlockFormattingContext&>(*FormattingContext::parent());
}

BlockFormattingContext const& InlineFormattingContext::parent() const
{
    return static_cast<BlockFormattingContext const&>(*FormattingContext::parent());
}

CSSPixels InlineFormattingContext::leftmost_x_offset_at(CSSPixels y) const
{
    // NOTE: Floats are relative to the BFC root box, not necessarily the containing block of this IFC.
    auto box_in_root_rect = content_box_rect_in_ancestor_coordinate_space(containing_block(), parent().root());
    CSSPixels y_in_root = box_in_root_rect.y() + y;
    auto space_and_containing_margin = parent().space_used_and_containing_margin_for_floats(y_in_root);
    auto left_side_floats_limit_to_right = space_and_containing_margin.left_total_containing_margin + space_and_containing_margin.left_used_space;
    if (box_in_root_rect.x() >= left_side_floats_limit_to_right) {
        // The left edge of the containing block is to the right of the rightmost left-side float.
        // We start placing inline content at the left edge of the containing block.
        return 0;
    }
    // The left edge of the containing block is to the left of the rightmost left-side float.
    // We adjust the inline content insertion point by the overlap between the containing block and the float.
    return left_side_floats_limit_to_right - max(CSSPixels(0), box_in_root_rect.x());
}

AvailableSize InlineFormattingContext::available_space_for_line(CSSPixels y) const
{
    auto intrusions = parent().intrusion_by_floats_into_box(containing_block(), y);
    if (m_available_space->width.is_definite()) {
        return AvailableSize::make_definite(m_available_space->width.to_px_or_zero() - (intrusions.left + intrusions.right));
    } else {
        return m_available_space->width;
    }
}

CSSPixels InlineFormattingContext::automatic_content_width() const
{
    return m_automatic_content_width;
}

CSSPixels InlineFormattingContext::automatic_content_height() const
{
    return m_automatic_content_height;
}

void InlineFormattingContext::run(Box const&, LayoutMode layout_mode, AvailableSpace const& available_space)
{
    VERIFY(containing_block().children_are_inline());
    m_available_space = available_space;
    generate_line_boxes(layout_mode);

    CSSPixels content_height = 0;

    for (auto& line_box : m_containing_block_state.line_boxes) {
        content_height += line_box.height();
    }

    // NOTE: We ask the parent BFC to calculate the automatic content width of this IFC.
    //       This ensures that any floated boxes are taken into account.
    m_automatic_content_width = parent().greatest_child_width(containing_block());
    m_automatic_content_height = content_height;
}

void InlineFormattingContext::dimension_box_on_line(Box const& box, LayoutMode layout_mode)
{
    auto width_of_containing_block = m_available_space->width.to_px_or_zero();
    auto& box_state = m_state.get_mutable(box);
    auto const& computed_values = box.computed_values();

    box_state.margin_left = computed_values.margin().left().to_px(box, width_of_containing_block);
    box_state.border_left = computed_values.border_left().width;
    box_state.padding_left = computed_values.padding().left().to_px(box, width_of_containing_block);

    box_state.margin_right = computed_values.margin().right().to_px(box, width_of_containing_block);
    box_state.border_right = computed_values.border_right().width;
    box_state.padding_right = computed_values.padding().right().to_px(box, width_of_containing_block);

    box_state.margin_top = computed_values.margin().top().to_px(box, width_of_containing_block);
    box_state.border_top = computed_values.border_top().width;
    box_state.padding_top = computed_values.padding().top().to_px(box, width_of_containing_block);

    box_state.padding_bottom = computed_values.padding().bottom().to_px(box, width_of_containing_block);
    box_state.border_bottom = computed_values.border_bottom().width;
    box_state.margin_bottom = computed_values.margin().bottom().to_px(box, width_of_containing_block);

    if (box_is_sized_as_replaced_element(box)) {
        box_state.set_content_width(compute_width_for_replaced_element(box, *m_available_space));
        box_state.set_content_height(compute_height_for_replaced_element(box, *m_available_space));

        if (is<SVGSVGBox>(box))
            (void)layout_inside(box, layout_mode, box_state.available_inner_space_or_constraints_from(*m_available_space));
        return;
    }

    // Any box that has simple flow inside should have generated line box fragments already.
    if (box.display().is_flow_inside()) {
        dbgln("FIXME: InlineFormattingContext::dimension_box_on_line got unexpected box in inline context:");
        dump_tree(box);
        return;
    }

    auto const& width_value = box.computed_values().width();
    CSSPixels unconstrained_width = 0;
    if (should_treat_width_as_auto(box, *m_available_space)) {
        auto result = calculate_shrink_to_fit_widths(box);

        if (m_available_space->width.is_definite()) {
            auto available_width = m_available_space->width.to_px_or_zero()
                - box_state.margin_left
                - box_state.border_left
                - box_state.padding_left
                - box_state.padding_right
                - box_state.border_right
                - box_state.margin_right;

            unconstrained_width = min(max(result.preferred_minimum_width, available_width), result.preferred_width);
        } else {
            unconstrained_width = result.preferred_width;
        }
    } else {
        if (width_value.contains_percentage() && !m_available_space->width.is_definite()) {
            // NOTE: We can't resolve percentages yet. We'll have to wait until after inner layout.
        } else {
            auto inner_width = calculate_inner_width(box, m_available_space->width, width_value);
            unconstrained_width = inner_width.to_px(box);
        }
    }

    CSSPixels width = unconstrained_width;
    if (!should_treat_max_width_as_none(box, m_available_space->width)) {
        auto max_width = calculate_inner_width(box, m_available_space->width, box.computed_values().max_width()).to_px(box);
        width = min(width, max_width);
    }

    auto computed_min_width = box.computed_values().min_width();
    if (!computed_min_width.is_auto()) {
        auto min_width = calculate_inner_width(box, m_available_space->width, computed_min_width).to_px(box);
        width = max(width, min_width);
    }

    box_state.set_content_width(width);

    auto independent_formatting_context = layout_inside(box, layout_mode, box_state.available_inner_space_or_constraints_from(*m_available_space));

    auto const& height_value = box.computed_values().height();
    if (should_treat_height_as_auto(box, *m_available_space)) {
        // FIXME: (10.6.6) If 'height' is 'auto', the height depends on the element's descendants per 10.6.7.
        parent().compute_height(box, AvailableSpace(AvailableSize::make_indefinite(), AvailableSize::make_indefinite()));
    } else {
        auto inner_height = calculate_inner_height(box, AvailableSize::make_definite(m_containing_block_state.content_height()), height_value);
        box_state.set_content_height(inner_height.to_px(box));
    }

    if (independent_formatting_context)
        independent_formatting_context->parent_context_did_dimension_child_root_box();
}

void InlineFormattingContext::apply_justification_to_fragments(CSS::TextJustify text_justify, LineBox& line_box, bool is_last_line)
{
    switch (text_justify) {
    case CSS::TextJustify::None:
        return;
    // FIXME: These two cases currently fall back to auto, handle them as well.
    case CSS::TextJustify::InterCharacter:
    case CSS::TextJustify::InterWord:
    case CSS::TextJustify::Auto:
        break;
    }

    // https://www.w3.org/TR/css-text-3/#text-align-property
    // Unless otherwise specified by text-align-last, the last line before a forced break or the end of the block is start-aligned.
    // FIXME: Support text-align-last.
    if (is_last_line || line_box.m_has_forced_break)
        return;

    CSSPixels excess_horizontal_space = line_box.original_available_width().to_px_or_zero() - line_box.width();
    CSSPixels excess_horizontal_space_including_whitespace = excess_horizontal_space;
    size_t whitespace_count = 0;
    for (auto& fragment : line_box.fragments()) {
        if (fragment.is_justifiable_whitespace()) {
            ++whitespace_count;
            excess_horizontal_space_including_whitespace += fragment.width();
        }
    }

    CSSPixels justified_space_width = whitespace_count > 0 ? (excess_horizontal_space_including_whitespace / whitespace_count) : 0;

    // This is the amount that each fragment will be offset by. If a whitespace
    // fragment is shorter than the justified space width, it increases to push
    // subsequent fragments, and decreases to pull them back otherwise.
    CSSPixels running_diff = 0;
    for (size_t i = 0; i < line_box.fragments().size(); ++i) {
        auto& fragment = line_box.fragments()[i];

        auto offset = fragment.offset();
        offset.translate_by(running_diff, 0);
        fragment.set_offset(offset);

        if (fragment.is_justifiable_whitespace()
            && fragment.width() != justified_space_width) {
            running_diff += justified_space_width - fragment.width();
            fragment.set_width(justified_space_width);
        }
    }
}

void InlineFormattingContext::generate_line_boxes(LayoutMode layout_mode)
{
    auto& containing_block_state = m_state.get_mutable(containing_block());
    auto& line_boxes = containing_block_state.line_boxes;
    line_boxes.clear_with_capacity();

    InlineLevelIterator iterator(*this, m_state, containing_block(), layout_mode);
    LineBuilder line_builder(*this, m_state);

    for (;;) {
        auto item_opt = iterator.next(line_builder.available_width_for_current_line());
        if (!item_opt.has_value())
            break;
        auto& item = item_opt.value();

        // Ignore collapsible whitespace chunks at the start of line, and if the last fragment already ends in whitespace.
        if (item.is_collapsible_whitespace && (line_boxes.is_empty() || line_boxes.last().is_empty_or_ends_in_whitespace()))
            continue;

        switch (item.type) {
        case InlineLevelIterator::Item::Type::ForcedBreak: {
            line_builder.break_line(LineBuilder::ForcedBreak::Yes);
            if (item.node) {
                auto introduce_clearance = parent().clear_floating_boxes(*item.node, *this);
                if (introduce_clearance == BlockFormattingContext::DidIntroduceClearance::Yes)
                    parent().reset_margin_state();
            }
            break;
        }
        case InlineLevelIterator::Item::Type::Element: {
            auto& box = verify_cast<Layout::Box>(*item.node);
            compute_inset(box);
            line_builder.break_if_needed(item.border_box_width());
            line_builder.append_box(box, item.border_start + item.padding_start, item.padding_end + item.border_end, item.margin_start, item.margin_end);
            break;
        }
        case InlineLevelIterator::Item::Type::AbsolutelyPositionedElement:
            if (is<Box>(*item.node))
                parent().add_absolutely_positioned_box(static_cast<Layout::Box const&>(*item.node));
            break;

        case InlineLevelIterator::Item::Type::FloatingElement:
            if (is<Box>(*item.node)) {
                auto introduce_clearance = parent().clear_floating_boxes(*item.node, *this);
                if (introduce_clearance == BlockFormattingContext::DidIntroduceClearance::Yes)
                    parent().reset_margin_state();
                parent().layout_floating_box(static_cast<Layout::Box const&>(*item.node), containing_block(), layout_mode, *m_available_space, 0, &line_builder);
            }
            break;

        case InlineLevelIterator::Item::Type::Text: {
            auto& text_node = verify_cast<Layout::TextNode>(*item.node);

            if (text_node.computed_values().white_space() != CSS::WhiteSpace::Nowrap && line_builder.break_if_needed(item.border_box_width())) {
                // If whitespace caused us to break, we swallow the whitespace instead of
                // putting it on the next line.

                // If we're in a whitespace-collapsing context, we can simply check the flag.
                if (item.is_collapsible_whitespace)
                    break;

                // In whitespace-preserving contexts (white-space: pre*), we have to check manually.
                auto view = text_node.text_for_rendering().substring_view(item.offset_in_node, item.length_in_node);
                if (view.is_whitespace())
                    break;
            }
            line_builder.append_text_chunk(
                text_node,
                item.offset_in_node,
                item.length_in_node,
                item.border_start + item.padding_start,
                item.padding_end + item.border_end,
                item.margin_start,
                item.margin_end,
                item.width,
                text_node.line_height());
            break;
        }
        }
    }

    for (auto& line_box : line_boxes) {
        line_box.trim_trailing_whitespace();
    }

    line_builder.remove_last_line_if_empty();

    auto const& containing_block = this->containing_block();
    auto text_align = containing_block.computed_values().text_align();
    auto text_justify = containing_block.computed_values().text_justify();
    if (text_align == CSS::TextAlign::Justify) {
        for (size_t i = 0; i < line_boxes.size(); i++) {
            auto& line_box = line_boxes[i];
            auto is_last_line = i == line_boxes.size() - 1;
            apply_justification_to_fragments(text_justify, line_box, is_last_line);
        }
    }
}

bool InlineFormattingContext::any_floats_intrude_at_y(CSSPixels y) const
{
    auto box_in_root_rect = content_box_rect_in_ancestor_coordinate_space(containing_block(), parent().root());
    CSSPixels y_in_root = box_in_root_rect.y() + y;
    auto space_and_containing_margin = parent().space_used_and_containing_margin_for_floats(y_in_root);
    return space_and_containing_margin.left_used_space > 0 || space_and_containing_margin.right_used_space > 0;
}

bool InlineFormattingContext::can_fit_new_line_at_y(CSSPixels y) const
{
    auto top_intrusions = parent().intrusion_by_floats_into_box(containing_block(), y);
    auto bottom_intrusions = parent().intrusion_by_floats_into_box(containing_block(), y + containing_block().line_height() - 1);

    auto left_edge = [](auto& space) -> CSSPixels {
        return space.left;
    };

    auto right_edge = [this](auto& space) -> CSSPixels {
        return m_available_space->width.to_px_or_zero() - space.right;
    };

    auto top_left_edge = left_edge(top_intrusions);
    auto top_right_edge = right_edge(top_intrusions);
    auto bottom_left_edge = left_edge(bottom_intrusions);
    auto bottom_right_edge = right_edge(bottom_intrusions);

    if (top_left_edge > bottom_right_edge)
        return false;
    if (bottom_left_edge > top_right_edge)
        return false;
    return true;
}

bool InlineFormattingContext::can_determine_size_of_child() const
{
    return parent().can_determine_size_of_child();
}

void InlineFormattingContext::determine_width_of_child(Box const& box, AvailableSpace const& available_space)
{
    return parent().determine_width_of_child(box, available_space);
}

void InlineFormattingContext::determine_height_of_child(Box const& box, AvailableSpace const& available_space)
{
    return parent().determine_height_of_child(box, available_space);
}

CSSPixels InlineFormattingContext::vertical_float_clearance() const
{
    return m_vertical_float_clearance;
}

void InlineFormattingContext::set_vertical_float_clearance(CSSPixels vertical_float_clearance)
{
    m_vertical_float_clearance = vertical_float_clearance;
}

}
