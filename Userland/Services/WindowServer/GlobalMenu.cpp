/*
 * Copyright (c) 2022, Filiph Sandström <filiph.sandstrom@filfatstudios.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Badge.h>
#include <LibGfx/Font/Font.h>
#include <LibGfx/StylePainter.h>
#include <Services/Taskbar/GlobalMenuWindow.h>
#include <WindowServer/Compositor.h>
#include <WindowServer/ConnectionFromClient.h>
#include <WindowServer/GlobalMenu.h>
#include <WindowServer/Screen.h>
#include <WindowServer/WindowManager.h>

namespace WindowServer {

static GlobalMenu* s_the;

GlobalMenu::GlobalMenu()
{
    s_the = this;
    m_ladyball = Gfx::Bitmap::load_from_file("/res/icons/16x16/ladyball.png"sv).release_value_but_fixme_should_propagate_errors();
}

GlobalMenu::~GlobalMenu()
{
}

GlobalMenu& GlobalMenu::the()
{
    VERIFY(s_the);
    return *s_the;
}

void GlobalMenu::set_enabled(bool enabled)
{
    if (enabled == m_enabled)
        return;

    if (enabled) {
        m_enabled = true;

        m_window = Window::construct(*this, WindowType::GlobalMenu);
        m_window->set_title("GlobalMenu");
        m_window->set_has_alpha_channel(true);
        m_active_window = nullptr;
        m_dirty = true;
        return;
    }

    m_enabled = false;
    if (m_window)
        m_window->destroy();

    m_window = nullptr;
    m_active_window = nullptr;
    m_painter = nullptr;
}

void GlobalMenu::set_rect(Gfx::IntRect const& rect)
{
    if (!m_enabled)
        return;

    m_window->set_rect(rect);
    m_dirty = true;
    m_active_window = nullptr;
    m_painter = nullptr;

    handle_active_window_changed();
}

void GlobalMenu::invalidate()
{
    invalidate(rect());
}

void GlobalMenu::invalidate(Gfx::IntRect rect)
{
    m_window->invalidate(rect, true);
    m_dirty = true;
    paint();
}

int GlobalMenu::paint_title(bool paint)
{
    auto text = !m_active_window ? "SerenityOS" : m_active_window->title();
    auto& icon = !m_active_window ? *m_ladyball : m_active_window->icon();

    auto& wm = WindowManager::the();
    auto& font = wm.font().bold_variant();
    auto palette = wm.palette();

    auto icon_rect = icon.rect();
    icon_rect.center_vertically_within(rect());
    icon_rect.set_x(rect().x());
    icon_rect.set_y(icon_rect.y());

    if (paint)
        m_painter->blit(icon_rect.location(), icon, icon.rect(), 1.0f);
    auto icon_width = icon_rect.x() + icon_rect.width() + 8;

    Gfx::IntRect text_rect = { icon_width, 0, static_cast<int>(font.width(text)) + 2, m_window->height() };
    if (paint)
        m_painter->draw_ui_text(text_rect, text, font, Gfx::TextAlignment::CenterLeft, palette.window_text());

    int component_width = icon_width + text_rect.width();
    return component_width;
}

void GlobalMenu::event(Core::Event& event)
{
    if (!m_enabled || !m_window || !m_painter)
        return;

    switch (event.type()) {
    case Event::MouseMove:
    case Event::MouseDown:
    case Event::MouseUp:
        handle_mouse_event(static_cast<MouseEvent const&>(event));
        break;
    case Event::WindowEntered:
        m_dirty = true;
        m_hovering = true;
        break;
    case Event::WindowLeft:
        m_dirty = true;
        m_hovering = false;
        invalidate();
        break;
    default:
        break;
    }

    paint();
}

void GlobalMenu::handle_mouse_event(MouseEvent const& event)
{
    if (!m_active_window)
        return;

    auto active_menu = m_active_window->menubar();
    if (!active_menu.has_menus())
        return;

    Menu* hovered_menu = nullptr;
    active_menu.for_each_menu([&](Menu& menu) {
        if (menu.rect_in_window_menubar().contains(event.position())) {
            hovered_menu = &menu;
            handle_menu_mouse_event(&menu, event);
            return IterationDecision::Break;
        } else if (MenuManager::the().hovered_menu() == &menu && event.type() == Event::Type::MouseDown) {
            // Make sure we only close menus from the menubar
            if (menu.menu_window() && menu.menu_window()->rect().contains(event.position()))
                return IterationDecision::Break;

            MenuManager::the().close_everyone();
            MenuManager::the().set_hovered_menu(nullptr);
            return IterationDecision::Break;
        }
        return IterationDecision::Continue;
    });

    if (hovered_menu && hovered_menu != MenuManager::the().hovered_menu()) {
        MenuManager::the().set_hovered_menu(hovered_menu);
        invalidate();
    }
}

bool GlobalMenu::has_active_menu()
{
    return m_active_window && m_active_window == WindowManager::the().window_with_active_menu();
}

void GlobalMenu::handle_menu_mouse_event(Menu* menu, MouseEvent const& event)
{
    bool is_hover_with_any_menu_open = m_active_window == WindowManager::the().window_with_active_menu();
    bool is_mousedown_with_left_button = event.type() == MouseEvent::MouseDown && event.button() == MouseButton::Primary;
    bool should_open_menu = menu != MenuManager::the().current_menu() && (is_hover_with_any_menu_open || is_mousedown_with_left_button);
    bool should_close_menu = menu == MenuManager::the().current_menu() && is_mousedown_with_left_button;

    if (should_close_menu) {
        invalidate();
        MenuManager::the().close_everyone();
        return;
    }

    if (should_open_menu) {
        open_menubar_menu(menu);
    }
}

void GlobalMenu::open_menubar_menu(Menu* menu)
{
    MenuManager::the().close_everyone();
    auto position = menu->rect_in_window_menubar().bottom_left().translated(rect().location());
    menu->set_unadjusted_position(position);
    auto& window = menu->ensure_menu_window(position);
    auto window_rect = window.rect();

    window.set_rect(position.x() + 1, GlobalMenuWindow::global_menu_height(), window_rect.width(), window_rect.height());

    MenuManager::the().open_menu(*menu);
    WindowManager::the().set_window_with_active_menu(m_active_window);
    invalidate();
}

void GlobalMenu::paint()
{
    if (!m_enabled || !m_window || rect().is_empty() || !m_dirty)
        return;
    if (!m_painter)
        m_painter = make<Gfx::Painter>(*m_window->backing_store());

    m_dirty = false;
    m_painter->clear_rect(rect(), Gfx::Color::Transparent);

    paint_title();
    if (!m_active_window)
        return;

    auto active_menu = m_active_window->menubar();
    if (!active_menu.has_menus())
        return;

    auto& wm = WindowManager::the();
    auto& font = wm.font();
    auto palette = wm.palette();

    active_menu.for_each_menu([&](Menu& menu) {
        auto rect = menu.rect_in_window_menubar();

        auto is_open = menu.is_open();
        Color text_color = (is_open ? palette.menu_selection_text() : palette.window_text());

        bool paint_as_pressed = is_open;
        if ((paint_as_pressed)) {
            m_painter->fill_rect(rect, palette.menu_selection());
        }

        bool paint_as_hovered = !paint_as_pressed && m_hovering && &menu == MenuManager::the().hovered_menu();
        if ((paint_as_hovered)) {
            m_painter->fill_rect(rect, palette.hover_highlight());
        }

        m_painter->draw_ui_text(rect, menu.name(), is_open ? font.bold_variant() : font, Gfx::TextAlignment::Center, text_color);
        return IterationDecision::Continue;
    });
}

void GlobalMenu::handle_active_window_changed()
{
    if (!m_enabled || !m_window)
        return;

    auto newly_active_window = WindowManager::the().active_window();
    if (!newly_active_window || newly_active_window->title().is_empty()) {
        m_active_window = nullptr;
    } else {
        if (newly_active_window->type() != WindowType::Normal) {
            m_active_window = nullptr;
        } else {
            m_active_window = newly_active_window;
            auto active_menu = m_active_window->menubar();

            // Trick the menubar into a relayout.
            active_menu.font_changed(m_active_window->rect());

            // FIXME: Only run this when needed (like on a new window or when the menu has changed).
            if (active_menu.has_menus()) {
                active_menu.for_each_menu([&](Menu& menu) {
                    auto menu_rect = menu.rect_in_window_menubar().translated(paint_title(false), 0);
                    menu_rect.center_vertically_within(rect());
                    menu.set_rect_in_window_menubar({ { rect().left() + menu_rect.left(), rect().y() }, { menu_rect.width(), rect().height() + 1 } });
                    return IterationDecision::Continue;
                });
            }
        }
    }

    invalidate();
}

void GlobalMenu::handle_active_window_closed()
{
    if (!m_enabled)
        return;

    m_active_window = nullptr;
    handle_active_window_changed();
}
}
