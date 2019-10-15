#include <LibHTML/Layout/LayoutBlock.h>
#include <LibHTML/Layout/LayoutBreak.h>

LayoutBreak::LayoutBreak(const HTMLBRElement& element)
    : LayoutNodeWithStyleAndBoxModelMetrics(&element, StyleProperties::create())
{
    set_inline(true);
}

LayoutBreak::~LayoutBreak()
{
}

void LayoutBreak::split_into_lines(LayoutBlock& block)
{
    block.add_line_box();
}
