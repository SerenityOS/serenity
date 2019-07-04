#pragma once

#include <AK/Badge.h>
#include <AK/HashMap.h>
#include <AK/OwnPtr.h>
#include <LibGUI/GShortcut.h>

class GAction;
class GKeyEvent;
class GEventLoop;
class GMenuBar;
class Point;

class GApplication {
public:
    static GApplication& the();
    GApplication(int argc, char** argv);
    ~GApplication();

    int exec();
    void quit(int = 0);

    void set_menubar(OwnPtr<GMenuBar>&&);
    GAction* action_for_key_event(const GKeyEvent&);

    void register_global_shortcut_action(Badge<GAction>, GAction&);
    void unregister_global_shortcut_action(Badge<GAction>, GAction&);

    void show_tooltip(const StringView&, const Point& screen_location);
    void hide_tooltip();

private:
    OwnPtr<GEventLoop> m_event_loop;
    OwnPtr<GMenuBar> m_menubar;
    HashMap<GShortcut, GAction*> m_global_shortcut_actions;
    class TooltipWindow;
    TooltipWindow* m_tooltip_window { nullptr };
};
