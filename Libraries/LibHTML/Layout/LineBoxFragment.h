#pragma once

#include <LibDraw/Rect.h>

class LayoutNode;
class RenderingContext;

class LineBoxFragment {
    friend class LineBox;
public:
    LineBoxFragment(const LayoutNode& layout_node, int start, int length, const Rect& rect)
        : m_layout_node(layout_node)
        , m_start(start)
        , m_length(length)
        , m_rect(rect)
    {
    }

    const LayoutNode& layout_node() const { return m_layout_node; }
    int start() const { return m_start; }
    int length() const { return m_length; }
    const Rect& rect() const { return m_rect; }
    Rect& rect() { return m_rect; }

    void render(RenderingContext&);

private:
    const LayoutNode& m_layout_node;
    int m_start { 0 };
    int m_length { 0 };
    Rect m_rect;
};
