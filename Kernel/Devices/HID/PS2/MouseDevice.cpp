/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2023, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Arch/Delay.h>
#include <Kernel/Debug.h>
#include <Kernel/Devices/DeviceManagement.h>
#include <Kernel/Devices/HID/PS2/MouseDevice.h>
#include <Kernel/Sections.h>

namespace Kernel {

#define PS2MOUSE_INTELLIMOUSE_ID 0x03
#define PS2MOUSE_INTELLIMOUSE_EXPLORER_ID 0x04

UNMAP_AFTER_INIT PS2MouseDevice::PS2MouseDevice(PS2Controller const& ps2_controller, PS2PortIndex port_index, PS2DeviceType device_type, MouseDevice const& mouse_device)
    : PS2Device(ps2_controller, port_index, device_type)
    , m_mouse_device(mouse_device)
{
    if (device_type == PS2DeviceType::ScrollWheelMouse || device_type == PS2DeviceType::MouseWith5Buttons) {
        m_has_wheel = true;
    }

    if (device_type == PS2DeviceType::MouseWith5Buttons) {
        m_has_five_buttons = true;
    }
}

UNMAP_AFTER_INIT PS2MouseDevice::~PS2MouseDevice() = default;

void PS2MouseDevice::handle_byte_read_from_serial_input(u8 byte)
{
    auto commit_packet = [this]() {
        m_data_state = 0;
        dbgln_if(PS2MOUSE_DEBUG, "PS2Mouse: {}, {} {} {}",
            m_data.bytes[1],
            m_data.bytes[2],
            (m_data.bytes[0] & 1) ? "Left" : "",
            (m_data.bytes[0] & 2) ? "Right" : "");
        m_mouse_device->handle_mouse_packet_input_event(parse_data_packet(m_data));
    };

    VERIFY(m_data_state < sizeof(m_data.bytes) / sizeof(m_data.bytes[0]));
    m_data.bytes[m_data_state] = byte;

    switch (m_data_state) {
    case 0:
        if (!(byte & 0x08)) {
            dbgln("PS2Mouse: Stream out of sync.");
            return;
        }
        ++m_data_state;
        return;
    case 1:
        ++m_data_state;
        return;
    case 2:
        if (m_has_wheel) {
            ++m_data_state;
            return;
        }
        commit_packet();
        return;
    case 3:
        VERIFY(m_has_wheel);
        commit_packet();
        return;
    }
    VERIFY_NOT_REACHED();
}

MousePacket PS2MouseDevice::parse_data_packet(RawPacket const& raw_packet)
{
    int x = raw_packet.bytes[1];
    int y = raw_packet.bytes[2];
    int z = 0;
    int w = 0;
    if (m_has_wheel) {
        // FIXME: For non-Intellimouse, this is a full byte.
        //        However, for now, m_has_wheel is only set for Intellimouse.
        z = (char)(raw_packet.bytes[3] & 0x0f);

        // -1 in 4 bits
        if (z == 15)
            z = -1;

        if ((raw_packet.bytes[3] & 0xc0) == 0x40) {
            // FIXME: Scroll only functions correctly when the sign is flipped there
            w = -z;
            z = 0;
        } else {
            w = 0;
        }
    }
    bool x_overflow = raw_packet.bytes[0] & 0x40;
    bool y_overflow = raw_packet.bytes[0] & 0x80;
    bool x_sign = raw_packet.bytes[0] & 0x10;
    bool y_sign = raw_packet.bytes[0] & 0x20;
    if (x && x_sign)
        x -= 0x100;
    if (y && y_sign)
        y -= 0x100;
    if (x_overflow || y_overflow) {
        x = 0;
        y = 0;
    }
    MousePacket packet;
    packet.x = x;
    packet.y = y;
    packet.z = z;
    packet.w = w;
    packet.buttons = raw_packet.bytes[0] & 0x07;

    if (m_has_five_buttons) {
        if (raw_packet.bytes[3] & 0x10)
            packet.buttons |= MousePacket::BackwardButton;
        if (raw_packet.bytes[3] & 0x20)
            packet.buttons |= MousePacket::ForwardButton;
    }

    packet.is_relative = true;
    dbgln_if(PS2MOUSE_DEBUG, "PS2 Relative Mouse: Buttons {:x}", packet.buttons);
    dbgln_if(PS2MOUSE_DEBUG, "Mouse: X {}, Y {}, Z {}, W {}", packet.x, packet.y, packet.z, packet.w);
    return packet;
}

static ErrorOr<void> send_ps2_command_while_device_port_locked(PS2Controller& ps2_controller, PS2PortIndex port_index, PS2DeviceCommand command, u8 data)
{
    TRY(ps2_controller.send_command_while_device_port_locked(port_index, command, data));
    return {};
}

static ErrorOr<void> send_ps2_command_while_device_port_locked(PS2Controller& ps2_controller, PS2PortIndex port_index, PS2DeviceCommand command)
{
    TRY(ps2_controller.send_command_while_device_port_locked(port_index, command));
    return {};
}

bool PS2MouseDevice::is_valid_mouse_type(PS2DeviceType device_type)
{
    if (device_type == PS2DeviceType::ScrollWheelMouse)
        return true;
    if (device_type == PS2DeviceType::MouseWith5Buttons)
        return true;
    if (device_type == PS2DeviceType::StandardMouse)
        return true;
    return false;
}

static ErrorOr<void> set_sample_rate_on_mouse_device(PS2Controller& ps2_controller, PS2PortIndex port_index, u8 rate)
{
    TRY(send_ps2_command_while_device_port_locked(ps2_controller, port_index, PS2DeviceCommand::SetSampleRate, rate));
    return {};
}

ErrorOr<PS2DeviceType> PS2MouseDevice::do_initialization_sequence(PS2Controller& ps2_controller, PS2PortIndex port_index)
{
    SpinlockLocker locker(ps2_controller.device_port_spinlock(port_index));

    TRY(ps2_controller.reset_while_device_port_locked(port_index));
    auto device_id_byte = TRY(ps2_controller.read_from_device_while_device_port_locked(port_index));

    TRY(send_ps2_command_while_device_port_locked(ps2_controller, port_index, PS2DeviceCommand::SetDefaults));
    TRY(send_ps2_command_while_device_port_locked(ps2_controller, port_index, PS2DeviceCommand::EnablePacketStreaming));

    Array<u8, 2> device_id_bytes = { device_id_byte, 0 };
    if (device_id_bytes[0] == 0) {
        // Send magical wheel initiation sequence.
        TRY(set_sample_rate_on_mouse_device(ps2_controller, port_index, 200));
        TRY(set_sample_rate_on_mouse_device(ps2_controller, port_index, 100));
        TRY(set_sample_rate_on_mouse_device(ps2_controller, port_index, 80));
        device_id_bytes = TRY(ps2_controller.read_device_id_while_device_port_locked(port_index));
    }

    if (device_id_bytes[0] == PS2MOUSE_INTELLIMOUSE_ID) {
        // Try to enable 5 buttons as well!
        TRY(set_sample_rate_on_mouse_device(ps2_controller, port_index, 200));
        TRY(set_sample_rate_on_mouse_device(ps2_controller, port_index, 200));
        TRY(set_sample_rate_on_mouse_device(ps2_controller, port_index, 80));
        device_id_bytes = TRY(ps2_controller.read_device_id_while_device_port_locked(port_index));
    }

    if (device_id_bytes[0] == PS2MOUSE_INTELLIMOUSE_EXPLORER_ID)
        return PS2DeviceType::MouseWith5Buttons;
    else if (device_id_bytes[0] == PS2MOUSE_INTELLIMOUSE_ID)
        return PS2DeviceType::ScrollWheelMouse;
    else if (device_id_bytes[0] == 0)
        return PS2DeviceType::StandardMouse;
    return Error::from_errno(ENODEV);
}

UNMAP_AFTER_INIT ErrorOr<NonnullOwnPtr<PS2Device>> PS2MouseDevice::probe_and_initialize_instance(PS2Controller& ps2_controller, PS2PortIndex port_index, PS2DeviceType device_type)
{
    if (!is_valid_mouse_type(device_type))
        return Error::from_errno(ENODEV);

    auto mouse_device = TRY(MouseDevice::try_to_initialize());
    device_type = TRY(do_initialization_sequence(ps2_controller, port_index));
    auto device = TRY(adopt_nonnull_own_or_enomem(new (nothrow) PS2MouseDevice(ps2_controller, port_index, device_type, mouse_device)));
    return device;
}

}
