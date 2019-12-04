#include "TaskbarWindow.h"
#include <LibGUI/GApplication.h>
#include <signal.h>

int main(int argc, char** argv)
{
    GApplication app(argc, argv);
    TaskbarWindow window;
    window.show();

    signal(SIGCHLD, [](int signo) {
        (void)signo;
        wait(nullptr);
    });

    return app.exec();
}
