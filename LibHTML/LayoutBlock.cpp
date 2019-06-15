#include <LibHTML/Element.h>
#include <LibHTML/LayoutBlock.h>

LayoutBlock::LayoutBlock(Element& element)
    : LayoutNode(&element)
{
}

LayoutBlock::~LayoutBlock()
{
}
