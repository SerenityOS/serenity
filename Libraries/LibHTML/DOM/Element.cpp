#include <LibHTML/CSS/StyleResolver.h>
#include <LibHTML/DOM/Element.h>
#include <LibHTML/Layout/LayoutBlock.h>
#include <LibHTML/Layout/LayoutInline.h>

Element::Element(Document& document, const String& tag_name)
    : ParentNode(document, NodeType::ELEMENT_NODE)
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
    return {};
}

void Element::set_attribute(const String& name, const String& value)
{
    if (auto* attribute = find_attribute(name))
        attribute->set_value(value);
    else
        m_attributes.empend(name, value);

    parse_attribute(name, value);
}

void Element::set_attributes(Vector<Attribute>&& attributes)
{
    m_attributes = move(attributes);

    for (auto& attribute : m_attributes)
        parse_attribute(attribute.name(), attribute.value());
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

RefPtr<LayoutNode> Element::create_layout_node(const StyleResolver& resolver, const StyleProperties* parent_style) const
{
    auto style = resolver.resolve_style(*this, parent_style);

    auto display_property = style->property(CSS::PropertyID::Display);
    String display = display_property.has_value() ? display_property.release_value()->to_string() : "inline";

    if (display == "none")
        return nullptr;
    if (display == "block" || display == "list-item")
        return adopt(*new LayoutBlock(this, move(style)));
    if (display == "inline")
        return adopt(*new LayoutInline(*this, move(style)));

    ASSERT_NOT_REACHED();
}

void Element::parse_attribute(const String&, const String&)
{
}
