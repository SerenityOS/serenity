/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021-2023, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Types.h>
#include <Kernel/Debug.h>
#include <Kernel/Devices/DeviceManagement.h>
#include <Kernel/Devices/HID/Management.h>
#include <Kernel/Devices/HID/PS2/KeyboardDevice.h>
#include <Kernel/Devices/HID/ScanCodeEvent.h>
#include <Kernel/Sections.h>

namespace Kernel {

void PS2KeyboardDevice::handle_byte_read_from_serial_input(u8 byte)
{
    u8 ch = byte & 0x7f;
    bool pressed = !(byte & 0x80);
    if (byte == 0xe0) {
        m_has_e0_prefix = true;
        return;
    }

    ScanCodeEvent event {};
    event.pressed = pressed;
    event.e0_prefix = m_has_e0_prefix;
    m_has_e0_prefix = false;

    dbgln_if(KEYBOARD_DEBUG, "Keyboard::handle_byte_read_from_serial_input: {:#02x} {}", ch, (pressed ? "down" : "up"));
    event.scan_code_value = ch;
    m_keyboard_device->handle_scan_code_input_event(event);
}

ErrorOr<NonnullOwnPtr<PS2Device>> PS2KeyboardDevice::probe_and_initialize_instance(PS2Controller& ps2_controller, PS2PortIndex port_index, PS2DeviceType device_type)
{
    if (device_type != PS2DeviceType::MF2Keyboard)
        return Error::from_errno(ENODEV);

    auto keyboard_device = TRY(KeyboardDevice::try_to_initialize());
    {
        SpinlockLocker locker(ps2_controller.device_port_spinlock(port_index));
        TRY(ps2_controller.reset_while_device_port_locked(port_index));
    }
    return TRY(adopt_nonnull_own_or_enomem(new (nothrow) PS2KeyboardDevice(ps2_controller, port_index, device_type, keyboard_device)));
}

// FIXME: UNMAP_AFTER_INIT might not be correct, because in practice PS/2 devices
// are hot pluggable.
UNMAP_AFTER_INIT PS2KeyboardDevice::PS2KeyboardDevice(PS2Controller const& ps2_controller, PS2PortIndex port_index, PS2DeviceType device_type, KeyboardDevice const& keyboard_device)
    : PS2Device(ps2_controller, port_index, device_type)
    , m_keyboard_device(keyboard_device)
{
}

// FIXME: UNMAP_AFTER_INIT might not be correct, because in practice PS/2 devices
// are hot pluggable.
UNMAP_AFTER_INIT PS2KeyboardDevice::~PS2KeyboardDevice() = default;

}
