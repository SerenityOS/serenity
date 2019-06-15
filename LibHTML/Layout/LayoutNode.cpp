#include <LibHTML/Layout/LayoutNode.h>

LayoutNode::LayoutNode(const Node* node)
    : m_node(node)
{
}

LayoutNode::~LayoutNode()
{
}

void LayoutNode::retain()
{
    ASSERT(m_retain_count);
    ++m_retain_count;
}

void LayoutNode::release()
{
    ASSERT(m_retain_count);
    if (!--m_retain_count)
        delete this;
}

void LayoutNode::append_child(Retained<LayoutNode> node)
{
    if (m_last_child)
        m_last_child->set_next_sibling(node.ptr());
    node->m_parent_node = this;
    m_last_child = &node.leak_ref();
    if (!m_first_child)
        m_first_child = m_last_child;
}
