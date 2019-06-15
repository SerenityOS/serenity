#include <AK/Retained.h>
#include <LibHTML/Node.h>

Node::Node(NodeType type)
    : m_type(type)
{
}

Node::~Node()
{
}

void Node::retain()
{
    ASSERT(m_retain_count);
    ++m_retain_count;
}

void Node::release()
{
    ASSERT(m_retain_count);
    if (!--m_retain_count)
        delete this;
}
