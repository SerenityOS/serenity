/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Utf8View.h>
#include <LibWeb/Layout/FormattingState.h>
#include <LibWeb/Layout/InitialContainingBlock.h>
#include <LibWeb/Layout/LineBoxFragment.h>
#include <LibWeb/Layout/TextNode.h>
#include <ctype.h>

namespace Web::Layout {

bool LineBoxFragment::ends_in_whitespace() const
{
    auto text = this->text();
    if (text.is_empty())
        return false;
    return isspace(text[text.length() - 1]);
}

bool LineBoxFragment::is_justifiable_whitespace() const
{
    return text() == " ";
}

StringView LineBoxFragment::text() const
{
    if (!is<TextNode>(layout_node()))
        return {};
    return verify_cast<TextNode>(layout_node()).text_for_rendering().substring_view(m_start, m_length);
}

const Gfx::FloatRect LineBoxFragment::absolute_rect() const
{
    Gfx::FloatRect rect { {}, size() };
    rect.set_location(m_layout_node.containing_block()->paint_box()->absolute_position());
    rect.translate_by(offset());
    return rect;
}

int LineBoxFragment::text_index_at(float x) const
{
    if (!is<TextNode>(layout_node()))
        return 0;
    auto& layout_text = verify_cast<TextNode>(layout_node());
    auto& font = layout_text.font();
    Utf8View view(text());

    float relative_x = x - absolute_x();
    float glyph_spacing = font.glyph_spacing();

    if (relative_x < 0)
        return 0;

    float width_so_far = 0;
    for (auto it = view.begin(); it != view.end(); ++it) {
        float glyph_width = font.glyph_or_emoji_width(*it);
        if ((width_so_far + (glyph_width + glyph_spacing) / 2) > relative_x)
            return m_start + view.byte_offset_of(it);
        width_so_far += glyph_width + glyph_spacing;
    }
    return m_start + m_length;
}

Gfx::FloatRect LineBoxFragment::selection_rect(const Gfx::Font& font) const
{
    if (layout_node().selection_state() == Node::SelectionState::None)
        return {};

    if (layout_node().selection_state() == Node::SelectionState::Full)
        return absolute_rect();

    auto selection = layout_node().root().selection().normalized();
    if (!selection.is_valid())
        return {};
    if (!is<TextNode>(layout_node()))
        return {};

    const auto start_index = m_start;
    const auto end_index = m_start + m_length;
    auto text = this->text();

    if (layout_node().selection_state() == Node::SelectionState::StartAndEnd) {
        // we are in the start/end node (both the same)
        if (start_index > selection.end().index_in_node)
            return {};
        if (end_index < selection.start().index_in_node)
            return {};

        if (selection.start().index_in_node == selection.end().index_in_node)
            return {};

        auto selection_start_in_this_fragment = max(0, selection.start().index_in_node - m_start);
        auto selection_end_in_this_fragment = min(m_length, selection.end().index_in_node - m_start);
        auto pixel_distance_to_first_selected_character = font.width(text.substring_view(0, selection_start_in_this_fragment));
        auto pixel_width_of_selection = font.width(text.substring_view(selection_start_in_this_fragment, selection_end_in_this_fragment - selection_start_in_this_fragment)) + 1;

        auto rect = absolute_rect();
        rect.set_x(rect.x() + pixel_distance_to_first_selected_character);
        rect.set_width(pixel_width_of_selection);

        return rect;
    }
    if (layout_node().selection_state() == Node::SelectionState::Start) {
        // we are in the start node
        if (end_index < selection.start().index_in_node)
            return {};

        auto selection_start_in_this_fragment = max(0, selection.start().index_in_node - m_start);
        auto selection_end_in_this_fragment = m_length;
        auto pixel_distance_to_first_selected_character = font.width(text.substring_view(0, selection_start_in_this_fragment));
        auto pixel_width_of_selection = font.width(text.substring_view(selection_start_in_this_fragment, selection_end_in_this_fragment - selection_start_in_this_fragment)) + 1;

        auto rect = absolute_rect();
        rect.set_x(rect.x() + pixel_distance_to_first_selected_character);
        rect.set_width(pixel_width_of_selection);

        return rect;
    }
    if (layout_node().selection_state() == Node::SelectionState::End) {
        // we are in the end node
        if (start_index > selection.end().index_in_node)
            return {};

        auto selection_start_in_this_fragment = 0;
        auto selection_end_in_this_fragment = min(selection.end().index_in_node - m_start, m_length);
        auto pixel_distance_to_first_selected_character = font.width(text.substring_view(0, selection_start_in_this_fragment));
        auto pixel_width_of_selection = font.width(text.substring_view(selection_start_in_this_fragment, selection_end_in_this_fragment - selection_start_in_this_fragment)) + 1;

        auto rect = absolute_rect();
        rect.set_x(rect.x() + pixel_distance_to_first_selected_character);
        rect.set_width(pixel_width_of_selection);

        return rect;
    }
    return {};
}

float LineBoxFragment::height_of_inline_level_box(FormattingState const& state) const
{
    auto height = [&] {
        // From "10.8 Line height calculations: the 'line-height' and 'vertical-align' properties"
        // https://www.w3.org/TR/CSS22/visudet.html#line-height

        // For replaced elements, inline-block elements, and inline-table elements, this is the height of their margin box.
        // FIXME: Support inline-table elements.
        if (layout_node().is_replaced_box() || layout_node().is_inline_block()) {
            auto const& fragment_box_state = state.get(static_cast<Box const&>(layout_node()));
            return fragment_box_state.margin_box_height();
        }
        // For inline boxes, this is their 'line-height'.
        return layout_node().line_height();
    }();
    if (auto length_percentage = layout_node().computed_values().vertical_align().get_pointer<CSS::LengthPercentage>(); length_percentage && length_percentage->is_length())
        height += length_percentage->length().to_px(layout_node());
    return height;
}

float LineBoxFragment::top_of_inline_level_box(FormattingState const& state) const
{
    // FIXME: Support inline-table elements.
    if (layout_node().is_replaced_box() || layout_node().is_inline_block()) {
        auto const& fragment_box_state = state.get(static_cast<Box const&>(layout_node()));
        return m_offset.y() - fragment_box_state.margin_box_top();
    }
    return m_offset.y() - (layout_node().line_height() - layout_node().computed_values().font_size()) / 2;
}

float LineBoxFragment::bottom_of_inline_level_box(FormattingState const& state) const
{
    auto bottom = [&] {
        // FIXME: Support inline-table elements.
        if (layout_node().is_replaced_box() || layout_node().is_inline_block()) {
            auto const& fragment_box_state = state.get(static_cast<Box const&>(layout_node()));
            return m_offset.y() + fragment_box_state.content_height + fragment_box_state.margin_box_bottom();
        }
        return m_offset.y() + (layout_node().line_height() - layout_node().computed_values().font_size()) / 2;
    }();
    if (auto length_percentage = layout_node().computed_values().vertical_align().get_pointer<CSS::LengthPercentage>(); length_percentage && length_percentage->is_length())
        bottom += length_percentage->length().to_px(layout_node());
    return bottom;
}

}
