#include <LibHTML/DOM/Element.h>
#include <LibHTML/Layout/LayoutTable.h>
#include <LibHTML/Layout/LayoutTableRow.h>

LayoutTable::LayoutTable(const Element& element, NonnullRefPtr<StyleProperties> style)
    : LayoutBlock(&element, move(style))
{
}

LayoutTable::~LayoutTable()
{
}

void LayoutTable::layout()
{

    LayoutBlock::layout();
}

LayoutTableRow* LayoutTable::first_row()
{
    return first_child_of_type<LayoutTableRow>();
}

const LayoutTableRow* LayoutTable::first_row() const
{
    return first_child_of_type<LayoutTableRow>();
}
