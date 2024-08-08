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
#include <Kernel/Arch/PowerState.h>
#include <Kernel/Devices/TTY/VirtualConsole.h>
#include <Kernel/FileSystem/FileSystem.h>
#include <Kernel/FileSystem/VirtualFileSystem.h>
#include <Kernel/Firmware/ACPI/Parser.h>
#include <Kernel/Library/Panic.h>
#include <Kernel/Sections.h>
#include <Kernel/Tasks/FinalizerTask.h>
#include <Kernel/Tasks/PowerStateSwitchTask.h>
#include <Kernel/Tasks/Process.h>
#include <Kernel/Tasks/Scheduler.h>

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
    Optional<Process&> finalizer_process;
    Process::all_instances().for_each([&](Process& process) {
        if (process.pid() == g_finalizer->process().pid())
            finalizer_process = process;
    });
    VERIFY(finalizer_process.has_value());

    // Allow init process and finalizer task to be killed.
    g_in_system_shutdown = true;

    // Make sure to kill all user processes first, otherwise we might get weird hangups.
    TRY(kill_all_user_processes());

    size_t alive_process_count = 0;
    Process::all_instances().for_each([&](Process& process) {
        if (!process.is_kernel_process() && !process.is_dead())
            alive_process_count++;
    });
    // Don't panic here (since we may panic in a bit anyways) but report the probable cause of an unclean shutdown.
    if (alive_process_count != 0)
        dbgln("We're not the last process alive; proper shutdown may fail!");

    VirtualConsole::switch_to_debug_console();

    dbgln("Syncing all file systems...");
    FileSystem::sync();

    dbgln("Unmounting all file systems...");

    size_t collected_contexts_count = 0;
    do {
        Array<RefPtr<VFSRootContext>, 16> contexts;
        VFSRootContext::all_root_contexts_list(Badge<PowerStateSwitchTask> {}).with([&collected_contexts_count, &contexts](auto& list) {
            size_t iteration_collect_count = min(contexts.size(), list.size_slow());
            for (collected_contexts_count = 0; collected_contexts_count < iteration_collect_count; collected_contexts_count++) {
                contexts[collected_contexts_count] = list.take_first();
            }
        });
        for (size_t index = 0; index < collected_contexts_count; index++) {
            VERIFY(contexts[index]);
            TRY(contexts[index]->do_full_teardown({}));
        }
    } while (collected_contexts_count > 0);

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

ErrorOr<void> PowerStateSwitchTask::kill_all_user_processes()
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

    return {};
}

}
