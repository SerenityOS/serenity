#pragma once

#include <LibGUI/Action.h>

namespace Browser {

class WindowActions {
public:
    static WindowActions& the();

    WindowActions(GUI::Window&);

    Function<void()> on_create_new_tab;
    Function<void()> on_next_tab;
    Function<void()> on_previous_tab;
    Function<void()> on_about;

    GUI::Action& create_new_tab_action() { return *m_create_new_tab_action; }
    GUI::Action& next_tab_action() { return *m_next_tab_action; }
    GUI::Action& previous_tab_action() { return *m_previous_tab_action; }
    GUI::Action& about_action() { return *m_about_action; }

private:
    RefPtr<GUI::Action> m_create_new_tab_action;
    RefPtr<GUI::Action> m_next_tab_action;
    RefPtr<GUI::Action> m_previous_tab_action;
    RefPtr<GUI::Action> m_about_action;
};

}
