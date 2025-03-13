/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Arch/x86_64/Hypervisor/VMWareBackdoor.h>
#include <Kernel/Arch/x86_64/ISABus/Input/VMWareMouseDevice.h>
#include <Kernel/Devices/Device.h>
#include <Kernel/Sections.h>

namespace Kernel {

UNMAP_AFTER_INIT ErrorOr<NonnullOwnPtr<VMWareMouseDevice>> VMWareMouseDevice::try_to_initialize(SerialIOController const& serial_io_controller, SerialIOController::PortIndex port_index, MouseDevice const& mouse_device)
{
    // FIXME: return the correct error
    if (!VMWareBackdoor::the())
        return Error::from_errno(EIO);
    if (!VMWareBackdoor::the()->vmmouse_is_absolute())
        return Error::from_errno(EIO);
    auto device = TRY(adopt_nonnull_own_or_enomem(new (nothrow) VMWareMouseDevice(serial_io_controller, port_index, mouse_device)));
    TRY(device->initialize());
    return device;
}

void VMWareMouseDevice::handle_byte_read_from_serial_input(u8)
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
        m_mouse_device->handle_mouse_packet_input_event(mouse_packet);
    }
}

VMWareMouseDevice::VMWareMouseDevice(SerialIOController const& serial_io_controller, SerialIOController::PortIndex port_index, MouseDevice const& mouse_device)
    : PS2MouseDevice(serial_io_controller, port_index, mouse_device)
{
}
VMWareMouseDevice::~VMWareMouseDevice() = default;

}
