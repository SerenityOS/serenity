/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2020, Shannon Booth <shannon@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Menu.h"
#include "Event.h"
#include "MenuItem.h"
#include "MenuManager.h"
#include "Screen.h"
#include "Window.h"
#include "WindowManager.h"
#include <AK/CharacterTypes.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/CharacterBitmap.h>
#include <LibGfx/Font/Font.h>
#include <LibGfx/Painter.h>
#include <LibGfx/StylePainter.h>
#include <LibGfx/Triangle.h>
#include <WindowServer/ConnectionFromClient.h>
#include <WindowServer/WindowClientEndpoint.h>

namespace WindowServer {

u32 find_ampersand_shortcut_character(StringView string)
{
    Utf8View utf8_view { string };
    for (auto it = utf8_view.begin(); it != utf8_view.end(); ++it) {
        if (*it == '&') {
            ++it;
            if (it != utf8_view.end() && *it != '&')
                return *it;
        }
    }
    return 0;
}

Menu::Menu(ConnectionFromClient* client, int menu_id, String name, int minimum_width)
    : Core::EventReceiver(client)
    , m_client(client)
    , m_menu_id(menu_id)
    , m_name(move(name))
    , m_minimum_width(minimum_width)
{
    m_alt_shortcut_character = find_ampersand_shortcut_character(m_name);
}

Gfx::Font const& Menu::font() const
{
    return Gfx::FontDatabase::default_font();
}

static constexpr Gfx::CharacterBitmap s_submenu_arrow_bitmap {
    "         "
    "   #     "
    "   ##    "
    "   ###   "
    "   ####  "
    "   ###   "
    "   ##    "
    "   #     "
    "         "sv,
    9, 9
};

static constexpr int s_item_icon_width = 16;
static constexpr int s_stripe_width = 24;

int Menu::content_width() const
{
    int widest_text = 0;
    int widest_shortcut = 0;
    for (auto& item : m_items) {
        if (!item->is_visible())
            continue;
        if (item->type() != MenuItem::Text)
            continue;
        auto& use_font = item->is_default() ? font().bold_variant() : font();
        int text_width = use_font.width(Gfx::parse_ampersand_string(item->text()));
        if (!item->shortcut_text().is_empty()) {
            int shortcut_width = use_font.width(item->shortcut_text());
            widest_shortcut = max(shortcut_width, widest_shortcut);
        }
        widest_text = max(widest_text, text_width);
    }

    int widest_item = widest_text + s_stripe_width;
    if (widest_shortcut)
        widest_item += padding_between_text_and_shortcut() + widest_shortcut;

    return max(m_minimum_width, max(widest_item, rect_in_window_menubar().width()) + horizontal_padding() + frame_thickness() * 2);
}

int Menu::item_height() const
{
    return max(static_cast<int>(ceilf(font().preferred_line_height())), s_item_icon_width + 2) + 4;
}

void Menu::redraw()
{
    if (!menu_window())
        return;
    draw();
    menu_window()->invalidate();
}

void Menu::redraw(MenuItem const& menu_item)
{
    if (!menu_window())
        return;
    if (!menu_item.is_visible())
        return;
    draw(menu_item);
    menu_window()->invalidate(menu_item.rect());
}

void Menu::invalidate_menu_window()
{
    m_menu_window = nullptr;
}

Window& Menu::ensure_menu_window(Gfx::IntPoint position)
{
    auto& screen = Screen::closest_to_location(position);
    int width = this->content_width();

    auto calculate_window_rect = [&]() -> Gfx::IntRect {
        int window_height_available = screen.height() - frame_thickness() * 2;
        int max_window_height = (window_height_available / item_height()) * item_height() + frame_thickness() * 2;
        int content_height = 0;
        // find the last visible item to determine the required menu height
        for (size_t i = m_items.size(); i > 0; i--) {
            if (m_items[i - 1]->is_visible()) {
                content_height = m_items[i - 1]->rect().bottom() + frame_thickness();
                break;
            }
        }
        int window_height = min(max_window_height, content_height);
        if (window_height < content_height) {
            m_scrollable = true;
            m_max_scroll_offset = item_count() - window_height / item_height() + 2;
        }
        return { position, { width, window_height } };
    };

    Gfx::IntPoint next_item_location(frame_thickness(), frame_thickness());
    for (auto& item : m_items) {
        if (!item->is_visible())
            continue;
        int height = 0;
        if (item->type() == MenuItem::Text)
            height = item_height();
        else if (item->type() == MenuItem::Separator)
            height = 8;
        item->set_rect({ next_item_location, { width - frame_thickness() * 2, height } });
        next_item_location.translate_by(0, height);
    }

    if (m_menu_window) {
        // We might be on a different screen than previously, so recalculate the
        // menu's rectangle as we have more or less screen available now
        auto new_rect = calculate_window_rect();
        if (new_rect != m_menu_window->rect()) {
            auto size_changed = new_rect.size() != m_menu_window->rect().size();
            m_menu_window->set_rect(new_rect);
            if (size_changed)
                draw();
        }
    } else {
        auto window = Window::construct(*this, WindowType::Menu);
        window->set_visible(false);
        window->set_rect(calculate_window_rect());
        m_menu_window = move(window);
        draw();
    }
    return *m_menu_window;
}

size_t Menu::visible_item_count() const
{
    if (!is_scrollable())
        return m_items.size();
    VERIFY(m_menu_window);
    // Make space for up/down arrow indicators
    return m_menu_window->height() / item_height() - 2;
}

Gfx::IntRect Menu::stripe_rect()
{
    return { frame_thickness(), frame_thickness(), s_stripe_width, menu_window()->height() - frame_thickness() * 2 };
}

void Menu::draw()
{
    auto palette = WindowManager::the().palette();
    m_theme_index_at_last_paint = MenuManager::the().theme_index();

    VERIFY(menu_window());

    // When an application has an empty menu, we don't want to draw it
    if (menu_window()->backing_store() == nullptr)
        return;

    Gfx::Painter painter(*menu_window()->backing_store());

    Gfx::IntRect rect { {}, menu_window()->size() };
    painter.draw_rect(rect, Color::Black);
    painter.fill_rect(rect.shrunken(2, 2), palette.menu_base());

    // Draw the stripe first, which may extend outside of individual items. We can
    // skip this step when painting an individual item since we're drawing all of them
    painter.fill_rect(stripe_rect(), palette.menu_stripe());

    if (is_scrollable()) {
        bool can_go_up = m_scroll_offset > 0;
        bool can_go_down = m_scroll_offset < m_max_scroll_offset;
        Gfx::IntRect up_indicator_rect { frame_thickness(), frame_thickness(), content_width(), item_height() };
        painter.draw_text(up_indicator_rect, "\xE2\xAC\x86"sv, Gfx::TextAlignment::Center, can_go_up ? palette.menu_base_text() : palette.color(ColorRole::DisabledText));
        Gfx::IntRect down_indicator_rect { frame_thickness(), menu_window()->height() - item_height() - frame_thickness(), content_width(), item_height() };
        painter.draw_text(down_indicator_rect, "\xE2\xAC\x87"sv, Gfx::TextAlignment::Center, can_go_down ? palette.menu_base_text() : palette.color(ColorRole::DisabledText));
    }

    int visible_item_count = this->visible_item_count();
    for (int i = 0; i < visible_item_count; ++i)
        draw(*m_items[m_scroll_offset + i], true);
}

void Menu::draw(MenuItem const& item, bool is_drawing_all)
{
    if (!item.is_visible())
        return;

    auto palette = WindowManager::the().palette();
    int width = this->content_width();
    Gfx::Painter painter(*menu_window()->backing_store());
    painter.add_clip_rect(item.rect());

    auto stripe_rect = this->stripe_rect();
    if (!is_drawing_all) {
        // If we're redrawing all of them then we already did this in draw()
        painter.fill_rect(stripe_rect, palette.menu_stripe());
        for (auto& rect : item.rect().shatter(stripe_rect))
            painter.fill_rect(rect, palette.menu_base());
    }

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
                Gfx::IntRect checkbox_rect { item.rect().x() + 5, 0, 13, 13 };
                checkbox_rect.center_vertically_within(text_rect);
                Gfx::StylePainter::paint_check_box(painter, checkbox_rect, palette, item.is_enabled(), item.is_checked(), false);
            }
        } else if (item.icon()) {
            Gfx::IntRect icon_rect { item.rect().x() + 3, 0, s_item_icon_width, s_item_icon_width };
            icon_rect.center_vertically_within(text_rect);

            if (&item == hovered_item() && item.is_enabled()) {
                auto shadow_color = palette.menu_selection().darkened(0.7f);
                painter.blit_filtered(icon_rect.location().translated(1, 1), *item.icon(), item.icon()->rect(), [&shadow_color](auto) {
                    return shadow_color;
                });
                icon_rect.translate_by(-1, -1);
            }
            if (item.is_enabled())
                painter.blit(icon_rect.location(), *item.icon(), item.icon()->rect());
            else
                painter.blit_disabled(icon_rect.location(), *item.icon(), item.icon()->rect(), palette);
        }
        auto& previous_font = painter.font();
        if (item.is_default())
            painter.set_font(previous_font.bold_variant());
        painter.draw_ui_text(text_rect, item.text(), painter.font(), Gfx::TextAlignment::CenterLeft, text_color);
        if (!item.shortcut_text().is_empty()) {
            painter.draw_text(item.rect().translated(-right_padding(), 0), item.shortcut_text(), Gfx::TextAlignment::CenterRight, text_color);
        }
        painter.set_font(previous_font);
        if (item.is_submenu()) {
            Gfx::IntRect submenu_arrow_rect {
                item.rect().right() - static_cast<int>(s_submenu_arrow_bitmap.width()) - 3,
                0,
                s_submenu_arrow_bitmap.width(),
                s_submenu_arrow_bitmap.height()
            };
            submenu_arrow_rect.center_vertically_within(item.rect());
            painter.draw_bitmap(submenu_arrow_rect.location(), s_submenu_arrow_bitmap, text_color);
        }
    } else if (item.type() == MenuItem::Separator) {
        Gfx::IntPoint p1(item.rect().translated(stripe_rect.width() + 4, 0).x(), item.rect().center().y() - 1);
        Gfx::IntPoint p2(width - 7, item.rect().center().y() - 1);
        painter.draw_line(p1, p2, palette.threed_shadow1());
        painter.draw_line(p1.translated(0, 1), p2.translated(0, 1), palette.threed_highlight());
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
    if (auto* hovered_item = this->hovered_item()) {
        if (hovered_item->is_submenu()) {
            VERIFY(menu_window());
            MenuManager::the().close_everyone_not_in_lineage(*hovered_item->submenu());
            hovered_item->submenu()->do_popup(hovered_item->rect().top_right().translated(-1, 0).translated(menu_window()->rect().location()), make_input, true);
        } else {
            MenuManager::the().close_everyone_not_in_lineage(*this);
            VERIFY(menu_window());
            set_visible(true);
        }
    }
}

