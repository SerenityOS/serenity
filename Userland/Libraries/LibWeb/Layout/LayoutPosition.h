/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/RefPtr.h>
#include <LibJS/Heap/GCPtr.h>
#include <LibWeb/Forward.h>
#include <LibWeb/Layout/Node.h>

namespace Web::Layout {

class Node;

struct LayoutPosition {
    RefPtr<Node> layout_node;
    int index_in_node { 0 };

    DOM::Position to_dom_position() const;
};

class LayoutRange {
public:
    LayoutRange() = default;
    LayoutRange(LayoutPosition const& start, LayoutPosition const& end)
        : m_start(start)
        , m_end(end)
    {
    }

    bool is_valid() const { return m_start.layout_node && m_end.layout_node; }

    void set(LayoutPosition const& start, LayoutPosition const& end)
    {
        m_start = start;
        m_end = end;
    }

    void set_start(LayoutPosition const& start) { m_start = start; }
    void set_end(LayoutPosition const& end) { m_end = end; }

    LayoutPosition const& start() const { return m_start; }
    LayoutPosition& start() { return m_start; }
    LayoutPosition const& end() const { return m_end; }
    LayoutPosition& end() { return m_end; }

    LayoutRange normalized() const;

    JS::NonnullGCPtr<DOM::Range> to_dom_range() const;

private:
    LayoutPosition m_start;
    LayoutPosition m_end;
};

}
