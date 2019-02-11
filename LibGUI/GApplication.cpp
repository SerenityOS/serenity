#include <LibGUI/GApplication.h>
#include <LibGUI/GEventLoop.h>
#include <LibGUI/GMenuBar.h>

GApplication::GApplication(int argc, char** argv)
{
    m_event_loop = make<GEventLoop>();
}

GApplication::~GApplication()
{
}

int GApplication::exec()
{
    return m_event_loop->exec();
}

void GApplication::set_menubar(OwnPtr<GMenuBar>&& menubar)
{
    m_menubar = move(menubar);
}

