#include "WSMenu.h"
#include "WSEvent.h"
#include "WSEventLoop.h"
#include "WSMenuItem.h"
#include "WSMenuManager.h"
#include "WSScreen.h"
#include "WSWindow.h"
#include "WSWindowManager.h"
#include <LibDraw/CharacterBitmap.h>
#include <LibDraw/Font.h>
#include <LibDraw/GraphicsBitmap.h>
#include <LibDraw/Painter.h>
#include <LibDraw/StylePainter.h>
#include <WindowServer/WSAPITypes.h>
#include <WindowServer/WSClientConnection.h>

WSMenu::WSMenu(WSClientConnection* client, int menu_id, const String& name)
    : CObject(client)
    , m_client(client)
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

static const char* s_submenu_arrow_bitmap_data = {
    "         "
    "   #     "
    "   ##    "
    "   ###   "
    "   ####  "
    "   ###   "
    "   ##    "
    "   #     "
    "         "
};

static CharacterBitmap* s_checked_bitmap;
static const int s_checked_bitmap_width = 9;
static const int s_checked_bitmap_height = 9;
static const int s_submenu_arrow_bitmap_width = 9;
static const int s_submenu_arrow_bitmap_height = 9;
static const int s_item_icon_width = 16;
static const int s_checkbox_or_icon_padding = 6;
static const int s_stripe_width = 23;

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
        widest_text = max(widest_text, text_width);
    }

    int widest_item = widest_text + s_stripe_width;
    if (widest_shortcut)
        widest_item += padding_between_text_and_shortcut() + widest_shortcut;

    return max(widest_item, rect_in_menubar().width()) + horizontal_padding() + frame_thickness() * 2;
}

int WSMenu::height() const
{
    if (m_items.is_empty())
        return 0;
    return (m_items.last().rect().bottom() + 1) + frame_thickness();
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

        auto window = WSWindow::construct(*this, WSWindowType::Menu);
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
    painter.fill_rect(rect.shrunken(6, 6), Color::White);
    StylePainter::paint_window_frame(painter, rect);
    int width = this->width();

    if (!s_checked_bitmap)
        s_checked_bitmap = &CharacterBitmap::create_from_ascii(s_checked_bitmap_data, s_checked_bitmap_width, s_checked_bitmap_height).leak_ref();

    bool has_checkable_items = false;
    bool has_items_with_icon = false;
    for (auto& item : m_items) {
        has_checkable_items = has_checkable_items | item.is_checkable();
        has_items_with_icon = has_items_with_icon | !!item.icon();
    }

    Rect stripe_rect { frame_thickness(), frame_thickness(), s_stripe_width, height() - frame_thickness() * 2 };
    painter.fill_rect(stripe_rect, Color::WarmGray);
    painter.draw_line(stripe_rect.top_right(), stripe_rect.bottom_right(), Color::from_rgb(0xbbb7b0));

    for (auto& item : m_items) {
        if (item.type() == WSMenuItem::Text) {
            Color text_color = Color::Black;
            if (&item == m_hovered_item && item.is_enabled()) {
                painter.fill_rect(item.rect(), Color::from_rgb(0xad714f));
                painter.draw_rect(item.rect(), Color::from_rgb(0x793016));
                text_color = Color::White;
            } else if (!item.is_enabled()) {
                text_color = Color::MidGray;
            }
            Rect text_rect = item.rect().translated(stripe_rect.width() + 6, 0);
            if (item.is_checkable()) {
                Rect checkmark_rect { item.rect().x() + 7, 0, s_checked_bitmap_width, s_checked_bitmap_height };
                checkmark_rect.center_vertically_within(text_rect);
                Rect checkbox_rect = checkmark_rect.inflated(4, 4);
                painter.fill_rect(checkbox_rect, Color::White);
                StylePainter::paint_frame(painter, checkbox_rect, FrameShape::Container, FrameShadow::Sunken, 2);
                if (item.is_checked()) {
                    painter.draw_bitmap(checkmark_rect.location(), *s_checked_bitmap, Color::Black);
                }
            } else if (item.icon()) {
                Rect icon_rect { item.rect().x() + 3, 0, s_item_icon_width, s_item_icon_width };
                icon_rect.center_vertically_within(text_rect);
                painter.blit(icon_rect.location(), *item.icon(), item.icon()->rect());
            }
            painter.draw_text(text_rect, item.text(), TextAlignment::CenterLeft, text_color);
            if (!item.shortcut_text().is_empty()) {
                painter.draw_text(item.rect().translated(-right_padding(), 0), item.shortcut_text(), TextAlignment::CenterRight, text_color);
            }
            if (item.is_submenu()) {
                static auto& submenu_arrow_bitmap = CharacterBitmap::create_from_ascii(s_submenu_arrow_bitmap_data, s_submenu_arrow_bitmap_width, s_submenu_arrow_bitmap_height).leak_ref();
                Rect submenu_arrow_rect {
                    item.rect().right() - s_submenu_arrow_bitmap_width - 2,
                    0,
                    s_submenu_arrow_bitmap_width,
                    s_submenu_arrow_bitmap_height
                };
                submenu_arrow_rect.center_vertically_within(item.rect());
                painter.draw_bitmap(submenu_arrow_rect.location(), submenu_arrow_bitmap, Color::Black);
            }
        } else if (item.type() == WSMenuItem::Separator) {
            Point p1(item.rect().translated(stripe_rect.width() + 4, 0).x(), item.rect().center().y() - 1);
            Point p2(width - 7, item.rect().center().y() - 1);
            painter.draw_line(p1, p2, Color::MidGray);
            painter.draw_line(p1.translated(0, 1), p2.translated(0, 1), Color::White);
        }
    }
}

void WSMenu::event(CEvent& event)
{
    if (event.type() == WSEvent::MouseMove) {
        ASSERT(menu_window());
        auto* item = item_at(static_cast<const WSMouseEvent&>(event).position());
        if (m_hovered_item == item)
            return;
        m_hovered_item = item;
        if (m_hovered_item && m_hovered_item->is_submenu()) {
            WSWindowManager::the().menu_manager().close_everyone_not_in_lineage(*m_hovered_item->submenu());
            m_hovered_item->submenu()->popup(m_hovered_item->rect().top_right().translated(menu_window()->rect().location()), true);
        } else {
            WSWindowManager::the().menu_manager().close_everyone_not_in_lineage(*this);
        }
        redraw();
        return;
    }

    if (event.type() == WSEvent::MouseUp) {
        ASSERT(menu_window());
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

    WSWindowManager::the().menu_manager().close_everyone();

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
    WSWindowManager::the().menu_manager().close_menu_and_descendants(*this);
}

void WSMenu::popup(const Point& position, bool is_submenu)
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
    WSWindowManager::the().menu_manager().set_current_menu(this, is_submenu);
}

bool WSMenu::is_menu_ancestor_of(const WSMenu& other) const
{
    for (auto& item : m_items) {
        if (!item.is_submenu())
            continue;
        auto& submenu = *const_cast<WSMenuItem&>(item).submenu();
        if (&submenu == &other)
            return true;
        if (submenu.is_menu_ancestor_of(other))
            return true;
    }
    return false;
}
