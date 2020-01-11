#include <LibCore/CFile.h>

#include "ASEventLoop.h"

int main(int, char**)
{
    if (pledge("stdio thread shared_buffer rpath wpath cpath unix fattr", nullptr) < 0) {
        perror("pledge");
        return 1;
    }
    ASEventLoop event_loop;
    if (pledge("stdio thread shared_buffer rpath wpath unix", nullptr) < 0) {
        perror("pledge");
        return 1;
    }
    return event_loop.exec();
}
