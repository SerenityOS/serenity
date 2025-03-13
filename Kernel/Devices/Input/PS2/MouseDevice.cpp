/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2023, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Arch/Delay.h>
#include <Kernel/Debug.h>
#include <Kernel/Devices/Device.h>
#include <Kernel/Devices/Input/PS2/MouseDevice.h>
#include <Kernel/Sections.h>

namespace Kernel {

#define PS2MOUSE_INTELLIMOUSE_ID 0x03
#define PS2MOUSE_INTELLIMOUSE_EXPLORER_ID 0x04

UNMAP_AFTER_INIT PS2MouseDevice::PS2MouseDevice(SerialIOController const& serial_io_controller, SerialIOController::PortIndex port_index, MouseDevice const& mouse_device)
    : SerialIODevice(serial_io_controller, port_index)
    , m_mouse_device(mouse_device)
{
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

ErrorOr<u8> PS2MouseDevice::get_device_id()
{
    TRY(send_command(SerialIOController::DeviceCommand::GetDeviceID));
    return read_from_device();
}

ErrorOr<u8> PS2MouseDevice::read_from_device()
{
    return attached_controller().read_from_device(attached_port_index());
}

ErrorOr<void> PS2MouseDevice::send_command(SerialIOController::DeviceCommand command)
{
    TRY(attached_controller().send_command(attached_port_index(), command));
    return {};
}

ErrorOr<void> PS2MouseDevice::send_command(SerialIOController::DeviceCommand command, u8 data)
{
    TRY(attached_controller().send_command(attached_port_index(), command, data));
    return {};
}

ErrorOr<void> PS2MouseDevice::set_sample_rate(u8 rate)
{
    TRY(send_command(SerialIOController::DeviceCommand::SetSampleRate, rate));
    return {};
}

UNMAP_AFTER_INIT ErrorOr<NonnullOwnPtr<PS2MouseDevice>> PS2MouseDevice::try_to_initialize(SerialIOController const& serial_io_controller, SerialIOController::PortIndex port_index, MouseDevice const& mouse_device)
{
    auto device = TRY(adopt_nonnull_own_or_enomem(new (nothrow) PS2MouseDevice(serial_io_controller, port_index, mouse_device)));
    TRY(device->initialize());
    return device;
}

UNMAP_AFTER_INIT ErrorOr<int> PS2MouseDevice::reset_device()
{
    auto do_reset = [this] -> ErrorOr<int> {
        TRY(attached_controller().reset_device(attached_port_index()));
        return TRY(read_from_device());
    };
    auto maybe_device_id = do_reset();
    for (int attempt = 1; attempt < 10; attempt++) {
        if (!maybe_device_id.is_error())
            return maybe_device_id;
        microseconds_delay(500);
        maybe_device_id = do_reset();
    }
    return maybe_device_id;
}

UNMAP_AFTER_INIT ErrorOr<void> PS2MouseDevice::initialize()
{
    int device_id = TRY(reset_device());

    TRY(send_command(SerialIOController::DeviceCommand::SetDefaults));
    TRY(send_command(SerialIOController::DeviceCommand::EnablePacketStreaming));

    if (device_id != PS2MOUSE_INTELLIMOUSE_ID) {
        // Send magical wheel initiation sequence.
        TRY(set_sample_rate(200));
        TRY(set_sample_rate(100));
        TRY(set_sample_rate(80));
        device_id = TRY(get_device_id());
    }
    if (device_id == PS2MOUSE_INTELLIMOUSE_ID) {
        m_has_wheel = true;
        dmesgln("PS2MouseDevice: Mouse wheel enabled!");
    } else {
        dmesgln("PS2MouseDevice: No mouse wheel detected!");
    }

    if (device_id == PS2MOUSE_INTELLIMOUSE_ID) {
        // Try to enable 5 buttons as well!
        TRY(set_sample_rate(200));
        TRY(set_sample_rate(200));
        TRY(set_sample_rate(80));
        device_id = TRY(get_device_id());
    }

    if (device_id == PS2MOUSE_INTELLIMOUSE_EXPLORER_ID) {
        m_has_five_buttons = true;
        dmesgln("PS2MouseDevice: 5 buttons enabled!");
    }
    return {};
}

}
