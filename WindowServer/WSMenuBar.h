#pragma once

#include "WSMenu.h"
#include <AK/Vector.h>
#include <AK/Weakable.h>
#include <AK/WeakPtr.h>

class WSMenuBar : public Weakable<WSMenuBar> {
public:
    WSMenuBar(int client_id, int menubar_id);
    ~WSMenuBar();

    int client_id() const { return m_client_id; }
    int menubar_id() const { return m_menubar_id; }
    void add_menu(WSMenu* menu) { m_menus.append(menu); }

    template<typename Callback>
    void for_each_menu(Callback callback)
    {
        for (auto& menu : m_menus) {
            if (!callback(*menu))
                return;
        }
    }

private:
    int m_client_id { 0 };
    int m_menubar_id { 0 };
    Vector<WSMenu*> m_menus;
};
