/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
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
    if (m_start.layout_node == m_end.layout_node) {
        if (m_start.index_in_node < m_end.index_in_node)
            return *this;
        return { m_end, m_start };
    }
    if (m_start.layout_node->is_before(*m_end.layout_node))
        return *this;
    return { m_end, m_start };
}

NonnullRefPtr<DOM::Range> LayoutRange::to_dom_range() const
{
    VERIFY(is_valid());

    auto start = m_start.to_dom_position();
    auto end = m_end.to_dom_position();

    return DOM::Range::create(*start.node(), start.offset(), *end.node(), end.offset());
}

}
