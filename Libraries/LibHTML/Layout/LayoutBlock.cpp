#include <LibHTML/DOM/Element.h>
#include <LibHTML/Layout/LayoutBlock.h>

LayoutBlock::LayoutBlock(const Node* node, const StyledNode* styled_node)
    : LayoutNode(node, styled_node)
{
}

LayoutBlock::~LayoutBlock()
{
}

LayoutNode& LayoutBlock::inline_wrapper()
{
    if (!last_child() || !last_child()->is_block()) {
        append_child(adopt(*new LayoutBlock(nullptr, nullptr)));
    }
    return *last_child();
}

void LayoutBlock::layout()
{
    compute_width();

    LayoutNode::layout();

    compute_height();
}

void LayoutBlock::compute_width()
{
}

void LayoutBlock::compute_height()
{
}
