#include "IRCAppWindow.h"
#include "IRCClient.h"
#include <LibGUI/GApplication.h>
#include <stdio.h>

int main(int argc, char** argv)
{
    GApplication app(argc, argv);

    IRCAppWindow app_window;
    app_window.set_should_exit_event_loop_on_close(true);
    app_window.show();

    printf("Entering main loop...\n");
    return app.exec();
}
