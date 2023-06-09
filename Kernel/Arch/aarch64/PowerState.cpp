/*
 * Copyright (c) 2023, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Arch/PowerState.h>
#include <Kernel/Arch/aarch64/RPi/Watchdog.h>

namespace Kernel {

void arch_specific_reboot()
{
}

void arch_specific_poweroff()
{
    RPi::Watchdog::the().system_shutdown();
}

}
