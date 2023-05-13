/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2023, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/CircularQueue.h>
#include <Kernel/API/MousePacket.h>
#include <Kernel/Bus/SerialIO/PS2/Controller.h>
#include <Kernel/Bus/SerialIO/PS2/Device.h>
#include <Kernel/Devices/HID/MouseDevice.h>
#include <Kernel/Random.h>

namespace Kernel {
class PS2MouseDevice final : public PS2Device {
    friend class DeviceManagement;

public:
    static bool is_valid_mouse_type(PS2DeviceType device_type);
    static ErrorOr<PS2DeviceType> do_initialization_sequence(PS2Controller&, PS2PortIndex);

    static ErrorOr<NonnullOwnPtr<PS2Device>> probe_and_initialize_instance(PS2Controller&, PS2PortIndex, PS2DeviceType);

    virtual ~PS2MouseDevice() override;

    // ^PS2Device
    virtual void handle_byte_read_from_serial_input(u8 byte) override;

protected:
    PS2MouseDevice(PS2Controller const&, PS2PortIndex, PS2DeviceType, MouseDevice const&);

    struct RawPacket {
        union {
            u32 dword;
            u8 bytes[4];
        };
    };

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
