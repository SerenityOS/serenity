/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/NonnullOwnPtrVector.h>
#include <AK/Vector.h>
#include <LibWeb/Layout/LineBoxFragment.h>

namespace Web::Layout {

class LineBox {
public:
    LineBox() { }

    float width() const { return m_width; }

    void add_fragment(Node const& layout_node, int start, int length, float leading_size, float trailing_size, float content_width, float content_height, float border_box_top, float border_box_bottom, LineBoxFragment::Type = LineBoxFragment::Type::Normal);

    const NonnullOwnPtrVector<LineBoxFragment>& fragments() const { return m_fragments; }
    NonnullOwnPtrVector<LineBoxFragment>& fragments() { return m_fragments; }

    void trim_trailing_whitespace();

    bool is_empty_or_ends_in_whitespace() const;
    bool is_empty() const { return m_fragments.is_empty(); }

private:
    friend class BlockContainer;
    friend class InlineFormattingContext;
    friend class LineBuilder;

    NonnullOwnPtrVector<LineBoxFragment> m_fragments;
    float m_width { 0 };
};

}
