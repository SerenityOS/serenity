#include "GListBox.h"
#include <SharedGraphics/Font.h>
#include <SharedGraphics/Painter.h>

GListBox::GListBox(GWidget* parent)
    : GWidget(parent)
{
}

GListBox::~GListBox()
{
}

Rect GListBox::item_rect(int index) const
{
    int item_height = font().glyph_height() + 2;
    return Rect { 2, 2 + (index * item_height), width() - 4, item_height };
}

void GListBox::paintEvent(GPaintEvent&)
{
    Painter painter(*this);

    // FIXME: Reduce overdraw.
    painter.fill_rect(rect(), Color::White);
    painter.draw_rect(rect(), Color::Black);

    if (isFocused())
        painter.draw_focus_rect(rect());

    for (int i = m_scrollOffset; i < static_cast<int>(m_items.size()); ++i) {
        auto itemRect = item_rect(i);
        Rect textRect(itemRect.x() + 1, itemRect.y() + 1, itemRect.width() - 2, itemRect.height() - 2);

        Color itemTextColor = foregroundColor();
        if (m_selectedIndex == i) {
            if (isFocused())
                painter.fill_rect(itemRect, Color(0, 32, 128));
            else
                painter.fill_rect(itemRect, Color(96, 96, 96));
            itemTextColor = Color::White;
        }
        painter.draw_text(textRect, m_items[i], Painter::TextAlignment::TopLeft, itemTextColor);
    }
}

void GListBox::mouseDownEvent(GMouseEvent& event)
{
    dbgprintf("GListBox::mouseDownEvent %d,%d\n", event.x(), event.y());
    for (int i = m_scrollOffset; i < static_cast<int>(m_items.size()); ++i) {
        auto itemRect = item_rect(i);
        if (itemRect.contains(event.position())) {
            m_selectedIndex = i;
            dbgprintf("GListBox: selected item %u (\"%s\")\n", i, m_items[i].characters());
            update();
            return;
        }
    }
}

void GListBox::addItem(String&& item)
{
    m_items.append(move(item));
    if (m_selectedIndex == -1)
        m_selectedIndex = 0;
}

