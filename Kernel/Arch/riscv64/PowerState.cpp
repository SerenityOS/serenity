/*
 * Copyright (c) 2023, Sönke Holz <sholz8530@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Arch/PowerState.h>
#include <Kernel/Arch/riscv64/SBI.h>

namespace Kernel {

void arch_specific_reboot()
{
    auto ret = SBI::SystemReset::system_reset(SBI::SystemReset::ResetType::ColdReboot, SBI::SystemReset::ResetReason::NoReason);
    dbgln("SBI: Failed to reboot: {}", ret);
    dbgln("SBI: Attempting to shut down using the legacy extension...");
    SBI::Legacy::shutdown();
}

void arch_specific_poweroff()
{
    auto ret = SBI::SystemReset::system_reset(SBI::SystemReset::ResetType::Shutdown, SBI::SystemReset::ResetReason::NoReason);
    dbgln("SBI: Failed to shut down: {}", ret);
    dbgln("SBI: Attempting to shut down using the legacy extension...");
    SBI::Legacy::shutdown();
}

}
