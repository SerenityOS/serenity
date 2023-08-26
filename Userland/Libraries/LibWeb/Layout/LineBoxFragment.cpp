/*
 * Copyright (c) 2018-2023, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Utf8View.h>
#include <LibWeb/DOM/Range.h>
#include <LibWeb/Layout/LayoutState.h>
#include <LibWeb/Layout/LineBoxFragment.h>
#include <LibWeb/Layout/TextNode.h>
#include <LibWeb/Layout/Viewport.h>
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

CSSPixelRect const LineBoxFragment::absolute_rect() const
{
    CSSPixelRect rect { {}, size() };
    rect.set_location(m_layout_node->containing_block()->paintable_box()->absolute_position());
    rect.translate_by(offset());
    return rect;
}

int LineBoxFragment::text_index_at(CSSPixels x) const
{
    if (!is<TextNode>(layout_node()))
        return 0;
    auto& layout_text = verify_cast<TextNode>(layout_node());
    auto& font = layout_text.font();
    Utf8View view(text());

    CSSPixels relative_x = x - absolute_x();
    CSSPixels glyph_spacing = CSSPixels(font.glyph_spacing());

    if (relative_x < 0)
        return 0;

    CSSPixels width_so_far = 0;
    for (auto it = view.begin(); it != view.end(); ++it) {
        auto previous_it = it;
        CSSPixels glyph_width = CSSPixels(font.glyph_or_emoji_width(it));

        if ((width_so_far + glyph_width + glyph_spacing / 2) > relative_x)
            return m_start + view.byte_offset_of(previous_it);

        width_so_far += glyph_width + glyph_spacing;
    }

    return m_start + m_length;
}

CSSPixelRect LineBoxFragment::selection_rect(Gfx::Font const& font) const
{
    if (layout_node().selection_state() == Node::SelectionState::None)
        return {};

    if (layout_node().selection_state() == Node::SelectionState::Full)
        return absolute_rect();

    if (!is<TextNode>(layout_node()))
        return {};

    auto selection = layout_node().root().selection();
    if (!selection)
        return {};
    auto range = selection->range();
    if (!range)
        return {};

    // FIXME: m_start and m_length should be unsigned and then we won't need these casts.
    auto const start_index = static_cast<unsigned>(m_start);
    auto const end_index = static_cast<unsigned>(m_start) + static_cast<unsigned>(m_length);
    auto text = this->text();

    if (layout_node().selection_state() == Node::SelectionState::StartAndEnd) {
        // we are in the start/end node (both the same)
        if (start_index > range->end_offset())
            return {};
        if (end_index < range->start_offset())
            return {};

        if (range->start_offset() == range->end_offset())
            return {};

        auto selection_start_in_this_fragment = max(0, range->start_offset() - m_start);
        auto selection_end_in_this_fragment = min(m_length, range->end_offset() - m_start);
        auto pixel_distance_to_first_selected_character = CSSPixels(font.width(text.substring_view(0, selection_start_in_this_fragment)));
        auto pixel_width_of_selection = CSSPixels(font.width(text.substring_view(selection_start_in_this_fragment, selection_end_in_this_fragment - selection_start_in_this_fragment))) + 1;

        auto rect = absolute_rect();
        rect.set_x(rect.x() + pixel_distance_to_first_selected_character);
        rect.set_width(pixel_width_of_selection);

        return rect;
    }
    if (layout_node().selection_state() == Node::SelectionState::Start) {
        // we are in the start node
        if (end_index < range->start_offset())
            return {};

        auto selection_start_in_this_fragment = max(0, range->start_offset() - m_start);
        auto selection_end_in_this_fragment = m_length;
        auto pixel_distance_to_first_selected_character = CSSPixels(font.width(text.substring_view(0, selection_start_in_this_fragment)));
        auto pixel_width_of_selection = CSSPixels(font.width(text.substring_view(selection_start_in_this_fragment, selection_end_in_this_fragment - selection_start_in_this_fragment))) + 1;

        auto rect = absolute_rect();
        rect.set_x(rect.x() + pixel_distance_to_first_selected_character);
        rect.set_width(pixel_width_of_selection);

        return rect;
    }
    if (layout_node().selection_state() == Node::SelectionState::End) {
        // we are in the end node
        if (start_index > range->end_offset())
            return {};

        auto selection_start_in_this_fragment = 0;
        auto selection_end_in_this_fragment = min(range->end_offset() - m_start, m_length);
        auto pixel_distance_to_first_selected_character = CSSPixels(font.width(text.substring_view(0, selection_start_in_this_fragment)));
        auto pixel_width_of_selection = CSSPixels(font.width(text.substring_view(selection_start_in_this_fragment, selection_end_in_this_fragment - selection_start_in_this_fragment))) + 1;

        auto rect = absolute_rect();
        rect.set_x(rect.x() + pixel_distance_to_first_selected_character);
        rect.set_width(pixel_width_of_selection);

        return rect;
    }
    return {};
}

bool LineBoxFragment::is_atomic_inline() const
{
    return layout_node().is_replaced_box() || (layout_node().display().is_inline_outside() && !layout_node().display().is_flow_inside());
}

}
