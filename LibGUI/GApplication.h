#pragma once

#include <AK/OwnPtr.h>

class GEventLoop;
class GMenuBar;

class GApplication {
public:
    GApplication(int argc, char** argv);

    int exec();

    void set_menubar(OwnPtr<GMenuBar>&&);

private:
    OwnPtr<GEventLoop> m_event_loop;
    OwnPtr<GMenuBar> m_menubar;
};
