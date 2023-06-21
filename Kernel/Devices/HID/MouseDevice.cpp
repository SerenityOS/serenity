/*
 * Copyright (c) 2021-2023, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Devices/DeviceManagement.h>
#include <Kernel/Devices/HID/Management.h>
#include <Kernel/Devices/HID/MouseDevice.h>

namespace Kernel {

ErrorOr<NonnullRefPtr<MouseDevice>> MouseDevice::try_to_initialize()
{
    return *TRY(DeviceManagement::try_create_device<MouseDevice>());
}

MouseDevice::MouseDevice()
    : HIDDevice(10, HIDManagement::the().generate_minor_device_number_for_mouse())
{
}

void MouseDevice::handle_mouse_packet_input_event(MousePacket packet)
{
    m_entropy_source.add_random_event(packet);
    {
        SpinlockLocker lock(m_queue_lock);
        m_queue.enqueue(packet);
    }
    evaluate_block_conditions();
}

MouseDevice::~MouseDevice() = default;

bool MouseDevice::can_read(OpenFileDescription const&, u64) const
{
    SpinlockLocker lock(m_queue_lock);
    return !m_queue.is_empty();
}

ErrorOr<size_t> MouseDevice::read(OpenFileDescription&, u64, UserOrKernelBuffer& buffer, size_t size)
{
    VERIFY(size > 0);
    size_t nread = 0;
    size_t remaining_space_in_buffer = static_cast<size_t>(size) - nread;
    SpinlockLocker lock(m_queue_lock);
    while (!m_queue.is_empty() && remaining_space_in_buffer) {
        auto packet = m_queue.dequeue();

        dbgln_if(MOUSE_DEBUG, "Mouse Read: Buttons {:x}", packet.buttons);
        dbgln_if(MOUSE_DEBUG, "PS2 Mouse: X {}, Y {}, Z {}, W {}, Relative {}", packet.x, packet.y, packet.z, packet.w, packet.buttons);
        dbgln_if(MOUSE_DEBUG, "PS2 Mouse Read: Filter packets");

        size_t bytes_read_from_packet = min(remaining_space_in_buffer, sizeof(MousePacket));
        TRY(buffer.write(&packet, nread, bytes_read_from_packet));
        nread += bytes_read_from_packet;
        remaining_space_in_buffer -= bytes_read_from_packet;
    }
    return nread;
}

}
