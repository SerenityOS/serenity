#pragma once

#include "WSMenu.h"
#include <LibCore/CObject.h>
#include <LibCore/CTimer.h>
#include <WindowServer/WSCPUMonitor.h>
#include <WindowServer/WSWindow.h>

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

    void set_needs_window_resize();

    WSMenu* current_menu() { return m_current_menu.ptr(); }
    void set_current_menu(WSMenu*, bool is_submenu = false);

    void close_bar();
    void close_everyone();
    void close_everyone_not_in_lineage(WSMenu&);
    void close_menu_and_descendants(WSMenu&);

private:
    void close_menus(const Vector<WSMenu*>&);

    WSWindow& window() { return *m_window; }
    const WSWindow& window() const { return *m_window; }

    void handle_menu_mouse_event(WSMenu&, const WSMouseEvent&);

    void draw();
    void tick_clock();

    RefPtr<WSWindow> m_window;
    WSCPUMonitor m_cpu_monitor;
    String m_username;
    RefPtr<CTimer> m_timer;

    WeakPtr<WSMenu> m_current_menu;
    Vector<WeakPtr<WSMenu>> m_open_menu_stack;

    bool m_needs_window_resize { false };
    bool m_bar_open { false };
};
