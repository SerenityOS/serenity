#include <LibHTML/Layout/LayoutListItem.h>
#include <LibHTML/Layout/LayoutListItemMarker.h>

LayoutListItem::LayoutListItem(const Element& element, NonnullRefPtr<StyleProperties> style)
    : LayoutBlock(&element, move(style))
{
}

LayoutListItem::~LayoutListItem()
{
}

void LayoutListItem::layout()
{
    LayoutBlock::layout();

    if (!m_marker) {
        m_marker = adopt(*new LayoutListItemMarker);
        prepend_child(*m_marker);
    }

    Rect marker_rect { rect().x() - 8, rect().y(), 4, rect().height() };
    m_marker->set_rect(marker_rect);
}
