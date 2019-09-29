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

RefPtr<LayoutNode> Node::create_layout_node(const StyleResolver& resolver, const StyleProperties* parent_properties) const
{
    if (is_document())
        return adopt(*new LayoutDocument(static_cast<const Document&>(*this), {}));

    auto style_properties = resolver.resolve_style(static_cast<const Element&>(*this), parent_properties);
    auto display_property = style_properties.property("display");
    String display = display_property.has_value() ? display_property.release_value()->to_string() : "inline";

    if (is_text())
        return adopt(*new LayoutText(static_cast<const Text&>(*this), move(style_properties)));
    if (display == "none")
        return nullptr;
    if (display == "block" || display == "list-item")
        return adopt(*new LayoutBlock(this, move(style_properties)));
    if (display == "inline")
        return adopt(*new LayoutInline(*this, move(style_properties)));

    ASSERT_NOT_REACHED();
}

RefPtr<LayoutNode> Node::create_layout_tree(const StyleResolver& resolver, const StyleProperties* parent_properties) const
{
    auto layout_node = create_layout_node(resolver, parent_properties);
    if (!layout_node)
        return nullptr;

    if (!has_children())
        return layout_node;

    Vector<RefPtr<LayoutNode>> layout_children;
    bool have_inline_children = false;
    bool have_block_children = false;

    static_cast<const ParentNode&>(*this).for_each_child([&](const Node& child) {
        auto layout_child = child.create_layout_tree(resolver, &layout_node->style_properties());
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
            if (layout_child->is_text() && static_cast<const LayoutText&>(*layout_child).text() == " ")
                continue;
            layout_node->inline_wrapper().append_child(*layout_child);
        } else {
            layout_node->append_child(*layout_child);
        }
    return layout_node;
}

const HTMLAnchorElement* Node::enclosing_link_element() const
{
    if (is_element() && tag_name().to_lowercase() == "a")
        return static_cast<const HTMLAnchorElement*>(this);
    return parent() ? parent()->enclosing_link_element() : nullptr;
}

const HTMLElement* Node::enclosing_html_element() const
{
    if (is_html_element())
        return static_cast<const HTMLElement*>(this);
    return parent() ? parent()->enclosing_html_element() : nullptr;
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
