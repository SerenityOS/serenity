#pragma once

#include "WSMenu.h"
#include <AK/Vector.h>
#include <AK/Weakable.h>
#include <AK/WeakPtr.h>

class Process;

class WSMenuBar : public Weakable<WSMenuBar> {
public:
    WSMenuBar(int menubar_id, Process&);
    ~WSMenuBar();

    int menubar_id() const { return m_menubar_id; }
    const Process* process() const { return m_process.ptr(); }
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
    int m_menubar_id { 0 };
    WeakPtr<Process> m_process;
    Vector<WSMenu*> m_menus;
};
