#include <LibHTML/DOM/Element.h>
#include <LibHTML/Layout/LayoutTableCell.h>
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

LayoutTableCell* LayoutTableRow::first_cell()
{
    return first_child_of_type<LayoutTableCell>();
}

const LayoutTableCell* LayoutTableRow::first_cell() const
{
    return first_child_of_type<LayoutTableCell>();
}

LayoutTableRow* LayoutTableRow::next_row()
{
    return next_sibling_of_type<LayoutTableRow>();
}

const LayoutTableRow* LayoutTableRow::next_row() const
{
    return next_sibling_of_type<LayoutTableRow>();
}
