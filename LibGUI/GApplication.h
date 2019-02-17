#pragma once

#include <AK/OwnPtr.h>

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

private:
    OwnPtr<GEventLoop> m_event_loop;
    OwnPtr<GMenuBar> m_menubar;
};
