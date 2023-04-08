/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/CircularQueue.h>
#include <AK/Types.h>
#include <Kernel/API/KeyCode.h>
#include <Kernel/Arch/x86_64/ISABus/I8042Controller.h>
#include <Kernel/Interrupts/IRQHandler.h>
#include <Kernel/Random.h>

namespace Kernel {

class PS2KeyboardDevice final : public IRQHandler
    , public PS2Device {
    friend class DeviceManagement;

public:
    static ErrorOr<NonnullOwnPtr<PS2KeyboardDevice>> try_to_initialize(I8042Controller const&, KeyboardDevice const&);
    virtual ~PS2KeyboardDevice() override;
    ErrorOr<void> initialize();

    virtual StringView purpose() const override { return "PS2KeyboardDevice"sv; }

    // ^PS2Device
    virtual void irq_handle_byte_read(u8 byte) override;
    virtual void enable_interrupts() override
    {
        enable_irq();
    }
    virtual Type instrument_type() const override { return Type::Keyboard; }

private:
    PS2KeyboardDevice(I8042Controller const&, KeyboardDevice const&);

    // ^IRQHandler
    virtual bool handle_irq(RegisterState const&) override;

    bool m_has_e0_prefix { false };

    NonnullRefPtr<KeyboardDevice> const m_keyboard_device;
};

}
