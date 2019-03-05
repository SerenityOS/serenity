#include <LibGUI/GApplication.h>
#include <LibGUI/GEventLoop.h>
#include <LibGUI/GMenuBar.h>
#include <LibGUI/GAction.h>

static GApplication* s_the;

GApplication& GApplication::the()
{
    ASSERT(s_the);
    return *s_the;
}

GApplication::GApplication(int argc, char** argv)
{
    (void)argc;
    (void)argv;
    ASSERT(!s_the);
    s_the = this;
    m_event_loop = make<GEventLoop>();
}

GApplication::~GApplication()
{
    s_the = nullptr;
}

int GApplication::exec()
{
    int exit_code = m_event_loop->exec();
    // NOTE: Maybe it would be cool to return instead of exit()?
    //       This would require cleaning up all the GObjects on the heap.
    exit(exit_code);
    return exit_code;
}

void GApplication::quit(int exit_code)
{
    m_event_loop->quit(exit_code);
}

void GApplication::set_menubar(OwnPtr<GMenuBar>&& menubar)
{
    if (m_menubar)
        m_menubar->notify_removed_from_application(Badge<GApplication>());
    m_menubar = move(menubar);
    if (m_menubar)
        m_menubar->notify_added_to_application(Badge<GApplication>());
}

void GApplication::register_shortcut_action(Badge<GAction>, GAction& action)
{
    m_shortcut_actions.set(action.shortcut(), &action);
}

void GApplication::unregister_shortcut_action(Badge<GAction>, GAction& action)
{
    m_shortcut_actions.remove(action.shortcut());
}

GAction* GApplication::action_for_key_event(const GKeyEvent& event)
{
    auto it = m_shortcut_actions.find(GShortcut(event.modifiers(), (KeyCode)event.key()));
    if (it == m_shortcut_actions.end())
        return nullptr;
    return (*it).value;
}
