/*
 * Copyright (c) 2023, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Format.h>
#include <Kernel/Arch/PowerState.h>
#include <Kernel/FileSystem/FileSystem.h>
#include <Kernel/Firmware/ACPI/Parser.h>
#include <Kernel/Firmware/PowerState.h>
#include <Kernel/TTY/ConsoleManagement.h>
#include <Kernel/Tasks/Process.h>

namespace Kernel::Firmware {

void reboot()
{
    MutexLocker locker(Process::current().big_lock());

    dbgln("acquiring FS locks...");
    FileSystem::lock_all();
    dbgln("syncing mounted filesystems...");
    FileSystem::sync();
    dbgln("attempting reboot via ACPI");
    if (ACPI::is_enabled())
        ACPI::Parser::the()->try_acpi_reboot();

    arch_specific_reboot();

    dbgln("reboot attempts failed, applications will stop responding.");
    dmesgln("Reboot can't be completed. It's safe to turn off the computer!");
    Processor::halt();
}

void poweroff()
{
    MutexLocker locker(Process::current().big_lock());

    ConsoleManagement::the().switch_to_debug();

    dbgln("acquiring FS locks...");
    FileSystem::lock_all();
    dbgln("syncing mounted filesystems...");
    FileSystem::sync();

    dbgln("attempting system shutdown...");
    arch_specific_poweroff();

    dbgln("shutdown attempts failed, applications will stop responding.");
    dmesgln("Shutdown can't be completed. It's safe to turn off the computer!");
    Processor::halt();
}

}
