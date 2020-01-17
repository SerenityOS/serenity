#include "LookupServer.h"
#include <LibCore/CEventLoop.h>
#include <stdio.h>

int main(int argc, char** argv)
{
    (void)argc;
    (void)argv;

    if (pledge("stdio accept unix inet cpath rpath fattr", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    CEventLoop event_loop;
    LookupServer server;

    if (pledge("stdio accept inet", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    return event_loop.exec();
}
