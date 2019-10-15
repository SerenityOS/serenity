#include <LibHTML/DOM/Element.h>
#include <LibHTML/Layout/LayoutBlock.h>
#include <LibHTML/Layout/LayoutReplaced.h>

LayoutReplaced::LayoutReplaced(const Element& element, NonnullRefPtr<StyleProperties> style)
    : LayoutBox(&element, move(style))
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

    auto* line_box = &container.ensure_last_line_box();
    if (line_box->width() + width() > container.width())
        line_box = &container.add_line_box();
    line_box->add_fragment(*this, 0, 0, width(), height());
}
