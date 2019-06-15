#include <LibHTML/Element.h>
#include <LibHTML/LayoutInline.h>

LayoutInline::LayoutInline(Element& element)
    : LayoutNode(&element)
{
}

LayoutInline::~LayoutInline()
{
}
