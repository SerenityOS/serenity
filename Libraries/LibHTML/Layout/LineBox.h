#pragma once

#include <AK/Vector.h>
#include <LibHTML/Layout/LineBoxFragment.h>

class LineBox {
public:
    LineBox() {}

    int width() const { return m_width; }

    void add_fragment(const LayoutNode& layout_node, int start, int length, int width, int height);

    const Vector<LineBoxFragment>& fragments() const { return m_fragments; }
    Vector<LineBoxFragment>& fragments() { return m_fragments; }

private:
    Vector<LineBoxFragment> m_fragments;
    int m_width { 0 };
};
