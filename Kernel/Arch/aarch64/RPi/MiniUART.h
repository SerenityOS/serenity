/*
 * Copyright (c) 2023, Daniel Bertalan <dani@danielbertalan.dev>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/Devices/CharacterDevice.h>
#include <Kernel/Devices/Device.h>
#include <Kernel/Locking/Spinlock.h>
#include <Kernel/Memory/TypedMapping.h>

namespace Kernel::RPi {

struct MiniUARTRegisters;

// Makes the secondary "mini UART" (UART1) available to the userspace.
// See bcm2711-peripherals.pdf chapter "2.2. Mini UART".
class MiniUART final : public CharacterDevice {
    friend class Kernel::Device;

public:
    static ErrorOr<NonnullRefPtr<MiniUART>> create();

    virtual ~MiniUART() override;

    // ^CharacterDevice
    virtual bool can_read(OpenFileDescription const&, u64) const override;
    virtual ErrorOr<size_t> read(OpenFileDescription&, u64, UserOrKernelBuffer&, size_t) override;
    virtual bool can_write(OpenFileDescription const&, u64) const override;
    virtual ErrorOr<size_t> write(OpenFileDescription&, u64, UserOrKernelBuffer const&, size_t) override;

    void put_char(u8);

private:
    MiniUART();

    // ^CharacterDevice
    virtual StringView class_name() const override { return "MiniUART"sv; }

    void set_baud_rate(u32);

    bool m_last_put_char_was_carriage_return { false };
    Spinlock<LockRank::None> m_serial_lock {};
    Memory::TypedMapping<MiniUARTRegisters volatile> m_registers;
};
}
