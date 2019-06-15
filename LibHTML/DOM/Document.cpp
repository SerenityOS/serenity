#include <LibHTML/DOM/Document.h>
#include <LibHTML/DOM/Element.h>
#include <LibHTML/Layout/LayoutDocument.h>
#include <stdio.h>

Document::Document()
    : ParentNode(NodeType::DOCUMENT_NODE)
{
}

Document::~Document()
{
}

static void create_layout_tree_for_node(Node& node)
{
    if (auto layout_node = node.create_layout_node()) {
        node.set_layout_node(*layout_node);
#ifdef DEBUG_LAYOUT_TREE_BUILD
        if (node.is_element()) {
            printf("created layout node for <%s>, parent is %p, parent ln is %p\n", static_cast<const Element&>(node).tag_name().characters(), node.parent_node(), node.parent_node()->layout_node());
        }
#endif
        if (node.parent_node() && node.parent_node()->layout_node())
            node.parent_node()->layout_node()->append_child(*layout_node);
    }
    if (node.is_parent_node()) {
        static_cast<ParentNode&>(node).for_each_child([&](auto& child) {
            create_layout_tree_for_node(child);
        });
    }
}

void Document::build_layout_tree()
{
    create_layout_tree_for_node(*this);
}

RetainPtr<LayoutNode> Document::create_layout_node()
{
    return adopt(*new LayoutDocument(*this));
}
