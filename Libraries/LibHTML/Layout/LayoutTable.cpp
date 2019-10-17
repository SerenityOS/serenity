#include <LibHTML/DOM/Element.h>
#include <LibHTML/Layout/LayoutTable.h>

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
