#include "TaskbarWindow.h"
#include <LibGUI/GApplication.h>
#include <signal.h>
#include <stdio.h>

int main(int argc, char** argv)
{
    if (pledge("stdio shared_buffer proc rpath unix cpath fattr", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    GApplication app(argc, argv);

    if (pledge("stdio shared_buffer proc rpath unix", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    TaskbarWindow window;
    window.show();

    signal(SIGCHLD, [](int signo) {
        (void)signo;
        wait(nullptr);
    });

    return app.exec();
}
