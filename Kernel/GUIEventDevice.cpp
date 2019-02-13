#include "GUIEventDevice.h"
#include <Kernel/Process.h>
#include <AK/Lock.h>
#include <LibC/errno_numbers.h>
#include <WindowServer/WSMessageLoop.h>

//#define GUIEVENTDEVICE_DEBUG

GUIEventDevice::GUIEventDevice()
    : CharacterDevice(66, 1)
{
}

GUIEventDevice::~GUIEventDevice()
{
}

bool GUIEventDevice::can_read(Process& process) const
{
    return !process.gui_events().is_empty();
}

ssize_t GUIEventDevice::read(Process& process, byte* buffer, size_t size)
{
#ifdef GUIEVENTDEVICE_DEBUG
    dbgprintf("GUIEventDevice::read(): %s<%u>, size=%u, sizeof(GUI_Event)=%u\n", process.name().characters(), process.pid(), size, sizeof(GUI_Event));
#endif
    if (process.gui_events().is_empty())
        return 0;
    LOCKER(process.gui_events_lock());
    ASSERT(size == sizeof(GUI_Event));
    *reinterpret_cast<GUI_Event*>(buffer) = process.gui_events().take_first();
    return size;
}

ssize_t GUIEventDevice::write(Process& process, const byte* data, size_t size)
{
    return WSMessageLoop::the().on_receive_from_client(process.gui_client_id(), data, size);
}
