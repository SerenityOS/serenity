/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
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

#include <AK/FileSystemPath.h>
#include <AK/QuickSort.h>
#include <LibCore/DirIterator.h>
#include <LibGfx/Font.h>
#include <LibGfx/Painter.h>
#include <WindowServer/WSMenuManager.h>
#include <WindowServer/WSScreen.h>
#include <WindowServer/WSWindowManager.h>
#include <unistd.h>

//#define DEBUG_MENUS

static WSMenuManager* s_the;

WSMenuManager& WSMenuManager::the()
{
    ASSERT(s_the);
    return *s_the;
}

WSMenuManager::WSMenuManager()
{
    s_the = this;
    m_username = getlogin();
    m_needs_window_resize = true;

    HashTable<String> seen_app_categories;
    {
        Core::DirIterator dt("/res/apps", Core::DirIterator::SkipDots);
        while (dt.has_next()) {
            auto af_name = dt.next_path();
            auto af_path = String::format("/res/apps/%s", af_name.characters());
            auto af = Core::ConfigFile::open(af_path);
            if (!af->has_key("App", "Name") || !af->has_key("App", "Executable"))
                continue;
            auto app_name = af->read_entry("App", "Name");
            auto app_executable = af->read_entry("App", "Executable");
            auto app_category = af->read_entry("App", "Category");
            auto app_icon_path = af->read_entry("Icons", "16x16");
            m_apps.append({ app_executable, app_name, app_icon_path, app_category });
            seen_app_categories.set(app_category);
        }
    }

    Vector<String> sorted_app_categories;
    for (auto& category : seen_app_categories)
        sorted_app_categories.append(category);
    quick_sort(sorted_app_categories.begin(), sorted_app_categories.end(), [](auto& a, auto& b) { return a < b; });

    u8 system_menu_name[] = { 0xc3, 0xb8, 0 };
    m_system_menu = WSMenu::construct(nullptr, -1, String((const char*)system_menu_name));

    // First we construct all the necessary app category submenus.
    for (const auto& category : sorted_app_categories) {

        if (m_app_category_menus.contains(category))
            continue;
        auto category_menu = WSMenu::construct(nullptr, 5000 + m_app_category_menus.size(), category);
        category_menu->on_item_activation = [this](auto& item) {
            if (item.identifier() >= 1 && item.identifier() <= 1u + m_apps.size() - 1) {
                if (fork() == 0) {
                    const auto& bin = m_apps[item.identifier() - 1].executable;
                    execl(bin.characters(), bin.characters(), nullptr);
                    ASSERT_NOT_REACHED();
                }
            }
        };
        auto item = make<WSMenuItem>(*m_system_menu, -1, category);
        item->set_submenu_id(category_menu->menu_id());
        m_system_menu->add_item(move(item));
        m_app_category_menus.set(category, move(category_menu));
    }

    // Then we create and insert all the app menu items into the right place.
    int app_identifier = 1;
    for (const auto& app : m_apps) {
        auto parent_menu = m_app_category_menus.get(app.category).value_or(*m_system_menu);
        parent_menu->add_item(make<WSMenuItem>(*m_system_menu, app_identifier++, app.name, String(), true, false, false, Gfx::Bitmap::load_from_file(app.icon_path)));
    }

    m_system_menu->add_item(make<WSMenuItem>(*m_system_menu, WSMenuItem::Separator));

    m_themes_menu = WSMenu::construct(nullptr, 9000, "Themes");

    auto themes_menu_item = make<WSMenuItem>(*m_system_menu, 100, "Themes");
    themes_menu_item->set_submenu_id(m_themes_menu->menu_id());
    m_system_menu->add_item(move(themes_menu_item));

    {
        Core::DirIterator dt("/res/themes", Core::DirIterator::SkipDots);
        while (dt.has_next()) {
            auto theme_name = dt.next_path();
            auto theme_path = String::format("/res/themes/%s", theme_name.characters());
            m_themes.append({ FileSystemPath(theme_name).title(), theme_path });
        }
        quick_sort(m_themes.begin(), m_themes.end(), [](auto& a, auto& b) { return a.name < b.name; });
    }

    {
        int theme_identifier = 9000;
        for (auto& theme : m_themes) {
            m_themes_menu->add_item(make<WSMenuItem>(*m_themes_menu, theme_identifier++, theme.name));
        }
    }

    m_themes_menu->on_item_activation = [this](WSMenuItem& item) {
        auto& theme = m_themes[(int)item.identifier() - 9000];
        WSWindowManager::the().update_theme(theme.path, theme.name);
        ++m_theme_index;
    };

    m_system_menu->add_item(make<WSMenuItem>(*m_system_menu, WSMenuItem::Separator));
    m_system_menu->add_item(make<WSMenuItem>(*m_system_menu, 100, "Reload WM Config File"));

    m_system_menu->add_item(make<WSMenuItem>(*m_system_menu, WSMenuItem::Separator));
    m_system_menu->add_item(make<WSMenuItem>(*m_system_menu, 200, "About...", String(), true, false, false, Gfx::Bitmap::load_from_file("/res/icons/16x16/ladybug.png")));
    m_system_menu->add_item(make<WSMenuItem>(*m_system_menu, WSMenuItem::Separator));
    m_system_menu->add_item(make<WSMenuItem>(*m_system_menu, 300, "Shutdown..."));
    m_system_menu->on_item_activation = [this](WSMenuItem& item) {
        if (item.identifier() >= 1 && item.identifier() <= 1u + m_apps.size() - 1) {
            if (fork() == 0) {
                const auto& bin = m_apps[item.identifier() - 1].executable;
                execl(bin.characters(), bin.characters(), nullptr);
                ASSERT_NOT_REACHED();
            }
        }
        switch (item.identifier()) {
        case 100:
            WSWindowManager::the().reload_config(true);
            break;
        case 200:
            if (fork() == 0) {
                execl("/bin/About", "/bin/About", nullptr);
                ASSERT_NOT_REACHED();
            }
            return;
        case 300:
            if (fork() == 0) {
                execl("/bin/SystemDialog", "/bin/SystemDialog", "--shutdown", nullptr);
                ASSERT_NOT_REACHED();
            }
            return;
        }
#ifdef DEBUG_MENUS
        dbg() << "WSMenu 1 item activated: " << item.text();
#endif
    };

    // NOTE: This ensures that the system menu has the correct dimensions.
    set_current_menubar(nullptr);

    m_window = WSWindow::construct(*this, WSWindowType::Menubar);
    m_window->set_rect(menubar_rect());
}

