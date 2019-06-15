#include <LibHTML/Element.h>

Element::Element(const String& tag_name)
    : ParentNode(NodeType::ELEMENT_NODE)
    , m_tag_name(tag_name)
{
}

Element::~Element()
{
}

