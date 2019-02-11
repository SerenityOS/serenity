#pragma once

#include "WSMenu.h"
#include <AK/Vector.h>

class WSMenuBar {
public:
    WSMenuBar();
    ~WSMenuBar();

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
    Vector<WSMenu*> m_menus;
};
