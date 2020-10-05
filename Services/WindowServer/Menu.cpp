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

#include "Menu.h"
#include "Event.h"
#include "MenuItem.h"
#include "MenuManager.h"
#include "Screen.h"
#include "Window.h"
#include "WindowManager.h"
#include <LibGfx/Bitmap.h>
#include <LibGfx/CharacterBitmap.h>
#include <LibGfx/Font.h>
#include <LibGfx/Painter.h>
#include <LibGfx/StylePainter.h>
#include <LibGfx/Triangle.h>
#include <WindowServer/ClientConnection.h>
#include <WindowServer/WindowClientEndpoint.h>

namespace WindowServer {

Menu::Menu(ClientConnection* client, int menu_id, const String& name)
    : Core::Object(client)
    , m_client(client)
    , m_menu_id(menu_id)
    , m_name(move(name))
{
}

Menu::~Menu()
{
}

void Menu::set_title_font(const Gfx::Font& font)
{
    m_title_font = &font;
}

const Gfx::Font& Menu::title_font() const
{
    return *m_title_font;
}

const Gfx::Font& Menu::font() const
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

int Menu::content_width() const
{
    int widest_text = 0;
    int widest_shortcut = 0;
    for (auto& item : m_items) {
        if (item.type() != MenuItem::Text)
            continue;
        auto& use_font = item.is_default() ? Gfx::Font::default_bold_font() : font();
        int text_width = use_font.width(item.text());
        if (!item.shortcut_text().is_empty()) {
            int shortcut_width = use_font.width(item.shortcut_text());
            widest_shortcut = max(shortcut_width, widest_shortcut);
        }
        widest_text = max(widest_text, text_width);
    }

    int widest_item = widest_text + s_stripe_width;
    if (widest_shortcut)
        widest_item += padding_between_text_and_shortcut() + widest_shortcut;

    return max(widest_item, rect_in_menubar().width()) + horizontal_padding() + frame_thickness() * 2;
}

void Menu::redraw()
{
    if (!menu_window())
        return;
    draw();
    menu_window()->invalidate();
}

Window& Menu::ensure_menu_window()
{
    if (m_menu_window)
        return *m_menu_window;

    int width = this->content_width();

    Gfx::IntPoint next_item_location(frame_thickness(), frame_thickness());
    for (auto& item : m_items) {
        int height = 0;
        if (item.type() == MenuItem::Text)
            height = item_height();
        else if (item.type() == MenuItem::Separator)
            height = 8;
        item.set_rect({ next_item_location, { width - frame_thickness() * 2, height } });
        next_item_location.move_by(0, height);
    }

    int window_height_available = Screen::the().height() - MenuManager::the().menubar_rect().height() - frame_thickness() * 2;
    int max_window_height = (window_height_available / item_height()) * item_height() + frame_thickness() * 2;
    int content_height = m_items.is_empty() ? 0 : (m_items.last().rect().bottom() + 1) + frame_thickness();
    int window_height = min(max_window_height, content_height);
    if (window_height < content_height) {
        m_scrollable = true;
        m_max_scroll_offset = item_count() - window_height / item_height() + 2;
    }

    auto window = Window::construct(*this, WindowType::Menu);
    window->set_rect(0, 0, width, window_height);
    m_menu_window = move(window);
    draw();

    return *m_menu_window;
}

int Menu::visible_item_count() const
{
    if (!is_scrollable())
        return m_items.size();
    ASSERT(m_menu_window);
    // Make space for up/down arrow indicators
    return m_menu_window->height() / item_height() - 2;
}

void Menu::draw()
{
    auto palette = WindowManager::the().palette();
    m_theme_index_at_last_paint = MenuManager::the().theme_index();

    ASSERT(menu_window());
    ASSERT(menu_window()->backing_store());
    Gfx::Painter painter(*menu_window()->backing_store());

    Gfx::IntRect rect { {}, menu_window()->size() };
    Gfx::StylePainter::paint_window_frame(painter, rect, palette);
    painter.fill_rect(rect.shrunken(6, 6), palette.menu_base());
    int width = this->content_width();

    if (!s_checked_bitmap)
        s_checked_bitmap = &Gfx::CharacterBitmap::create_from_ascii(s_checked_bitmap_data, s_checked_bitmap_width, s_checked_bitmap_height).leak_ref();

    bool has_checkable_items = false;
    bool has_items_with_icon = false;
    for (auto& item : m_items) {
        has_checkable_items = has_checkable_items | item.is_checkable();
        has_items_with_icon = has_items_with_icon | !!item.icon();
    }

    Gfx::IntRect stripe_rect { frame_thickness(), frame_thickness(), s_stripe_width, menu_window()->height() - frame_thickness() * 2 };
    painter.fill_rect(stripe_rect, palette.menu_stripe());
    painter.draw_line(stripe_rect.top_right(), stripe_rect.bottom_right(), palette.menu_stripe().darkened());

    int visible_item_count = this->visible_item_count();

    if (is_scrollable()) {
        bool can_go_up = m_scroll_offset > 0;
        bool can_go_down = m_scroll_offset < m_max_scroll_offset;
        Gfx::IntRect up_indicator_rect { frame_thickness(), frame_thickness(), content_width(), item_height() };
        painter.draw_text(up_indicator_rect, "\xE2\xAC\x86", Gfx::TextAlignment::Center, can_go_up ? palette.menu_base_text() : palette.color(ColorRole::DisabledText));
        Gfx::IntRect down_indicator_rect { frame_thickness(), menu_window()->height() - item_height() - frame_thickness(), content_width(), item_height() };
        painter.draw_text(down_indicator_rect, "\xE2\xAC\x87", Gfx::TextAlignment::Center, can_go_down ? palette.menu_base_text() : palette.color(ColorRole::DisabledText));
    }

    for (int i = 0; i < visible_item_count; ++i) {
        auto& item = m_items.at(m_scroll_offset + i);
        if (item.type() == MenuItem::Text) {
            Color text_color = palette.menu_base_text();
            if (&item == hovered_item() && item.is_enabled()) {
                painter.fill_rect(item.rect(), palette.menu_selection());
                painter.draw_rect(item.rect(), palette.menu_selection().darkened());
                text_color = palette.menu_selection_text();
            } else if (!item.is_enabled()) {
                text_color = Color::MidGray;
            }
            Gfx::IntRect text_rect = item.rect().translated(stripe_rect.width() + 6, 0);
            if (item.is_checkable()) {
                if (item.is_exclusive()) {
                    Gfx::IntRect radio_rect { item.rect().x() + 5, 0, 12, 12 };
                    radio_rect.center_vertically_within(text_rect);
                    Gfx::StylePainter::paint_radio_button(painter, radio_rect, palette, item.is_checked(), false);
                } else {
                    Gfx::IntRect checkmark_rect { item.rect().x() + 7, 0, s_checked_bitmap_width, s_checked_bitmap_height };
                    checkmark_rect.center_vertically_within(text_rect);
                    Gfx::IntRect checkbox_rect = checkmark_rect.inflated(4, 4);
                    painter.fill_rect(checkbox_rect, palette.base());
                    Gfx::StylePainter::paint_frame(painter, checkbox_rect, palette, Gfx::FrameShape::Container, Gfx::FrameShadow::Sunken, 2);
                    if (item.is_checked()) {
                        painter.draw_bitmap(checkmark_rect.location(), *s_checked_bitmap, palette.button_text());
                    }
                }
            } else if (item.icon()) {
                Gfx::IntRect icon_rect { item.rect().x() + 3, 0, s_item_icon_width, s_item_icon_width };
                icon_rect.center_vertically_within(text_rect);
                painter.blit(icon_rect.location(), *item.icon(), item.icon()->rect());
            }
            auto& previous_font = painter.font();
            if (item.is_default())
                painter.set_font(Gfx::Font::default_bold_font());
            painter.draw_text(text_rect, item.text(), Gfx::TextAlignment::CenterLeft, text_color);
            if (!item.shortcut_text().is_empty()) {
                painter.draw_text(item.rect().translated(-right_padding(), 0), item.shortcut_text(), Gfx::TextAlignment::CenterRight, text_color);
            }
            painter.set_font(previous_font);
            if (item.is_submenu()) {
                static auto& submenu_arrow_bitmap = Gfx::CharacterBitmap::create_from_ascii(s_submenu_arrow_bitmap_data, s_submenu_arrow_bitmap_width, s_submenu_arrow_bitmap_height).leak_ref();
                Gfx::IntRect submenu_arrow_rect {
                    item.rect().right() - s_submenu_arrow_bitmap_width - 2,
                    0,
                    s_submenu_arrow_bitmap_width,
                    s_submenu_arrow_bitmap_height
                };
                submenu_arrow_rect.center_vertically_within(item.rect());
                painter.draw_bitmap(submenu_arrow_rect.location(), submenu_arrow_bitmap, text_color);
            }
        } else if (item.type() == MenuItem::Separator) {
            Gfx::IntPoint p1(item.rect().translated(stripe_rect.width() + 4, 0).x(), item.rect().center().y() - 1);
            Gfx::IntPoint p2(width - 7, item.rect().center().y() - 1);
            painter.draw_line(p1, p2, palette.threed_shadow1());
            painter.draw_line(p1.translated(0, 1), p2.translated(0, 1), palette.threed_highlight());
        }
    }
}

MenuItem* Menu::hovered_item() const
{
    if (m_hovered_item_index == -1)
        return nullptr;
    return const_cast<MenuItem*>(&item(m_hovered_item_index));
}

void Menu::update_for_new_hovered_item(bool make_input)
{
    if (hovered_item() && hovered_item()->is_submenu()) {
        ASSERT(menu_window());
        MenuManager::the().close_everyone_not_in_lineage(*hovered_item()->submenu());
        hovered_item()->submenu()->do_popup(hovered_item()->rect().top_right().translated(menu_window()->rect().location()), make_input);
    } else {
        MenuManager::the().close_everyone_not_in_lineage(*this);
        ensure_menu_window().set_visible(true);
    }
    redraw();
}

void Menu::open_hovered_item()
{
    ASSERT(menu_window());
    ASSERT(menu_window()->is_visible());
    if (!hovered_item())
        return;
    if (hovered_item()->is_enabled())
        did_activate(*hovered_item());
    clear_hovered_item();
}

void Menu::descend_into_submenu_at_hovered_item()
{
    ASSERT(hovered_item());
    auto submenu = hovered_item()->submenu();
    ASSERT(submenu);
    MenuManager::the().open_menu(*submenu, false);
    submenu->set_hovered_item(0);
    ASSERT(submenu->hovered_item()->type() != MenuItem::Separator);
}

void Menu::handle_mouse_move_event(const MouseEvent& mouse_event)
{
    ASSERT(menu_window());
    MenuManager::the().set_current_menu(this);
    if (hovered_item() && hovered_item()->is_submenu()) {

        auto item = *hovered_item();
        auto submenu_top_left = item.rect().location() + Gfx::IntPoint { item.rect().width(), 0 };
        auto submenu_bottom_left = submenu_top_left + Gfx::IntPoint { 0, item.submenu()->menu_window()->height() };

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

    update_for_new_hovered_item();
    return;
}

void Menu::event(Core::Event& event)
{
    if (event.type() == Event::MouseMove) {
        handle_mouse_move_event(static_cast<const MouseEvent&>(event));
        return;
    }

    if (event.type() == Event::MouseUp) {
        open_hovered_item();
        return;
    }

    if (event.type() == Event::MouseWheel && is_scrollable()) {
        ASSERT(menu_window());
        auto& mouse_event = static_cast<const MouseEvent&>(event);
        m_scroll_offset += mouse_event.wheel_delta();
        m_scroll_offset = clamp(m_scroll_offset, 0, m_max_scroll_offset);

        int index = item_index_at(mouse_event.position());
        if (m_hovered_item_index == index)
            return;

        m_hovered_item_index = index;
        update_for_new_hovered_item();
        return;
    }

    if (event.type() == Event::KeyDown) {
        auto key = static_cast<KeyEvent&>(event).key();

        if (!(key == Key_Up || key == Key_Down || key == Key_Left || key == Key_Right || key == Key_Return))
            return;

        ASSERT(menu_window());
        ASSERT(menu_window()->is_visible());

        // Default to the first item on key press if one has not been selected yet
        if (!hovered_item()) {
            m_hovered_item_index = 0;
            update_for_new_hovered_item(key == Key_Right);
            return;
        }

        if (key == Key_Up) {
            ASSERT(m_items.at(0).type() != MenuItem::Separator);

            if (is_scrollable() && m_hovered_item_index == 0)
                return;

            auto original_index = m_hovered_item_index;
            do {
                if (m_hovered_item_index == 0)
                    m_hovered_item_index = m_items.size() - 1;
                else
                    --m_hovered_item_index;
                if (m_hovered_item_index == original_index)
                    return;
            } while (hovered_item()->type() == MenuItem::Separator || !hovered_item()->is_enabled());

            ASSERT(m_hovered_item_index >= 0 && m_hovered_item_index <= static_cast<int>(m_items.size()) - 1);

            if (is_scrollable() && m_hovered_item_index < m_scroll_offset)
                --m_scroll_offset;

            update_for_new_hovered_item();
            return;
        }

        if (key == Key_Down) {
            ASSERT(m_items.at(0).type() != MenuItem::Separator);

            if (is_scrollable() && m_hovered_item_index == static_cast<int>(m_items.size()) - 1)
                return;

            auto original_index = m_hovered_item_index;
            do {
                if (m_hovered_item_index == static_cast<int>(m_items.size()) - 1)
                    m_hovered_item_index = 0;
                else
                    ++m_hovered_item_index;
                if (m_hovered_item_index == original_index)
                    return;
            } while (hovered_item()->type() == MenuItem::Separator || !hovered_item()->is_enabled());

            ASSERT(m_hovered_item_index >= 0 && m_hovered_item_index <= static_cast<int>(m_items.size()) - 1);

            if (is_scrollable() && m_hovered_item_index >= (m_scroll_offset + visible_item_count()))
                ++m_scroll_offset;

            update_for_new_hovered_item();
            return;
        }
    }
    Core::Object::event(event);
}

void Menu::clear_hovered_item()
{
    if (!hovered_item())
        return;
    m_hovered_item_index = -1;
    redraw();
}

void Menu::did_activate(MenuItem& item)
{
    if (item.type() == MenuItem::Type::Separator)
        return;

    if (on_item_activation)
        on_item_activation(item);

    MenuManager::the().close_bar();

    if (m_client)
        m_client->post_message(Messages::WindowClient::MenuItemActivated(m_menu_id, item.identifier()));
}

bool Menu::activate_default()
{
    for (auto& item : m_items) {
        if (item.type() == MenuItem::Type::Separator)
            continue;
        if (item.is_enabled() && item.is_default()) {
            did_activate(item);
            return true;
        }
    }
    return false;
}

MenuItem* Menu::item_with_identifier(unsigned identifier)
{
    for (auto& item : m_items) {
        if (item.identifier() == identifier)
            return &item;
    }
    return nullptr;
}

int Menu::item_index_at(const Gfx::IntPoint& position)
{
    int i = 0;
    for (auto& item : m_items) {
        if (item.rect().contains(position))
            return i;
        ++i;
    }
    return -1;
}

void Menu::close()
{
    MenuManager::the().close_menu_and_descendants(*this);
}

void Menu::redraw_if_theme_changed()
{
    if (m_theme_index_at_last_paint != MenuManager::the().theme_index())
        redraw();
}

void Menu::popup(const Gfx::IntPoint& position)
{
    do_popup(position, true);
}

void Menu::do_popup(const Gfx::IntPoint& position, bool make_input)
{
    if (is_empty()) {
        dbg() << "Menu: Empty menu popup";
        return;
    }

    auto& window = ensure_menu_window();
    redraw_if_theme_changed();

    const int margin = 30;
    Gfx::IntPoint adjusted_pos = position;

    if (adjusted_pos.x() + window.width() >= Screen::the().width() - margin) {
        adjusted_pos = adjusted_pos.translated(-window.width(), 0);
    }
    if (adjusted_pos.y() + window.height() >= Screen::the().height() - margin) {
        adjusted_pos = adjusted_pos.translated(0, -window.height());
    }

    if (adjusted_pos.y() < MenuManager::the().menubar_rect().height())
        adjusted_pos.set_y(MenuManager::the().menubar_rect().height());

    window.move_to(adjusted_pos);
    window.set_visible(true);
    MenuManager::the().open_menu(*this, make_input);
    WindowManager::the().did_popup_a_menu({});
}

bool Menu::is_menu_ancestor_of(const Menu& other) const
{
    for (auto& item : m_items) {
        if (!item.is_submenu())
            continue;
        auto& submenu = *item.submenu();
        if (&submenu == &other)
            return true;
        if (submenu.is_menu_ancestor_of(other))
            return true;
    }
    return false;
}

}
