/*
 * Copyright (c) 2023, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Devices/DeviceManagement.h>
#include <Kernel/Devices/HID/AllMiceDevice.h>
#include <Kernel/Devices/HID/Management.h>

namespace Kernel {

NonnullRefPtr<AllMiceDevice> AllMiceDevice::must_create()
{
    return *MUST(DeviceManagement::try_create_device<AllMiceDevice>());
}

AllMiceDevice::AllMiceDevice()
    : CharacterDevice(12, 0)
{
}

void AllMiceDevice::enqueue_mouse_packet(MousePacket packet)
{
    {
        SpinlockLocker lock(m_queue_lock);
        m_queue.enqueue(packet);
    }
    evaluate_block_conditions();
}

AllMiceDevice::~AllMiceDevice() = default;

bool AllMiceDevice::can_read(OpenFileDescription const&, u64) const
{
    SpinlockLocker lock(m_queue_lock);
    return !m_queue.is_empty();
}

ErrorOr<size_t> AllMiceDevice::read(OpenFileDescription&, u64, UserOrKernelBuffer& buffer, size_t size)
{
    VERIFY(size > 0);
    size_t nread = 0;
    size_t remaining_space_in_buffer = static_cast<size_t>(size) - nread;
    SpinlockLocker lock(m_queue_lock);
    while (!m_queue.is_empty() && remaining_space_in_buffer) {
        auto packet = m_queue.dequeue();

        size_t bytes_read_from_packet = min(remaining_space_in_buffer, sizeof(MousePacket));
        TRY(buffer.write(&packet, nread, bytes_read_from_packet));
        nread += bytes_read_from_packet;
        remaining_space_in_buffer -= bytes_read_from_packet;
    }
    return nread;
}

}
