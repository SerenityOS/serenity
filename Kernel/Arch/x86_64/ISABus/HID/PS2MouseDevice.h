/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/CircularQueue.h>
#include <Kernel/API/MousePacket.h>
#include <Kernel/Arch/x86_64/ISABus/I8042Controller.h>
#include <Kernel/Interrupts/IRQHandler.h>
#include <Kernel/Security/Random.h>

namespace Kernel {
class PS2MouseDevice : public IRQHandler
    , public PS2Device {
    friend class DeviceManagement;

public:
    static ErrorOr<NonnullOwnPtr<PS2MouseDevice>> try_to_initialize(I8042Controller const&, MouseDevice const&);
    ErrorOr<void> initialize();

    virtual ~PS2MouseDevice() override;

    virtual StringView purpose() const override { return "PS2MouseDevice"sv; }

    // ^PS2Device
    virtual void irq_handle_byte_read(u8 byte) override;
    virtual void enable_interrupts() override
    {
        enable_irq();
    }
    virtual Type instrument_type() const override { return Type::Mouse; }

protected:
    PS2MouseDevice(I8042Controller const&, MouseDevice const&);

    // ^IRQHandler
    virtual bool handle_irq(RegisterState const&) override;

    struct RawPacket {
        union {
            u32 dword;
            u8 bytes[4];
        };
    };

    ErrorOr<u8> read_from_device();
    ErrorOr<u8> send_command(u8 command);
    ErrorOr<u8> send_command(u8 command, u8 data);
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
