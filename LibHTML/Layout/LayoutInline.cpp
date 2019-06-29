#include <LibHTML/DOM/Element.h>
#include <LibHTML/Layout/LayoutInline.h>

LayoutInline::LayoutInline(const Node& node)
    : LayoutNode(&node)
{
}

LayoutInline::~LayoutInline()
{
}
