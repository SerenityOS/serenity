#include <LibHTML/DOM/Element.h>
#include <LibHTML/Layout/LayoutReplaced.h>

LayoutReplaced::LayoutReplaced(const Element& element, NonnullRefPtr<StyleProperties> style)
    : LayoutNode(&element, move(style))
{
    // FIXME: Allow non-inline replaced elements.
    set_inline(true);
}

LayoutReplaced::~LayoutReplaced()
{
}
