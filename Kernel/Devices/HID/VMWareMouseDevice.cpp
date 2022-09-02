/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Arch/x86/Hypervisor/VMWareBackdoor.h>
#include <Kernel/Devices/DeviceManagement.h>
#include <Kernel/Devices/HID/VMWareMouseDevice.h>
#include <Kernel/Sections.h>

namespace Kernel {

UNMAP_AFTER_INIT ErrorOr<NonnullLockRefPtr<VMWareMouseDevice>> VMWareMouseDevice::try_to_initialize(I8042Controller const& ps2_controller)
{
    // FIXME: return the correct error
    if (!VMWareBackdoor::the())
        return Error::from_errno(EIO);
    if (!VMWareBackdoor::the()->vmmouse_is_absolute())
        return Error::from_errno(EIO);
    auto mouse_device = TRY(DeviceManagement::try_create_device<VMWareMouseDevice>(ps2_controller));
    TRY(mouse_device->initialize());
    return mouse_device;
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

VMWareMouseDevice::VMWareMouseDevice(I8042Controller const& ps2_controller)
    : PS2MouseDevice(ps2_controller)
{
}
VMWareMouseDevice::~VMWareMouseDevice() = default;

}
