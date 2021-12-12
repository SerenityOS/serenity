/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Arch/x86/IO.h>
#include <Kernel/FileSystem/FileSystem.h>
#include <Kernel/Firmware/ACPI/Parser.h>
#include <Kernel/Firmware/PowerStateSwitch.h>
#include <Kernel/Process.h>
#include <Kernel/Sections.h>
#include <Kernel/TTY/ConsoleManagement.h>

namespace Kernel {

mode_t PowerStateSwitchNode::permissions() const
{
    return S_IRUSR | S_IRGRP | S_IWUSR | S_IWGRP;
}

UNMAP_AFTER_INIT NonnullRefPtr<PowerStateSwitchNode> PowerStateSwitchNode::must_create(FirmwareSysFSDirectory& firmware_directory)
{
    return adopt_ref_if_nonnull(new (nothrow) PowerStateSwitchNode(firmware_directory)).release_nonnull();
}

UNMAP_AFTER_INIT PowerStateSwitchNode::PowerStateSwitchNode(FirmwareSysFSDirectory&)
    : SysFSComponent()
{
}

ErrorOr<void> PowerStateSwitchNode::truncate(u64 size)
{
    // Note: This node doesn't store any useful data anyway, so we can safely
    // truncate this to zero (essentially ignoring the request without failing).
    if (size != 0)
        return EPERM;
    return {};
}

ErrorOr<size_t> PowerStateSwitchNode::write_bytes(off_t offset, size_t count, UserOrKernelBuffer const& data, OpenFileDescription*)
{
    if (Checked<off_t>::addition_would_overflow(offset, count))
        return EOVERFLOW;
    if (offset > 0)
        return EINVAL;
    if (count > 1)
        return EINVAL;

    char buf[1];
    TRY(data.read(buf, 1));
    switch (buf[0]) {
    case '0':
        return EINVAL;
    case '1':
        reboot();
        VERIFY_NOT_REACHED();
    case '2':
        poweroff();
        VERIFY_NOT_REACHED();
    default:
        return EINVAL;
    }
    VERIFY_NOT_REACHED();
}

void PowerStateSwitchNode::reboot()
{
    MutexLocker locker(Process::current().big_lock());

    dbgln("acquiring FS locks...");
    FileSystem::lock_all();
    dbgln("syncing mounted filesystems...");
    FileSystem::sync();
    dbgln("attempting reboot via ACPI");
    if (ACPI::is_enabled())
        ACPI::Parser::the()->try_acpi_reboot();
    dbgln("attempting reboot via KB Controller...");
    IO::out8(0x64, 0xFE);
    dbgln("reboot attempts failed, applications will stop responding.");
    dmesgln("Reboot can't be completed. It's safe to turn off the computer!");
    Processor::halt();
}

void PowerStateSwitchNode::poweroff()
{
    MutexLocker locker(Process::current().big_lock());

    ConsoleManagement::the().switch_to_debug();

    dbgln("acquiring FS locks...");
    FileSystem::lock_all();
    dbgln("syncing mounted filesystems...");
    FileSystem::sync();
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
