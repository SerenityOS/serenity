#include <SharedGraphics/Font.h>
#include <WindowServer/WSScreen.h>
#include <WindowServer/WSWindowManager.h>
#include <WindowServer/WSMessageLoop.h>
#include <WindowServer/WSWindow.h>
#include <unistd.h>
#include <fcntl.h>
#include <sys/ioctl.h>
#include <sys/mman.h>

int main(int, char**)
{
    dbgprintf("WindowServer starting...\n");
    WSMessageLoop loop;

    int bxvga_fd = open("/dev/bxvga", O_RDWR);
    ASSERT(bxvga_fd >= 0);

    struct BXVGAResolution {
        int width;
        int height;
    };
    BXVGAResolution resolution { 1024, 768 };
    int rc = ioctl(bxvga_fd, 1985, (int)&resolution);
    ASSERT(rc == 0);

    size_t framebuffer_size_in_bytes = resolution.width * resolution.height * sizeof(RGBA32) * 2;
    void* framebuffer = mmap(nullptr, framebuffer_size_in_bytes, PROT_READ | PROT_WRITE, MAP_SHARED, bxvga_fd, 0);
    ASSERT(framebuffer && framebuffer != (void*)-1);

    WSScreen screen((dword*)framebuffer, resolution.width, resolution.height);

    WSWindowManager window_manager;
    window_manager.set_framebuffer_fd(bxvga_fd);

    dbgprintf("Entering WindowServer main loop.\n");
    WSMessageLoop::the().exec();

    ASSERT_NOT_REACHED();
}
