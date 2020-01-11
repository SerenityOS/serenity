#include "IRCAppWindow.h"
#include "IRCClient.h"
#include <LibGUI/GApplication.h>
#include <stdio.h>

int main(int argc, char** argv)
{
    if (pledge("stdio inet dns unix shared_buffer cpath rpath fattr", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    GApplication app(argc, argv);

    if (pledge("stdio inet dns unix shared_buffer rpath", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    IRCAppWindow app_window;
    app_window.show();

    printf("Entering main loop...\n");
    return app.exec();
}