WSMenuManager::~WSMenuManager()
{
}

bool WSMenuManager::is_open(const WSMenu& menu) const
{
    for (int i = 0; i < m_open_menu_stack.size(); ++i) {
        if (&menu == m_open_menu_stack[i].ptr())
            return true;
    }
    return false;
}

void WSMenuManager::draw()
{
    auto& wm = WSWindowManager::the();
    auto palette = wm.palette();
    auto menubar_rect = this->menubar_rect();

    if (m_needs_window_resize) {
        int username_width = Gfx::Font::default_bold_font().width(m_username);

        m_username_rect = {
            menubar_rect.right() - menubar_menu_margin() / 2 - Gfx::Font::default_bold_font().width(m_username),
            menubar_rect.y(),
            username_width,
            menubar_rect.height()
        };

        int right_edge_x = m_username_rect.left() - 4;
        for (auto& existing_applet : m_applets) {
            if (!existing_applet)
                continue;

            Gfx::Rect new_applet_rect(right_edge_x - existing_applet->size().width(), 0, existing_applet->size().width(), existing_applet->size().height());
            Gfx::Rect dummy_menubar_rect(0, 0, 0, 18);
            new_applet_rect.center_vertically_within(dummy_menubar_rect);

            existing_applet->set_rect_in_menubar(new_applet_rect);
            right_edge_x = existing_applet->rect_in_menubar().x() - 4;
        }

        m_window->set_rect(menubar_rect);
        m_needs_window_resize = false;
    }

    Gfx::Painter painter(*window().backing_store());

    painter.fill_rect(menubar_rect, palette.window());
    painter.draw_line({ 0, menubar_rect.bottom() }, { menubar_rect.right(), menubar_rect.bottom() }, palette.threed_shadow1());
    int index = 0;
    for_each_active_menubar_menu([&](WSMenu& menu) {
        Color text_color = palette.window_text();
        if (is_open(menu)) {
            painter.fill_rect(menu.rect_in_menubar(), palette.menu_selection());
            painter.draw_rect(menu.rect_in_menubar(), palette.menu_selection().darkened());
            text_color = Color::White;
        }
        painter.draw_text(
            menu.text_rect_in_menubar(),
            menu.name(),
            index == 1 ? wm.app_menu_font() : wm.menu_font(),
            Gfx::TextAlignment::CenterLeft,
            text_color);
        ++index;
        return IterationDecision::Continue;
    });

    painter.draw_text(m_username_rect, m_username, Gfx::Font::default_bold_font(), Gfx::TextAlignment::CenterRight, palette.window_text());

    for (auto& applet : m_applets) {
        if (!applet)
            continue;
        draw_applet(*applet);
    }
}

