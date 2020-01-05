#pragma once

#include "WSMenu.h"
#include "WSMenuBar.h"
#include <AK/HashMap.h>
#include <LibCore/CObject.h>
#include <WindowServer/WSWindow.h>

class AClientConnection;

class WSMenuManager final : public CObject {
    C_OBJECT(WSMenuManager)
public:
    WSMenuManager();
    virtual ~WSMenuManager() override;

    void setup();
    void refresh();

    virtual void event(CEvent&) override;

    bool is_open(const WSMenu&) const;

    Vector<WeakPtr<WSMenu>>& open_menu_stack() { return m_open_menu_stack; }

    Rect menubar_rect() const;
    static int menubar_menu_margin() { return 16; }

    void set_needs_window_resize();

    WSMenu* current_menu() { return m_current_menu.ptr(); }
    void set_current_menu(WSMenu*, bool is_submenu = false);

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
    void invalidate_applet(const WSWindow&, const Rect&);

    Color menu_selection_color() const { return m_menu_selection_color; }
    WSMenu* system_menu() { return m_system_menu; }
    WSMenu* find_internal_menu_by_id(int);
    int theme_index() const { return m_theme_index; }

    template<typename Callback>
    void for_each_active_menubar_menu(Callback callback)
    {
        if (callback(*system_menu()) == IterationDecision::Break)
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

    Rect m_username_rect;

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
