#pragma once

#include "WSMenu.h"
#include <AK/Vector.h>
#include <AK/WeakPtr.h>

class Process;

class WSMenuBar {
public:
    explicit WSMenuBar(Process&);
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
    WeakPtr<Process> m_process;
    Vector<WSMenu*> m_menus;
};
