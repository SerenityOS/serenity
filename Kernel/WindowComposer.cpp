#include "WindowComposer.h"
#include "Process.h"
#include <Widgets/Font.h>
#include <Widgets/FrameBuffer.h>
#include <Widgets/WindowManager.h>
#include <Widgets/RootWidget.h>
#include <Widgets/EventLoop.h>
#include <Widgets/MsgBox.h>

void WindowComposer_main()
{
    Font::initialize();
    FrameBuffer::initialize();
    EventLoop::initialize();
    WindowManager::initialize();
    AbstractScreen::initialize();

    auto info = current->get_display_info();

    dbgprintf("Screen is %ux%ux%ubpp\n", info.width, info.height, info.bpp);

    FrameBuffer framebuffer((dword*)info.framebuffer, info.width, info.height);
    RootWidget rw;
    EventLoop loop;

    WindowManager::the().setRootWidget(&rw);

    MsgBox(nullptr, "Serenity Operating System");

    dbgprintf("Entering WindowComposer main loop.\n");
    loop.exec();

    ASSERT_NOT_REACHED();
}
