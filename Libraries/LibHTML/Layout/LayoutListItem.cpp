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
        if (first_child())
            m_marker->set_inline(first_child()->is_inline());
        append_child(*m_marker);
    }

    FloatRect marker_rect { x() - 8, y(), 4, height() };
    m_marker->set_rect(marker_rect);
}
