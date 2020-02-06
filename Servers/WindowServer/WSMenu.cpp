/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2020, Shannon Booth <shannon.ml.booth@gmail.com>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include "WSMenu.h"
#include "WSEvent.h"
#include "WSEventLoop.h"
#include "WSMenuItem.h"
#include "WSMenuManager.h"
#include "WSScreen.h"
#include "WSWindow.h"
#include "WSWindowManager.h"
#include <LibGfx/CharacterBitmap.h>
#include <LibGfx/Font.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/Painter.h>
#include <LibGfx/StylePainter.h>
#include <LibGfx/Triangle.h>
#include <WindowServer/WSClientConnection.h>
#include <WindowServer/WindowClientEndpoint.h>

WSMenu::WSMenu(WSClientConnection* client, int menu_id, const String& name)
    : Core::Object(client)
    , m_client(client)
    , m_menu_id(menu_id)
    , m_name(move(name))
{
}

WSMenu::~WSMenu()
{
}

const Gfx::Font& WSMenu::font() const
{
    return Gfx::Font::default_font();
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

static Gfx::CharacterBitmap* s_checked_bitmap;
static const int s_checked_bitmap_width = 9;
static const int s_checked_bitmap_height = 9;
static const int s_submenu_arrow_bitmap_width = 9;
static const int s_submenu_arrow_bitmap_height = 9;
static const int s_item_icon_width = 16;
static const int s_stripe_width = 23;

int WSMenu::content_width() const
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

void WSMenu::redraw()
{
    if (!menu_window())
        return;
    draw();
    menu_window()->invalidate();
}

WSWindow& WSMenu::ensure_menu_window()
{
    int width = this->content_width();
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

        int window_height_available = WSScreen::the().height() - WSMenuManager::the().menubar_rect().height() - frame_thickness() * 2;
        int max_window_height = (window_height_available / item_height()) * item_height() + frame_thickness() * 2;
        int content_height = m_items.is_empty() ? 0 : (m_items.last().rect().bottom() + 1) + frame_thickness();
        int window_height = min(max_window_height, content_height);
        if (window_height < content_height) {
            m_scrollable = true;
            m_max_scroll_offset = item_count() - window_height / item_height() + 2;
        }

        auto window = WSWindow::construct(*this, WSWindowType::Menu);
        window->set_rect(0, 0, width, window_height);
        m_menu_window = move(window);
        draw();
    }
    return *m_menu_window;
}

int WSMenu::visible_item_count() const
{
    if (!is_scrollable())
        return m_items.size();
    ASSERT(m_menu_window);
    // Make space for up/down arrow indicators
    return m_menu_window->height() / item_height() - 2;
}

