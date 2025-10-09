/*
 * Copyright (c) 2023, Sönke Holz <soenke.holz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Arch/PowerState.h>
#include <Kernel/Arch/riscv64/SBI.h>

namespace Kernel {

static SBI::SystemReset::ResetReason power_off_or_reboot_reason_to_sbi_reset_reason(PowerOffOrRebootReason reason)
{
    if (reason == PowerOffOrRebootReason::NoReason)
        return SBI::SystemReset::ResetReason::NoReason;
    if (reason == PowerOffOrRebootReason::SystemFailure)
        return SBI::SystemReset::ResetReason::SystemFailure;

    VERIFY_NOT_REACHED();
}

void arch_specific_reboot(PowerOffOrRebootReason reason)
{
    auto ret = SBI::SystemReset::system_reset(SBI::SystemReset::ResetType::ColdReboot, power_off_or_reboot_reason_to_sbi_reset_reason(reason));
    dbgln("SBI: Failed to reboot: {}", ret);
    dbgln("SBI: Attempting to shut down using the legacy extension...");
    SBI::Legacy::shutdown();
}

void arch_specific_poweroff(PowerOffOrRebootReason reason)
{
    auto ret = SBI::SystemReset::system_reset(SBI::SystemReset::ResetType::Shutdown, power_off_or_reboot_reason_to_sbi_reset_reason(reason));
    dbgln("SBI: Failed to shut down: {}", ret);
    dbgln("SBI: Attempting to shut down using the legacy extension...");
    SBI::Legacy::shutdown();
}

}
