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

void PS2KeyboardDevice::handle_byte_read_for_scan_code_set1(u8 byte)
{
    u8 ch = byte & 0x7f;
    bool pressed = !(byte & 0x80);
    dbgln_if(KEYBOARD_DEBUG, "Keyboard::handle_byte_read_for_scan_code_set1: {:#02x} {}", ch, (pressed ? "down" : "up"));
    if (byte == 0xe0) {
        m_has_e0_prefix = true;
        return;
    }

    ScanCodeEvent event {};
    event.sent_scan_code_set = ScanCodeSet::Set1;
    if (m_has_e0_prefix) {
        event.scan_code_bytes[0] = 0xe0;
        event.scan_code_bytes[1] = byte;
        event.bytes_count = 2;
    } else {
        event.scan_code_bytes[0] = byte;
        event.bytes_count = 1;
    }
    m_has_e0_prefix = false;
    m_keyboard_device->handle_scan_code_input_event(event);
}

void PS2KeyboardDevice::handle_byte_read_for_scan_code_set2(u8 byte)
{
    dbgln_if(KEYBOARD_DEBUG, "Keyboard::handle_byte_read_for_scan_code_set2: {:#02x}", byte);

    ScanCodeEvent event {};
    event.sent_scan_code_set = ScanCodeSet::Set2;
    if (m_received_bytes_count == 0) {
        if (byte == 0xe0 || byte == 0xf0 || byte == 0xe1) {
            m_received_bytes[0] = byte;
            m_received_bytes_count++;
            return;
        }
        event.scan_code_bytes[0] = byte;
        event.bytes_count = 1;
        m_received_bytes_count = 0;
        m_keyboard_device->handle_scan_code_input_event(event);
        return;
    } else if (m_received_bytes_count == 1) {
        if (byte == 0xf0) {
            VERIFY(m_received_bytes[0] == 0xe0);
            m_received_bytes[1] = byte;
            m_received_bytes_count++;
            return;
        }
        if (m_received_bytes[0] == 0xe0 && byte == 0x12) {
            m_received_bytes[1] = byte;
            m_received_bytes_count++;
            return;
        }

        if (m_received_bytes[0] == 0xe1 && byte == 0x14) {
            m_received_bytes[1] = byte;
            m_received_bytes_count++;
            return;
        }

        event.scan_code_bytes[0] = m_received_bytes[0];
        event.scan_code_bytes[1] = byte;
        event.bytes_count = 2;
        m_received_bytes_count = 0;
        m_keyboard_device->handle_scan_code_input_event(event);
        return;
    } else if (m_received_bytes_count == 2) {
        if (m_received_bytes[0] == 0xe0 && m_received_bytes[1] == 0x12 && byte == 0xe0) {
            m_received_bytes[2] = byte;
            m_received_bytes_count++;
            return;
        }

        if (m_received_bytes[0] == 0xe0 && m_received_bytes[1] == 0xf0 && byte == 0x7c) {
            m_received_bytes[2] = byte;
            m_received_bytes_count++;
            return;
        }

        if (m_received_bytes[0] == 0xe1) {
            VERIFY(m_received_bytes[1] == 0x14);
            m_received_bytes[2] = byte;
            m_received_bytes_count++;
            return;
        }

        event.scan_code_bytes[0] = m_received_bytes[0];
        event.scan_code_bytes[1] = m_received_bytes[1];
        event.scan_code_bytes[2] = byte;
        event.bytes_count = 3;
        m_received_bytes_count = 0;
        m_keyboard_device->handle_scan_code_input_event(event);
        return;
    } else if (m_received_bytes_count == 3) {
        if (m_received_bytes[0] == 0xe0
            && m_received_bytes[1] == 0x12
            && m_received_bytes[2] == 0xe0
            && byte == 0x7c) {
            ScanCodeEvent event {};
            event.sent_scan_code_set = ScanCodeSet::Set2;
            event.scan_code_bytes[0] = m_received_bytes[0];
            event.scan_code_bytes[1] = m_received_bytes[1];
            event.scan_code_bytes[2] = m_received_bytes[2];
            event.scan_code_bytes[3] = byte;
            event.bytes_count = 4;
            m_received_bytes_count = 0;
            m_keyboard_device->handle_scan_code_input_event(event);
            return;
        }

        m_received_bytes[3] = byte;
        m_received_bytes_count++;
        return;
    } else if (m_received_bytes_count == 4) {
        m_received_bytes[4] = byte;
        m_received_bytes_count++;
        return;
    } else if (m_received_bytes_count == 5) {
        if (m_received_bytes[0] == 0xe0
            && m_received_bytes[1] == 0xf0
            && m_received_bytes[2] == 0x7c
            && m_received_bytes[3] == 0xe0
            && m_received_bytes[4] == 0xf0
            && byte == 0x12) {

            event.scan_code_bytes[0] = m_received_bytes[0];
            event.scan_code_bytes[1] = m_received_bytes[1];
            event.scan_code_bytes[2] = m_received_bytes[2];
            event.scan_code_bytes[3] = m_received_bytes[3];
            event.scan_code_bytes[4] = m_received_bytes[4];
            event.scan_code_bytes[5] = byte;
            event.bytes_count = 6;
            m_received_bytes_count = 0;
            m_keyboard_device->handle_scan_code_input_event(event);
            return;
        }
        m_received_bytes[5] = byte;
        m_received_bytes_count++;
        return;
    } else if (m_received_bytes_count == 6) {
        m_received_bytes[6] = byte;
        m_received_bytes_count++;
        return;
    } else if (m_received_bytes_count == 7) {
        VERIFY(m_received_bytes[0] == 0xe1);
        VERIFY(m_received_bytes[1] == 0x14);
        VERIFY(m_received_bytes[2] == 0x77);
        VERIFY(m_received_bytes[3] == 0xe1);
        VERIFY(m_received_bytes[4] == 0xf0);
        VERIFY(m_received_bytes[5] == 0x14);
        VERIFY(m_received_bytes[6] == 0xf0);
        VERIFY(m_received_bytes[7] == 0x77);
        event.scan_code_bytes[0] = m_received_bytes[0];
        event.scan_code_bytes[1] = m_received_bytes[1];
        event.scan_code_bytes[2] = m_received_bytes[2];
        event.scan_code_bytes[3] = m_received_bytes[3];
        event.scan_code_bytes[4] = m_received_bytes[4];
        event.scan_code_bytes[5] = m_received_bytes[5];
        event.scan_code_bytes[6] = m_received_bytes[6];
        event.scan_code_bytes[7] = byte;
        event.bytes_count = 8;
        m_received_bytes_count = 0;
        m_keyboard_device->handle_scan_code_input_event(event);
        return;
    } else {
        VERIFY_NOT_REACHED();
    }
}

