#include <AK/Function.h>
#include <LibHTML/CSS/StyleResolver.h>
#include <LibHTML/CSS/StyledNode.h>
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

RefPtr<StyledNode> Frame::generate_style_tree()
{
    if (!m_document)
        return nullptr;

    auto& resolver = m_document->style_resolver();
    Function<RefPtr<StyledNode>(const Node&, StyledNode*)> resolve_style = [&](const Node& node, StyledNode* parent_styled_node) -> RefPtr<StyledNode> {
        RefPtr<StyledNode> styled_node;
        if (node.is_element())
            styled_node = resolver.create_styled_node(static_cast<const Element&>(node));
        else if (node.is_document())
            styled_node = resolver.create_styled_node(static_cast<const Document&>(node));
        if (!styled_node)
            return nullptr;
        if (parent_styled_node)
            parent_styled_node->append_child(*styled_node);
        static_cast<const ParentNode&>(node).for_each_child([&](const Node& child) {
            if (!child.is_element())
                return;
            auto styled_child_node = resolve_style(static_cast<const Element&>(child), styled_node.ptr());
            printf("Created StyledNode{%p} for Element{%p}\n", styled_child_node.ptr(), &node);
        });
        return styled_node;
    };
    auto styled_root = resolve_style(*m_document, nullptr);
    dump_tree(*styled_root);
    return styled_root;
}

void Frame::layout()
{
    if (!m_document)
        return;

    auto styled_root = generate_style_tree();

    auto create_layout_node = [](const StyledNode& styled_node) -> RefPtr<LayoutNode> {
        if (styled_node.node() && styled_node.node()->is_document())
            return adopt(*new LayoutDocument(static_cast<const Document&>(*styled_node.node())));
        switch (styled_node.display()) {
        case Display::None:
            return nullptr;
        case Display::Block:
            return adopt(*new LayoutBlock(*styled_node.node()));
        case Display::Inline:
            return adopt(*new LayoutInline(*styled_node.node()));
        default:
            ASSERT_NOT_REACHED();
        }
    };

    Function<RefPtr<LayoutNode>(const StyledNode&, LayoutNode*)> resolve_layout = [&](const StyledNode& styled_node, LayoutNode* parent_layout_node) -> RefPtr<LayoutNode> {
        auto layout_node = create_layout_node(styled_node);
        if (!layout_node)
            return nullptr;
        if (parent_layout_node)
            parent_layout_node->append_child(*layout_node);
        if (styled_node.has_children()) {
            for (auto* child = styled_node.first_child(); child; child = child->next_sibling()) {
                resolve_layout(*child, layout_node.ptr());
            }
        }
        return layout_node;
    };

    auto layout_root = resolve_layout(*styled_root, nullptr);

    layout_root->style().size().set_width(m_size.width());

    printf("\033[33;1mLayout tree before layout:\033[0m\n");
    dump_tree(*layout_root);

    layout_root->layout();

    printf("\033[33;1mLayout tree after layout:\033[0m\n");
    dump_tree(*layout_root);
}
