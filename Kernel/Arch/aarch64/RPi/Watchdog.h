/*
 * Copyright (c) 2023, Daniel Bertalan <dani@danielbertalan.dev>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/Memory/TypedMapping.h>

namespace Kernel::RPi {

struct WatchdogRegisters;

class Watchdog {
public:
    Watchdog();
    static Watchdog& the();

    void system_shutdown();

private:
    Memory::TypedMapping<WatchdogRegisters volatile> m_registers;
};
}
