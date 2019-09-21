#include <LibHTML/DOM/Element.h>
#include <LibHTML/Layout/LayoutInline.h>

LayoutInline::LayoutInline(const Node& node, StyleProperties&& style_properties)
    : LayoutNode(&node, move(style_properties))
{
}

LayoutInline::~LayoutInline()
{
}
