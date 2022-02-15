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
    explicit LineBuilder(InlineFormattingContext&);
    ~LineBuilder();

    void break_line();
    void append_box(Box&, float leading_size, float trailing_size);
    void append_text_chunk(TextNode&, size_t offset_in_node, size_t length_in_node, float leading_size, float trailing_size, float content_width, float content_height);

    void break_if_needed(LayoutMode layout_mode, float next_item_width, bool should_force_break)
    {
        if (should_break(layout_mode, next_item_width, should_force_break))
            break_line();
    }

    float available_width_for_current_line() const { return m_available_width_for_current_line; }

    void update_last_line();

    void remove_last_line_if_empty();

private:
    void begin_new_line(bool increment_y);

    bool should_break(LayoutMode, float next_item_width, bool should_force_break);

    InlineFormattingContext& m_context;
    float m_available_width_for_current_line { 0 };
    float m_current_y { 0 };
    float m_max_height_on_current_line { 0 };

    bool m_last_line_needs_update { false };
};

}