void Menu::open_hovered_item(bool leave_menu_open)
{
    VERIFY(menu_window());
    VERIFY(menu_window()->is_visible());
    if (!hovered_item())
        return;
    if (hovered_item()->is_enabled()) {
        did_activate(*hovered_item(), leave_menu_open);
        if (!leave_menu_open)
            clear_hovered_item();
    }
}

void Menu::descend_into_submenu_at_hovered_item()
{
    VERIFY(hovered_item());
    auto submenu = hovered_item()->submenu();
    VERIFY(submenu);
    MenuManager::the().open_menu(*submenu, true);
    submenu->set_hovered_index(0);
    VERIFY(submenu->hovered_item()->type() != MenuItem::Separator);
}

void Menu::handle_mouse_move_event(MouseEvent const& mouse_event)
{
    VERIFY(menu_window());
    MenuManager::the().set_current_menu(this);
    if (hovered_item() && hovered_item()->is_submenu()) {

        auto item = *hovered_item();
        auto submenu_top = item.submenu()->menu_window()->rect().top_left() - menu_window()->position();
        auto submenu_bottom = item.submenu()->menu_window()->rect().bottom_left() - menu_window()->position();

        if (item.submenu()->opens_to_the_left()) {
            submenu_top = item.submenu()->menu_window()->rect().top_right() - menu_window()->position();
            submenu_bottom = item.submenu()->menu_window()->rect().bottom_right() - menu_window()->position();
        }

        auto safe_hover_triangle = Gfx::Triangle { m_last_position_in_hover, submenu_top, submenu_bottom };
        m_last_position_in_hover = mouse_event.position();

        // Don't update the hovered item if mouse is moving towards a submenu
        if (safe_hover_triangle.contains(mouse_event.position()))
            return;
    }

    int index = item_index_at(mouse_event.position());
    set_hovered_index(index);
}

