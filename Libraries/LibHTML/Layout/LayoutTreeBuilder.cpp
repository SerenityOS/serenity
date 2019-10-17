#include <LibHTML/DOM/Document.h>
#include <LibHTML/DOM/ParentNode.h>
#include <LibHTML/Layout/LayoutNode.h>
#include <LibHTML/Layout/LayoutTable.h>
#include <LibHTML/Layout/LayoutText.h>
#include <LibHTML/Layout/LayoutTreeBuilder.h>

LayoutTreeBuilder::LayoutTreeBuilder()
{
}

static RefPtr<LayoutNode> create_layout_tree(Node& node, const StyleProperties* parent_style)
{
    auto layout_node = node.create_layout_node(parent_style);
    if (!layout_node)
        return nullptr;

    if (!node.has_children())
        return layout_node;

    NonnullRefPtrVector<LayoutNode> layout_children;
    bool have_inline_children = false;
    bool have_block_children = false;

    to<ParentNode>(node).for_each_child([&](Node& child) {
        auto layout_child = create_layout_tree(child, &layout_node->style());
        if (!layout_child)
            return;
        if (layout_child->is_inline())
            have_inline_children = true;
        if (layout_child->is_block())
            have_block_children = true;
        layout_children.append(layout_child.release_nonnull());
    });

    for (auto& layout_child : layout_children) {
        if (have_block_children && have_inline_children && layout_child.is_inline()) {
            if (is<LayoutText>(layout_child) && to<LayoutText>(layout_child).text_for_style(*parent_style) == " ")
                continue;
            layout_node->inline_wrapper().append_child(layout_child);
        } else {
            layout_node->append_child(layout_child);
        }
    }

    if (have_inline_children && !have_block_children)
        layout_node->set_children_are_inline(true);

    // FIXME: This is really hackish. Some layout nodes don't care about inline children.
    if (is<LayoutTable>(layout_node))
        layout_node->set_children_are_inline(false);

    return layout_node;
}

RefPtr<LayoutNode> LayoutTreeBuilder::build(Node& node)
{
    // FIXME: Support building partial trees.
    ASSERT(is<Document>(node));
    return create_layout_tree(node, nullptr);
}
