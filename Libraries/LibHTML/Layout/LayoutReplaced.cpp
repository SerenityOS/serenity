#include <LibHTML/DOM/Element.h>
#include <LibHTML/Layout/LayoutBlock.h>
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

void LayoutReplaced::split_into_lines(LayoutBlock& container)
{
    layout();

    if (container.line_boxes().is_empty())
        container.line_boxes().append(LineBox());
    container.line_boxes().last().add_fragment(*this, 0, 0, rect().width(), rect().height());
}
