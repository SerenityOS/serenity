/*
 * Copyright (c) 2025, Tomás Simões <tomasprsimoes@tecnico.ulisboa.pt>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Debug.h>
#include <Kernel/Devices/BaseDevices.h>
#include <Kernel/Devices/Device.h>
#include <Kernel/Devices/Generic/NullDevice.h>
#include <Kernel/Devices/TTY/TTY.h>
#include <Kernel/FileSystem/Custody.h>
#include <Kernel/Memory/Region.h>
#include <Kernel/Tasks/PerformanceManager.h>
#include <Kernel/Tasks/Process.h>
#include <Kernel/Tasks/Scheduler.h>
#include <Kernel/Tasks/ScopedProcessList.h>

namespace Kernel {

// https://pubs.opengroup.org/onlinepubs/9799919799/functions/posix_spawn.html
ErrorOr<FlatPtr> Process::sys$posix_spawn(Userspace<Syscall::SC_posix_spawn_params const*> user_params)
{
    VERIFY_PROCESS_BIG_LOCK_ACQUIRED(this);
    TRY(require_promise(Pledge::proc));
    TRY(require_promise(Pledge::exec));

    auto params = TRY(copy_typed_from_user(user_params));

    if (params.arguments.length > ARG_MAX || params.environment.length > ARG_MAX)
        return E2BIG;
    if (params.arguments.length == 0)
        return EINVAL;

    if (params.attr_data.ptr() != 0 || params.attr_data_size != 0 || params.serialized_file_actions_data.ptr() != 0 || params.serialized_file_actions_data_size != 0) {
        // FIXME: Implement spawn attributes and spawn file actions handling.
        return ENOTSUP;
    }

    auto path = TRY(get_syscall_path_argument(params.path));

    auto copy_user_strings = [](auto const& list, auto& output) -> ErrorOr<void> {
        if (!list.length)
            return {};
        Checked<size_t> size = sizeof(*list.strings);
        size *= list.length;
        if (size.has_overflow())
            return EOVERFLOW;
        Vector<Syscall::StringArgument, 32> strings;
        TRY(strings.try_resize(list.length));
        TRY(copy_from_user(strings.data(), list.strings, size.value()));
        for (size_t i = 0; i < list.length; ++i) {
            auto string = TRY(try_copy_kstring_from_user(strings[i]));
            TRY(output.try_append(move(string)));
        }
        return {};
    };

    Vector<NonnullOwnPtr<KString>> arguments;
    TRY(copy_user_strings(params.arguments, arguments));

    Vector<NonnullOwnPtr<KString>> environment;
    TRY(copy_user_strings(params.environment, environment));

    auto credentials = this->credentials();
    auto [child, child_first_thread] = TRY(Process::create({}, credentials->uid(), credentials->gid(), pid(), m_is_kernel_process, vfs_root_context(), hostname_context(), current_directory(), nullptr, tty(), nullptr));

    ArmedScopeGuard thread_finalizer_guard = [&child_first_thread]() {
        SpinlockLocker lock(g_scheduler_lock);
        child_first_thread->detach();
        child_first_thread->set_state(Thread::State::Dying);
    };

    // "It is implementation-defined whether the fork handlers are run when posix_spawn() or posix_spawnp() is called."
    // We don't run them, as they are currently implemented in LibC.

    TRY(child->m_fds.with_exclusive([&](auto& child_fds) {
        return m_fds.with_exclusive([&](auto const& parent_fds) {
            return child_fds.try_clone(parent_fds);
        });

        // FD_CLOEXEC is handled by do_exec().
        // FIXME: Support FD_CLOFORK.
    }));

    // FIXME: "If file descriptor 0, 1, or 2 would otherwise be closed in the new process image created by posix_spawn() or posix_spawnp(),
    //         implementations may open an unspecified file for the file descriptor in the new process image."

    // Copy protected data which isn't set by do_exec().
    child->with_mutable_protected_data([&](auto& child_protected_data) {
        with_protected_data([&](auto const& parent_protected_data) {
            child_protected_data.umask = parent_protected_data.umask;
            child_protected_data.process_group = parent_protected_data.process_group;
            child_protected_data.credentials = parent_protected_data.credentials;
        });
    });

    dbgln_if(FORK_DEBUG, "posix_spawn: child={}", child);

    // A child created via posix_spawn inherits a copy of its parent's signal mask
    child_first_thread->update_signal_mask(Thread::current()->signal_mask());

    Thread* new_main_thread = nullptr;
    InterruptsState previous_interrupts_state = InterruptsState::Enabled;
    TRY(child->exec(move(path), move(arguments), move(environment), new_main_thread, previous_interrupts_state));
    thread_finalizer_guard.disarm();

    m_scoped_process_list.with([&](auto const& list_ptr) {
        if (list_ptr) {
            child->m_scoped_process_list.with([&](auto& child_list_ptr) {
                child_list_ptr = list_ptr;
            });
            list_ptr->attach(*child);
        }
    });

    Process::register_new(*child);

    // NOTE: All user processes have a leaked ref on them. It's balanced by Thread::WaitBlockerSet::finalize().
    child->ref();

    PerformanceManager::add_process_created_event(*child);

    SpinlockLocker lock(g_scheduler_lock);
    new_main_thread->set_affinity(Thread::current()->affinity());
    new_main_thread->set_state(Thread::State::Runnable);

    return child->pid().value();
}
}