void Menu::event(Core::Event& event)
{
    if (event.type() == Event::MouseMove) {
        handle_mouse_move_event(static_cast<MouseEvent const&>(event));
        return;
    }

    if (event.type() == Event::MouseUp) {
        open_hovered_item(static_cast<MouseEvent&>(event).modifiers() & KeyModifier::Mod_Ctrl);
        return;
    }

    if (event.type() == Event::MouseWheel && is_scrollable()) {
        VERIFY(menu_window());
        auto& mouse_event = static_cast<MouseEvent const&>(event);
        auto previous_scroll_offset = m_scroll_offset;
        m_scroll_offset += mouse_event.wheel_delta_y();
        m_scroll_offset = clamp(m_scroll_offset, 0, m_max_scroll_offset);
        if (m_scroll_offset != previous_scroll_offset)
            redraw();

        int index = item_index_at(mouse_event.position());
        set_hovered_index(index);
        return;
    }

    if (event.type() == Event::KeyDown) {
        auto key = static_cast<KeyEvent&>(event).key();

        if (!(key == Key_Up || key == Key_Down || key == Key_Left || key == Key_Right || key == Key_Return))
            return;

        VERIFY(menu_window());
        VERIFY(menu_window()->is_visible());

        if (!hovered_item()) {
            if (key == Key_Up) {
                // Default to the last enabled, non-separator item on key press if one has not been selected yet
                for (auto i = static_cast<int>(m_items.size()) - 1; i >= 0; i--) {
                    auto& item = m_items.at(i);
                    if (!item->is_visible())
                        continue;
                    if (item->type() != MenuItem::Separator && item->is_enabled()) {
                        set_hovered_index(i, key == Key_Right);
                        break;
                    }
                }
            } else {
                // Default to the first enabled, non-separator item on key press if one has not been selected yet
                int counter = 0;
                for (auto const& item : m_items) {
                    if (!item->is_visible())
                        continue;
                    if (item->type() != MenuItem::Separator && item->is_enabled()) {
                        set_hovered_index(counter, key == Key_Right);
                        break;
                    }
                    counter++;
                }
            }
            return;
        }

        if (key == Key_Up) {
            VERIFY(item(0).type() != MenuItem::Separator);

            if (is_scrollable() && m_hovered_item_index == 0)
                return;

            auto original_index = m_hovered_item_index;
            auto new_index = original_index;
            do {
                if (new_index == 0)
                    new_index = m_items.size() - 1;
                else
                    --new_index;
                if (new_index == original_index)
                    return;
            } while (item(new_index).type() == MenuItem::Separator || !item(new_index).is_enabled() || !item(new_index).is_visible());

            VERIFY(new_index >= 0);
            VERIFY(new_index <= static_cast<int>(m_items.size()) - 1);

            if (is_scrollable() && new_index < m_scroll_offset)
                --m_scroll_offset;

            set_hovered_index(new_index);
            return;
        }

        if (key == Key_Down) {
            VERIFY(item(0).type() != MenuItem::Separator);

            if (is_scrollable() && m_hovered_item_index == static_cast<int>(m_items.size()) - 1)
                return;

            auto original_index = m_hovered_item_index;
            auto new_index = original_index;
            do {
                if (new_index == static_cast<int>(m_items.size()) - 1)
                    new_index = 0;
                else
                    ++new_index;
                if (new_index == original_index)
                    return;
            } while (item(new_index).type() == MenuItem::Separator || !item(new_index).is_enabled() || !item(new_index).is_visible());

            VERIFY(new_index >= 0);
            VERIFY(new_index <= static_cast<int>(m_items.size()) - 1);

            if (is_scrollable() && new_index >= (m_scroll_offset + static_cast<int>(visible_item_count())))
                ++m_scroll_offset;

            set_hovered_index(new_index);
            return;
        }
    }
    Core::EventReceiver::event(event);
}

