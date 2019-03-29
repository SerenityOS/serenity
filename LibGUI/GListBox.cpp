#include "GListBox.h"
#include <SharedGraphics/Font.h>
#include <LibGUI/GPainter.h>

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

void GListBox::paint_event(GPaintEvent& event)
{
    GPainter painter(*this);
    painter.add_clip_rect(event.rect());

    painter.fill_rect({ rect().x() + 1, rect().y() + 1, rect().width() - 2, rect().height() - 2 }, background_color());
    painter.draw_rect(rect(), foreground_color());

    if (is_focused())
        painter.draw_focus_rect(rect());

    for (int i = m_scroll_offset; i < static_cast<int>(m_items.size()); ++i) {
        auto item_rect = this->item_rect(i);
        Rect text_rect(item_rect.x() + 1, item_rect.y() + 1, item_rect.width() - 2, item_rect.height() - 2);

        Color item_text_color = foreground_color();
        if (m_selected_index == i) {
            if (is_focused())
                painter.fill_rect(item_rect, Color(0, 32, 128));
            else
                painter.fill_rect(item_rect, Color(96, 96, 96));
            item_text_color = Color::White;
        }
        painter.draw_text(item_rect, m_items[i], TextAlignment::TopLeft, item_text_color);
    }
}

void GListBox::mousedown_event(GMouseEvent& event)
{
    dbgprintf("GListBox::mouseDownEvent %d,%d\n", event.x(), event.y());
    for (int i = m_scroll_offset; i < static_cast<int>(m_items.size()); ++i) {
        auto item_rect = this->item_rect(i);
        if (item_rect.contains(event.position())) {
            m_selected_index = i;
            dbgprintf("GListBox: selected item %u (\"%s\")\n", i, m_items[i].characters());
            update();
            return;
        }
    }
}

void GListBox::add_item(String&& item)
{
    m_items.append(move(item));
    if (m_selected_index == -1)
        m_selected_index = 0;
}

