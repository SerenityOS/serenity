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

    int bxvga_fd = current->sys$open("/dev/bxvga", O_RDWR);
    ASSERT(bxvga_fd >= 0);

    struct BXVGAResolution {
        int width;
        int height;
    };
    BXVGAResolution resolution { 1024, 768 };

    int rc = current->sys$ioctl(bxvga_fd, 1985, (int)&resolution);
    ASSERT(rc == 0);

    Syscall::SC_mmap_params params;
    memset(&params, 0, sizeof(params));
    params.fd = bxvga_fd;
    params.prot = PROT_READ | PROT_WRITE;
    params.flags = MAP_SHARED;
    params.size = resolution.width * resolution.height * sizeof(RGBA32) * 2;
    params.offset = 0;
    kprintf("Calling sys$mmap in WS\n");
    void* framebuffer = current->sys$mmap(&params);
    ASSERT(framebuffer && framebuffer != (void*)-1);

    WSScreen screen((dword*)framebuffer, resolution.width, resolution.height);

    WSWindowManager::the().set_framebuffer_fd(bxvga_fd);

    dbgprintf("Entering WindowServer main loop.\n");
    WSMessageLoop::the().exec();

    ASSERT_NOT_REACHED();
}
