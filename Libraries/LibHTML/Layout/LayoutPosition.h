#pragma once

#include <AK/RefPtr.h>

class LayoutNode;

struct LayoutPosition {
    bool operator>=(const LayoutPosition& other) const
    {
        if (layout_node == other.layout_node)
            return index_in_node >= other.index_in_node;

        // FIXME: Implement.
        return true;
    }

    bool operator<=(const LayoutPosition& other) const
    {
        if (layout_node == other.layout_node)
            return index_in_node <= other.index_in_node;

        // FIXME: Implement.
        return false;
    }

    RefPtr<LayoutNode> layout_node;
    int index_in_node { 0 };
};

class LayoutRange {
public:
    LayoutRange() {}
    LayoutRange(const LayoutPosition& start, const LayoutPosition& end)
        : m_start(start)
        , m_end(end)
    {
    }

    bool is_valid() const { return m_start.layout_node && m_end.layout_node; }

    void set(const LayoutPosition& start, const LayoutPosition& end)
    {
        m_start = start;
        m_end = end;
    }

    void set_start(const LayoutPosition& start) { m_start = start; }
    void set_end(const LayoutPosition& end) { m_end = end; }

    const LayoutPosition& start() const { return m_start; }
    const LayoutPosition& end() const { return m_end; }

private:
    LayoutPosition m_start;
    LayoutPosition m_end;
};