void WSMenuManager::tick_clock()
{
    refresh();
}

void WSMenuManager::refresh()
{
    if (!m_window)
        return;
    draw();
    window().invalidate();
}

void WSMenuManager::event(Core::Event& event)
{
    if (WSWindowManager::the().active_window_is_modal())
        return Core::Object::event(event);

    if (event.type() == WSEvent::MouseMove || event.type() == WSEvent::MouseUp || event.type() == WSEvent::MouseDown || event.type() == WSEvent::MouseWheel) {

        auto& mouse_event = static_cast<WSMouseEvent&>(event);
        for_each_active_menubar_menu([&](WSMenu& menu) {
            if (menu.rect_in_menubar().contains(mouse_event.position())) {
                handle_menu_mouse_event(menu, mouse_event);
                return IterationDecision::Break;
            }
            return IterationDecision::Continue;
        });

        for (auto& applet : m_applets) {
            if (!applet)
                continue;
            if (!applet->rect_in_menubar().contains(mouse_event.position()))
                continue;
            auto local_event = mouse_event.translated(-applet->rect_in_menubar().location());
            applet->event(local_event);
        }
    }

    if (static_cast<WSEvent&>(event).is_key_event()) {
        auto& key_event = static_cast<const WSKeyEvent&>(event);

        if (key_event.type() == WSEvent::KeyUp && key_event.key() == Key_Escape) {
            close_everyone();
            return;
        }

        if (event.type() == WSEvent::KeyDown) {
            for_each_active_menubar_menu([&](WSMenu& menu) {
                if (is_open(menu))
                    menu.dispatch_event(event);
                return IterationDecision::Continue;
            });
        }
    }

    return Core::Object::event(event);
}

