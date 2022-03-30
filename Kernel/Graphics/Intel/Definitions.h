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
    DPLLDivisorA0 = 0x6040,
    DPLLDivisorA1 = 0x6044,
    DPLLControlA = 0x6014,
    DPLLControlB = 0x6018,
    DPLLMultiplierA = 0x601C,
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

}
