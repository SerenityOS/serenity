#include <WindowServer/WSScreen.h>
#include <WindowServer/WSWindowManager.h>
#include <WindowServer/WSMessageLoop.h>

int main(int, char**)
{
    WSMessageLoop loop;
    WSScreen screen(1024, 768);
    WSWindowManager window_manager;

    dbgprintf("Entering WindowServer main loop.\n");
    WSMessageLoop::the().exec();
    ASSERT_NOT_REACHED();
}
