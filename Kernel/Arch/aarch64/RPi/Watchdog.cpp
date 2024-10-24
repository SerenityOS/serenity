/*
 * Copyright (c) 2023, Daniel Bertalan <dani@danielbertalan.dev>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Singleton.h>
#include <Kernel/Arch/aarch64/RPi/MMIO.h>
#include <Kernel/Arch/aarch64/RPi/Watchdog.h>

namespace Kernel::RPi {
struct WatchdogRegisters {
    u32 rstc;
    u32 rsts;
    u32 wdog;
};

constexpr u32 PASSWORD = 0x5a000000;
constexpr u32 RSTS_PARTITION_MASK = 0xfffffaaa;
constexpr u32 RSTS_PARTITION_SHUTDOWN = 0x00000555;
constexpr u32 RSTC_WRCFG_MASK = 0xffffffcf;
constexpr u32 RSTC_WRCFG_FULL_RESET = 0x00000020;

Watchdog::Watchdog()
    : m_registers(MMIO::the().peripheral<WatchdogRegisters>(0x10'001c).release_value_but_fixme_should_propagate_errors())
{
}

Watchdog& Watchdog::the()
{
    static Singleton<Watchdog> watchdog;
    return watchdog;
}

// This is the same mechanism used by Linux, the ARM Trusted Firmware and U-Boot to trigger a system shutdown.
// See e.g. https://github.com/ARM-software/arm-trusted-firmware/blob/dcf430656ca8ef964fa55ad9eb81cf838c7837f2/plat/rpi/common/rpi3_pm.c#L231-L249
void Watchdog::system_shutdown()
{
    // The Raspberry Pi hardware doesn't support powering off. Setting the reboot target partition to this
    // special value will cause the firmware to halt the CPU and put it in a low power state when the watchdog
    // timer expires. When running under Qemu, this will cause the emulator to exit.
    m_registers->rsts = PASSWORD | (m_registers->rsts & RSTS_PARTITION_MASK) | RSTS_PARTITION_SHUTDOWN;
    // Set the timeout to 10 ticks (~150us).
    m_registers->wdog = PASSWORD | 10;
    // Start the watchdog.
    m_registers->rstc = PASSWORD | (m_registers->rstc & RSTC_WRCFG_MASK) | RSTC_WRCFG_FULL_RESET;
}
}
