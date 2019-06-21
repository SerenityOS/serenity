#include <LibHTML/DOM/Node.h>
#include <LibHTML/Layout/LayoutNode.h>

Node::Node(NodeType type)
    : m_type(type)
{
}

Node::~Node()
{
}

void Node::ref()
{
    ASSERT(m_retain_count);
    ++m_retain_count;
}

void Node::deref()
{
    ASSERT(m_retain_count);
    if (!--m_retain_count)
        delete this;
}

RetainPtr<LayoutNode> Node::create_layout_node()
{
    return nullptr;
}

void Node::set_layout_node(Retained<LayoutNode> layout_node)
{
    m_layout_node = move(layout_node);
}
