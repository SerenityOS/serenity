/*
 * Copyright (c) 2026, the SerenityOS developers
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

namespace Kernel {

enum class HardwareTimerType {
    // x86
    i8253,                   // PIT
    RTC,                     // Real Time Clock
    HighPrecisionEventTimer, // also known as IA-PC HPET
    LocalAPICTimer,          // Local APIC
    TSC,                     // Time Stamp Counter

    // AArch64
    RPiTimer,
    ARMv8Timer,

    // RISC-V
    RISCVTimer,
};

}
