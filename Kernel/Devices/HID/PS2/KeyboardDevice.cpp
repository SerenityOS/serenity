/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
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

UNMAP_AFTER_INIT ErrorOr<NonnullOwnPtr<PS2KeyboardDevice>> PS2KeyboardDevice::try_to_initialize(PS2Controller const& ps2_controller, PS2PortIndex port_index, KeyboardDevice const& keyboard_device)
{
    auto device = TRY(adopt_nonnull_own_or_enomem(new (nothrow) PS2KeyboardDevice(ps2_controller, port_index, keyboard_device)));
    TRY(device->initialize());
    return device;
}

UNMAP_AFTER_INIT ErrorOr<void> PS2KeyboardDevice::initialize()
{
    return m_ps2_controller->reset_device(m_attached_port_index);
}

// FIXME: UNMAP_AFTER_INIT might not be correct, because in practice PS/2 devices
// are hot pluggable.
UNMAP_AFTER_INIT PS2KeyboardDevice::PS2KeyboardDevice(PS2Controller const& ps2_controller, PS2PortIndex port_index, KeyboardDevice const& keyboard_device)
    : PS2Device(ps2_controller, port_index)
    , m_keyboard_device(keyboard_device)
{
}

// FIXME: UNMAP_AFTER_INIT might not be correct, because in practice PS/2 devices
// are hot pluggable.
UNMAP_AFTER_INIT PS2KeyboardDevice::~PS2KeyboardDevice() = default;

}
