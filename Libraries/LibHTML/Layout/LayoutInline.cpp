#include <LibHTML/DOM/Element.h>
#include <LibHTML/Layout/LayoutInline.h>

LayoutInline::LayoutInline(const Node& node, const StyledNode& styled_node)
    : LayoutNode(&node, &styled_node)
{
}

LayoutInline::~LayoutInline()
{
}
