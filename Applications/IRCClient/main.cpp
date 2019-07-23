#include "IRCAppWindow.h"
#include "IRCClient.h"
#include <LibGUI/GApplication.h>
#include <stdio.h>

int main(int argc, char** argv)
{
    GApplication app(argc, argv);

    IRCAppWindow app_window;
    app_window.show();

    printf("Entering main loop...\n");
    return app.exec();
}
