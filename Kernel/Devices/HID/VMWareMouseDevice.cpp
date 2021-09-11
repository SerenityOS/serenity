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
    VERIFY(VMWareBackdoor::the());
    VERIFY(VMWareBackdoor::the()->vmmouse_is_absolute());
    // We won't receive complete packets with the backdoor enabled,
    // we will only get one byte for each event, which we'll just
    // discard. If we were to wait until we *think* that we got a
    // full PS/2 packet then we would create a backlog in the VM
    // because we wouldn't read the appropriate number of mouse
    // packets from VMWareBackdoor.
    auto mouse_packet = VMWareBackdoor::the()->receive_mouse_packet();
    if (mouse_packet.has_value()) {
        m_entropy_source.add_random_event(mouse_packet.value());
        {
            SpinlockLocker lock(m_queue_lock);
            m_queue.enqueue(mouse_packet.value());
        }
        evaluate_block_conditions();
    }
    return;
}

VMWareMouseDevice::VMWareMouseDevice(const I8042Controller& ps2_controller)
    : PS2MouseDevice(ps2_controller)
{
}
VMWareMouseDevice::~VMWareMouseDevice()
{
}

}
