/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Layout/BlockFormattingContext.h>
#include <LibWeb/Layout/LineBuilder.h>
#include <LibWeb/Layout/TextNode.h>

namespace Web::Layout {

LineBuilder::LineBuilder(InlineFormattingContext& context, LayoutState& layout_state)
    : m_context(context)
    , m_layout_state(layout_state)
    , m_containing_block_state(layout_state.get_mutable(context.containing_block()))
{
    begin_new_line(false);
}

LineBuilder::~LineBuilder()
{
    if (m_last_line_needs_update)
        update_last_line();
}

void LineBuilder::break_line(Optional<float> next_item_width)
{
    update_last_line();
    size_t break_count = 0;
    bool floats_intrude_at_current_y = false;
    do {
        m_containing_block_state.line_boxes.append(LineBox());
        begin_new_line(true, break_count == 0);
        break_count++;
        floats_intrude_at_current_y = m_context.any_floats_intrude_at_y(m_current_y);
    } while ((floats_intrude_at_current_y && !m_context.can_fit_new_line_at_y(m_current_y))
        || (next_item_width.has_value()
            && next_item_width.value() > m_available_width_for_current_line
            && floats_intrude_at_current_y));
}

void LineBuilder::begin_new_line(bool increment_y, bool is_first_break_in_sequence)
{
    if (increment_y) {
        if (is_first_break_in_sequence) {
            // First break is simple, just go to the start of the next line.
            m_current_y += max(m_max_height_on_current_line, m_context.containing_block().line_height());
        } else {
            // We're doing more than one break in a row.
            // This means we're trying to squeeze past intruding floats.
            // Scan 1px at a time until we find a Y value where a new line can fit.
            // FIXME: This is super dumb and inefficient.
            float candidate_y = m_current_y + 1;
            while (true) {
                if (m_context.can_fit_new_line_at_y(candidate_y))
                    break;
                ++candidate_y;
            }
            m_current_y = candidate_y;
        }
    }
    recalculate_available_space();
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

void LineBuilder::append_box(Box const& box, float leading_size, float trailing_size, float leading_margin, float trailing_margin)
{
    auto& box_state = m_layout_state.get_mutable(box);
    auto& line_box = ensure_last_line_box();
    line_box.add_fragment(box, 0, 0, leading_size, trailing_size, leading_margin, trailing_margin, box_state.content_width(), box_state.content_height(), box_state.border_box_top(), box_state.border_box_bottom());
    m_max_height_on_current_line = max(m_max_height_on_current_line, box_state.border_box_height());

    box_state.containing_line_box_fragment = LineBoxFragmentCoordinate {
        .line_box_index = m_containing_block_state.line_boxes.size() - 1,
        .fragment_index = line_box.fragments().size() - 1,
    };
}

void LineBuilder::append_text_chunk(TextNode const& text_node, size_t offset_in_node, size_t length_in_node, float leading_size, float trailing_size, float leading_margin, float trailing_margin, float content_width, float content_height)
{
    ensure_last_line_box().add_fragment(text_node, offset_in_node, length_in_node, leading_size, trailing_size, leading_margin, trailing_margin, content_width, content_height, 0, 0);
    m_max_height_on_current_line = max(m_max_height_on_current_line, content_height);
}

float LineBuilder::y_for_float_to_be_inserted_here(Box const& box)
{
    auto const& box_state = m_layout_state.get(box);
    auto const width = box_state.margin_box_width();
    auto const height = box_state.margin_box_height();

    float candidate_y = m_current_y;

    float current_line_width = ensure_last_line_box().width();
    // If there's already inline content on the current line, check if the new float can fit
    // alongside the content. If not, place it on the next line.
    if (current_line_width > 0 && roundf(current_line_width + width) > m_available_width_for_current_line)
        candidate_y += m_context.containing_block().line_height();

    // Then, look for the next Y position where we can fit the new float.
    // FIXME: This is super dumb, we move 1px downwards per iteration and stop
    //        when we find an Y value where we don't collide with other floats.
    while (true) {
        auto space_at_y_top = m_context.available_space_for_line(candidate_y);
        auto space_at_y_bottom = m_context.available_space_for_line(candidate_y + height);
        if (width >= space_at_y_top || width >= space_at_y_bottom) {
            if (!m_context.any_floats_intrude_at_y(candidate_y) && !m_context.any_floats_intrude_at_y(candidate_y + height)) {
                return candidate_y;
            }
        } else {
            return candidate_y;
        }
        candidate_y += 1;
    }
}

bool LineBuilder::should_break(float next_item_width)
{
    if (!isfinite(m_available_width_for_current_line))
        return false;

    auto const& line_boxes = m_containing_block_state.line_boxes;
    if (line_boxes.is_empty() || line_boxes.last().is_empty()) {
        // If we don't have a single line box yet *and* there are no floats intruding
        // at this Y coordinate, we don't need to break before inserting anything.
        if (!m_context.any_floats_intrude_at_y(m_current_y))
            return false;
        if (!m_context.any_floats_intrude_at_y(m_current_y + m_context.containing_block().line_height()))
            return false;
    }
    auto current_line_width = ensure_last_line_box().width();
    return roundf(current_line_width + next_item_width) > m_available_width_for_current_line;
}

static float box_baseline(LayoutState const& state, Box const& box)
{
    auto const& box_state = state.get(box);

    auto const& vertical_align = box.computed_values().vertical_align();
    if (vertical_align.has<CSS::VerticalAlign>()) {
        switch (vertical_align.get<CSS::VerticalAlign>()) {
        case CSS::VerticalAlign::Top:
            return box_state.border_box_top();
        case CSS::VerticalAlign::Bottom:
            return box_state.content_height() + box_state.border_box_bottom();
        default:
            break;
        }
    }

    if (!box_state.line_boxes.is_empty())
        return box_state.border_box_top() + box_state.offset.y() + box_state.line_boxes.last().baseline();
    if (box.has_children() && !box.children_are_inline()) {
        auto const* child_box = box.last_child_of_type<Box>();
        VERIFY(child_box);
        return box_baseline(state, *child_box);
    }
    return box_state.border_box_height();
}

void LineBuilder::update_last_line()
{
    m_last_line_needs_update = false;
    auto& line_boxes = m_containing_block_state.line_boxes;

    if (line_boxes.is_empty())
        return;

    auto& line_box = line_boxes.last();

    auto text_align = m_context.containing_block().computed_values().text_align();

    auto current_line_height = max(m_max_height_on_current_line, m_context.containing_block().line_height());
    float x_offset_top = m_context.leftmost_x_offset_at(m_current_y);
    float x_offset_bottom = m_context.leftmost_x_offset_at(m_current_y + current_line_height - 1);
    float x_offset = max(x_offset_top, x_offset_bottom);

    float excess_horizontal_space = m_available_width_for_current_line - line_box.width();

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

    auto line_box_baseline = [&] {
        float line_box_baseline = 0;
        for (auto& fragment : line_box.fragments()) {
            auto const& font = fragment.layout_node().font();
            auto const line_height = fragment.layout_node().line_height();
            auto const font_metrics = font.pixel_metrics();
            auto const typographic_height = font_metrics.ascent + font_metrics.descent;
            auto const leading = line_height - typographic_height;
            auto const half_leading = leading / 2;

            // The CSS specification calls this AD (A+D, Ascent + Descent).

            float fragment_baseline = 0;
            if (fragment.layout_node().is_text_node()) {
                fragment_baseline = font_metrics.ascent;
            } else {
                auto const& box = verify_cast<Layout::Box>(fragment.layout_node());
                fragment_baseline = box_baseline(m_layout_state, box);
            }

            fragment_baseline += half_leading;

            // Remember the baseline used for this fragment. This will be used when painting the fragment.
            fragment.set_baseline(fragment_baseline);

            // NOTE: For fragments with a <length> vertical-align, shift the line box baseline down by the length.
            //       This ensures that we make enough vertical space on the line for any manually-aligned fragments.
            if (auto length_percentage = fragment.layout_node().computed_values().vertical_align().template get_pointer<CSS::LengthPercentage>(); length_percentage && length_percentage->is_length())
                fragment_baseline += length_percentage->length().to_px(fragment.layout_node());

            line_box_baseline = max(line_box_baseline, fragment_baseline);
        }
        return line_box_baseline;
    }();

    // Start with the "strut", an imaginary zero-width box at the start of each line box.
    auto strut_top = m_current_y;
    auto strut_bottom = m_current_y + m_context.containing_block().line_height();

    float uppermost_box_top = strut_top;
    float lowermost_box_bottom = strut_bottom;

    for (size_t i = 0; i < line_box.fragments().size(); ++i) {
        auto& fragment = line_box.fragments()[i];

        float new_fragment_x = roundf(x_offset + fragment.offset().x());
        float new_fragment_y = 0;

        auto y_value_for_alignment = [&](CSS::VerticalAlign vertical_align) {
            switch (vertical_align) {
            case CSS::VerticalAlign::Baseline:
                return m_current_y + line_box_baseline - fragment.baseline() + fragment.border_box_top();
            case CSS::VerticalAlign::Top:
                return m_current_y + fragment.border_box_top();
            case CSS::VerticalAlign::Middle:
            case CSS::VerticalAlign::Bottom:
            case CSS::VerticalAlign::Sub:
            case CSS::VerticalAlign::Super:
            case CSS::VerticalAlign::TextBottom:
            case CSS::VerticalAlign::TextTop:
                // FIXME: These are all 'baseline'
                return m_current_y + line_box_baseline - fragment.baseline() + fragment.border_box_top();
            }
            VERIFY_NOT_REACHED();
        };

        auto const& vertical_align = fragment.layout_node().computed_values().vertical_align();
        if (vertical_align.has<CSS::VerticalAlign>()) {
            new_fragment_y = y_value_for_alignment(vertical_align.get<CSS::VerticalAlign>());
        } else {
            if (auto length_percentage = vertical_align.get_pointer<CSS::LengthPercentage>(); length_percentage && length_percentage->is_length()) {
                auto vertical_align_amount = length_percentage->length().to_px(fragment.layout_node());
                new_fragment_y = y_value_for_alignment(CSS::VerticalAlign::Baseline) - vertical_align_amount;
            }
        }

        fragment.set_offset({ new_fragment_x, floorf(new_fragment_y) });

        float top_of_inline_box = 0;
        float bottom_of_inline_box = 0;
        {
            // FIXME: Support inline-table elements.
            if (fragment.layout_node().is_replaced_box() || fragment.layout_node().is_inline_block()) {
                auto const& fragment_box_state = m_layout_state.get(static_cast<Box const&>(fragment.layout_node()));
                top_of_inline_box = fragment.offset().y() - fragment_box_state.margin_box_top();
                bottom_of_inline_box = fragment.offset().y() + fragment_box_state.content_height() + fragment_box_state.margin_box_bottom();
            } else {
                auto font_metrics = fragment.layout_node().font().pixel_metrics();
                auto typographic_height = font_metrics.ascent + font_metrics.descent;
                auto leading = fragment.layout_node().line_height() - typographic_height;
                auto half_leading = leading / 2;
                top_of_inline_box = fragment.offset().y() + fragment.baseline() - font_metrics.ascent - half_leading;
                bottom_of_inline_box = fragment.offset().y() + fragment.baseline() + font_metrics.descent + half_leading;
            }
            if (auto length_percentage = fragment.layout_node().computed_values().vertical_align().template get_pointer<CSS::LengthPercentage>(); length_percentage && length_percentage->is_length())
                bottom_of_inline_box += length_percentage->length().to_px(fragment.layout_node());
        }

        uppermost_box_top = min(uppermost_box_top, top_of_inline_box);
        lowermost_box_bottom = max(lowermost_box_bottom, bottom_of_inline_box);
    }

    // 3. The line box height is the distance between the uppermost box top and the lowermost box bottom.
    line_box.m_height = lowermost_box_bottom - uppermost_box_top;

    line_box.m_bottom = m_current_y + line_box.m_height;
    line_box.m_baseline = line_box_baseline;
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

void LineBuilder::recalculate_available_space()
{
    auto current_line_height = max(m_max_height_on_current_line, m_context.containing_block().line_height());
    auto available_at_top_of_line_box = m_context.available_space_for_line(m_current_y);
    auto available_at_bottom_of_line_box = m_context.available_space_for_line(m_current_y + current_line_height - 1);
    m_available_width_for_current_line = min(available_at_bottom_of_line_box, available_at_top_of_line_box);
}

}
