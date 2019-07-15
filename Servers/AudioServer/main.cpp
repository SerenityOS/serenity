#include <LibCore/CFile.h>

#include "ASEventLoop.h"

int main(int, char**)
{
    ASEventLoop event_loop;
    return event_loop.exec();
}
