/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/ACPI/Parser.h>
#include <Kernel/FileSystem/FileSystem.h>
#include <Kernel/IO.h>
#include <Kernel/Process.h>
#include <Kernel/TTY/ConsoleManagement.h>

namespace Kernel {

KResultOr<int> Process::sys$reboot()
{
    if (!is_superuser())
        return EPERM;

    REQUIRE_NO_PROMISES;

    dbgln("acquiring FS locks...");
    FS::lock_all();
    dbgln("syncing mounted filesystems...");
    FS::sync();
    dbgln("attempting reboot via ACPI");
    if (ACPI::is_enabled())
        ACPI::Parser::the()->try_acpi_reboot();
    dbgln("attempting reboot via KB Controller...");
    IO::out8(0x64, 0xFE);

    return 0;
}

KResultOr<int> Process::sys$halt()
{
    if (!is_superuser())
        return EPERM;

    REQUIRE_NO_PROMISES;
    ConsoleManagement::the().switch_to_debug();

    dbgln("acquiring FS locks...");
    FS::lock_all();
    dbgln("syncing mounted filesystems...");
    FS::sync();
    dbgln("attempting system shutdown...");
    // QEMU Shutdown
    IO::out16(0x604, 0x2000);
    // If we're here, the shutdown failed. Try VirtualBox shutdown.
    IO::out16(0x4004, 0x3400);
    // VirtualBox shutdown failed. Try Bochs/Old QEMU shutdown.
    IO::out16(0xb004, 0x2000);
    dbgln("shutdown attempts failed, applications will stop responding.");
    dmesgln("Shutdown can't be completed. It's safe to turn off the computer!");
    Processor::halt();
}

}
