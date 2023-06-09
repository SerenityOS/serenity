/*
 * Copyright (c) 2023, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Arch/PowerState.h>
#include <Kernel/Arch/x86_64/I8042Reboot.h>
#include <Kernel/Arch/x86_64/Shutdown.h>

namespace Kernel {

void arch_specific_reboot()
{
    i8042_reboot();
}

void arch_specific_poweroff()
{
    qemu_shutdown();
    virtualbox_shutdown();
}

}
