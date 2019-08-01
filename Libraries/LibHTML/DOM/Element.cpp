#include <LibHTML/DOM/Element.h>
#include <LibHTML/Layout/LayoutBlock.h>
#include <LibHTML/Layout/LayoutInline.h>

Element::Element(const String& tag_name)
    : ParentNode(NodeType::ELEMENT_NODE)
    , m_tag_name(tag_name)
{
}

Element::~Element()
{
}

Attribute* Element::find_attribute(const String& name)
{
    for (auto& attribute : m_attributes) {
        if (attribute.name() == name)
            return &attribute;
    }
    return nullptr;
}

const Attribute* Element::find_attribute(const String& name) const
{
    for (auto& attribute : m_attributes) {
        if (attribute.name() == name)
            return &attribute;
    }
    return nullptr;
}

String Element::attribute(const String& name) const
{
    if (auto* attribute = find_attribute(name))
        return attribute->value();
    return { };
}

void Element::set_attribute(const String& name, const String& value)
{
    if (auto* attribute = find_attribute(name))
        attribute->set_value(value);
    else
        m_attributes.empend(name, value);
}

void Element::set_attributes(Vector<Attribute>&& attributes)
{
    m_attributes = move(attributes);
}

bool Element::has_class(const StringView& class_name) const
{
    auto value = attribute("class");
    if (value.is_empty())
        return false;
    auto parts = value.split_view(' ');
    for (auto& part : parts) {
        if (part == class_name)
            return true;
    }
    return false;
}
