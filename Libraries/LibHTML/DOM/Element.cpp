#include <LibHTML/CSS/StyleResolver.h>
#include <LibHTML/DOM/Document.h>
#include <LibHTML/DOM/Element.h>
#include <LibHTML/Layout/LayoutBlock.h>
#include <LibHTML/Layout/LayoutInline.h>
#include <LibHTML/Layout/LayoutListItem.h>
#include <LibHTML/Layout/LayoutTable.h>
#include <LibHTML/Layout/LayoutTableCell.h>
#include <LibHTML/Layout/LayoutTableRow.h>
#include <LibHTML/Layout/LayoutTreeBuilder.h>

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

RefPtr<LayoutNode> Element::create_layout_node(const StyleProperties* parent_style) const
{
    auto style = document().style_resolver().resolve_style(*this, parent_style);
    const_cast<Element&>(*this).m_resolved_style = style;
    auto display = style->string_or_fallback(CSS::PropertyID::Display, "inline");

    if (display == "none")
        return nullptr;
    if (display == "block")
        return adopt(*new LayoutBlock(this, move(style)));
    if (display == "inline")
        return adopt(*new LayoutInline(*this, move(style)));
    if (display == "list-item")
        return adopt(*new LayoutListItem(*this, move(style)));
    if (display == "table")
        return adopt(*new LayoutTable(*this, move(style)));
    if (display == "table-row")
        return adopt(*new LayoutTableRow(*this, move(style)));
    if (display == "table-cell")
        return adopt(*new LayoutTableCell(*this, move(style)));
    if (display == "inline-block")
        return adopt(*new LayoutBlock(this, move(style)));

    ASSERT_NOT_REACHED();
}

void Element::parse_attribute(const String&, const String&)
{
}

enum class StyleDifference {
    None,
    NeedsRepaint,
    NeedsRelayout,
};

static StyleDifference compute_style_difference(const StyleProperties& old_style, const StyleProperties& new_style, const Document& document)
{
    if (old_style == new_style)
        return StyleDifference::None;

    bool needs_repaint = false;
    bool needs_relayout = false;

    if (new_style.color_or_fallback(CSS::PropertyID::Color, document, Color::Black) != old_style.color_or_fallback(CSS::PropertyID::Color, document, Color::Black))
        needs_repaint = true;
    else if (new_style.color_or_fallback(CSS::PropertyID::BackgroundColor, document, Color::Black) != old_style.color_or_fallback(CSS::PropertyID::BackgroundColor, document, Color::Black))
        needs_repaint = true;

    if (needs_relayout)
        return StyleDifference::NeedsRelayout;
    if (needs_repaint)
        return StyleDifference::NeedsRepaint;
    return StyleDifference::None;
}

void Element::recompute_style()
{
    set_needs_style_update(false);
    ASSERT(parent());
    auto* parent_layout_node = parent()->layout_node();
    if (!parent_layout_node)
        return;
    ASSERT(parent_layout_node);
    auto style = document().style_resolver().resolve_style(*this, &parent_layout_node->style());
    m_resolved_style = style;
    if (!layout_node()) {
        if (style->string_or_fallback(CSS::PropertyID::Display, "inline") == "none")
            return;
        // We need a new layout tree here!
        LayoutTreeBuilder tree_builder;
        tree_builder.build(*this);
        return;
    }
    auto diff = compute_style_difference(layout_node()->style(), *style, document());
    if (diff == StyleDifference::None)
        return;
    layout_node()->set_style(*style);
    if (diff == StyleDifference::NeedsRelayout) {
        ASSERT_NOT_REACHED();
    }
    if (diff == StyleDifference::NeedsRepaint) {
        layout_node()->set_needs_display();
    }
}
