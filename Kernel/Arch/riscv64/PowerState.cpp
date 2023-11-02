/*
 * Copyright (c) 2023, Sönke Holz <sholz8530@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Arch/PowerState.h>

namespace Kernel {

void arch_specific_reboot()
{
    TODO_RISCV64();
}

void arch_specific_poweroff()
{
    TODO_RISCV64();
}

}
