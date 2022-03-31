/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>

namespace Kernel::IntelGraphics {

enum class GlobalGenerationRegister {
    PipeAConf = 0x70008,
    PipeBConf = 0x71008,
    AnalogDisplayPort = 0x61100,
    VGADisplayPlaneControl = 0x71400,
};

struct PLLSettings;

struct PLLParameterLimit {
    size_t min, max;
};

struct PLLMaxSettings {
    PLLParameterLimit dot_clock, vco, n, m, m1, m2, p, p1, p2;
};

struct PLLSettings {
    bool is_valid() const { return (n != 0 && m1 != 0 && m2 != 0 && p1 != 0 && p2 != 0); }
    u64 compute_dot_clock(u64 refclock) const
    {
        return (refclock * (5 * m1 + m2) / n) / (p1 * p2);
    }

    u64 compute_vco(u64 refclock) const
    {
        return refclock * (5 * m1 + m2) / n;
    }

    u64 compute_m() const
    {
        return 5 * m1 + m2;
    }

    u64 compute_p() const
    {
        return p1 * p2;
    }
    u64 n { 0 };
    u64 m1 { 0 };
    u64 m2 { 0 };
    u64 p1 { 0 };
    u64 p2 { 0 };
};

enum class DisplayPortAuxiliaryOperation {
    I2CWrite = 0,
    I2CRead = 1,
    MOT = 4,
    NativeWrite = 0x8,
    NativeRead = 0x9,
};

struct [[gnu::packed]] DisplayPortAuxChannelRegisters {
    u32 control;
    u32 data1;
    u32 data2;
    u32 data3;
    u32 data4;
};

}
