#include "GUIEventDevice.h"
#include <Kernel/Process.h>
#include <AK/Lock.h>
#include <LibC/errno_numbers.h>

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

ssize_t GUIEventDevice::read(byte* buffer, size_t size)
{
#ifdef GUIEVENTDEVICE_DEBUG
    dbgprintf("GUIEventDevice::read(): %s<%u>, size=%u, sizeof(GUI_Event)=%u\n", current->name().characters(), current->pid(), size, sizeof(GUI_Event));
#endif
    if (current->gui_events().is_empty())
        return 0;
    LOCKER(current->gui_events_lock());
    ASSERT(size == sizeof(GUI_Event));
    *reinterpret_cast<GUI_Event*>(buffer) = current->gui_events().take_first();
    return size;
}

ssize_t GUIEventDevice::write(const byte*, size_t)
{
    return -EINVAL;
}
