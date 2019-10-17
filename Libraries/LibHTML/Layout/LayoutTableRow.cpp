#include <LibHTML/DOM/Element.h>
#include <LibHTML/Layout/LayoutTableRow.h>

LayoutTableRow::LayoutTableRow(const Element& element, NonnullRefPtr<StyleProperties> style)
    : LayoutBox(&element, move(style))
{
}

LayoutTableRow::~LayoutTableRow()
{
}

void LayoutTableRow::layout()
{
    LayoutBox::layout();
}
