/*
 * Copyright (c) 2021, Nico Weber <thakis@chromium.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>

namespace Prekernel {

struct TimerRegisters;

class Timer {
public:
    static Timer& the();

    u64 microseconds_since_boot();

private:
    Timer();

    TimerRegisters volatile* m_registers;
};

}
