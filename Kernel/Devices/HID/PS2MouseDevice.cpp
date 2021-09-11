/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Memory.h>
#include <Kernel/Debug.h>
#include <Kernel/Devices/DeviceManagement.h>
#include <Kernel/Devices/HID/PS2MouseDevice.h>
#include <Kernel/Devices/VMWareBackdoor.h>
#include <Kernel/IO.h>
#include <Kernel/Sections.h>

namespace Kernel {

#define IRQ_MOUSE 12

#define PS2MOUSE_SET_RESOLUTION 0xE8
#define PS2MOUSE_STATUS_REQUEST 0xE9
#define PS2MOUSE_REQUEST_SINGLE_PACKET 0xEB
#define PS2MOUSE_GET_DEVICE_ID 0xF2
#define PS2MOUSE_SET_SAMPLE_RATE 0xF3
#define PS2MOUSE_ENABLE_PACKET_STREAMING 0xF4
#define PS2MOUSE_DISABLE_PACKET_STREAMING 0xF5
#define PS2MOUSE_SET_DEFAULTS 0xF6
#define PS2MOUSE_RESEND 0xFE
#define PS2MOUSE_RESET 0xFF

#define PS2MOUSE_INTELLIMOUSE_ID 0x03
#define PS2MOUSE_INTELLIMOUSE_EXPLORER_ID 0x04

UNMAP_AFTER_INIT PS2MouseDevice::PS2MouseDevice(const I8042Controller& ps2_controller)
    : IRQHandler(IRQ_MOUSE)
    , MouseDevice()
    , I8042Device(ps2_controller)
{
}

UNMAP_AFTER_INIT PS2MouseDevice::~PS2MouseDevice()
{
}

bool PS2MouseDevice::handle_irq(const RegisterState&)
{
    // The controller will read the data and call irq_handle_byte_read
    // for the appropriate device
    return m_i8042_controller->irq_process_input_buffer(instrument_type());
}

void PS2MouseDevice::irq_handle_byte_read(u8 byte)
{
    auto commit_packet = [&] {
        m_data_state = 0;
        dbgln_if(PS2MOUSE_DEBUG, "PS2Mouse: {}, {} {} {}",
            m_data.bytes[1],
            m_data.bytes[2],
            (m_data.bytes[0] & 1) ? "Left" : "",
            (m_data.bytes[0] & 2) ? "Right" : "");

        m_entropy_source.add_random_event(m_data.dword);

        {
            SpinlockLocker lock(m_queue_lock);
            m_queue.enqueue(parse_data_packet(m_data));
        }
        evaluate_block_conditions();
    };

    VERIFY(m_data_state < sizeof(m_data.bytes) / sizeof(m_data.bytes[0]));
    m_data.bytes[m_data_state] = byte;

    switch (m_data_state) {
    case 0:
        if (!(byte & 0x08)) {
            dbgln("PS2Mouse: Stream out of sync.");
            break;
        }
        ++m_data_state;
        break;
    case 1:
        ++m_data_state;
        break;
    case 2:
        if (m_has_wheel) {
            ++m_data_state;
            break;
        }
        commit_packet();
        break;
    case 3:
        VERIFY(m_has_wheel);
        commit_packet();
        break;
    }
}

MousePacket PS2MouseDevice::parse_data_packet(const RawPacket& raw_packet)
{
    int x = raw_packet.bytes[1];
    int y = raw_packet.bytes[2];
    int z = 0;
    if (m_has_wheel) {
        // FIXME: For non-Intellimouse, this is a full byte.
        //        However, for now, m_has_wheel is only set for Intellimouse.
        z = (char)(raw_packet.bytes[3] & 0x0f);

        // -1 in 4 bits
        if (z == 15)
            z = -1;
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
    packet.buttons = raw_packet.bytes[0] & 0x07;

    if (m_has_five_buttons) {
        if (raw_packet.bytes[3] & 0x10)
            packet.buttons |= MousePacket::BackButton;
        if (raw_packet.bytes[3] & 0x20)
            packet.buttons |= MousePacket::ForwardButton;
    }

    packet.is_relative = true;
    dbgln_if(PS2MOUSE_DEBUG, "PS2 Relative Mouse: Buttons {:x}", packet.buttons);
    dbgln_if(PS2MOUSE_DEBUG, "Mouse: X {}, Y {}, Z {}", packet.x, packet.y, packet.z);
    return packet;
}

u8 PS2MouseDevice::get_device_id()
{
    if (send_command(PS2MOUSE_GET_DEVICE_ID) != I8042_ACK)
        return 0;
    return read_from_device();
}

u8 PS2MouseDevice::read_from_device()
{
    return m_i8042_controller->read_from_device(instrument_type());
}

u8 PS2MouseDevice::send_command(u8 command)
{
    u8 response = m_i8042_controller->send_command(instrument_type(), command);
    if (response != I8042_ACK)
        dbgln("PS2MouseDevice: Command {} got {} but expected ack: {}", command, response, I8042_ACK);
    return response;
}

u8 PS2MouseDevice::send_command(u8 command, u8 data)
{
    u8 response = m_i8042_controller->send_command(instrument_type(), command, data);
    if (response != I8042_ACK)
        dbgln("PS2MouseDevice: Command {} got {} but expected ack: {}", command, response, I8042_ACK);
    return response;
}

void PS2MouseDevice::set_sample_rate(u8 rate)
{
    send_command(PS2MOUSE_SET_SAMPLE_RATE, rate);
}

UNMAP_AFTER_INIT RefPtr<PS2MouseDevice> PS2MouseDevice::try_to_initialize(const I8042Controller& ps2_controller)
{
    auto mouse_device_or_error = DeviceManagement::try_create_device<PS2MouseDevice>(ps2_controller);
    // FIXME: Find a way to propagate errors
    VERIFY(!mouse_device_or_error.is_error());
    if (mouse_device_or_error.value()->initialize())
        return mouse_device_or_error.release_value();
    return nullptr;
}

UNMAP_AFTER_INIT bool PS2MouseDevice::initialize()
{
    if (!m_i8042_controller->reset_device(instrument_type())) {
        dbgln("PS2MouseDevice: I8042 controller failed to reset device");
        return false;
    }

    u8 device_id = read_from_device();

    // Set default settings.
    if (send_command(PS2MOUSE_SET_DEFAULTS) != I8042_ACK)
        return false;

    if (send_command(PS2MOUSE_ENABLE_PACKET_STREAMING) != I8042_ACK)
        return false;

    if (device_id != PS2MOUSE_INTELLIMOUSE_ID) {
        // Send magical wheel initiation sequence.
        set_sample_rate(200);
        set_sample_rate(100);
        set_sample_rate(80);
        device_id = get_device_id();
    }
    if (device_id == PS2MOUSE_INTELLIMOUSE_ID) {
        m_has_wheel = true;
        dmesgln("PS2MouseDevice: Mouse wheel enabled!");
    } else {
        dmesgln("PS2MouseDevice: No mouse wheel detected!");
    }

    if (device_id == PS2MOUSE_INTELLIMOUSE_ID) {
        // Try to enable 5 buttons as well!
        set_sample_rate(200);
        set_sample_rate(200);
        set_sample_rate(80);
        device_id = get_device_id();
    }

    if (device_id == PS2MOUSE_INTELLIMOUSE_EXPLORER_ID) {
        m_has_five_buttons = true;
        dmesgln("PS2MouseDevice: 5 buttons enabled!");
    }
    return true;
}

}
