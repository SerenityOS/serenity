#pragma once

#include <LibGUI/Action.h>

namespace Browser {

class WindowActions {
public:
    static WindowActions& the();

    WindowActions(GUI::Window&);

    Function<void()> on_create_new_tab;

    GUI::Action& create_new_tab_action() { return *m_create_new_tab_action; }

private:
    RefPtr<GUI::Action> m_create_new_tab_action;
};

}
