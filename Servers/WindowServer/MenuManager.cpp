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

#include <AK/Badge.h>
#include <AK/FileSystemPath.h>
#include <AK/QuickSort.h>
#include <LibCore/DirIterator.h>
#include <LibGfx/Font.h>
#include <LibGfx/Painter.h>
#include <WindowServer/AppletManager.h>
#include <WindowServer/MenuManager.h>
#include <WindowServer/Screen.h>
#include <WindowServer/WindowManager.h>
#include <unistd.h>

//#define DEBUG_MENUS

namespace WindowServer {

static MenuManager* s_the;

MenuManager& MenuManager::the()
{
    ASSERT(s_the);
    return *s_the;
}

MenuManager::MenuManager()
{
    s_the = this;
    m_needs_window_resize = true;

    // NOTE: This ensures that the system menu has the correct dimensions.
    set_current_menubar(nullptr);

    m_window = Window::construct(*this, WindowType::Menubar);
    m_window->set_rect(menubar_rect());
}

MenuManager::~MenuManager()
{
}

bool MenuManager::is_open(const Menu& menu) const
{
    for (size_t i = 0; i < m_open_menu_stack.size(); ++i) {
        if (&menu == m_open_menu_stack[i].ptr())
            return true;
    }
    return false;
}

void MenuManager::draw()
{
    auto& wm = WindowManager::the();
    auto palette = wm.palette();
    auto menubar_rect = this->menubar_rect();

    if (m_needs_window_resize) {
        m_window->set_rect(menubar_rect);
        AppletManager::the().calculate_applet_rects(window());
        m_needs_window_resize = false;
    }

    Gfx::Painter painter(*window().backing_store());

    painter.fill_rect(menubar_rect, palette.window());
    painter.draw_line({ 0, menubar_rect.bottom() }, { menubar_rect.right(), menubar_rect.bottom() }, palette.threed_shadow1());

    for_each_active_menubar_menu([&](Menu& menu) {
        Color text_color = palette.window_text();
        if (is_open(menu)) {
            painter.fill_rect(menu.rect_in_menubar(), palette.menu_selection());
            painter.draw_rect(menu.rect_in_menubar(), palette.menu_selection().darkened());
            text_color = palette.menu_selection_text();
        }
        painter.draw_text(
            menu.text_rect_in_menubar(),
            menu.name(),
            menu.title_font(),
            Gfx::TextAlignment::CenterLeft,
            text_color);
        return IterationDecision::Continue;
    });

    AppletManager::the().draw();
}

void MenuManager::refresh()
{
    if (!m_window)
        return;
    draw();
    window().invalidate();
}

void MenuManager::event(Core::Event& event)
{
    if (WindowManager::the().active_window_is_modal())
        return Core::Object::event(event);

    if (static_cast<Event&>(event).is_mouse_event()) {
        handle_mouse_event(static_cast<MouseEvent&>(event));
        return;
    }

    if (static_cast<Event&>(event).is_key_event()) {
        auto& key_event = static_cast<const KeyEvent&>(event);

        if (key_event.type() == Event::KeyUp && key_event.key() == Key_Escape) {
            close_everyone();
            return;
        }

        if (event.type() == Event::KeyDown) {
            for_each_active_menubar_menu([&](Menu& menu) {
                if (is_open(menu))
                    menu.dispatch_event(event);
                return IterationDecision::Continue;
            });
        }
    }

    return Core::Object::event(event);
}

void MenuManager::handle_mouse_event(MouseEvent& mouse_event)
{
    bool handled_menubar_event = false;
    for_each_active_menubar_menu([&](Menu& menu) {
        if (menu.rect_in_menubar().contains(mouse_event.position())) {
            handle_menu_mouse_event(menu, mouse_event);
            handled_menubar_event = true;
            return IterationDecision::Break;
        }
        return IterationDecision::Continue;
    });
    if (handled_menubar_event)
        return;

    if (has_open_menu()) {
        auto* topmost_menu = m_open_menu_stack.last().ptr();
        ASSERT(topmost_menu);
        auto* window = topmost_menu->menu_window();
        ASSERT(window);
        ASSERT(window->is_visible());

        bool event_is_inside_current_menu = window->rect().contains(mouse_event.position());
        if (event_is_inside_current_menu) {
            WindowManager::the().set_hovered_window(window);
            auto translated_event = mouse_event.translated(-window->position());
            WindowManager::the().deliver_mouse_event(*window, translated_event);
            return;
        }

        if (topmost_menu->hovered_item())
            topmost_menu->clear_hovered_item();
        if (mouse_event.type() == Event::MouseDown || mouse_event.type() == Event::MouseUp) {
            auto* window_menu_of = topmost_menu->window_menu_of();
            if (window_menu_of) {
                bool event_is_inside_taskbar_button = window_menu_of->taskbar_rect().contains(mouse_event.position());
                if (event_is_inside_taskbar_button && !topmost_menu->is_window_menu_open()) {
                    topmost_menu->set_window_menu_open(true);
                    return;
                }
            }

            if (mouse_event.type() == Event::MouseDown) {
                close_bar();
                topmost_menu->set_window_menu_open(false);
            }
        }

        if (mouse_event.type() == Event::MouseMove) {
            for (auto& menu : m_open_menu_stack) {
                if (!menu)
                    continue;
                if (!menu->menu_window()->rect().contains(mouse_event.position()))
                    continue;
                WindowManager::the().set_hovered_window(menu->menu_window());
                auto translated_event = mouse_event.translated(-menu->menu_window()->position());
                WindowManager::the().deliver_mouse_event(*menu->menu_window(), translated_event);
                break;
            }
        }
        return;
    }

    AppletManager::the().dispatch_event(static_cast<Event&>(mouse_event));
}

void MenuManager::handle_menu_mouse_event(Menu& menu, const MouseEvent& event)
{
    bool is_hover_with_any_menu_open = event.type() == MouseEvent::MouseMove
        && has_open_menu()
        && (m_open_menu_stack.first()->menubar() || m_open_menu_stack.first() == m_system_menu.ptr());
    bool is_mousedown_with_left_button = event.type() == MouseEvent::MouseDown && event.button() == MouseButton::Left;
    bool should_open_menu = &menu != m_current_menu && (is_hover_with_any_menu_open || is_mousedown_with_left_button);

    if (is_mousedown_with_left_button)
        m_bar_open = !m_bar_open;

    if (should_open_menu && m_bar_open) {
        open_menu(menu);
        return;
    }

    if (!m_bar_open)
        close_everyone();
}

void MenuManager::set_needs_window_resize()
{
    m_needs_window_resize = true;
}

void MenuManager::close_all_menus_from_client(Badge<ClientConnection>, ClientConnection& client)
{
    if (!has_open_menu())
        return;
    if (m_open_menu_stack.first()->client() != &client)
        return;
    close_everyone();
}

void MenuManager::close_everyone()
{
    for (auto& menu : m_open_menu_stack) {
        if (menu && menu->menu_window())
            menu->menu_window()->set_visible(false);
        menu->clear_hovered_item();
    }
    m_open_menu_stack.clear();
    m_current_menu = nullptr;
    refresh();
}

void MenuManager::close_everyone_not_in_lineage(Menu& menu)
{
    Vector<Menu*> menus_to_close;
    for (auto& open_menu : m_open_menu_stack) {
        if (!open_menu)
            continue;
        if (&menu == open_menu.ptr() || open_menu->is_menu_ancestor_of(menu))
            continue;
        menus_to_close.append(open_menu);
    }
    close_menus(menus_to_close);
}

void MenuManager::close_menus(const Vector<Menu*>& menus)
{
    for (auto& menu : menus) {
        if (menu == m_current_menu)
            m_current_menu = nullptr;
        if (menu->menu_window())
            menu->menu_window()->set_visible(false);
        menu->clear_hovered_item();
        m_open_menu_stack.remove_first_matching([&](auto& entry) {
            return entry == menu;
        });
    }
    refresh();
}

static void collect_menu_subtree(Menu& menu, Vector<Menu*>& menus)
{
    menus.append(&menu);
    for (int i = 0; i < menu.item_count(); ++i) {
        auto& item = menu.item(i);
        if (!item.is_submenu())
            continue;
        collect_menu_subtree(*const_cast<MenuItem&>(item).submenu(), menus);
    }
}

void MenuManager::close_menu_and_descendants(Menu& menu)
{
    Vector<Menu*> menus_to_close;
    collect_menu_subtree(menu, menus_to_close);
    close_menus(menus_to_close);
}

void MenuManager::toggle_menu(Menu& menu)
{
    if (is_open(menu)) {
        close_menu_and_descendants(menu);
        return;
    }
    open_menu(menu);
}

void MenuManager::open_menu(Menu& menu)
{
    if (is_open(menu))
        return;
    if (!menu.is_empty()) {
        menu.redraw_if_theme_changed();
        auto& menu_window = menu.ensure_menu_window();
        menu_window.move_to({ menu.rect_in_menubar().x(), menu.rect_in_menubar().bottom() + 2 });
        menu_window.set_visible(true);
    }
    set_current_menu(&menu);
    refresh();
}

void MenuManager::set_current_menu(Menu* menu, bool is_submenu)
{
    if (menu == m_current_menu)
        return;

    if (!is_submenu) {
        if (menu)
            close_everyone_not_in_lineage(*menu);
        else
            close_everyone();
    }

    if (!menu) {
        m_current_menu = nullptr;
        return;
    }

    m_current_menu = menu->make_weak_ptr();
    if (m_open_menu_stack.find([menu](auto& other) { return menu == other.ptr(); }).is_end())
        m_open_menu_stack.append(menu->make_weak_ptr());
}

void MenuManager::close_bar()
{
    close_everyone();
    m_bar_open = false;
}

Gfx::Rect MenuManager::menubar_rect() const
{
    return { 0, 0, Screen::the().rect().width(), 18 };
}

void MenuManager::set_current_menubar(MenuBar* menubar)
{
    if (menubar)
        m_current_menubar = menubar->make_weak_ptr();
    else
        m_current_menubar = nullptr;
#ifdef DEBUG_MENUS
    dbg() << "[WM] Current menubar is now " << menubar;
#endif
    Gfx::Point next_menu_location { MenuManager::menubar_menu_margin() / 2, 0 };
    for_each_active_menubar_menu([&](Menu& menu) {
        int text_width = menu.title_font().width(menu.name());
        menu.set_rect_in_menubar({ next_menu_location.x() - MenuManager::menubar_menu_margin() / 2, 0, text_width + MenuManager::menubar_menu_margin(), menubar_rect().height() - 1 });
        menu.set_text_rect_in_menubar({ next_menu_location, { text_width, menubar_rect().height() } });
        next_menu_location.move_by(menu.rect_in_menubar().width(), 0);
        return IterationDecision::Continue;
    });
    refresh();
}

void MenuManager::close_menubar(MenuBar& menubar)
{
    if (current_menubar() == &menubar)
        set_current_menubar(nullptr);
}

void MenuManager::set_system_menu(Menu& menu)
{
    m_system_menu = menu.make_weak_ptr();
    set_current_menubar(m_current_menubar);
}

void MenuManager::did_change_theme()
{
    ++m_theme_index;
    refresh();
}

}
