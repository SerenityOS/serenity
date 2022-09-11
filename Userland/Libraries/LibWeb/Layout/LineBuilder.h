/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/Layout/InlineFormattingContext.h>

namespace Web::Layout {

class LineBuilder {
    AK_MAKE_NONCOPYABLE(LineBuilder);
    AK_MAKE_NONMOVABLE(LineBuilder);

public:
    LineBuilder(InlineFormattingContext&, LayoutState&);
    ~LineBuilder();

    void break_line(Optional<float> next_item_width = {});
    void append_box(Box const&, float leading_size, float trailing_size, float leading_margin, float trailing_margin);
    void append_text_chunk(TextNode const&, size_t offset_in_node, size_t length_in_node, float leading_size, float trailing_size, float leading_margin, float trailing_margin, float content_width, float content_height);

    // Returns whether a line break occurred.
    bool break_if_needed(float next_item_width)
    {
        if (should_break(next_item_width)) {
            break_line(next_item_width);
            return true;
        }
        return false;
    }

    float available_width_for_current_line() const { return m_available_width_for_current_line; }

    void update_last_line();

    void remove_last_line_if_empty();

    float current_y() const { return m_current_y; }

    void adjust_last_line_after_inserting_floating_box(Badge<BlockFormattingContext>, CSS::Float, float space_used_by_float);

private:
    void begin_new_line(bool increment_y);

    bool should_break(float next_item_width);

    LineBox& ensure_last_line_box();

    InlineFormattingContext& m_context;
    LayoutState& m_layout_state;
    LayoutState::UsedValues& m_containing_block_state;
    float m_available_width_for_current_line { 0 };
    float m_current_y { 0 };
    float m_max_height_on_current_line { 0 };

    bool m_last_line_needs_update { false };
};

}
