/*
 * Copyright (c) 2023, Daniel Bertalan <dani@danielbertalan.dev>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

namespace Kernel::RPi {

struct WatchdogRegisters;

class Watchdog {
public:
    static Watchdog& the();

    void system_shutdown();

private:
    Watchdog();

    WatchdogRegisters volatile* m_registers;
};
}
