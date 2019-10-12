#include <LibHTML/Layout/LayoutBlock.h>
#include <LibHTML/Layout/LayoutBreak.h>

LayoutBreak::LayoutBreak(const HTMLBRElement& element)
    : LayoutNode(&element)
{
    set_inline(true);
}

LayoutBreak::~LayoutBreak()
{
}

void LayoutBreak::split_into_lines(LayoutBlock& block)
{
    block.line_boxes().append(LineBox());
}
