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
#include <AK/Debug.h>
#include <AK/QuickSort.h>
#include <LibCore/DirIterator.h>
#include <LibGfx/Font.h>
#include <LibGfx/Painter.h>
#include <WindowServer/AppletManager.h>
#include <WindowServer/ClientConnection.h>
#include <WindowServer/MenuManager.h>
#include <WindowServer/Screen.h>
#include <WindowServer/WindowManager.h>
#include <unistd.h>

namespace WindowServer {

static MenuManager* s_the;
static constexpr int s_search_timeout = 3000;

MenuManager& MenuManager::the()
{
    VERIFY(s_the);
    return *s_the;
}

MenuManager::MenuManager()
{
    s_the = this;
    m_needs_window_resize = true;

    m_window = Window::construct(*this, WindowType::Menubar);
    m_window->set_rect(menubar_rect());
    m_window->set_visible(false);

    m_search_timer = Core::Timer::create_single_shot(0, [this] {
        m_current_search.clear();
    });
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
    painter.draw_line({ 0, menubar_rect.bottom() - 1 }, { menubar_rect.right(), menubar_rect.bottom() - 1 }, palette.threed_shadow1());
    painter.draw_line({ 0, menubar_rect.bottom() }, { menubar_rect.right(), menubar_rect.bottom() }, palette.threed_shadow2());

    AppletManager::the().draw();
}

void MenuManager::refresh()
{
    if (!m_window)
        return;
    draw();
    window().invalidate();

    ClientConnection::for_each_client([&](ClientConnection& client) {
        client.for_each_menu([&](Menu& menu) {
            menu.redraw();
            return IterationDecision::Continue;
        });
    });
}

void MenuManager::event(Core::Event& event)
{
    auto& wm = WindowManager::the();

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

        if (key_event.key() == Key_Backspace) {
            m_current_search.clear();
            return;
        }

        if (m_current_menu && event.type() == Event::KeyDown
            && ((key_event.key() >= Key_A && key_event.key() <= Key_Z)
                || (key_event.key() >= Key_0 && key_event.key() <= Key_9))) {
            m_current_search.append_code_point(key_event.code_point());
            m_search_timer->restart(s_search_timeout);
            for (int i = 0; i < m_current_menu->item_count(); ++i) {
                auto text = m_current_menu->item(i).text();
                if (text.to_lowercase().starts_with(m_current_search.to_string().to_lowercase())) {
                    m_current_menu->set_hovered_item(i);
                    return;
                }
            }
            return;
        }

        if (event.type() == Event::KeyDown) {

            if (key_event.key() == Key_Left) {
                auto it = m_open_menu_stack.find_if([&](const auto& other) { return m_current_menu == other.ptr(); });
                VERIFY(!it.is_end());

                // Going "back" a menu should be the previous menu in the stack
                if (it.index() > 0)
                    set_current_menu(m_open_menu_stack.at(it.index() - 1));
                else {
                    if (m_current_menu->hovered_item())
                        m_current_menu->set_hovered_item(-1);
                    else {
                        auto* target_menu = previous_menu(m_current_menu);
                        if (target_menu) {
                            target_menu->ensure_menu_window().move_to(target_menu->rect_in_window_menubar().bottom_left().translated(wm.window_with_active_menu()->frame().rect().location()).translated(wm.window_with_active_menu()->frame().menubar_rect().location()));
                            open_menu(*target_menu);
                            wm.window_with_active_menu()->invalidate_menubar();
                        }
                    }
                }
                close_everyone_not_in_lineage(*m_current_menu);
                return;
            }

            if (key_event.key() == Key_Right) {
                auto hovered_item = m_current_menu->hovered_item();
                if (hovered_item && hovered_item->is_submenu())
                    m_current_menu->descend_into_submenu_at_hovered_item();
                else if (m_open_menu_stack.size() <= 1 && wm.window_with_active_menu()) {
                    auto* target_menu = next_menu(m_current_menu);
                    if (target_menu) {
                        target_menu->ensure_menu_window().move_to(target_menu->rect_in_window_menubar().bottom_left().translated(wm.window_with_active_menu()->frame().rect().location()).translated(wm.window_with_active_menu()->frame().menubar_rect().location()));
                        open_menu(*target_menu);
                        wm.window_with_active_menu()->invalidate_menubar();
                        close_everyone_not_in_lineage(*target_menu);
                    }
                }
                return;
            }

            if (key_event.key() == Key_Return) {
                auto hovered_item = m_current_menu->hovered_item();
                if (!hovered_item || !hovered_item->is_enabled())
                    return;
                if (hovered_item->is_submenu())
                    m_current_menu->descend_into_submenu_at_hovered_item();
                else
                    m_current_menu->open_hovered_item(key_event.modifiers() & KeyModifier::Mod_Ctrl);
                return;
            }
            m_current_menu->dispatch_event(event);
        }
    }

    return Core::Object::event(event);
}

