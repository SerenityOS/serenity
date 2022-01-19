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
}

LineBuilder::~LineBuilder()
{
    update_last_line();
}

void LineBuilder::break_line()
{
    update_last_line();
    m_context.containing_block().line_boxes().append(LineBox());
    begin_new_line();
}

void LineBuilder::begin_new_line()
{
    m_current_y += m_max_height_on_current_line;
    auto space = m_context.available_space_for_line(m_current_y);
    m_available_width_for_current_line = space.right - space.left;
    m_max_height_on_current_line = 0;
}

void LineBuilder::append_box(Box& box)
{
    m_context.containing_block().line_boxes().last().add_fragment(box, 0, 0, box.width(), box.height());
    m_max_height_on_current_line = max(m_max_height_on_current_line, box.height());
}

void LineBuilder::append_text_chunk(TextNode& text_node, size_t offset_in_node, size_t length_in_node, float width, float height)
{
    m_context.containing_block().line_boxes().last().add_fragment(text_node, offset_in_node, length_in_node, width, height);
    m_max_height_on_current_line = max(m_max_height_on_current_line, height);
}

void LineBuilder::break_if_needed(LayoutMode layout_mode, float next_item_width)
{
    if (layout_mode == LayoutMode::AllPossibleLineBreaks
        || (m_context.containing_block().line_boxes().last().width() + next_item_width) > m_available_width_for_current_line)
        break_line();
}

void LineBuilder::update_last_line()
{
    if (m_context.containing_block().line_boxes().is_empty())
        return;

    auto& line_box = m_context.containing_block().line_boxes().last();

    auto text_align = m_context.containing_block().computed_values().text_align();
    float x_offset = m_context.available_space_for_line(m_current_y).left;

    float excess_horizontal_space = m_context.containing_block().width() - line_box.width();

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

    for (size_t i = 0; i < line_box.fragments().size(); ++i) {
        auto& fragment = line_box.fragments()[i];

        // Vertically align everyone's bottom to the line.
        // FIXME: Support other kinds of vertical alignment.
        fragment.set_offset({ roundf(x_offset + fragment.offset().x()), m_current_y + (m_max_height_on_current_line - fragment.height()) });

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

}