void Menu::clear_hovered_item()
{
    set_hovered_index(-1);
}

void Menu::start_activation_animation(MenuItem& item)
{
    if (!WindowManager::the().system_effects().animate_menus())
        return;
    VERIFY(menu_window());
    VERIFY(menu_window()->backing_store());
    auto window = Window::construct(*this, WindowType::Menu);
    window->set_frameless(true);
    window->set_hit_testing_enabled(false);
    window->set_has_alpha_channel(true);
    window->set_rect(item.rect().translated(m_menu_window->rect().location()));
    window->set_event_filter([](Core::Event&) {
        // ignore all events
        return false;
    });

    VERIFY(window->backing_store());

    NonnullRefPtr<Gfx::Bitmap> original_bitmap = *menu_window()->backing_store();
    Gfx::IntRect item_rect = item.rect();
    Gfx::Painter painter(*window->backing_store());
    painter.blit({}, original_bitmap, item_rect, 0.8f, true); // start out transparent so we don't have to recompute occlusions
    window->invalidate();

    struct AnimationInfo {
        RefPtr<Core::Timer> timer;
        RefPtr<Window> window;
        u8 step { 8 }; // Must be even number!

        AnimationInfo(NonnullRefPtr<Window>&& window)
            : window(move(window))
        {
        }
    };
    auto animation = adopt_own(*new AnimationInfo(move(window)));
    auto& timer = animation->timer;
    timer = Core::Timer::create_repeating(50, [animation = animation.ptr(), animation_ref = move(animation), original_bitmap, item_rect] {
        VERIFY(animation->step % 2 == 0);
        animation->step -= 2;
        if (animation->step == 0) {
            animation->window->set_visible(false);
            animation->timer->stop();
            animation->timer = nullptr; // break circular reference
            return;
        }

        float opacity = (float)animation->step / 10.0f;
        Gfx::Painter painter(*animation->window->backing_store());
        painter.clear_rect({ {}, animation->window->rect().size() }, Color::Transparent);
        painter.blit({}, original_bitmap, item_rect, opacity, true);
        animation->window->invalidate();
    });
    timer->start();
}

