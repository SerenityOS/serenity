#include <LibHTML/DOM/Node.h>
#include <LibHTML/Layout/LayoutNode.h>

Node::Node(NodeType type)
    : m_type(type)
{
}

Node::~Node()
{
}

RefPtr<LayoutNode> Node::create_layout_node()
{
    return nullptr;
}

void Node::set_layout_node(NonnullRefPtr<LayoutNode> layout_node)
{
    m_layout_node = move(layout_node);
}
