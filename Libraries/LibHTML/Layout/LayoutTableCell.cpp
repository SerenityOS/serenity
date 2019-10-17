#include <LibHTML/DOM/Element.h>
#include <LibHTML/Layout/LayoutTableCell.h>

LayoutTableCell::LayoutTableCell(const Element& element, NonnullRefPtr<StyleProperties> style)
    : LayoutBlock(&element, move(style))
{
}

LayoutTableCell::~LayoutTableCell()
{
}
