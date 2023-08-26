/*
 * Copyright (c) 2023, kleines Filmröllchen <filmroellchen@serenityos.org>
 * Copyright (c) 2023, Liav A. <liavalb@hotmail.co.il>
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
#include <Kernel/Arch/PowerState.h>
#include <Kernel/Devices/TTY/ConsoleManagement.h>
#include <Kernel/FileSystem/FileSystem.h>
#include <Kernel/FileSystem/VirtualFileSystem.h>
#include <Kernel/Firmware/ACPI/Parser.h>
#include <Kernel/Library/Panic.h>
#include <Kernel/Locking/WritableOnce.h>
#include <Kernel/Sections.h>
#include <Kernel/Tasks/FinalizerTask.h>
#include <Kernel/Tasks/PowerManagementTask.h>
#include <Kernel/Tasks/Process.h>
#include <Kernel/Tasks/Scheduler.h>
#include <Kernel/Tasks/WaitQueue.h>

namespace Kernel {

enum class PowerStateCommand {
    None,
    Shutdown,
    Reboot,
};

static constexpr StringView power_management_task_name = "Power Management Task"sv;
Thread* g_power_management_task_thread;
WritableOnce<PowerStateCommand> g_power_management_task_command { PowerStateCommand::None };
bool g_in_system_shutdown { false };
READONLY_AFTER_INIT WaitQueue* g_power_management_wait_queue;

void PowerManagementTask::task(void*)
{
    Thread::current()->set_priority(THREAD_PRIORITY_HIGH);
    while (!Process::current().is_dying()) {
        // The order of this if-else is important: We want to continue trying to finalize the threads in case
        // Thread::finalize_dying_threads set g_finalizer_has_work back to true due to OOM conditions
        auto command = g_power_management_task_command.get();
        if (command != PowerStateCommand::None) {
            switch (command) {
            case PowerStateCommand::Reboot: {
                perform_shutdown(DoReboot::Yes);
                VERIFY_NOT_REACHED();
            }
            case PowerStateCommand::Shutdown: {
                perform_shutdown(DoReboot::No);
                VERIFY_NOT_REACHED();
            }
            default:
                PANIC("Power Management Task: Unknown command sent to perform!");
            }
        } else
            g_power_management_wait_queue->wait_forever(power_management_task_name);
    }
    Process::current().sys$exit(0);
    VERIFY_NOT_REACHED();
}

void PowerManagementTask::shutdown()
{
    if (g_power_management_task_command.set(PowerStateCommand::Shutdown).is_error()) {
        dmesgln("Power Management Task: Already has a command to perform!");
        return;
    }
    g_power_management_wait_queue->wake_all();
}

void PowerManagementTask::reboot()
{
    if (g_power_management_task_command.set(PowerStateCommand::Reboot).is_error()) {
        dmesgln("Power Management Task: Already has a command to perform!");
        return;
    }
    g_power_management_wait_queue->wake_all();
}

UNMAP_AFTER_INIT void PowerManagementTask::spawn()
{
    g_power_management_wait_queue = new WaitQueue;
    VERIFY(g_power_management_task_thread == nullptr);
    auto [_, power_management_task_thread] = MUST(Process::create_kernel_process(
        power_management_task_name, PowerManagementTask::task, nullptr));
    g_power_management_task_thread = move(power_management_task_thread);
}

static void kill_all_user_processes()
{
    {
        SpinlockLocker lock(g_scheduler_lock);
        Process::all_instances().for_each([&](Process& process) {
            if (!process.is_kernel_process())
                process.die();
        });
    }

    // Although we *could* finalize processes ourselves (g_in_system_shutdown allows this),
    // we're nice citizens and let the finalizer task perform final duties before we kill it.
    Scheduler::notify_finalizer();
    int alive_process_count = 1;
    MonotonicTime last_status_time = TimeManagement::the().monotonic_time();
    while (alive_process_count > 0) {
        Scheduler::yield();
        alive_process_count = 0;
        Process::all_instances().for_each([&](Process& process) {
            if (!process.is_kernel_process() && !process.is_dead())
                alive_process_count++;
        });

        if (TimeManagement::the().monotonic_time() - last_status_time > Duration::from_seconds(2)) {
            last_status_time = TimeManagement::the().monotonic_time();
            dmesgln("Waiting on {} processes to exit...", alive_process_count);

            if constexpr (PROCESS_DEBUG) {
                Process::all_instances().for_each_const([&](Process const& process) {
                    if (!process.is_kernel_process() && !process.is_dead()) {
                        dbgln("Process (user) {:2} dead={} dying={} ({})",
                            process.pid(), process.is_dead(), process.is_dying(),
                            process.name().with([](auto& name) { return name.representable_view(); }));
                    }
                });
            }
        }
    }
}

void PowerManagementTask::perform_shutdown(DoReboot do_reboot)
{
    // We assume that by this point userland has tried as much as possible to shut down everything in an orderly fashion.
    // Therefore, we force kill remaining processes, including Kernel processes, except the finalizer and ourselves.
    dbgln("Killing remaining processes...");
    Optional<Process&> finalizer_process;
    Process::all_instances().for_each([&](Process& process) {
        if (process.pid() == g_finalizer->process().pid())
            finalizer_process = process;
    });
    VERIFY(finalizer_process.has_value());

    // Allow init process and finalizer task to be killed.
    g_in_system_shutdown = true;

    // Make sure to kill all user processes first, otherwise we might get weird hangups.
    kill_all_user_processes();

    size_t alive_process_count = 0;
    Process::all_instances().for_each([&](Process& process) {
        if (!process.is_kernel_process() && !process.is_dead())
            alive_process_count++;
    });
    // Don't panic here (since we may panic in a bit anyways) but report the probable cause of an unclean shutdown.
    if (alive_process_count != 0)
        dbgln("We're not the last process alive; proper shutdown may fail!");

    ConsoleManagement::the().switch_to_debug();

    dbgln("Locking all file systems...");
    FileSystem::lock_all();
    FileSystem::sync();

    dbgln("Unmounting all file systems...");

    if (auto result = VirtualFileSystem::the().unmount_all({}); result.is_error())
        dmesgln("Unmounting all file systems failed due to {}", result.release_error());

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
