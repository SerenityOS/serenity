#include <LibHTML/DOM/Element.h>
#include <LibHTML/Layout/LayoutBlock.h>
#include <LibHTML/Layout/LayoutInline.h>

LayoutInline::LayoutInline(const Element& element, NonnullRefPtr<StyleProperties> style)
    : LayoutNodeWithStyleAndBoxModelMetrics(&element, move(style))
{
    set_inline(true);
}

LayoutInline::~LayoutInline()
{
}
