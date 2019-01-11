#include "ListBox.h"
#include "Painter.h"
#include "Font.h"
#include "Window.h"

ListBox::ListBox(Widget* parent)
    : Widget(parent)
{
}

ListBox::~ListBox()
{
}

unsigned ListBox::itemHeight() const
{
    return font().glyphHeight() + 2;
}

void ListBox::paintEvent(PaintEvent&)
{
    Painter painter(*this);

    // FIXME: Reduce overdraw.
    painter.fillRect(rect(), Color::White);
    painter.drawRect(rect(), Color::Black);

    if (isFocused())
        painter.drawFocusRect(rect());

    for (unsigned i = m_scrollOffset; i < m_items.size(); ++i) {
        Rect itemRect(2, 2 + (i * itemHeight()), width() - 4, itemHeight());
        Rect textRect(itemRect.x() + 1, itemRect.y() + 1, itemRect.width() - 2, itemRect.height() - 2);

        Color itemTextColor = foregroundColor();
        if (m_selectedIndex == i) {
            if (isFocused())
                painter.fillRect(itemRect, Color(0, 32, 128));
            else
                painter.fillRect(itemRect, Color(96, 96, 96));
            itemTextColor = Color::White;
        }
        painter.drawText(textRect, m_items[i], Painter::TextAlignment::TopLeft, itemTextColor);
    }
}

void ListBox::mouseDownEvent(MouseEvent& event)
{
    printf("ListBox::mouseDownEvent %d,%d\n", event.x(), event.y());
    for (unsigned i = m_scrollOffset; i < m_items.size(); ++i) {
        Rect itemRect(1, 1 + (i * itemHeight()), width() - 2, itemHeight());
        if (itemRect.contains(event.position())) {
            m_selectedIndex = i;
            printf("ListBox: selected item %u (\"%s\")\n", i, m_items[i].characters());
            update();
            return;
        }
    }
}

void ListBox::addItem(String&& item)
{
    m_items.append(move(item));
    if (m_selectedIndex == -1)
        m_selectedIndex = 0;
}

