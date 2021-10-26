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

    void add_fragment(Node& layout_node, int start, int length, float width, float height, LineBoxFragment::Type = LineBoxFragment::Type::Normal);

    const NonnullOwnPtrVector<LineBoxFragment>& fragments() const { return m_fragments; }
    NonnullOwnPtrVector<LineBoxFragment>& fragments() { return m_fragments; }

    void trim_trailing_whitespace();

    bool is_empty_or_ends_in_whitespace() const;
    bool ends_with_forced_line_break() const;

private:
    friend class BlockContainer;
    friend class InlineFormattingContext;
    NonnullOwnPtrVector<LineBoxFragment> m_fragments;
    float m_width { 0 };
};

}
