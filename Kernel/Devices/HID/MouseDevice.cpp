/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
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
