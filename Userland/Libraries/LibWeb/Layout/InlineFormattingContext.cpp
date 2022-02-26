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

namespace Web::Layout {

InlineFormattingContext::InlineFormattingContext(FormattingState& state, BlockContainer const& containing_block, BlockFormattingContext& parent)
    : FormattingContext(Type::Inline, state, containing_block, &parent)
{
}

InlineFormattingContext::~InlineFormattingContext()
{
}

BlockFormattingContext& InlineFormattingContext::parent()
{
    return static_cast<BlockFormattingContext&>(*FormattingContext::parent());
}

BlockFormattingContext const& InlineFormattingContext::parent() const
{
    return static_cast<BlockFormattingContext const&>(*FormattingContext::parent());
}

InlineFormattingContext::AvailableSpaceForLineInfo InlineFormattingContext::available_space_for_line(float y) const
{
    // NOTE: Floats are relative to the BFC root box, not necessarily the containing block of this IFC.
    auto box_in_root_rect = margin_box_rect_in_ancestor_coordinate_space(containing_block(), parent().root(), m_state);
    float y_in_root = box_in_root_rect.y() + y;

    AvailableSpaceForLineInfo info;

    auto const& bfc = parent();

    for (ssize_t i = bfc.left_side_floats().boxes.size() - 1; i >= 0; --i) {
        auto const& floating_box = bfc.left_side_floats().boxes.at(i);
        auto rect = margin_box_rect_in_ancestor_coordinate_space(floating_box, parent().root(), m_state);
        if (rect.contains_vertically(y_in_root)) {
            info.left = rect.right() + 1;
            break;
        }
    }

    info.right = m_state.get(containing_block()).content_width;

    for (ssize_t i = bfc.right_side_floats().boxes.size() - 1; i >= 0; --i) {
        auto const& floating_box = bfc.right_side_floats().boxes.at(i);
        auto rect = margin_box_rect_in_ancestor_coordinate_space(floating_box, parent().root(), m_state);
        if (rect.contains_vertically(y_in_root)) {
            info.right = rect.left();
            break;
        }
    }

    return info;
}

void InlineFormattingContext::run(Box const&, LayoutMode layout_mode)
{
    VERIFY(containing_block().children_are_inline());

    generate_line_boxes(layout_mode);

    containing_block().for_each_child([&](auto& child) {
        VERIFY(child.is_inline());
        if (is<Box>(child) && child.is_absolutely_positioned()) {
            parent().add_absolutely_positioned_box(static_cast<Box&>(child));
            return;
        }
    });

    float min_line_height = containing_block().line_height();
    float max_line_width = 0;
    float content_height = 0;

    for (auto& line_box : m_state.get(containing_block()).line_boxes) {
        float max_height = min_line_height;
        for (auto& fragment : line_box.fragments()) {
            max_height = max(max_height, fragment.border_box_height());
        }
        max_line_width = max(max_line_width, line_box.width());
        content_height += max_height;
    }

    auto& containing_block_state = m_state.get_mutable(containing_block());

    if (layout_mode != LayoutMode::Default) {
        containing_block_state.content_width = max_line_width;
    }

    containing_block_state.content_height = content_height;
}

void InlineFormattingContext::dimension_box_on_line(Box const& box, LayoutMode layout_mode)
{
    auto width_of_containing_block = CSS::Length::make_px(m_state.get(containing_block()).content_width);
    auto& box_state = m_state.get_mutable(box);
    auto const& computed_values = box.computed_values();

    box_state.margin_left = computed_values.margin().left.resolved(box, width_of_containing_block).to_px(box);
    box_state.border_left = computed_values.border_left().width;
    box_state.padding_left = computed_values.padding().left.resolved(box, width_of_containing_block).to_px(box);
    box_state.margin_right = computed_values.margin().right.resolved(box, width_of_containing_block).to_px(box);
    box_state.border_right = computed_values.border_right().width;
    box_state.padding_right = computed_values.padding().right.resolved(box, width_of_containing_block).to_px(box);

    if (is<ReplacedBox>(box)) {
        auto& replaced = verify_cast<ReplacedBox>(box);
        box_state.content_width = compute_width_for_replaced_element(m_state, replaced);
        box_state.content_height = compute_height_for_replaced_element(m_state, replaced);
        return;
    }

    if (box.is_inline_block()) {
        auto const& inline_block = verify_cast<BlockContainer>(box);
        auto const& containing_block_state = m_state.get(containing_block());

        auto& width_value = inline_block.computed_values().width();
        if (!width_value.has_value() || (width_value->is_length() && width_value->length().is_auto())) {
            auto result = calculate_shrink_to_fit_widths(inline_block);

            auto available_width = containing_block_state.content_width
                - box_state.margin_left
                - box_state.border_left
                - box_state.padding_left
                - box_state.padding_right
                - box_state.border_right
                - box_state.margin_right;

            auto width = min(max(result.preferred_minimum_width, available_width), result.preferred_width);
            box_state.content_width = width;
        } else {
            auto container_width = CSS::Length::make_px(containing_block_state.content_width);
            box_state.content_width = width_value->resolved(box, container_width).to_px(inline_block);
        }
        auto independent_formatting_context = layout_inside(inline_block, layout_mode);

        auto& height_value = inline_block.computed_values().height();
        if (!height_value.has_value() || (height_value->is_length() && height_value->length().is_auto())) {
            // FIXME: (10.6.6) If 'height' is 'auto', the height depends on the element's descendants per 10.6.7.
            BlockFormattingContext::compute_height(inline_block, m_state);
        } else {
            auto container_height = CSS::Length::make_px(containing_block_state.content_height);
            box_state.content_height = height_value->resolved(box, container_height).to_px(inline_block);
        }

        independent_formatting_context->parent_context_did_dimension_child_root_box();
        return;
    }

    // Non-replaced, non-inline-block, box on a line!?
    // I don't think we should be here. Dump the box tree so we can take a look at it.
    dbgln("FIXME: I've been asked to dimension a non-replaced, non-inline-block box on a line:");
    dump_tree(box);
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
        if (item.is_collapsible_whitespace && line_boxes.last().is_empty_or_ends_in_whitespace())
            continue;

        switch (item.type) {
        case InlineLevelIterator::Item::Type::ForcedBreak:
            line_builder.break_line();
            break;
        case InlineLevelIterator::Item::Type::Element: {
            auto& box = verify_cast<Layout::Box>(*item.node);
            line_builder.break_if_needed(layout_mode, item.border_box_width(), item.should_force_break);
            line_builder.append_box(box, item.border_start + item.padding_start, item.padding_end + item.border_end);
            break;
        }
        case InlineLevelIterator::Item::Type::Text: {
            auto& text_node = verify_cast<Layout::TextNode>(*item.node);
            line_builder.break_if_needed(layout_mode, item.border_box_width(), item.should_force_break);
            line_builder.append_text_chunk(
                text_node,
                item.offset_in_node,
                item.length_in_node,
                item.border_start + item.padding_start,
                item.padding_end + item.border_end,
                item.width,
                text_node.computed_values().font_size());
            break;
        }
        }
    }

    for (auto& line_box : line_boxes) {
        line_box.trim_trailing_whitespace();
    }

    line_builder.remove_last_line_if_empty();
}

}
