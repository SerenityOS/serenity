#include "LookupServer.h"
#include <LibCore/CEventLoop.h>

int main(int argc, char** argv)
{
    (void)argc;
    (void)argv;

    CEventLoop event_loop;
    LookupServer server;

    return event_loop.exec();
}
