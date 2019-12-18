#include <AK/StringBuilder.h>
#include <LibHTML/CSS/StyleResolver.h>
#include <LibHTML/DOM/Element.h>
#include <LibHTML/DOM/HTMLAnchorElement.h>
#include <LibHTML/DOM/Node.h>
#include <LibHTML/Layout/LayoutBlock.h>
#include <LibHTML/Layout/LayoutDocument.h>
#include <LibHTML/Layout/LayoutInline.h>
#include <LibHTML/Layout/LayoutNode.h>
#include <LibHTML/Layout/LayoutText.h>

Node::Node(Document& document, NodeType type)
    : m_document(document)
    , m_type(type)
{
}

Node::~Node()
{
}

const HTMLAnchorElement* Node::enclosing_link_element() const
{
    for (auto* node = this; node; node = node->parent()) {
        if (is<HTMLAnchorElement>(*node) && to<HTMLAnchorElement>(*node).has_attribute("href"))
            return to<HTMLAnchorElement>(node);
    }
    return nullptr;
}

const HTMLElement* Node::enclosing_html_element() const
{
    return first_ancestor_of_type<HTMLElement>();
}

String Node::text_content() const
{
    Vector<String> strings;
    StringBuilder builder;
    for (auto* child = first_child(); child; child = child->next_sibling()) {
        auto text = child->text_content();
        if (!text.is_empty()) {
            builder.append(child->text_content());
            builder.append(' ');
        }
    }
    if (builder.length() > 1)
        builder.trim(1);
    return builder.to_string();
}

const Element* Node::next_element_sibling() const
{
    for (auto* node = next_sibling(); node; node = node->next_sibling()) {
        if (node->is_element())
            return static_cast<const Element*>(node);
    }
    return nullptr;
}

const Element* Node::previous_element_sibling() const
{
    for (auto* node = previous_sibling(); node; node = node->previous_sibling()) {
        if (node->is_element())
            return static_cast<const Element*>(node);
    }
    return nullptr;
}

RefPtr<LayoutNode> Node::create_layout_node(const StyleProperties*) const
{
    return nullptr;
}

void Node::invalidate_style()
{
    for_each_in_subtree_of_type<Element>([&](auto& element) {
        element.set_needs_style_update(true);
        return IterationDecision::Continue;
    });
    document().schedule_style_update();
}

bool Node::is_link() const
{
    auto* enclosing_link = enclosing_link_element();
    if (!enclosing_link)
        return false;
    return enclosing_link->has_attribute("href");
}
