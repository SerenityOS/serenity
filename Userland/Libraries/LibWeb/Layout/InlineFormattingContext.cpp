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

constexpr float text_justification_threshold = 0.1;

InlineFormattingContext::InlineFormattingContext(LayoutState& state, BlockContainer const& containing_block, BlockFormattingContext& parent)
    : FormattingContext(Type::Inline, state, containing_block, &parent)
    , m_containing_block_state(state.get(containing_block))
{
    switch (m_containing_block_state.width_constraint) {
    case SizeConstraint::MinContent:
        m_effective_containing_block_width = 0;
        break;
    case SizeConstraint::MaxContent:
        m_effective_containing_block_width = INFINITY;
        break;
    default:
        m_effective_containing_block_width = m_containing_block_state.content_width();
        break;
    }
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

float InlineFormattingContext::leftmost_x_offset_at(float y) const
{
    // NOTE: Floats are relative to the BFC root box, not necessarily the containing block of this IFC.
    auto box_in_root_rect = margin_box_rect_in_ancestor_coordinate_space(containing_block(), parent().root(), m_state);
    float y_in_root = box_in_root_rect.y() + y;
    auto space = parent().space_used_by_floats(y_in_root);
    float containing_block_x = m_containing_block_state.offset.x();
    return max(space.left, containing_block_x) - containing_block_x;
}

float InlineFormattingContext::available_space_for_line(float y) const
{
    if (m_effective_containing_block_width == 0)
        return 0;
    if (!isfinite(m_effective_containing_block_width))
        return INFINITY;

    // NOTE: Floats are relative to the BFC root box, not necessarily the containing block of this IFC.
    auto box_in_root_rect = margin_box_rect_in_ancestor_coordinate_space(containing_block(), parent().root(), m_state);
    float y_in_root = box_in_root_rect.y() + y;
    auto space = parent().space_used_by_floats(y_in_root);

    auto const& root_block_state = m_state.get(parent().root());

    space.left = max(space.left, m_containing_block_state.offset.x()) - m_containing_block_state.offset.x();
    space.right = min(root_block_state.content_width() - space.right, m_containing_block_state.offset.x() + m_effective_containing_block_width);

    return space.right - space.left;
}

void InlineFormattingContext::run(Box const&, LayoutMode layout_mode)
{
    VERIFY(containing_block().children_are_inline());
    generate_line_boxes(layout_mode);
}

void InlineFormattingContext::dimension_box_on_line(Box const& box, LayoutMode layout_mode)
{
    auto width_of_containing_block = CSS::Length::make_px(m_effective_containing_block_width);
    auto& box_state = m_state.get_mutable(box);
    auto const& computed_values = box.computed_values();

    box_state.margin_left = computed_values.margin().left.resolved(box, width_of_containing_block).to_px(box);
    box_state.border_left = computed_values.border_left().width;
    box_state.padding_left = computed_values.padding().left.resolved(box, width_of_containing_block).to_px(box);

    box_state.margin_right = computed_values.margin().right.resolved(box, width_of_containing_block).to_px(box);
    box_state.border_right = computed_values.border_right().width;
    box_state.padding_right = computed_values.padding().right.resolved(box, width_of_containing_block).to_px(box);

    box_state.margin_top = computed_values.margin().top.resolved(box, width_of_containing_block).to_px(box);
    box_state.border_top = computed_values.border_top().width;
    box_state.padding_top = computed_values.padding().top.resolved(box, width_of_containing_block).to_px(box);

    box_state.padding_bottom = computed_values.padding().bottom.resolved(box, width_of_containing_block).to_px(box);
    box_state.border_bottom = computed_values.border_bottom().width;
    box_state.margin_bottom = computed_values.margin().bottom.resolved(box, width_of_containing_block).to_px(box);

    if (is<ReplacedBox>(box)) {
        auto& replaced = verify_cast<ReplacedBox>(box);

        if (is<SVGSVGBox>(box))
            (void)layout_inside(replaced, layout_mode);

        box_state.set_content_width(compute_width_for_replaced_element(m_state, replaced));
        box_state.set_content_height(compute_height_for_replaced_element(m_state, replaced));
        return;
    }

    if (box.is_inline_block()) {
        auto const& inline_block = verify_cast<BlockContainer>(box);

        auto& width_value = inline_block.computed_values().width();
        if (width_value.is_auto()) {
            auto result = calculate_shrink_to_fit_widths(inline_block);

            auto available_width = m_containing_block_state.content_width()
                - box_state.margin_left
                - box_state.border_left
                - box_state.padding_left
                - box_state.padding_right
                - box_state.border_right
                - box_state.margin_right;

            auto width = min(max(result.preferred_minimum_width, available_width), result.preferred_width);
            box_state.set_content_width(width);
        } else {
            auto container_width = CSS::Length::make_px(m_effective_containing_block_width);
            box_state.set_content_width(width_value.resolved(box, container_width).to_px(inline_block));
        }
        auto independent_formatting_context = layout_inside(inline_block, layout_mode);

        auto& height_value = inline_block.computed_values().height();
        if (height_value.is_auto()) {
            // FIXME: (10.6.6) If 'height' is 'auto', the height depends on the element's descendants per 10.6.7.
            BlockFormattingContext::compute_height(inline_block, m_state);
        } else {
            auto container_height = CSS::Length::make_px(m_containing_block_state.content_height());
            box_state.set_content_height(height_value.resolved(box, container_height).to_px(inline_block));
        }

        if (independent_formatting_context)
            independent_formatting_context->parent_context_did_dimension_child_root_box();
        return;
    }

    // Non-replaced, non-inline-block, box on a line!?
    // I don't think we should be here. Dump the box tree so we can take a look at it.
    dbgln("FIXME: I've been asked to dimension a non-replaced, non-inline-block box on a line:");
    dump_tree(box);
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

    float excess_horizontal_space = m_effective_containing_block_width - line_box.width();

    // Only justify the text if the excess horizontal space is less than or
    // equal to 10%, or if we are not looking at the last line box.
    if (is_last_line && excess_horizontal_space / m_effective_containing_block_width > text_justification_threshold)
        return;

    float excess_horizontal_space_including_whitespace = excess_horizontal_space;
    size_t whitespace_count = 0;
    for (auto& fragment : line_box.fragments()) {
        if (fragment.is_justifiable_whitespace()) {
            ++whitespace_count;
            excess_horizontal_space_including_whitespace += fragment.width();
        }
    }

    float justified_space_width = whitespace_count > 0 ? (excess_horizontal_space_including_whitespace / static_cast<float>(whitespace_count)) : 0;

    // This is the amount that each fragment will be offset by. If a whitespace
    // fragment is shorter than the justified space width, it increases to push
    // subsequent fragments, and decreases to pull them back otherwise.
    float running_diff = 0;
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
        case InlineLevelIterator::Item::Type::ForcedBreak:
            line_builder.break_line();
            break;
        case InlineLevelIterator::Item::Type::Element: {
            auto& box = verify_cast<Layout::Box>(*item.node);
            line_builder.break_if_needed(item.border_box_width());
            line_builder.append_box(box, item.border_start + item.padding_start, item.padding_end + item.border_end, item.margin_start, item.margin_end);
            break;
        }
        case InlineLevelIterator::Item::Type::AbsolutelyPositionedElement:
            if (is<Box>(*item.node))
                parent().add_absolutely_positioned_box(static_cast<Layout::Box const&>(*item.node));
            break;

        case InlineLevelIterator::Item::Type::FloatingElement:
            if (is<Box>(*item.node))
                parent().layout_floating_box(static_cast<Layout::Box const&>(*item.node), containing_block(), layout_mode, &line_builder);
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

}
