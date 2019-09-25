#include <LibHTML/Layout/LayoutBlock.h>
#include <LibHTML/Layout/LayoutNode.h>

LayoutNode::LayoutNode(const Node* node, StyleProperties&& style_properties)
    : m_node(node)
    , m_style_properties(style_properties)
{
}

LayoutNode::~LayoutNode()
{
}

void LayoutNode::layout()
{
    for_each_child([](auto& child) {
        child.layout();
    });
}

const LayoutBlock* LayoutNode::containing_block() const
{
    for (auto* ancestor = parent(); ancestor; ancestor = ancestor->parent()) {
        if (ancestor->is_block())
            return static_cast<const LayoutBlock*>(ancestor);
    }
    return nullptr;
}

void LayoutNode::render(RenderingContext& context)
{
    // TODO: render our background and border
    for_each_child([&](auto& child) {
        child.render(context);
    });
}