void WSMenuManager::handle_menu_mouse_event(WSMenu& menu, const WSMouseEvent& event)
{
    bool is_hover_with_any_menu_open = event.type() == WSMouseEvent::MouseMove
        && !m_open_menu_stack.is_empty()
        && (m_open_menu_stack.first()->menubar() || m_open_menu_stack.first() == m_system_menu.ptr());
    bool is_mousedown_with_left_button = event.type() == WSMouseEvent::MouseDown && event.button() == MouseButton::Left;
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

void WSMenuManager::set_needs_window_resize()
{
    m_needs_window_resize = true;
}

void WSMenuManager::close_all_menus_from_client(Badge<WSClientConnection>, WSClientConnection& client)
{
    if (m_open_menu_stack.is_empty())
        return;
    if (m_open_menu_stack.first()->client() != &client)
        return;
    close_everyone();
}

void WSMenuManager::close_everyone()
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

void WSMenuManager::close_everyone_not_in_lineage(WSMenu& menu)
{
    Vector<WSMenu*> menus_to_close;
    for (auto& open_menu : m_open_menu_stack) {
        if (!open_menu)
            continue;
        if (&menu == open_menu.ptr() || open_menu->is_menu_ancestor_of(menu))
            continue;
        menus_to_close.append(open_menu);
    }
    close_menus(menus_to_close);
}

void WSMenuManager::close_menus(const Vector<WSMenu*>& menus)
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

static void collect_menu_subtree(WSMenu& menu, Vector<WSMenu*>& menus)
{
    menus.append(&menu);
    for (int i = 0; i < menu.item_count(); ++i) {
        auto& item = menu.item(i);
        if (!item.is_submenu())
            continue;
        collect_menu_subtree(*const_cast<WSMenuItem&>(item).submenu(), menus);
    }
}

void WSMenuManager::close_menu_and_descendants(WSMenu& menu)
{
    Vector<WSMenu*> menus_to_close;
    collect_menu_subtree(menu, menus_to_close);
    close_menus(menus_to_close);
}

void WSMenuManager::toggle_menu(WSMenu& menu)
{
    if (is_open(menu)) {
        close_menu_and_descendants(menu);
        return;
    }
    open_menu(menu);
}

void WSMenuManager::open_menu(WSMenu& menu)
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

void WSMenuManager::set_current_menu(WSMenu* menu, bool is_submenu)
{
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

    m_open_menu_stack.append(menu->make_weak_ptr());
    m_current_menu = menu->make_weak_ptr();
}

void WSMenuManager::close_bar()
{
    close_everyone();
    m_bar_open = false;
}

void WSMenuManager::add_applet(WSWindow& applet)
{
    int right_edge_x = m_username_rect.left() - 4;
    for (auto& existing_applet : m_applets) {
        if (existing_applet)
            right_edge_x = existing_applet->rect_in_menubar().x() - 4;
    }

    Gfx::Rect new_applet_rect(right_edge_x - applet.size().width(), 0, applet.size().width(), applet.size().height());
    Gfx::Rect dummy_menubar_rect(0, 0, 0, 18);
    new_applet_rect.center_vertically_within(dummy_menubar_rect);

    applet.set_rect_in_menubar(new_applet_rect);
    m_applets.append(applet.make_weak_ptr());
}

void WSMenuManager::remove_applet(WSWindow& applet)
{
    m_applets.remove_first_matching([&](auto& entry) {
        return &applet == entry.ptr();
    });
}

void WSMenuManager::draw_applet(const WSWindow& applet)
{
    if (!applet.backing_store())
        return;
    Gfx::Painter painter(*window().backing_store());
    painter.fill_rect(applet.rect_in_menubar(), WSWindowManager::the().palette().window());
    painter.blit(applet.rect_in_menubar().location(), *applet.backing_store(), applet.backing_store()->rect());
}

void WSMenuManager::invalidate_applet(const WSWindow& applet, const Gfx::Rect& rect)
{
    draw_applet(applet);
    window().invalidate(rect.translated(applet.rect_in_menubar().location()));
}

Gfx::Rect WSMenuManager::menubar_rect() const
{
    return { 0, 0, WSScreen::the().rect().width(), 18 };
}

WSMenu* WSMenuManager::find_internal_menu_by_id(int menu_id)
{
    if (m_themes_menu->menu_id() == menu_id)
        return m_themes_menu.ptr();
    for (auto& it : m_app_category_menus) {
        if (menu_id == it.value->menu_id())
            return it.value;
    }
    return nullptr;
}

void WSMenuManager::set_current_menubar(WSMenuBar* menubar)
{
    if (menubar)
        m_current_menubar = menubar->make_weak_ptr();
    else
        m_current_menubar = nullptr;
#ifdef DEBUG_MENUS
    dbg() << "[WM] Current menubar is now " << menubar;
#endif
    Gfx::Point next_menu_location { WSMenuManager::menubar_menu_margin() / 2, 0 };
    int index = 0;
    for_each_active_menubar_menu([&](WSMenu& menu) {
        int text_width = index == 1 ? Gfx::Font::default_bold_font().width(menu.name()) : Gfx::Font::default_font().width(menu.name());
        menu.set_rect_in_menubar({ next_menu_location.x() - WSMenuManager::menubar_menu_margin() / 2, 0, text_width + WSMenuManager::menubar_menu_margin(), menubar_rect().height() - 1 });
        menu.set_text_rect_in_menubar({ next_menu_location, { text_width, menubar_rect().height() } });
        next_menu_location.move_by(menu.rect_in_menubar().width(), 0);
        ++index;
        return IterationDecision::Continue;
    });
    refresh();
}

void WSMenuManager::close_menubar(WSMenuBar& menubar)
{
    if (current_menubar() == &menubar)
        set_current_menubar(nullptr);
}