void MenuManager::handle_mouse_event(MouseEvent& mouse_event)
{
    if (has_open_menu()) {
        auto* topmost_menu = m_open_menu_stack.last().ptr();
        VERIFY(topmost_menu);
        auto* window = topmost_menu->menu_window();
        if (!window) {
            dbgln("MenuManager::handle_mouse_event: No menu window");
            return;
        }
        VERIFY(window->is_visible());

        bool event_is_inside_current_menu = window->rect().contains(mouse_event.position());
        if (event_is_inside_current_menu) {
            WindowManager::the().set_hovered_window(window);
            auto translated_event = mouse_event.translated(-window->position());
            WindowManager::the().deliver_mouse_event(*window, translated_event, true);
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
                for (auto& menu : m_open_menu_stack) {
                    if (!menu)
                        continue;
                    if (!menu->menu_window()->rect().contains(mouse_event.position()))
                        continue;
                    return;
                }
                MenuManager::the().close_everyone();
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
                WindowManager::the().deliver_mouse_event(*menu->menu_window(), translated_event, true);
                break;
            }
        }
        return;
    }

    AppletManager::the().dispatch_event(static_cast<Event&>(mouse_event));
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
        VERIFY(menu);
        if (menu->menu_window())
            menu->menu_window()->set_visible(false);
        menu->clear_hovered_item();
    }
    m_open_menu_stack.clear();
    m_current_search.clear();
    clear_current_menu();
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
            clear_current_menu();
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
        collect_menu_subtree(*item.submenu(), menus);
    }
}

void MenuManager::close_menu_and_descendants(Menu& menu)
{
    Vector<Menu*> menus_to_close;
    collect_menu_subtree(menu, menus_to_close);
    close_menus(menus_to_close);
}

void MenuManager::set_hovered_menu(Menu* menu)
{
    if (m_hovered_menu == menu)
        return;
    if (menu) {
        m_hovered_menu = menu->make_weak_ptr<Menu>();
    } else {
        // FIXME: This is quite aggressive. If we knew which window the previously hovered menu was in,
        //        we could just invalidate that one instead of iterating all windows in the client.
        if (auto* client = m_hovered_menu->client()) {
            client->for_each_window([&](Window& window) {
                window.invalidate_menubar();
                return IterationDecision::Continue;
            });
        }
        m_hovered_menu = nullptr;
    }
}

void MenuManager::open_menu(Menu& menu, bool as_current_menu)
{
    if (is_open(menu)) {
        if (as_current_menu || current_menu() != &menu) {
            // This menu is already open. If requested, or if the current
            // window doesn't match this one, then set it to this
            set_current_menu(&menu);
        }
        return;
    }

    if (!menu.is_empty()) {
        menu.redraw_if_theme_changed();
        if (!menu.menu_window())
            menu.ensure_menu_window();
        menu.menu_window()->set_visible(true);
    }

    if (m_open_menu_stack.find_if([&menu](auto& other) { return &menu == other.ptr(); }).is_end())
        m_open_menu_stack.append(menu);

    if (as_current_menu || !current_menu()) {
        // Only make this menu the current menu if requested, or if no
        // other menu is current
        set_current_menu(&menu);
    }

    refresh();
}

void MenuManager::clear_current_menu()
{
    Menu* previous_current_menu = m_current_menu;
    m_current_menu = nullptr;
    if (previous_current_menu) {
        // When closing the last menu, restore the previous active input window
        auto& wm = WindowManager::the();
        wm.restore_active_input_window(m_previous_input_window);
        if (auto* window = wm.window_with_active_menu()) {
            window->invalidate_menubar();
        }
        wm.set_window_with_active_menu(nullptr);
    }
}

void MenuManager::set_current_menu(Menu* menu)
{
    if (!menu) {
        clear_current_menu();
        return;
    }

    VERIFY(is_open(*menu));
    if (menu == m_current_menu) {
        return;
    }

    m_current_search.clear();

    Menu* previous_current_menu = m_current_menu;
    m_current_menu = menu;

    auto& wm = WindowManager::the();
    if (!previous_current_menu) {
        // When opening the first menu, store the current active input window
        if (auto* active_input = wm.active_input_window())
            m_previous_input_window = *active_input;
        else
            m_previous_input_window = nullptr;
    }

    wm.set_active_input_window(m_current_menu->menu_window());
}

Gfx::IntRect MenuManager::menubar_rect() const
{
    return { 0, 0, Screen::the().rect().width(), 19 };
}

Menu* MenuManager::previous_menu(Menu* current)
{
    auto& wm = WindowManager::the();
    if (!wm.window_with_active_menu())
        return nullptr;
    Menu* found = nullptr;
    Menu* previous = nullptr;
    wm.window_with_active_menu()->menubar()->for_each_menu([&](Menu& menu) {
        if (current == &menu) {
            found = previous;
            return IterationDecision::Break;
        }
        previous = &menu;
        return IterationDecision::Continue;
    });
    return found;
}

Menu* MenuManager::next_menu(Menu* current)
{
    Menu* found = nullptr;
    bool is_next = false;
    auto& wm = WindowManager::the();
    if (!wm.window_with_active_menu())
        return nullptr;
    wm.window_with_active_menu()->menubar()->for_each_menu([&](Menu& menu) {
        if (is_next) {
            found = &menu;
            return IterationDecision::Break;
        }
        if (current == &menu)
            is_next = true;
        return IterationDecision::Continue;
    });
    return found;
}

void MenuManager::did_change_theme()
{
    ++m_theme_index;
    refresh();
}

}