void WSMenu::draw()
{
    auto palette = WSWindowManager::the().palette();
    m_theme_index_at_last_paint = WSMenuManager::the().theme_index();

    ASSERT(menu_window());
    ASSERT(menu_window()->backing_store());
    Gfx::Painter painter(*menu_window()->backing_store());

    Gfx::Rect rect { {}, menu_window()->size() };
    painter.fill_rect(rect.shrunken(6, 6), palette.menu_base());
    Gfx::StylePainter::paint_window_frame(painter, rect, palette);
    int width = this->content_width();

    if (!s_checked_bitmap)
        s_checked_bitmap = &Gfx::CharacterBitmap::create_from_ascii(s_checked_bitmap_data, s_checked_bitmap_width, s_checked_bitmap_height).leak_ref();

    bool has_checkable_items = false;
    bool has_items_with_icon = false;
    for (auto& item : m_items) {
        has_checkable_items = has_checkable_items | item.is_checkable();
        has_items_with_icon = has_items_with_icon | !!item.icon();
    }

    Gfx::Rect stripe_rect { frame_thickness(), frame_thickness(), s_stripe_width, menu_window()->height() - frame_thickness() * 2 };
    painter.fill_rect(stripe_rect, palette.menu_stripe());
    painter.draw_line(stripe_rect.top_right(), stripe_rect.bottom_right(), palette.menu_stripe().darkened());

    int visible_item_count = this->visible_item_count();

    if (is_scrollable()) {
        bool can_go_up = m_scroll_offset > 0;
        bool can_go_down = m_scroll_offset < m_max_scroll_offset;
        Gfx::Rect up_indicator_rect { frame_thickness(), frame_thickness(), content_width(), item_height() };
        painter.draw_text(up_indicator_rect, "\xc3\xb6", Gfx::TextAlignment::Center, can_go_up ? palette.menu_base_text() : palette.color(ColorRole::DisabledText));
        Gfx::Rect down_indicator_rect { frame_thickness(), menu_window()->height() - item_height() - frame_thickness(), content_width(), item_height() };
        painter.draw_text(down_indicator_rect, "\xc3\xb7", Gfx::TextAlignment::Center, can_go_down ? palette.menu_base_text() : palette.color(ColorRole::DisabledText));
    }

    for (int i = 0; i < visible_item_count; ++i) {
        auto& item = m_items.at(m_scroll_offset + i);
        if (item.type() == WSMenuItem::Text) {
            Color text_color = palette.menu_base_text();
            if (&item == hovered_item() && item.is_enabled()) {
                painter.fill_rect(item.rect(), palette.menu_selection());
                painter.draw_rect(item.rect(), palette.menu_selection().darkened());
                text_color = palette.menu_selection_text();
            } else if (!item.is_enabled()) {
                text_color = Color::MidGray;
            }
            Gfx::Rect text_rect = item.rect().translated(stripe_rect.width() + 6, 0);
            if (item.is_checkable()) {
                if (item.is_exclusive()) {
                    Gfx::Rect radio_rect { item.rect().x() + 5, 0, 12, 12 };
                    radio_rect.center_vertically_within(text_rect);
                    Gfx::StylePainter::paint_radio_button(painter, radio_rect, palette, item.is_checked(), false);
                } else {
                    Gfx::Rect checkmark_rect { item.rect().x() + 7, 0, s_checked_bitmap_width, s_checked_bitmap_height };
                    checkmark_rect.center_vertically_within(text_rect);
                    Gfx::Rect checkbox_rect = checkmark_rect.inflated(4, 4);
                    painter.fill_rect(checkbox_rect, palette.base());
                    Gfx::StylePainter::paint_frame(painter, checkbox_rect, palette, Gfx::FrameShape::Container, Gfx::FrameShadow::Sunken, 2);
                    if (item.is_checked()) {
                        painter.draw_bitmap(checkmark_rect.location(), *s_checked_bitmap, palette.button_text());
                    }
                }
            } else if (item.icon()) {
                Gfx::Rect icon_rect { item.rect().x() + 3, 0, s_item_icon_width, s_item_icon_width };
                icon_rect.center_vertically_within(text_rect);
                painter.blit(icon_rect.location(), *item.icon(), item.icon()->rect());
            }
            painter.draw_text(text_rect, item.text(), Gfx::TextAlignment::CenterLeft, text_color);
            if (!item.shortcut_text().is_empty()) {
                painter.draw_text(item.rect().translated(-right_padding(), 0), item.shortcut_text(), Gfx::TextAlignment::CenterRight, text_color);
            }
            if (item.is_submenu()) {
                static auto& submenu_arrow_bitmap = Gfx::CharacterBitmap::create_from_ascii(s_submenu_arrow_bitmap_data, s_submenu_arrow_bitmap_width, s_submenu_arrow_bitmap_height).leak_ref();
                Gfx::Rect submenu_arrow_rect {
                    item.rect().right() - s_submenu_arrow_bitmap_width - 2,
                    0,
                    s_submenu_arrow_bitmap_width,
                    s_submenu_arrow_bitmap_height
                };
                submenu_arrow_rect.center_vertically_within(item.rect());
                painter.draw_bitmap(submenu_arrow_rect.location(), submenu_arrow_bitmap, text_color);
            }
        } else if (item.type() == WSMenuItem::Separator) {
            Point p1(item.rect().translated(stripe_rect.width() + 4, 0).x(), item.rect().center().y() - 1);
            Point p2(width - 7, item.rect().center().y() - 1);
            painter.draw_line(p1, p2, palette.threed_shadow1());
            painter.draw_line(p1.translated(0, 1), p2.translated(0, 1), palette.threed_highlight());
        }
    }
}

