/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGfx/Forward.h>
#include <LibGfx/Rect.h>
#include <LibWeb/Forward.h>

namespace Web::Layout {

class LineBoxFragment {
    friend class LineBox;

public:
    enum class Type {
        Normal,
        Leading,
        Trailing,
    };

    LineBoxFragment(Node const& layout_node, int start, int length, Gfx::FloatPoint const& offset, Gfx::FloatSize const& size, float border_box_top, float border_box_bottom, Type type)
        : m_layout_node(layout_node)
        , m_start(start)
        , m_length(length)
        , m_offset(offset)
        , m_size(size)
        , m_border_box_top(border_box_top)
        , m_border_box_bottom(border_box_bottom)
        , m_type(type)
    {
    }

    Node const& layout_node() const { return m_layout_node; }
    int start() const { return m_start; }
    int length() const { return m_length; }
    const Gfx::FloatRect absolute_rect() const;
    Type type() const { return m_type; }

    Gfx::FloatPoint const& offset() const { return m_offset; }
    void set_offset(Gfx::FloatPoint const& offset) { m_offset = offset; }

    // The baseline of a fragment is the number of pixels from the top to the text baseline.
    void set_baseline(float y) { m_baseline = y; }
    float baseline() const { return m_baseline; }

    Gfx::FloatSize const& size() const { return m_size; }
    void set_width(float width) { m_size.set_width(width); }
    void set_height(float height) { m_size.set_height(height); }
    float width() const { return m_size.width(); }
    float height() const { return m_size.height(); }

    float border_box_height() const { return m_border_box_top + height() + m_border_box_bottom; }
    float border_box_top() const { return m_border_box_top; }
    float border_box_bottom() const { return m_border_box_bottom; }

    float absolute_x() const { return absolute_rect().x(); }

    bool ends_in_whitespace() const;
    bool is_justifiable_whitespace() const;
    StringView text() const;

    int text_index_at(float x) const;

    Gfx::FloatRect selection_rect(Gfx::Font const&) const;

    float height_of_inline_level_box(FormattingState const&) const;
    float top_of_inline_level_box(FormattingState const&) const;
    float bottom_of_inline_level_box(FormattingState const&) const;

private:
    Node const& m_layout_node;
    int m_start { 0 };
    int m_length { 0 };
    Gfx::FloatPoint m_offset;
    Gfx::FloatSize m_size;
    float m_border_box_top { 0 };
    float m_border_box_bottom { 0 };
    float m_baseline { 0 };
    Type m_type { Type::Normal };
};

}
