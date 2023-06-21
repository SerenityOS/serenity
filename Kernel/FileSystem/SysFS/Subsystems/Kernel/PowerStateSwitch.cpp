/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Platform.h>
#if ARCH(X86_64)
#    include <Kernel/Arch/x86_64/I8042Reboot.h>
#    include <Kernel/Arch/x86_64/Shutdown.h>
#elif ARCH(AARCH64)
#    include <Kernel/Arch/aarch64/RPi/Watchdog.h>
#endif
#include <Kernel/FileSystem/FileSystem.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Kernel/PowerStateSwitch.h>
#include <Kernel/Firmware/ACPI/Parser.h>
#include <Kernel/Sections.h>
#include <Kernel/TTY/ConsoleManagement.h>
#include <Kernel/Tasks/Process.h>

namespace Kernel {

mode_t SysFSPowerStateSwitchNode::permissions() const
{
    return S_IRUSR | S_IRGRP | S_IWUSR | S_IWGRP;
}

UNMAP_AFTER_INIT NonnullRefPtr<SysFSPowerStateSwitchNode> SysFSPowerStateSwitchNode::must_create(SysFSDirectory const& parent_directory)
{
    return adopt_ref_if_nonnull(new (nothrow) SysFSPowerStateSwitchNode(parent_directory)).release_nonnull();
}

UNMAP_AFTER_INIT SysFSPowerStateSwitchNode::SysFSPowerStateSwitchNode(SysFSDirectory const& parent_directory)
    : SysFSComponent(parent_directory)
{
}

ErrorOr<void> SysFSPowerStateSwitchNode::truncate(u64 size)
{
    // Note: This node doesn't store any useful data anyway, so we can safely
    // truncate this to zero (essentially ignoring the request without failing).
    if (size != 0)
        return EPERM;
    return {};
}

ErrorOr<size_t> SysFSPowerStateSwitchNode::write_bytes(off_t offset, size_t count, UserOrKernelBuffer const& data, OpenFileDescription*)
{
    // Note: If we are in a jail, don't let the current process to change the power state.
    if (Process::current().is_currently_in_jail())
        return Error::from_errno(EPERM);
    if (Checked<off_t>::addition_would_overflow(offset, count))
        return Error::from_errno(EOVERFLOW);
    if (offset > 0)
        return Error::from_errno(EINVAL);
    if (count > 1)
        return Error::from_errno(EINVAL);
    char buf[1];
    TRY(data.read(buf, 1));
    switch (buf[0]) {
    case '1':
        reboot();
        VERIFY_NOT_REACHED();
    case '2':
        poweroff();
        VERIFY_NOT_REACHED();
    default:
        return Error::from_errno(EINVAL);
    }
}

void SysFSPowerStateSwitchNode::reboot()
{
    MutexLocker locker(Process::current().big_lock());

    dbgln("acquiring FS locks...");
    FileSystem::lock_all();
    dbgln("syncing mounted filesystems...");
    FileSystem::sync();
    dbgln("attempting reboot via ACPI");
    if (ACPI::is_enabled())
        ACPI::Parser::the()->try_acpi_reboot();
#if ARCH(X86_64)
    i8042_reboot();
#endif
    dbgln("reboot attempts failed, applications will stop responding.");
    dmesgln("Reboot can't be completed. It's safe to turn off the computer!");
    Processor::halt();
}

void SysFSPowerStateSwitchNode::poweroff()
{
    MutexLocker locker(Process::current().big_lock());

    ConsoleManagement::the().switch_to_debug();

    dbgln("acquiring FS locks...");
    FileSystem::lock_all();
    dbgln("syncing mounted filesystems...");
    FileSystem::sync();
    dbgln("attempting system shutdown...");
#if ARCH(X86_64)
    qemu_shutdown();
    virtualbox_shutdown();
#elif ARCH(AARCH64)
    RPi::Watchdog::the().system_shutdown();
#endif
    dbgln("shutdown attempts failed, applications will stop responding.");
    dmesgln("Shutdown can't be completed. It's safe to turn off the computer!");
    Processor::halt();
}

}
