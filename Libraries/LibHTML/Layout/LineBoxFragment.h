#pragma once

#include <LibDraw/FloatRect.h>

class LayoutNode;
class RenderingContext;

class LineBoxFragment {
    friend class LineBox;
public:
    LineBoxFragment(const LayoutNode& layout_node, int start, int length, const FloatRect& rect)
        : m_layout_node(layout_node)
        , m_start(start)
        , m_length(length)
        , m_rect(rect)
    {
    }

    const LayoutNode& layout_node() const { return m_layout_node; }
    int start() const { return m_start; }
    int length() const { return m_length; }
    const FloatRect& rect() const { return m_rect; }
    FloatRect& rect() { return m_rect; }

    float width() const { return m_rect.width(); }

    void render(RenderingContext&);

    bool is_justifiable_whitespace() const;
    StringView text() const;

private:
    const LayoutNode& m_layout_node;
    int m_start { 0 };
    int m_length { 0 };
    FloatRect m_rect;
};
