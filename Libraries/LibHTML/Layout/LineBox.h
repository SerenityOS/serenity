#pragma once

#include <AK/Vector.h>
#include <LibHTML/Layout/LineBoxFragment.h>

class LineBox {
public:
    LineBox() {}

    float width() const { return m_width; }

    void add_fragment(const LayoutNode& layout_node, int start, int length, int width, int height);

    const Vector<LineBoxFragment>& fragments() const { return m_fragments; }
    Vector<LineBoxFragment>& fragments() { return m_fragments; }

    void trim_trailing_whitespace();
private:
    friend class LayoutBlock;
    Vector<LineBoxFragment> m_fragments;
    float m_width { 0 };
};
