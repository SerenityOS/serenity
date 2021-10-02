/*
 * Copyright (c) 2021, Nico Weber <thakis@chromium.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Prekernel/Arch/aarch64/MMIO.h>
#include <Kernel/Prekernel/Arch/aarch64/Timer.h>

namespace Prekernel {

// "12.1 System Timer Registers" / "10.2 System Timer Registers"
struct TimerRegisters {
    u32 control_and_status;
    u32 counter_low;
    u32 counter_high;
    u32 compare[4];
};

// Bits of the `control_and_status` register.
// See "CS register" in Broadcom doc for details.
enum FlagBits {
    SystemTimerMatch0 = 1 << 0,
    SystemTimerMatch1 = 1 << 1,
    SystemTimerMatch2 = 1 << 2,
    SystemTimerMatch3 = 1 << 3,
};

Timer::Timer()
    : m_registers(MMIO::the().peripheral<TimerRegisters>(0x3000))
{
}

Timer& Timer::the()
{
    static Timer instance;
    return instance;
}

u64 Timer::microseconds_since_boot()
{
    u32 high = m_registers->counter_high;
    u32 low = m_registers->counter_low;
    if (high != m_registers->counter_high) {
        high = m_registers->counter_high;
        low = m_registers->counter_low;
    }
    return (static_cast<u64>(high) << 32) | low;
}

}
