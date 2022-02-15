/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Layout/LineBuilder.h>
#include <LibWeb/Layout/TextNode.h>

namespace Web::Layout {

LineBuilder::LineBuilder(InlineFormattingContext& context)
    : m_context(context)
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
    m_context.containing_block().line_boxes().append(LineBox());
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

void LineBuilder::append_box(Box& box, float leading_size, float trailing_size)
{
    m_context.containing_block().ensure_last_line_box().add_fragment(box, 0, 0, leading_size, trailing_size, box.content_width(), box.content_height());
    m_max_height_on_current_line = max(m_max_height_on_current_line, box.content_height());
}

void LineBuilder::append_text_chunk(TextNode& text_node, size_t offset_in_node, size_t length_in_node, float leading_size, float trailing_size, float content_width, float content_height)
{
    m_context.containing_block().ensure_last_line_box().add_fragment(text_node, offset_in_node, length_in_node, leading_size, trailing_size, content_width, content_height);
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
    auto const& line_boxes = m_context.containing_block().line_boxes();
    if (line_boxes.is_empty() || line_boxes.last().is_empty())
        return false;
    auto current_line_width = m_context.containing_block().line_boxes().last().width();
    return (current_line_width + next_item_width) > m_available_width_for_current_line;
}

void LineBuilder::update_last_line()
{
    m_last_line_needs_update = false;

    if (m_context.containing_block().line_boxes().is_empty())
        return;

    auto& line_box = m_context.containing_block().line_boxes().last();

    auto text_align = m_context.containing_block().computed_values().text_align();
    float x_offset = m_context.available_space_for_line(m_current_y).left;

    float excess_horizontal_space = m_context.containing_block().content_width() - line_box.width();

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
    float line_box_baseline = 0;
    for (auto& fragment : line_box.fragments()) {
        float fragment_baseline;
        if (fragment.layout_node().is_box()) {
            fragment_baseline = static_cast<Box const&>(fragment.layout_node()).content_height();
        } else {
            float font_baseline = fragment.layout_node().font().baseline();
            fragment_baseline = (max_height / 2.0f) + (font_baseline / 2.0f);
        }
        line_box_baseline = max(line_box_baseline, fragment_baseline);
    }

    for (size_t i = 0; i < line_box.fragments().size(); ++i) {
        auto& fragment = line_box.fragments()[i];

        // Vertically align everyone's bottom to the baseline.
        // FIXME: Support other kinds of vertical alignment.
        fragment.set_offset({ roundf(x_offset + fragment.offset().x()), m_current_y + (line_box_baseline - fragment.height()) });

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

    if (!line_box.fragments().is_empty()) {
        float left_edge = line_box.fragments().first().offset().x();
        float right_edge = line_box.fragments().last().offset().x() + line_box.fragments().last().width();
        float final_line_box_width = right_edge - left_edge;
        line_box.m_width = final_line_box_width;
    }
}

void LineBuilder::remove_last_line_if_empty()
{
    // If there's an empty line box at the bottom, just remove it instead of giving it height.
    auto& line_boxes = m_context.containing_block().line_boxes();
    if (!line_boxes.is_empty() && line_boxes.last().fragments().is_empty()) {
        line_boxes.take_last();
        m_last_line_needs_update = false;
    }
}

}
