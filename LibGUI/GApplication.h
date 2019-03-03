#pragma once

#include <AK/Badge.h>
#include <AK/OwnPtr.h>
#include <AK/HashMap.h>
#include <LibGUI/GShortcut.h>

class GAction;
class GKeyEvent;
class GEventLoop;
class GMenuBar;

class GApplication {
public:
    static GApplication& the();
    GApplication(int argc, char** argv);
    ~GApplication();

    int exec();
    void quit(int);

    void set_menubar(OwnPtr<GMenuBar>&&);
    GAction* action_for_key_event(const GKeyEvent&);

    void register_shortcut_action(Badge<GAction>, GAction&);
    void unregister_shortcut_action(Badge<GAction>, GAction&);

private:
    OwnPtr<GEventLoop> m_event_loop;
    OwnPtr<GMenuBar> m_menubar;
    HashMap<GShortcut, GAction*> m_shortcut_actions;
};
