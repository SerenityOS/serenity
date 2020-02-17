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

#pragma once

#include "Menu.h"
#include "MenuBar.h"
#include "Window.h"
#include <AK/HashMap.h>
#include <LibCore/Object.h>

namespace WindowServer {

class MenuManager final : public Core::Object {
    C_OBJECT(MenuManager)
public:
    static MenuManager& the();

    MenuManager();
    virtual ~MenuManager() override;

    void refresh();

    bool is_open(const Menu&) const;

    Vector<WeakPtr<Menu>>& open_menu_stack() { return m_open_menu_stack; }

    Gfx::Rect menubar_rect() const;
    static int menubar_menu_margin() { return 16; }

    void set_needs_window_resize();

    Menu* current_menu() { return m_current_menu.ptr(); }
    void set_current_menu(Menu*, bool is_submenu = false);
    void open_menu(Menu&);
    void toggle_menu(Menu&);

    MenuBar* current_menubar() { return m_current_menubar.ptr(); }
    void set_current_menubar(MenuBar*);
    void close_menubar(MenuBar&);

    void close_bar();
    void close_everyone();
    void close_everyone_not_in_lineage(Menu&);
    void close_menu_and_descendants(Menu&);

    void close_all_menus_from_client(Badge<ClientConnection>, ClientConnection&);

    void toggle_system_menu()
    {
        if (m_system_menu)
            toggle_menu(*m_system_menu);
    }

    Menu* system_menu() { return m_system_menu; }
    void set_system_menu(Menu&);

    Color menu_selection_color() const { return m_menu_selection_color; }
    int theme_index() const { return m_theme_index; }

    Window& window() { return *m_window; }

    template<typename Callback>
    void for_each_active_menubar_menu(Callback callback)
    {
        if (system_menu()) {
            if (callback(*system_menu()) == IterationDecision::Break)
                return;
        }
        if (m_current_menubar)
            m_current_menubar->for_each_menu(callback);
    }

    void did_change_theme();

private:
    const Gfx::Font& menu_font() const;
    const Gfx::Font& app_menu_font() const;

    void close_menus(const Vector<Menu*>&);

    const Window& window() const { return *m_window; }

    virtual void event(Core::Event&) override;
    void handle_mouse_event(MouseEvent&);
    void handle_menu_mouse_event(Menu&, const MouseEvent&);

    void draw();

    RefPtr<Window> m_window;

    WeakPtr<Menu> m_current_menu;
    Vector<WeakPtr<Menu>> m_open_menu_stack;

    WeakPtr<Menu> m_system_menu;

    bool m_needs_window_resize { false };
    bool m_bar_open { false };

    Color m_menu_selection_color;

    int m_theme_index { 0 };

    WeakPtr<MenuBar> m_current_menubar;
};

}
