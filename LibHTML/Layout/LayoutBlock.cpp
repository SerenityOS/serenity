#include <LibHTML/DOM/Element.h>
#include <LibHTML/Layout/LayoutBlock.h>

LayoutBlock::LayoutBlock(const Node& node)
    : LayoutNode(&node)
{
}

LayoutBlock::~LayoutBlock()
{
}

void LayoutBlock::layout()
{
    LayoutNode::layout();


}
