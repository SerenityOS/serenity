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
    void begin_new_line();
    void append_box(Box&);
    void append_text_chunk(TextNode&, size_t offset_in_node, size_t length_in_node, float width, float height);

    void break_if_needed(LayoutMode, float next_item_width);

    float available_width_for_current_line() const { return m_available_width_for_current_line; }

    void update_last_line();

private:
    InlineFormattingContext& m_context;
    float m_available_width_for_current_line { 0 };
    float m_current_y { 0 };
    float m_max_height_on_current_line { 0 };
};

}
