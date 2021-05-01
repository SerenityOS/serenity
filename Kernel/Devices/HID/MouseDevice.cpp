/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Devices/HID/HIDManagement.h>
#include <Kernel/Devices/HID/MouseDevice.h>

namespace Kernel {

MouseDevice::MouseDevice()
    : HIDDevice(10, HIDManagement::the().generate_minor_device_number_for_mouse())
{
}

MouseDevice::~MouseDevice()
{
}

bool MouseDevice::can_read(const FileDescription&, size_t) const
{
    ScopedSpinLock lock(m_queue_lock);
    return !m_queue.is_empty();
}

KResultOr<size_t> MouseDevice::read(FileDescription&, u64, UserOrKernelBuffer& buffer, size_t size)
{
    VERIFY(size > 0);
    size_t nread = 0;
    size_t remaining_space_in_buffer = static_cast<size_t>(size) - nread;
    ScopedSpinLock lock(m_queue_lock);
    while (!m_queue.is_empty() && remaining_space_in_buffer) {
        auto packet = m_queue.dequeue();
        lock.unlock();

        dbgln_if(MOUSE_DEBUG, "Mouse Read: Buttons {:x}", packet.buttons);
        dbgln_if(MOUSE_DEBUG, "PS2 Mouse: X {}, Y {}, Z {}, Relative {}", packet.x, packet.y, packet.z, packet.buttons);
        dbgln_if(MOUSE_DEBUG, "PS2 Mouse Read: Filter packets");

        size_t bytes_read_from_packet = min(remaining_space_in_buffer, sizeof(MousePacket));
        if (!buffer.write(&packet, nread, bytes_read_from_packet))
            return EFAULT;
        nread += bytes_read_from_packet;
        remaining_space_in_buffer -= bytes_read_from_packet;

        lock.lock();
    }
    return nread;
}

KResultOr<size_t> MouseDevice::write(FileDescription&, u64, const UserOrKernelBuffer&, size_t)
{
    return 0;
}

}