WSMenuItem* WSMenu::hovered_item() const
{
    if (m_hovered_item_index == -1)
        return nullptr;
    return const_cast<WSMenuItem*>(&item(m_hovered_item_index));
}

void WSMenu::update_for_new_hovered_item()
{
    if (hovered_item() && hovered_item()->is_submenu()) {
        WSMenuManager::the().close_everyone_not_in_lineage(*hovered_item()->submenu());
        hovered_item()->submenu()->popup(hovered_item()->rect().top_right().translated(menu_window()->rect().location()), true);
    } else {
        WSMenuManager::the().close_everyone_not_in_lineage(*this);
        WSMenuManager::the().set_current_menu(this);
        menu_window()->set_visible(true);
    }
    redraw();
}

void WSMenu::open_hovered_item()
{
    ASSERT(menu_window());
    ASSERT(menu_window()->is_visible());
    if (!hovered_item())
        return;
    if (hovered_item()->is_enabled())
        did_activate(*hovered_item());
    clear_hovered_item();
}

void WSMenu::decend_into_submenu_at_hovered_item()
{
    ASSERT(hovered_item());
    ASSERT(hovered_item()->is_submenu());
    auto submenu = hovered_item()->submenu();
    submenu->m_hovered_item_index = 0;
    ASSERT(submenu->hovered_item()->type() != WSMenuItem::Separator);
    submenu->update_for_new_hovered_item();
    m_in_submenu = true;
}

void WSMenu::handle_mouse_move_event(const WSMouseEvent& mouse_event)
{
    ASSERT(menu_window());
    if (hovered_item() && hovered_item()->is_submenu()) {

        auto item = *hovered_item();
        auto submenu_top_left = item.rect().location() + Point { item.rect().width(), 0 };
        auto submenu_bottom_left = submenu_top_left + Point { 0, item.submenu()->menu_window()->height() };

        auto safe_hover_triangle = Gfx::Triangle { m_last_position_in_hover, submenu_top_left, submenu_bottom_left };
        m_last_position_in_hover = mouse_event.position();

        // Don't update the hovered item if mouse is moving towards a submenu
        if (safe_hover_triangle.contains(mouse_event.position()))
            return;
    }

    int index = item_index_at(mouse_event.position());
    if (m_hovered_item_index == index)
        return;
    m_hovered_item_index = index;

    // FIXME: Tell parent menu (if it exists) that it is currently in a submenu
    m_in_submenu = false;
    update_for_new_hovered_item();
    return;
}

