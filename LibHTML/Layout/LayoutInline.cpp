#include <LibHTML/DOM/Element.h>
#include <LibHTML/Layout/LayoutInline.h>

LayoutInline::LayoutInline(Element& element)
    : LayoutNode(&element)
{
}

LayoutInline::~LayoutInline()
{
}
