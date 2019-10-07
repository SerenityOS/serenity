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

RefPtr<LayoutNode> Node::create_layout_tree(const StyleResolver& resolver, const StyleProperties* parent_style) const
{
    auto layout_node = create_layout_node(resolver, parent_style);
    if (!layout_node)
        return nullptr;

    if (!has_children())
        return layout_node;

    Vector<RefPtr<LayoutNode>> layout_children;
    bool have_inline_children = false;
    bool have_block_children = false;

    static_cast<const ParentNode&>(*this).for_each_child([&](const Node& child) {
        auto layout_child = child.create_layout_tree(resolver, &layout_node->style());
        if (!layout_child)
            return;
        if (!layout_child->is_block())
            have_inline_children = true;
        if (layout_child->is_block())
            have_block_children = true;
        layout_children.append(move(layout_child));
    });

    for (auto layout_child : layout_children)
        if (have_block_children && have_inline_children && !layout_child->is_block()) {
            if (layout_child->is_text() && static_cast<const LayoutText&>(*layout_child).text_for_style(*parent_style) == " ")
                continue;
            layout_node->inline_wrapper().append_child(*layout_child);
        } else {
            layout_node->append_child(*layout_child);
        }
    return layout_node;
}

const HTMLAnchorElement* Node::enclosing_link_element() const
{
    return first_ancestor_of_type<HTMLAnchorElement>();
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
