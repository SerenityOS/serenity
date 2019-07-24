#include "WSMenu.h"
#include "WSEvent.h"
#include "WSEventLoop.h"
#include "WSMenuItem.h"
#include "WSScreen.h"
#include "WSWindow.h"
#include "WSWindowManager.h"
#include <LibDraw/CharacterBitmap.h>
#include <LibDraw/Font.h>
#include <LibDraw/Painter.h>
#include <LibDraw/StylePainter.h>
#include <WindowServer/WSAPITypes.h>
#include <WindowServer/WSClientConnection.h>

WSMenu::WSMenu(WSClientConnection* client, int menu_id, const String& name)
    : m_client(client)
    , m_menu_id(menu_id)
    , m_name(move(name))
{
}

WSMenu::~WSMenu()
{
}

const Font& WSMenu::font() const
{
    return Font::default_font();
}

static const char* s_checked_bitmap_data = {
    "         "
    "       # "
    "      ## "
    "     ### "
    " ## ###  "
    " #####   "
    "  ###    "
    "   #     "
    "         "
};

static CharacterBitmap* s_checked_bitmap;
static const int s_checked_bitmap_width = 9;
static const int s_checked_bitmap_height = 9;
static const int s_checked_bitmap_padding = 6;

int WSMenu::width() const
{
    int widest_text = 0;
    int widest_shortcut = 0;
    for (auto& item : m_items) {
        if (item.type() != WSMenuItem::Text)
            continue;
        int text_width = font().width(item.text());
        if (!item.shortcut_text().is_empty()) {
            int shortcut_width = font().width(item.shortcut_text());
            widest_shortcut = max(shortcut_width, widest_shortcut);
        }
        if (item.is_checkable())
            text_width += s_checked_bitmap_width + s_checked_bitmap_padding;

        widest_text = max(widest_text, text_width);
    }

    int widest_item = widest_text;
    if (widest_shortcut)
        widest_item += padding_between_text_and_shortcut() + widest_shortcut;

    return max(widest_item, rect_in_menubar().width()) + horizontal_padding() + frame_thickness() * 2;
}

int WSMenu::height() const
{
    if (m_items.is_empty())
        return 0;
    return (m_items.last().rect().bottom() - 1) + frame_thickness() * 2;
}

void WSMenu::redraw()
{
    if (!menu_window())
        return;
    draw();
    menu_window()->invalidate();
}

WSWindow& WSMenu::ensure_menu_window()
{
    int width = this->width();
    if (!m_menu_window) {
        Point next_item_location(frame_thickness(), frame_thickness());
        for (auto& item : m_items) {
            int height = 0;
            if (item.type() == WSMenuItem::Text)
                height = item_height();
            else if (item.type() == WSMenuItem::Separator)
                height = 8;
            item.set_rect({ next_item_location, { width - frame_thickness() * 2, height } });
            next_item_location.move_by(0, height);
        }

        auto window = make<WSWindow>(*this, WSWindowType::Menu);
        window->set_opacity(0.95f);
        window->set_rect(0, 0, width, height());
        m_menu_window = move(window);
        draw();
    }
    return *m_menu_window;
}

void WSMenu::draw()
{
    ASSERT(menu_window());
    ASSERT(menu_window()->backing_store());
    Painter painter(*menu_window()->backing_store());

    Rect rect { {}, menu_window()->size() };
    painter.fill_rect(rect.shrunken(6, 6), Color::WarmGray);
    StylePainter::paint_window_frame(painter, rect);
    int width = this->width();

    if (!s_checked_bitmap)
        s_checked_bitmap = &CharacterBitmap::create_from_ascii(s_checked_bitmap_data, s_checked_bitmap_width, s_checked_bitmap_height).leak_ref();

    for (auto& item : m_items) {
        if (item.type() == WSMenuItem::Text) {
            Color text_color = Color::Black;
            if (&item == m_hovered_item) {
                painter.fill_rect(item.rect(), WSWindowManager::the().menu_selection_color());
                text_color = Color::White;
            }
            if (!item.is_enabled())
                text_color = Color::MidGray;
            Rect text_rect = item.rect().translated(left_padding(), 0);
            if (item.is_checkable()) {
                if (item.is_checked()) {
                    Rect checkmark_rect { text_rect.location().x(), 0, s_checked_bitmap_width, s_checked_bitmap_height };
                    checkmark_rect.center_vertically_within(text_rect);
                    painter.draw_bitmap(checkmark_rect.location(), *s_checked_bitmap, Color::Black);
                }
                text_rect.move_by(s_checked_bitmap_width + s_checked_bitmap_padding, 0);
            }
            painter.draw_text(text_rect, item.text(), TextAlignment::CenterLeft, text_color);
            if (!item.shortcut_text().is_empty()) {
                painter.draw_text(item.rect().translated(-right_padding(), 0), item.shortcut_text(), TextAlignment::CenterRight, text_color);
            }
        } else if (item.type() == WSMenuItem::Separator) {
            Point p1(4, item.rect().center().y());
            Point p2(width - 5, item.rect().center().y());
            painter.draw_line(p1, p2, Color::MidGray);
            painter.draw_line(p1.translated(0, 1), p2.translated(0, 1), Color::White);
        }
    }
}

void WSMenu::event(CEvent& event)
{
    ASSERT(menu_window());
    if (event.type() == WSEvent::MouseMove) {
        auto* item = item_at(static_cast<const WSMouseEvent&>(event).position());
        if (!item || m_hovered_item == item)
            return;
        m_hovered_item = item;
        redraw();
        return;
    }

    if (event.type() == WSEvent::MouseUp) {
        if (!m_hovered_item)
            return;
        if (m_hovered_item->is_enabled())
            did_activate(*m_hovered_item);
        clear_hovered_item();
        return;
    }

    CObject::event(event);
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

    WSAPI_ServerMessage message;
    message.type = WSAPI_ServerMessage::Type::MenuItemActivated;
    message.menu.menu_id = m_menu_id;
    message.menu.identifier = item.identifier();

    if (m_client)
        m_client->post_message(message);
}

WSMenuItem* WSMenu::item_with_identifier(unsigned identifer)
{
    for (auto& item : m_items) {
        if (item.identifier() == identifer)
            return &item;
    }
    return nullptr;
}

WSMenuItem* WSMenu::item_at(const Point& position)
{
    for (auto& item : m_items) {
        if (item.rect().contains(position))
            return &item;
    }
    return nullptr;
}

void WSMenu::close()
{
    WSWindowManager::the().close_menu(*this);
    if (menu_window())
        menu_window()->set_visible(false);
}

void WSMenu::popup(const Point& position)
{
    ASSERT(!is_empty());

    auto& window = ensure_menu_window();
    const int margin = 30;
    Point adjusted_pos = position;
    if (adjusted_pos.x() + window.width() >= WSScreen::the().width() - margin) {
        adjusted_pos = adjusted_pos.translated(-window.width(), 0);
    }
    if (adjusted_pos.y() + window.height() >= WSScreen::the().height() - margin) {
        adjusted_pos = adjusted_pos.translated(0, -window.height());
    }

    window.move_to(adjusted_pos);
    window.set_visible(true);
    WSWindowManager::the().set_current_menu(this);
}