void PS2KeyboardDevice::handle_byte_read_from_serial_input(u8 byte)
{
    switch (m_scan_code_set) {
    case ScanCodeSet::Set1:
        handle_byte_read_for_scan_code_set1(byte);
        break;
    case ScanCodeSet::Set2:
        handle_byte_read_for_scan_code_set2(byte);
        break;
    default:
        VERIFY_NOT_REACHED();
    }
}

UNMAP_AFTER_INIT ErrorOr<NonnullOwnPtr<PS2KeyboardDevice>> PS2KeyboardDevice::try_to_initialize(PS2Controller const& ps2_controller, PS2PortIndex port_index, ScanCodeSet scan_code_set, KeyboardDevice const& keyboard_device)
{
    auto device = TRY(adopt_nonnull_own_or_enomem(new (nothrow) PS2KeyboardDevice(ps2_controller, port_index, scan_code_set, keyboard_device)));
    TRY(device->initialize());
    return device;
}

UNMAP_AFTER_INIT ErrorOr<void> PS2KeyboardDevice::initialize()
{
    return m_ps2_controller->reset_device(m_attached_port_index);
}

// FIXME: UNMAP_AFTER_INIT might not be correct, because in practice PS/2 devices
// are hot pluggable.
UNMAP_AFTER_INIT PS2KeyboardDevice::PS2KeyboardDevice(PS2Controller const& ps2_controller, PS2PortIndex port_index, ScanCodeSet scan_code_set, KeyboardDevice const& keyboard_device)
    : PS2Device(ps2_controller, port_index)
    , m_keyboard_device(keyboard_device)
    , m_scan_code_set(scan_code_set)
{
}

// FIXME: UNMAP_AFTER_INIT might not be correct, because in practice PS/2 devices
// are hot pluggable.
UNMAP_AFTER_INIT PS2KeyboardDevice::~PS2KeyboardDevice() = default;

}
