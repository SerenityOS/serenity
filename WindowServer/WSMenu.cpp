#include "WSMenu.h"
#include "WSMenuItem.h"
#include "WSWindow.h"
#include "WSMessage.h"
#include "WSMessageLoop.h"
#include "WSWindowManager.h"
#include <SharedGraphics/Painter.h>
#include <SharedGraphics/Font.h>

WSMenu::WSMenu(Process& process, int menu_id, String&& name)
    : m_menu_id(menu_id)
    , m_name(move(name))
    , m_process(process.make_weak_ptr())
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

    return max(longest, rect_in_menubar().width()) + horizontal_padding();
}

int WSMenu::height() const
{
    if (m_items.is_empty())
        return 0;
    return (m_items.last()->rect().bottom() - 1) + vertical_padding();
}

void WSMenu::redraw()
{
    ASSERT(menu_window());
    draw();
    menu_window()->invalidate();
}

WSWindow& WSMenu::ensure_menu_window()
{
    if (!m_menu_window) {
        Point next_item_location(1, vertical_padding() / 2);
        for (auto& item : m_items) {
            int height = 0;
            if (item->type() == WSMenuItem::Text)
                height = item_height();
            else if (item->type() == WSMenuItem::Separator)
                height = 7;
            item->set_rect({ next_item_location, { width() - 2, height } });
            next_item_location.move_by(0, height);
        }

        auto window = make<WSWindow>(*this);
        WSWindowLocker locker(*window);
        window->set_rect(0, 0, width(), height());
        m_menu_window = move(window);
        draw();
    }
    return *m_menu_window;
}

void WSMenu::draw()
{
    ASSERT(menu_window());
    WSWindowLocker locker(*menu_window());
    ASSERT(menu_window()->backing());
    Painter painter(*menu_window()->backing());

    Rect rect { { }, menu_window()->size() };
    painter.draw_rect(rect, Color::White);
    painter.fill_rect(rect.shrunken(2, 2), Color::LightGray);

    for (auto& item : m_items) {
        if (item->type() == WSMenuItem::Text) {
            Color text_color = Color::Black;
            if (item.ptr() == m_hovered_item) {
                painter.fill_rect(item->rect(), WSWindowManager::the().menu_selection_color());
                text_color = Color::White;
            }
            painter.draw_text(item->rect().translated(left_padding(), 0), item->text(), TextAlignment::CenterLeft, text_color);
        } else if (item->type() == WSMenuItem::Separator) {
            Point p1(1, item->rect().center().y());
            Point p2(width() - 2, item->rect().center().y());
            painter.draw_line(p1, p2, Color::MidGray);
        }
    }
}

void WSMenu::on_window_message(WSMessage& message)
{
    WSWindowLocker locker(*menu_window());
    ASSERT(menu_window());
    if (message.type() == WSMessage::MouseMove) {
        auto* item = item_at(static_cast<WSMouseEvent&>(message).position());
        if (!item || m_hovered_item == item)
            return;
        m_hovered_item = item;
        redraw();
        return;
    }

    if (message.type() == WSMessage::MouseUp) {
        if (!m_hovered_item)
            return;
        did_activate(*m_hovered_item);
        clear_hovered_item();
        return;
    }
}

void WSMenu::clear_hovered_item()
{
    if (!m_hovered_item)
        return;
    m_hovered_item = nullptr;
    redraw();
}

void WSMenu::did_activate(WSMenuItem& item)
{
    if (on_item_activation)
        on_item_activation(item);

    close();

    GUI_Event gui_event;
    gui_event.type = GUI_Event::Type::MenuItemActivated;
    gui_event.menu.menu_id = m_menu_id;
    gui_event.menu.identifier = item.identifier();

    if (!m_process)
        return;
    LOCKER(m_process->gui_events_lock());
    m_process->gui_events().append(move(gui_event));
}

WSMenuItem* WSMenu::item_at(const Point& position)
{
    for (auto& item : m_items) {
        if (!item->rect().contains(position))
            continue;
        return item.ptr();
    }
    return nullptr;
}

void WSMenu::close()
{
    WSWindowManager::the().close_menu(*this);
};