void Menu::did_activate(MenuItem& item, bool leave_menu_open)
{
    if (item.type() == MenuItem::Type::Separator)
        return;

    if (m_client)
        m_client->async_menu_item_activated(m_menu_id, item.identifier());

    if (!leave_menu_open)
        start_activation_animation(item);

    if (on_item_activation)
        on_item_activation(item);

    if (!leave_menu_open)
        MenuManager::the().close_everyone();
}

bool Menu::activate_default()
{
    for (auto& item : m_items) {
        if (!item->is_visible())
            continue;
        if (item->type() == MenuItem::Type::Separator)
            continue;
        if (item->is_enabled() && item->is_default()) {
            did_activate(*item, false);
            return true;
        }
    }
    return false;
}

MenuItem* Menu::item_with_identifier(unsigned identifier)
{
    for (auto& item : m_items) {
        if (item->identifier() == identifier)
            return item;
    }
    return nullptr;
}

bool Menu::remove_item_with_identifier(unsigned identifier)
{
    return m_items.remove_first_matching([&](auto& item) { return item->identifier() == identifier; });
}

int Menu::item_index_at(Gfx::IntPoint position)
{
    for (int i = 0; i < static_cast<int>(m_items.size()); ++i) {
        auto const& item = m_items[i];
        if (!item->is_visible())
            continue;
        if (item->rect().contains(position))
            return i;
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

void Menu::open_button_menu(Gfx::IntPoint position, Gfx::IntRect const& button_rect)
{
    if (is_empty())
        return;

    auto& screen = Screen::closest_to_location(position);
    auto& window = ensure_menu_window(position);
    Gfx::IntPoint adjusted_pos = position;

    if (window.rect().right() - 1 > screen.width())
        adjusted_pos = adjusted_pos.translated(-(window.rect().right() - screen.width()), 0);

    if (window.rect().bottom() - 1 > screen.height())
        adjusted_pos = adjusted_pos.translated(0, -window.rect().height() - button_rect.height() + 1);

    window.set_rect(adjusted_pos.x(), adjusted_pos.y(), window.rect().width(), window.rect().height());
    window.move_to(adjusted_pos);
    MenuManager::the().open_menu(*this, true);
    WindowManager::the().did_popup_a_menu({});
}

void Menu::popup(Gfx::IntPoint position)
{
    do_popup(position, true);
}

void Menu::do_popup(Gfx::IntPoint position, bool make_input, bool as_submenu)
{
    if (is_empty()) {
        dbgln("Menu: Empty menu popup");
        return;
    }

    auto& screen = Screen::closest_to_location(position);
    auto& window = ensure_menu_window(position);
    redraw_if_theme_changed();

    constexpr auto margin = 10;
    Gfx::IntPoint adjusted_pos = m_unadjusted_position = position;
    m_opens_to_the_left = false;

    if (adjusted_pos.x() + window.width() >= screen.rect().right() - margin) {
        // Horizontally translate the window by its full width, i.e. flip it at its vertical axis.
        adjusted_pos = adjusted_pos.translated(-window.width(), 0);
        // If the window is a submenu, translate to the opposite side of its immediate ancestor
        if (auto* ancestor = MenuManager::the().closest_open_ancestor_of(*this); ancestor && as_submenu) {
            constexpr auto offset = 1 + frame_thickness() * 2;
            adjusted_pos = adjusted_pos.translated(-ancestor->menu_window()->width() + offset, 0);
        }
        m_opens_to_the_left = true;
    } else {
        // Even if no adjustment needs to be done, move the menu to the right by 1px so it's not
        // underneath the cursor and can be closed by another click at the same position.
        adjusted_pos.set_x(adjusted_pos.x() + 1);
    }
    if (adjusted_pos.y() + window.height() >= screen.rect().bottom() - margin) {
        // Vertically translate the window by its full height, i.e. flip it at its horizontal axis.
        auto offset = window.height();
        // ...but if it's a submenu, go back by one menu item height to keep the menu aligned with
        // its parent item, if possible.
        if (as_submenu)
            offset -= item_height();
        // Before translating, clamp the calculated offset to the current distance between the
        // screen and menu top edges to avoid going off-screen.
        adjusted_pos = adjusted_pos.translated(0, -min(offset, adjusted_pos.y()));
    }

    window.move_to(adjusted_pos);
    MenuManager::the().open_menu(*this, make_input);
    WindowManager::the().did_popup_a_menu({});
}

bool Menu::is_menu_ancestor_of(Menu const& other) const
{
    for (auto& item : m_items) {
        if (!item->is_submenu())
            continue;
        auto& submenu = *item->submenu();
        if (&submenu == &other)
            return true;
        if (submenu.is_menu_ancestor_of(other))
            return true;
    }
    return false;
}

void Menu::set_visible(bool visible)
{
    if (!menu_window())
        return;
    if (visible == menu_window()->is_visible())
        return;
    menu_window()->set_visible(visible);
    if (m_client)
        m_client->async_menu_visibility_did_change(m_menu_id, visible);
}

void Menu::update_alt_shortcuts_for_items()
{
    m_alt_shortcut_character_to_item_indices.clear();
    int i = 0;
    for (auto& item : m_items) {
        if (auto alt_shortcut = find_ampersand_shortcut_character(item->text())) {
            m_alt_shortcut_character_to_item_indices.ensure(to_ascii_lowercase(alt_shortcut)).append(i);
        }
        ++i;
    }
}

void Menu::add_item(NonnullOwnPtr<MenuItem> item)
{
    m_items.append(move(item));
    update_alt_shortcuts_for_items();
}

Vector<size_t> const* Menu::items_with_alt_shortcut(u32 alt_shortcut) const
{
    auto it = m_alt_shortcut_character_to_item_indices.find(to_ascii_lowercase(alt_shortcut));
    if (it == m_alt_shortcut_character_to_item_indices.end())
        return nullptr;
    return &it->value;
}

void Menu::set_hovered_index(int index, bool make_input)
{
    if (m_hovered_item_index == index)
        return;
    auto* old_hovered_item = hovered_item();
    if (old_hovered_item) {
        if (client() && old_hovered_item->type() != MenuItem::Type::Separator)
            client()->async_menu_item_left(m_menu_id, old_hovered_item->identifier());
    }
    m_hovered_item_index = index;
    update_for_new_hovered_item(make_input);
    if (auto* new_hovered_item = hovered_item()) {
        if (client() && new_hovered_item->type() != MenuItem::Type::Separator)
            client()->async_menu_item_entered(m_menu_id, new_hovered_item->identifier());
        redraw(*new_hovered_item);
    }
    if (old_hovered_item)
        redraw(*old_hovered_item);
}

void Menu::set_name(String name)
{
    m_name = move(name);
}

void Menu::set_minimum_width(int minimum_width)
{
    m_minimum_width = minimum_width;
}

bool Menu::is_open() const
{
    return MenuManager::the().is_open(*this);
}

}
