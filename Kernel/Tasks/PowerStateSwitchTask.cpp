/*
 * Copyright (c) 2023, kleines Filmröllchen <filmroellchen@serenityos.org>
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
#include <AK/StringView.h>
#include <Kernel/Arch/Delay.h>
#include <Kernel/Arch/PowerState.h>
#include <Kernel/FileSystem/FileSystem.h>
#include <Kernel/FileSystem/VirtualFileSystem.h>
#include <Kernel/Firmware/ACPI/Parser.h>
#include <Kernel/Library/Panic.h>
#include <Kernel/Sections.h>
#include <Kernel/TTY/ConsoleManagement.h>
#include <Kernel/Tasks/FinalizerTask.h>
#include <Kernel/Tasks/PowerStateSwitchTask.h>
#include <Kernel/Tasks/Process.h>
#include <Kernel/Tasks/ProcessManagement.h>
#include <Kernel/Tasks/Scheduler.h>
#include <Kernel/Tasks/WorkQueue.h>

namespace Kernel {

Thread* g_power_state_switch_task;
bool g_in_system_shutdown { false };

void PowerStateSwitchTask::power_state_switch_task(void* raw_entry_data)
{
    Thread::current()->set_priority(THREAD_PRIORITY_HIGH);
    auto entry_data = bit_cast<PowerStateCommand>(raw_entry_data);
    switch (entry_data) {
    case PowerStateCommand::Shutdown:
        MUST(PowerStateSwitchTask::perform_shutdown(DoReboot::No));
        break;
    case PowerStateCommand::Reboot:
        MUST(PowerStateSwitchTask::perform_shutdown(DoReboot::Yes));
        break;
    default:
        PANIC("Unknown power state command: {}", to_underlying(entry_data));
    }

    // Although common, the system may not halt through this task.
    // Clear the power state switch task so that it can be spawned again.
    g_power_state_switch_task = nullptr;
}

void PowerStateSwitchTask::spawn(PowerStateCommand command)
{
    VERIFY(g_power_state_switch_task == nullptr);
    auto [_, power_state_switch_task_thread] = MUST(Process::create_kernel_process(
        "Power State Switch Task"sv, power_state_switch_task, bit_cast<void*>(command)));
    g_power_state_switch_task = move(power_state_switch_task_thread);
}

ErrorOr<void> PowerStateSwitchTask::perform_shutdown(PowerStateSwitchTask::DoReboot do_reboot)
{
    // We assume that by this point userland has tried as much as possible to shut down everything in an orderly fashion.
    // Therefore, we force kill remaining processes, including Kernel processes, except the finalizer and ourselves.
    dbgln("Killing remaining processes...");
    // NOTE: Allow init process to be killed. Stop processes and threads from doing syscalls!
    g_in_system_shutdown = true;

    // NOTE: Make sure to kill all user processes first, otherwise we will not be able to
    // unmount filesystems afterwards.
    TRY(ProcessManagement::the().kill_all_user_processes({}));

    ConsoleManagement::the().switch_to_debug();

    dbgln("Locking & syncing all file systems...");
    FileSystem::lock_all();
    auto wait_until_work_queue_is_empty_or_timeout = [](WorkQueue& queue) -> bool {
        size_t milliseconds_waiting = 0;
        while (!queue.is_empty()) {
            // Wait at most 10 seconds until failing
            if (milliseconds_waiting > 10000)
                return false;
            microseconds_delay(1000);
        }
        return true;
    };
    if (!wait_until_work_queue_is_empty_or_timeout(*g_ata_work))
        dbgln("WARNING! ATA WorkQueue has not been flushed within 10 seconds");
    if (!wait_until_work_queue_is_empty_or_timeout(*g_io_work))
        dbgln("WARNING! IO WorkQueue has not been flushed within 10 seconds");
    FileSystem::sync();

    // Don't panic here (since we may panic in a bit anyways) but report the probable cause of an unclean shutdown.
    if (ProcessManagement::the().alive_processes_count(ProcessManagement::ProcessKind::User) != 0)
        dbgln("WARNING: Some user processes are still alive; proper shutdown may fail!");

    dbgln("Unmounting all file systems...");

    auto unmount_was_successful = true;
    while (unmount_was_successful) {
        unmount_was_successful = false;
        Vector<Mount&, 16> mounts;
        TRY(VirtualFileSystem::the().for_each_mount([&](auto const& mount) -> ErrorOr<void> {
            TRY(mounts.try_append(const_cast<Mount&>(mount)));
            return {};
        }));
        if (mounts.is_empty())
            break;
        auto const remaining_mounts = mounts.size();

        while (!mounts.is_empty()) {
            auto& mount = mounts.take_last();
            mount.guest_fs().flush_writes();

            auto mount_path = TRY(mount.absolute_path());
            auto& mount_inode = mount.guest();
            auto const result = VirtualFileSystem::the().unmount(mount_inode, mount_path->view());
            if (result.is_error()) {
                dbgln("Error during unmount of {}: {}", mount_path, result.error());
                // FIXME: For unknown reasons the root FS stays busy even after everything else has shut down and was unmounted.
                //        Until we find the underlying issue, allow an unclean shutdown here.
                if (remaining_mounts <= 1)
                    dbgln("BUG! One mount remaining; the root file system may not be unmountable at all. Shutting down anyways.");
            } else {
                unmount_was_successful = true;
            }
        }
    }

    // NOTE: We don't really need to kill kernel processes, because in contrast
    // to user processes, kernel processes will simply not make syscalls
    // or do some other unexpected behavior.
    // Therefore, we just lock the scheduler big lock to ensure nothing happens
    // beyond this point forward.
    SpinlockLocker lock(g_scheduler_lock);

    if (do_reboot == DoReboot::Yes) {
        dbgln("Attempting system reboot...");
        dbgln("attempting reboot via ACPI");
        if (ACPI::is_enabled())
            ACPI::Parser::the()->try_acpi_reboot();
        arch_specific_reboot();

        dmesgln("Reboot can't be completed. It's safe to turn off the computer!");
        Processor::halt();
        VERIFY_NOT_REACHED();
    }
    VERIFY(do_reboot == DoReboot::No);

    dbgln("Attempting system shutdown...");
    arch_specific_poweroff();
    dmesgln("Shutdown can't be completed. It's safe to turn off the computer!");
    Processor::halt();
    VERIFY_NOT_REACHED();
}
}
