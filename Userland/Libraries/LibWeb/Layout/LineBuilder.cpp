/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Layout/LineBuilder.h>
#include <LibWeb/Layout/TextNode.h>

namespace Web::Layout {

LineBuilder::LineBuilder(InlineFormattingContext& context, FormattingState& formatting_state)
    : m_context(context)
    , m_formatting_state(formatting_state)
    , m_containing_block_state(formatting_state.get_mutable(context.containing_block()))
{
    begin_new_line(false);
}

LineBuilder::~LineBuilder()
{
    if (m_last_line_needs_update)
        update_last_line();
}

void LineBuilder::break_line()
{
    update_last_line();
    m_containing_block_state.line_boxes.append(LineBox());
    begin_new_line(true);
}

void LineBuilder::begin_new_line(bool increment_y)
{
    if (increment_y)
        m_current_y += max(m_max_height_on_current_line, m_context.containing_block().line_height());
    auto space = m_context.available_space_for_line(m_current_y);
    m_available_width_for_current_line = space.right - space.left;
    m_max_height_on_current_line = 0;

    m_last_line_needs_update = true;
}

LineBox& LineBuilder::ensure_last_line_box()
{
    auto& line_boxes = m_containing_block_state.line_boxes;
    if (line_boxes.is_empty())
        line_boxes.append(LineBox {});
    return line_boxes.last();
}

void LineBuilder::append_box(Box const& box, float leading_size, float trailing_size)
{
    auto const& box_state = m_formatting_state.get(box);
    ensure_last_line_box().add_fragment(box, 0, 0, leading_size, trailing_size, box_state.content_width, box_state.content_height, box_state.border_box_top(), box_state.border_box_bottom());
    m_max_height_on_current_line = max(m_max_height_on_current_line, box_state.content_height);
}

void LineBuilder::append_text_chunk(TextNode const& text_node, size_t offset_in_node, size_t length_in_node, float leading_size, float trailing_size, float content_width, float content_height)
{
    ensure_last_line_box().add_fragment(text_node, offset_in_node, length_in_node, leading_size, trailing_size, content_width, content_height, 0, 0);
    m_max_height_on_current_line = max(m_max_height_on_current_line, content_height);
}

bool LineBuilder::should_break(LayoutMode layout_mode, float next_item_width, bool should_force_break)
{
    if (layout_mode == LayoutMode::AllPossibleLineBreaks)
        return true;
    if (should_force_break)
        return true;
    if (layout_mode == LayoutMode::OnlyRequiredLineBreaks)
        return false;
    auto const& line_boxes = m_containing_block_state.line_boxes;
    if (line_boxes.is_empty() || line_boxes.last().is_empty())
        return false;
    auto current_line_width = line_boxes.last().width();
    return (current_line_width + next_item_width) > m_available_width_for_current_line;
}

void LineBuilder::update_last_line()
{
    m_last_line_needs_update = false;
    auto& line_boxes = m_containing_block_state.line_boxes;

    if (line_boxes.is_empty())
        return;

    auto& line_box = line_boxes.last();

    auto text_align = m_context.containing_block().computed_values().text_align();
    float x_offset = m_context.available_space_for_line(m_current_y).left;

    float excess_horizontal_space = m_containing_block_state.content_width - line_box.width();

    switch (text_align) {
    case CSS::TextAlign::Center:
    case CSS::TextAlign::LibwebCenter:
        x_offset += excess_horizontal_space / 2;
        break;
    case CSS::TextAlign::Right:
        x_offset += excess_horizontal_space;
        break;
    case CSS::TextAlign::Left:
    case CSS::TextAlign::Justify:
    default:
        break;
    }

    float excess_horizontal_space_including_whitespace = excess_horizontal_space;
    size_t whitespace_count = 0;
    if (text_align == CSS::TextAlign::Justify) {
        for (auto& fragment : line_box.fragments()) {
            if (fragment.is_justifiable_whitespace()) {
                ++whitespace_count;
                excess_horizontal_space_including_whitespace += fragment.width();
            }
        }
    }

    float justified_space_width = whitespace_count > 0 ? (excess_horizontal_space_including_whitespace / static_cast<float>(whitespace_count)) : 0;

    // HACK: This is where we determine the baseline of this line box.
    //       We use the bottommost value of all the font baselines on the line and all the inline-block heights.
    // FIXME: Support all the various CSS baseline properties, etc.
    float max_height = max(m_max_height_on_current_line, m_context.containing_block().line_height());
    for (auto const& fragment : line_box.fragments()) {
        if (auto length_percentage = fragment.layout_node().computed_values().vertical_align().get_pointer<CSS::LengthPercentage>(); length_percentage && length_percentage->is_length()) {
            max_height = max(max_height, fragment.border_box_height() + length_percentage->length().to_px(fragment.layout_node()));
        }
    }

    float line_box_baseline = 0;
    for (auto& fragment : line_box.fragments()) {
        float extra_height_from_vertical_align = 0;
        if (auto length_percentage = fragment.layout_node().computed_values().vertical_align().get_pointer<CSS::LengthPercentage>(); length_percentage && length_percentage->is_length()) {
            extra_height_from_vertical_align = length_percentage->length().to_px(fragment.layout_node());
        }
        float fragment_baseline;
        if (fragment.layout_node().is_box()) {
            fragment_baseline = m_formatting_state.get(static_cast<Box const&>(fragment.layout_node())).border_box_height();
        } else {
            float font_baseline = fragment.layout_node().font().baseline();
            fragment_baseline = max_height + (font_baseline / 2.0f);
        }
        fragment_baseline += extra_height_from_vertical_align;
        line_box_baseline = max(line_box_baseline, fragment_baseline);
    }

    // Now we're going to align our fragments on the inline axis.
    // We need to remember how much the last fragment on the line was moved by this process,
    // since that is used to compute the final width of the entire line box.
    float last_fragment_x_adjustment = 0;

    for (size_t i = 0; i < line_box.fragments().size(); ++i) {
        auto& fragment = line_box.fragments()[i];

        float new_fragment_x = roundf(x_offset + fragment.offset().x());
        float new_fragment_y = 0;

        if (auto length_percentage = fragment.layout_node().computed_values().vertical_align().get_pointer<CSS::LengthPercentage>(); length_percentage && length_percentage->is_length()) {
            new_fragment_y = m_current_y + (line_box_baseline - (fragment.border_box_bottom() + length_percentage->length().to_px(fragment.layout_node())));
        } else {
            // Vertically align everyone's bottom to the baseline.
            // FIXME: Support other alignment values.
            new_fragment_y = m_current_y + (line_box_baseline - fragment.border_box_height());
        }

        last_fragment_x_adjustment = new_fragment_x - fragment.offset().x();
        fragment.set_offset({ new_fragment_x, new_fragment_y });

        if (text_align == CSS::TextAlign::Justify
            && fragment.is_justifiable_whitespace()
            && fragment.width() != justified_space_width) {
            float diff = justified_space_width - fragment.width();
            fragment.set_width(justified_space_width);
            // Shift subsequent sibling fragments to the right to adjust for change in width.
            for (size_t j = i + 1; j < line_box.fragments().size(); ++j) {
                auto offset = line_box.fragments()[j].offset();
                offset.translate_by(diff, 0);
                line_box.fragments()[j].set_offset(offset);
            }
        }
    }

    if (!line_box.fragments().is_empty())
        line_box.m_width += last_fragment_x_adjustment;
}

void LineBuilder::remove_last_line_if_empty()
{
    // If there's an empty line box at the bottom, just remove it instead of giving it height.
    auto& line_boxes = m_containing_block_state.line_boxes;
    if (!line_boxes.is_empty() && line_boxes.last().fragments().is_empty()) {
        line_boxes.take_last();
        m_last_line_needs_update = false;
    }
}
}
