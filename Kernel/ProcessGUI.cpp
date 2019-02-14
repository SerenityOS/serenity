#include "Process.h"
#include "MemoryManager.h"
#include <LibC/errno_numbers.h>
#include <SharedGraphics/Font.h>
#include <WindowServer/WSScreen.h>
#include <WindowServer/WSMessageLoop.h>
#include <WindowServer/WSWindow.h>
#include <WindowServer/WSWindowManager.h>
#include <WindowServer/WSMenuBar.h>
#include <Kernel/BochsVGADevice.h>

//#define LOG_GUI_SYSCALLS

void Process::initialize_gui_statics()
{
    new WSMessageLoop;
}

void Process::destroy_all_windows()
{
    if (!WSMessageLoop::the().running())
        return;
    WSMessageLoop::the().notify_client_died(gui_client_id());
}

DisplayInfo Process::set_video_resolution(int width, int height)
{
    DisplayInfo info;
    info.width = width;
    info.height = height;
    info.bpp = 32;
    info.pitch = width * 4;
    size_t framebuffer_size = width * height * 4 * 2;
    if (!m_display_framebuffer_region) {
        auto framebuffer_vmo = VMObject::create_framebuffer_wrapper(BochsVGADevice::the().framebuffer_address(), framebuffer_size);
        m_display_framebuffer_region = allocate_region_with_vmo(LinearAddress(0xe0000000), framebuffer_size, move(framebuffer_vmo), 0, "framebuffer", true, true);
    }
    info.framebuffer = m_display_framebuffer_region->laddr().as_ptr();

    BochsVGADevice::the().set_resolution(width, height);
    return info;
}
