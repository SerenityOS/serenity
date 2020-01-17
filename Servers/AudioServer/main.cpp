#include <LibCore/CFile.h>

#include "ASEventLoop.h"

int main(int, char**)
{
    if (pledge("stdio thread shared_buffer accept rpath wpath cpath unix fattr", nullptr) < 0) {
        perror("pledge");
        return 1;
    }
    ASEventLoop event_loop;
    if (pledge("stdio thread shared_buffer accept rpath wpath", nullptr) < 0) {
        perror("pledge");
        return 1;
    }
    return event_loop.exec();
}
