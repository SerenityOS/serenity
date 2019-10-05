#include <LibHTML/Layout/LayoutBlock.h>
#include <LibHTML/Layout/LayoutInline.h>

LayoutInline::LayoutInline(const Node& node, RefPtr<StyleProperties> style_properties)
    : LayoutNode(&node, move(style_properties))
{
    set_inline(true);
}

LayoutInline::~LayoutInline()
{
}
