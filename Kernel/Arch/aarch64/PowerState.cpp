/*
 * Copyright (c) 2023, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Arch/PowerState.h>
#include <Kernel/Arch/aarch64/RPi/Watchdog.h>
#include <Kernel/Firmware/DeviceTree/DeviceTree.h>

namespace Kernel {

void arch_specific_reboot()
{
}

void arch_specific_poweroff()
{
    if (DeviceTree::get().is_compatible_with("raspberrypi,3-model-b"sv) || DeviceTree::get().is_compatible_with("raspberrypi,4-model-b"sv))
        RPi::Watchdog::the().system_shutdown();
}

}
