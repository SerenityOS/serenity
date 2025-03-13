/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2023, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/API/MousePacket.h>
#include <Kernel/Bus/SerialIO/Device.h>
#include <Kernel/Bus/SerialIO/PS2Definitions.h>
#include <Kernel/Devices/Input/MouseDevice.h>
#include <Kernel/Security/Random.h>

namespace Kernel {
class PS2MouseDevice : public SerialIODevice {
    friend class Device;

public:
    static ErrorOr<NonnullOwnPtr<PS2MouseDevice>> try_to_initialize(SerialIOController const&, SerialIOController::PortIndex, MouseDevice const&);
    ErrorOr<void> initialize();

    virtual ~PS2MouseDevice() override;

    // ^SerialIODevice
    virtual void handle_byte_read_from_serial_input(u8 byte) override;

protected:
    PS2MouseDevice(SerialIOController const&, SerialIOController::PortIndex, MouseDevice const&);

    struct RawPacket {
        union {
            u32 dword;
            u8 bytes[4];
        };
    };

    ErrorOr<int> reset_device();
    ErrorOr<u8> read_from_device();
    ErrorOr<void> send_command(SerialIOController::DeviceCommand command);
    ErrorOr<void> send_command(SerialIOController::DeviceCommand command, u8 data);
    MousePacket parse_data_packet(RawPacket const&);
    ErrorOr<void> set_sample_rate(u8);
    ErrorOr<u8> get_device_id();

    u8 m_data_state { 0 };
    RawPacket m_data;
    bool m_has_wheel { false };
    bool m_has_five_buttons { false };

    NonnullRefPtr<MouseDevice> const m_mouse_device;
};

}
