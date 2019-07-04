#include <LibHTML/DOM/Element.h>
#include <LibHTML/Layout/LayoutBlock.h>

LayoutBlock::LayoutBlock(const Node& node, const StyledNode& styled_node)
    : LayoutNode(&node, styled_node)
{
}

LayoutBlock::~LayoutBlock()
{
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
