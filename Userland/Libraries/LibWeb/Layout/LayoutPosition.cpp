/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/DOM/Position.h>
#include <LibWeb/DOM/Range.h>
#include <LibWeb/Layout/LayoutPosition.h>

namespace Web::Layout {

DOM::Position LayoutPosition::to_dom_position() const
{
    if (!layout_node)
        return {};

    // FIXME: Verify that there are no shenanigans going on.
    return { const_cast<DOM::Node&>(*layout_node->dom_node()), (unsigned)index_in_node };
}

LayoutRange LayoutRange::normalized() const
{
    if (!is_valid())
        return {};
    if (m_start.layout_node.ptr() == m_end.layout_node.ptr()) {
        if (m_start.index_in_node < m_end.index_in_node)
            return *this;
        return { m_end, m_start };
    }
    if (m_start.layout_node->is_before(*m_end.layout_node))
        return *this;
    return { m_end, m_start };
}

JS::NonnullGCPtr<DOM::Range> LayoutRange::to_dom_range() const
{
    VERIFY(is_valid());

    auto start = m_start.to_dom_position();
    auto end = m_end.to_dom_position();

    return DOM::Range::create(*start.node(), start.offset(), *end.node(), end.offset());
}

}
