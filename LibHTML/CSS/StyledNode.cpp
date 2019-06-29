#include <LibHTML/CSS/StyledNode.h>

StyledNode::StyledNode(const Node* node)
    : m_node(node)
{
}

StyledNode::~StyledNode()
{
}

Display StyledNode::display() const
{
    auto it = m_property_values.find("display");
    if (it == m_property_values.end())
        return Display::Inline;
    auto value = it->value->to_string();
    if (value == "none")
        return Display::None;
    if (value == "block")
        return Display::Block;
    if (value == "inline")
        return Display::Inline;
    ASSERT_NOT_REACHED();
}
