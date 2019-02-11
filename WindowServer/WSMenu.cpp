#include "WSMenu.h"
#include "WSMenuItem.h"
#include "WSWindow.h"
#include "WSMessage.h"
#include "WSMessageLoop.h"
#include <SharedGraphics/Painter.h>
#include <SharedGraphics/Font.h>

WSMenu::WSMenu(const String& name)
    : m_name(name)
{
}

WSMenu::~WSMenu()
{
}

void WSMenu::set_menu_window(OwnPtr<WSWindow>&& menu_window)
{
    m_menu_window = move(menu_window);
}

const Font& WSMenu::font() const
{
    return Font::default_font();
}

int WSMenu::width() const
{
    int longest = 0;
    for (auto& item : m_items) {
        if (item->type() == WSMenuItem::Text)
            longest = max(longest, font().width(item->text()));
    }

    return max(longest, 80) + padding() * 2;
}

int WSMenu::height() const
{
    if (m_items.is_empty())
        return 0;
    return (m_items.last()->rect().bottom() - 1) + padding();
}

WSWindow& WSMenu::ensure_menu_window()
{
    if (!m_menu_window) {
        Point next_item_location(padding() / 2, padding() / 2);
        for (auto& item : m_items) {
            int height = 0;
            if (item->type() == WSMenuItem::Text)
                height = item_height();
            else if (item->type() == WSMenuItem::Separator)
                height = 7;
            item->set_rect({ next_item_location, { width() - padding(), height } });
            next_item_location.move_by(0, height);
        }

        auto window = make<WSWindow>(*this);
        window->set_rect(0, 0, width(), height());
        dbgprintf("Created menu window for menu '%s' (%u items) with rect %s\n", name().characters(), m_items.size(), window->rect().to_string().characters());
        m_menu_window = move(window);
        draw();
    }
    return *m_menu_window;
}

void WSMenu::draw()
{
    ASSERT(m_menu_window);
    ASSERT(m_menu_window->backing());
    Painter painter(*m_menu_window->backing());

    Rect rect { { }, m_menu_window->size() };

    painter.draw_rect(rect, Color::White);
    painter.fill_rect(rect.shrunken(2, 2), Color::LightGray);

    for (auto& item : m_items) {
        if (item->type() == WSMenuItem::Text) {
            Color text_color = Color::Black;
            if (item.ptr() == m_hovered_item) {
                painter.fill_rect(item->rect(), Color(0, 0, 128));
                text_color = Color::White;
            }
            painter.draw_text(item->rect(), item->text(), Painter::TextAlignment::CenterLeft, text_color);
        } else if (item->type() == WSMenuItem::Separator) {
            Point p1(padding(), item->rect().center().y());
            Point p2(width() - padding(), item->rect().center().y());
            painter.draw_line(p1, p2, Color::MidGray);
        }
    }
}

void WSMenu::on_window_message(WSMessage& message)
{
    dbgprintf("WSMenu::on_window_message: %u\n", message.type());
    if (message.type() == WSMessage::MouseMove) {
        auto& mouse_event = static_cast<WSMouseEvent&>(message);
        for (auto& item : m_items) {
            if (item->rect().contains(mouse_event.position())) {
                if (m_hovered_item == item.ptr())
                    return;
                m_hovered_item = item.ptr();
                draw();
                menu_window()->invalidate();
            }
        }
    }
}
