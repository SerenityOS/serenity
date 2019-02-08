#include "Process.h"
#include <SharedGraphics/Font.h>
#include <WindowServer/WSScreen.h>
#include <WindowServer/WSWindowManager.h>
#include <WindowServer/WSMessageLoop.h>
#include <WindowServer/WSWindow.h>

// NOTE: This actually runs as a kernel process.
//       I'd like to change this eventually.

void WindowServer_main()
{
    WSMessageLoop::the().set_server_process(*current);
    current->set_priority(Process::HighPriority);
    auto info = current->set_video_resolution(1024, 768);

    dbgprintf("Screen is %ux%ux%ubpp\n", info.width, info.height, info.bpp);

    WSScreen screen((dword*)info.framebuffer, info.width, info.height);

    WSWindowManager::the();

    dbgprintf("Entering WindowServer main loop.\n");
    WSMessageLoop::the().exec();

    ASSERT_NOT_REACHED();
}
