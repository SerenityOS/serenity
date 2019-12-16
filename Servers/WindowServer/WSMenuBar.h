#pragma once

#include "WSMenu.h"
#include <AK/Vector.h>
#include <AK/WeakPtr.h>
#include <AK/Weakable.h>

class WSMenuBar : public Weakable<WSMenuBar> {
public:
    WSMenuBar(WSClientConnection& client, int menubar_id);
    ~WSMenuBar();

    WSClientConnection& client() { return m_client; }
    const WSClientConnection& client() const { return m_client; }
    int menubar_id() const { return m_menubar_id; }
    void add_menu(WSMenu& menu)
    {
        menu.set_menubar(this);
        m_menus.append(&menu);
    }

    template<typename Callback>
    void for_each_menu(Callback callback)
    {
        for (auto& menu : m_menus) {
            if (callback(*menu) == IterationDecision::Break)
                return;
        }
    }

private:
    WSClientConnection& m_client;
    int m_menubar_id { 0 };
    Vector<WSMenu*> m_menus;
};
