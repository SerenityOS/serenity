/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Devices/DeviceManagement.h>
#include <Kernel/Devices/HID/VMWareMouseDevice.h>
#include <Kernel/Devices/VMWareBackdoor.h>
#include <Kernel/Sections.h>

namespace Kernel {

UNMAP_AFTER_INIT RefPtr<VMWareMouseDevice> VMWareMouseDevice::try_to_initialize(const I8042Controller& ps2_controller)
{
    if (!VMWareBackdoor::the())
        return {};
    if (!VMWareBackdoor::the()->vmmouse_is_absolute())
        return {};
    auto mouse_device_or_error = DeviceManagement::try_create_device<VMWareMouseDevice>(ps2_controller);
    // FIXME: Find a way to propagate errors
    VERIFY(!mouse_device_or_error.is_error());
    if (mouse_device_or_error.value()->initialize())
        return mouse_device_or_error.release_value();
    return {};
}

void VMWareMouseDevice::irq_handle_byte_read(u8)
{
    auto backdoor = VMWareBackdoor::the();
    VERIFY(backdoor);
    VERIFY(backdoor->vmmouse_is_absolute());

    // We will receive 4 bytes from the I8042 controller that we are going to
    // ignore. Instead, we will check with VMWareBackdoor to see how many bytes
    // of mouse event data are waiting for us. For each multiple of 4, we
    // produce a mouse packet.
    constexpr u8 max_iterations = 128;
    u8 current_iteration = 0;
    while (++current_iteration < max_iterations) {
        auto number_of_mouse_event_bytes = backdoor->read_mouse_status_queue_size();
        if (number_of_mouse_event_bytes == 0)
            break;
        VERIFY(number_of_mouse_event_bytes % 4 == 0);

        auto mouse_packet = backdoor->receive_mouse_packet();
        m_entropy_source.add_random_event(mouse_packet);
        {
            SpinlockLocker lock(m_queue_lock);
            m_queue.enqueue(mouse_packet);
        }
    }
    evaluate_block_conditions();
}

VMWareMouseDevice::VMWareMouseDevice(const I8042Controller& ps2_controller)
    : PS2MouseDevice(ps2_controller)
{
}
VMWareMouseDevice::~VMWareMouseDevice()
{
}

}
