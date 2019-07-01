#include <LibHTML/Layout/LayoutBlock.h>
#include <LibHTML/Layout/LayoutNode.h>

LayoutNode::LayoutNode(const Node* node)
    : m_node(node)
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
