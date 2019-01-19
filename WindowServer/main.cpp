#include "Process.h"
#include <SharedGraphics/Font.h>
#include <WindowServer/WSScreen.h>
#include <WindowServer/WSWindowManager.h>
#include <WindowServer/WSEventLoop.h>
#include <WindowServer/WSWindow.h>

// NOTE: This actually runs as a kernel process.
//       I'd like to change this eventually.

void WindowServer_main()
{
    auto info = current->get_display_info();

    dbgprintf("Screen is %ux%ux%ubpp\n", info.width, info.height, info.bpp);

    WSScreen screen((dword*)info.framebuffer, info.width, info.height);

    WSWindowManager::the();

    dbgprintf("Entering WindowServer main loop.\n");
    WSEventLoop::the().exec();

    ASSERT_NOT_REACHED();
}
