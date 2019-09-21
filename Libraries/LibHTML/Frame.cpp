#include <AK/Function.h>
#include <LibHTML/CSS/StyleResolver.h>
#include <LibHTML/DOM/Element.h>
#include <LibHTML/Dump.h>
#include <LibHTML/Frame.h>
#include <LibHTML/Layout/LayoutBlock.h>
#include <LibHTML/Layout/LayoutDocument.h>
#include <LibHTML/Layout/LayoutInline.h>
#include <stdio.h>

Frame::Frame()
    : m_size(800, 600)
{
}

Frame::~Frame()
{
}

void Frame::set_document(Document* document)
{
    m_document = document;
}

RefPtr<LayoutNode> Frame::generate_layout_tree()
{
    auto resolver = m_document->style_resolver();
    auto create_layout_node = [&](const Node& node) -> RefPtr<LayoutNode> {
        if (node.is_document())
            return adopt(*new LayoutDocument(static_cast<const Document&>(node), {}));

        auto style_properties = resolver.resolve_style(static_cast<const Element&>(node));
        auto display_property = style_properties.property("display");
        String display = display_property.has_value() ? display_property.release_value()->to_string() : "inline";

        if (display == "none")
            return nullptr;
        if (display == "block")
            return adopt(*new LayoutBlock(&node, move(style_properties)));
        if (display == "inline")
            return adopt(*new LayoutInline(node, move(style_properties)));

        ASSERT_NOT_REACHED();
    };

    Function<RefPtr<LayoutNode>(const Node&)> build_layout_tree;
    build_layout_tree = [&](const Node& node) -> RefPtr<LayoutNode> {
        auto layout_node = create_layout_node(node);
        if (!layout_node)
            return nullptr;
        if (!node.has_children())
            return layout_node;
        static_cast<const ParentNode&>(node).for_each_child([&](const Node& child) {
            auto layout_child = build_layout_tree(child);
            if (!layout_child)
                return;
            if (layout_child->is_inline())
                layout_node->inline_wrapper().append_child(*layout_child);
            else
                layout_node->append_child(*layout_child);
        });
        return layout_node;
    };
    return build_layout_tree(*m_document);
}

void Frame::layout()
{
    if (!m_document)
        return;

    auto layout_root = generate_layout_tree();

    layout_root->style().size().set_width(m_size.width());

    printf("\033[33;1mLayout tree before layout:\033[0m\n");
    dump_tree(*layout_root);

    layout_root->layout();

    printf("\033[33;1mLayout tree after layout:\033[0m\n");
    dump_tree(*layout_root);
}
