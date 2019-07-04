#include <LibHTML/DOM/Node.h>
#include <LibHTML/Layout/LayoutNode.h>

Node::Node(NodeType type)
    : m_type(type)
{
}

Node::~Node()
{
}
