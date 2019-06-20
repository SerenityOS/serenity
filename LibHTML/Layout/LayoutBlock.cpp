#include <LibHTML/DOM/Element.h>
#include <LibHTML/Layout/LayoutBlock.h>

LayoutBlock::LayoutBlock(Element& element)
    : LayoutNode(&element)
{
}

LayoutBlock::~LayoutBlock()
{
}

void LayoutBlock::layout()
{
    LayoutNode::layout();


}
