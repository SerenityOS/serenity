#include "WindowComposer.h"
#include "Process.h"
#include <Widgets/Font.h>
#include <Widgets/FrameBuffer.h>
#include <Widgets/WindowManager.h>
#include <Widgets/EventLoop.h>
#include <Widgets/Window.h>

void WindowComposer_main()
{
    auto info = current->get_display_info();

    dbgprintf("Screen is %ux%ux%ubpp\n", info.width, info.height, info.bpp);

    FrameBuffer framebuffer((dword*)info.framebuffer, info.width, info.height);

    WindowManager::the();

    dbgprintf("Entering WindowComposer main loop.\n");
    EventLoop::main().exec();

    ASSERT_NOT_REACHED();
}
