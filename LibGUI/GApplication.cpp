#include <LibGUI/GApplication.h>
#include <LibGUI/GEventLoop.h>
#include <LibGUI/GMenuBar.h>

static GApplication* s_the;

GApplication& GApplication::the()
{
    ASSERT(s_the);
    return *s_the;
}

GApplication::GApplication(int argc, char** argv)
{
    ASSERT(!s_the);
    s_the = this;
    m_event_loop = make<GEventLoop>();
}

GApplication::~GApplication()
{
}

int GApplication::exec()
{
    return m_event_loop->exec();
}

void GApplication::exit(int exit_code)
{
    m_event_loop->exit(exit_code);
}

void GApplication::set_menubar(OwnPtr<GMenuBar>&& menubar)
{
    m_menubar = move(menubar);
}

