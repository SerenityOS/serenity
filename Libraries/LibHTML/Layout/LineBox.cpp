#include <LibHTML/Layout/LineBox.h>

void LineBox::add_fragment(const LayoutNode& layout_node, int start, int length, int width, int height)
{
    if (!m_fragments.is_empty() && &m_fragments.last().layout_node() == &layout_node) {
        // The fragment we're adding is from the last LayoutNode on the line.
        // Expand the last fragment instead of adding a new one with the same LayoutNode.
        m_fragments.last().m_length = (start - m_fragments.last().m_start) + length;
        m_fragments.last().m_rect.set_width(m_fragments.last().m_rect.width() + width);
    } else {
        m_fragments.empend(layout_node, start, length, Rect(m_width, 0, width, height));
    }
    m_width += width;
}