void WSMenu::event(Core::Event& event)
{
    if (event.type() == WSEvent::MouseMove) {
        handle_mouse_move_event(static_cast<const WSMouseEvent&>(event));
        return;
    }

    if (event.type() == WSEvent::MouseUp) {
        open_hovered_item();
        return;
    }

    if (event.type() == WSEvent::MouseWheel && is_scrollable()) {
        ASSERT(menu_window());
        auto& mouse_event = static_cast<const WSMouseEvent&>(event);
        m_scroll_offset += mouse_event.wheel_delta();
        m_scroll_offset = clamp(m_scroll_offset, 0, m_max_scroll_offset);

        int index = item_index_at(mouse_event.position());
        if (m_hovered_item_index == index)
            return;

        m_hovered_item_index = index;
        update_for_new_hovered_item();
        return;
    }

    if (event.type() == WSEvent::KeyDown) {
        auto key = static_cast<WSKeyEvent&>(event).key();

        if (!(key == Key_Up || key == Key_Down || key == Key_Left || key == Key_Right || key == Key_Return))
            return;

        ASSERT(menu_window());
        ASSERT(menu_window()->is_visible());

        // Default to the first item on key press if one has not been selected yet
        if (!hovered_item()) {
            m_hovered_item_index = 0;
            update_for_new_hovered_item();
            return;
        }

        // Pass the event for the submenu that we are currently in to handle
        if (m_in_submenu && key != Key_Left) {
            ASSERT(hovered_item()->is_submenu());
            hovered_item()->submenu()->dispatch_event(event);
            return;
        }

        if (key == Key_Return) {
            if (hovered_item()->is_submenu())
                decend_into_submenu_at_hovered_item();
            else
                open_hovered_item();
            return;
        }

        if (key == Key_Up) {
            ASSERT(m_items.at(0).type() != WSMenuItem::Separator);

            if (is_scrollable() && m_hovered_item_index == 0)
                return;

            do {
                if (m_hovered_item_index == 0)
                    m_hovered_item_index = m_items.size() - 1;
                else if (m_hovered_item_index < 0)
                    return;
                else
                    --m_hovered_item_index;
            } while (hovered_item()->type() == WSMenuItem::Separator);

            if (is_scrollable() && m_hovered_item_index < m_scroll_offset)
                --m_scroll_offset;

            update_for_new_hovered_item();
            return;
        }

        if (key == Key_Down) {
            ASSERT(m_items.at(0).type() != WSMenuItem::Separator);

            if (is_scrollable() && m_hovered_item_index == m_items.size() - 1)
                return;

            do {
                if (m_hovered_item_index == m_items.size() - 1)
                    m_hovered_item_index = 0;
                else if (m_hovered_item_index > m_items.size() - 1)
                    return;
                else
                    ++m_hovered_item_index;
            } while (hovered_item()->type() == WSMenuItem::Separator);

            if (is_scrollable() && m_hovered_item_index >= (m_scroll_offset + visible_item_count()))
                ++m_scroll_offset;

            update_for_new_hovered_item();
            return;
        }

        if (key == Key_Left) {
            if (!m_in_submenu)
                return;

            ASSERT(hovered_item()->is_submenu());
            hovered_item()->submenu()->clear_hovered_item();
            m_in_submenu = false;
            return;
        }

        if (key == Key_Right) {
            if (hovered_item()->is_submenu())
                decend_into_submenu_at_hovered_item();
            return;
        }
    }
    Core::Object::event(event);
}

void WSMenu::clear_hovered_item()
{
    if (!hovered_item())
        return;
    m_hovered_item_index = -1;
    m_in_submenu = false;
    redraw();
}

void WSMenu::did_activate(WSMenuItem& item)
{
    if (item.type() == WSMenuItem::Type::Separator)
        return;

    if (on_item_activation)
        on_item_activation(item);

    WSMenuManager::the().close_bar();

    if (m_client)
        m_client->post_message(WindowClient::MenuItemActivated(m_menu_id, item.identifier()));
}

WSMenuItem* WSMenu::item_with_identifier(unsigned identifer)
{
    for (auto& item : m_items) {
        if (item.identifier() == identifer)
            return &item;
    }
    return nullptr;
}

int WSMenu::item_index_at(const Gfx::Point& position)
{
    int i = 0;
    for (auto& item : m_items) {
        if (item.rect().contains(position))
            return i;
        ++i;
    }
    return -1;
}

void WSMenu::close()
{
    WSMenuManager::the().close_menu_and_descendants(*this);
}

void WSMenu::redraw_if_theme_changed()
{
    if (m_theme_index_at_last_paint != WSMenuManager::the().theme_index())
        redraw();
}

void WSMenu::popup(const Gfx::Point& position, bool is_submenu)
{
    ASSERT(!is_empty());

    auto& window = ensure_menu_window();
    redraw_if_theme_changed();

    const int margin = 30;
    Point adjusted_pos = position;

    if (adjusted_pos.x() + window.width() >= WSScreen::the().width() - margin) {
        adjusted_pos = adjusted_pos.translated(-window.width(), 0);
    }
    if (adjusted_pos.y() + window.height() >= WSScreen::the().height() - margin) {
        adjusted_pos = adjusted_pos.translated(0, -window.height());
    }

    if (adjusted_pos.y() < WSMenuManager::the().menubar_rect().height())
        adjusted_pos.set_y(WSMenuManager::the().menubar_rect().height());

    window.move_to(adjusted_pos);
    window.set_visible(true);
    WSMenuManager::the().set_current_menu(this, is_submenu);
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
