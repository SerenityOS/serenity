/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Vector.h>
#include <LibWeb/Layout/LineBoxFragment.h>

namespace Web::Layout {

class LineBox {
public:
    LineBox() = default;

    float width() const { return m_width; }
    float height() const { return m_height; }
    float bottom() const { return m_bottom; }
    float baseline() const { return m_baseline; }

    void add_fragment(Node const& layout_node, int start, int length, float leading_size, float trailing_size, float leading_margin, float trailing_margin, float content_width, float content_height, float border_box_top, float border_box_bottom, LineBoxFragment::Type = LineBoxFragment::Type::Normal);

    Vector<LineBoxFragment> const& fragments() const { return m_fragments; }
    Vector<LineBoxFragment>& fragments() { return m_fragments; }

    void trim_trailing_whitespace();

    bool is_empty_or_ends_in_whitespace() const;
    bool is_empty() const { return m_fragments.is_empty(); }

private:
    friend class BlockContainer;
    friend class InlineFormattingContext;
    friend class LineBuilder;

    Vector<LineBoxFragment> m_fragments;
    float m_width { 0 };
    float m_height { 0 };
    float m_bottom { 0 };
    float m_baseline { 0 };
};

}
