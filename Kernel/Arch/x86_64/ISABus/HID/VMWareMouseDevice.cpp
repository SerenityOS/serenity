/*
 * Copyright (c) 2021-2023, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Arch/x86_64/Hypervisor/VMWareBackdoor.h>
#include <Kernel/Arch/x86_64/ISABus/HID/VMWareMouseDevice.h>
#include <Kernel/CommandLine.h>
#include <Kernel/Devices/DeviceManagement.h>
#include <Kernel/Devices/HID/PS2/MouseDevice.h>
#include <Kernel/Sections.h>

namespace Kernel {

ErrorOr<NonnullOwnPtr<PS2Device>> VMWareMouseDevice::probe_and_initialize_instance(PS2Controller& ps2_controller, PS2PortIndex port_index, PS2DeviceType device_type)
{
    if (!PS2MouseDevice::is_valid_mouse_type(device_type))
        return Error::from_errno(ENODEV);
    if (!VMWareBackdoor::the())
        return Error::from_errno(ENODEV);
    if (!kernel_command_line().is_vmmouse_enabled())
        return Error::from_errno(ENODEV);
    auto mouse_device = TRY(MouseDevice::try_to_initialize());
    device_type = TRY(PS2MouseDevice::do_initialization_sequence(ps2_controller, port_index));
    auto device = TRY(adopt_nonnull_own_or_enomem(new (nothrow) VMWareMouseDevice(ps2_controller, port_index, device_type, mouse_device)));
    VMWareBackdoor::the()->enable_absolute_vmmouse();
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

VMWareMouseDevice::VMWareMouseDevice(PS2Controller const& ps2_controller, PS2PortIndex port_index, PS2DeviceType device_type, MouseDevice const& mouse_device)
    : PS2Device(ps2_controller, port_index, device_type)
    , m_mouse_device(mouse_device)
{
}

VMWareMouseDevice::~VMWareMouseDevice() = default;

}
