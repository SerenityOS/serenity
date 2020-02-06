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

#include "WSMenu.h"
#include "WSMenuBar.h"
#include <AK/HashMap.h>
#include <LibCore/CObject.h>
#include <WindowServer/WSWindow.h>

class AClientConnection;

class WSMenuManager final : public Core::Object {
    C_OBJECT(WSMenuManager)
public:
    static WSMenuManager& the();

    WSMenuManager();
    virtual ~WSMenuManager() override;

    void refresh();

    virtual void event(Core::Event&) override;

    bool is_open(const WSMenu&) const;

    Vector<WeakPtr<WSMenu>>& open_menu_stack() { return m_open_menu_stack; }

    Rect menubar_rect() const;
    static int menubar_menu_margin() { return 16; }

    void set_needs_window_resize();

    WSMenu* current_menu() { return m_current_menu.ptr(); }
    void set_current_menu(WSMenu*, bool is_submenu = false);
    void open_menu(WSMenu&);
    void toggle_menu(WSMenu&);

    WSMenuBar* current_menubar() { return m_current_menubar.ptr(); }
    void set_current_menubar(WSMenuBar*);
    void close_menubar(WSMenuBar&);

    void close_bar();
    void close_everyone();
    void close_everyone_not_in_lineage(WSMenu&);
    void close_menu_and_descendants(WSMenu&);

    void close_all_menus_from_client(Badge<WSClientConnection>, WSClientConnection&);

    void add_applet(WSWindow&);
    void remove_applet(WSWindow&);
    void invalidate_applet(const WSWindow&, const Gfx::Rect&);

    Color menu_selection_color() const { return m_menu_selection_color; }
    WSMenu& system_menu() { return *m_system_menu; }
    WSMenu* find_internal_menu_by_id(int);
    int theme_index() const { return m_theme_index; }

    template<typename Callback>
    void for_each_active_menubar_menu(Callback callback)
    {
        if (callback(system_menu()) == IterationDecision::Break)
            return;
        if (m_current_menubar)
            m_current_menubar->for_each_menu(callback);
    }

private:
    void close_menus(const Vector<WSMenu*>&);

    WSWindow& window() { return *m_window; }
    const WSWindow& window() const { return *m_window; }

    void handle_menu_mouse_event(WSMenu&, const WSMouseEvent&);

    void draw();
    void draw_applet(const WSWindow&);
    void tick_clock();

    RefPtr<WSWindow> m_window;
    String m_username;

    WeakPtr<WSMenu> m_current_menu;
    Vector<WeakPtr<WSMenu>> m_open_menu_stack;

    Vector<WeakPtr<WSWindow>> m_applets;

    Gfx::Rect m_username_rect;

    bool m_needs_window_resize { false };
    bool m_bar_open { false };

    struct AppMetadata {
        String executable;
        String name;
        String icon_path;
        String category;
    };
    Vector<AppMetadata> m_apps;

    HashMap<String, NonnullRefPtr<WSMenu>> m_app_category_menus;

    struct ThemeMetadata {
        String name;
        String path;
    };

    RefPtr<WSMenu> m_system_menu;
    Color m_menu_selection_color;

    int m_theme_index { 0 };
    Vector<ThemeMetadata> m_themes;
    RefPtr<WSMenu> m_themes_menu;

    WeakPtr<WSMenuBar> m_current_menubar;
};
