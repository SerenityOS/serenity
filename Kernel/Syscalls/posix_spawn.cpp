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
ErrorOr<FlatPtr> Process::sys$posix_spawn(Userspace<Syscall::SC_posix_spawn_params const*> user_params)
{
    VERIFY_PROCESS_BIG_LOCK_ACQUIRED(this);
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
    Process::Name name {};
    Process::current().name().with([&name](auto& process_name) {
        name.store_characters(process_name.representable_view());
    });
    auto [child, child_first_thread] = TRY(Process::create(name.representable_view(), credentials->uid(), credentials->gid(), pid(), m_is_kernel_process, vfs_root_context(), hostname_context(), current_directory(), nullptr, tty(), nullptr));

    ArmedScopeGuard thread_finalizer_guard = [&child_first_thread]() {
        SpinlockLocker lock(g_scheduler_lock);
        child_first_thread->detach();
        child_first_thread->set_state(Thread::State::Dying);
    };

    // Since we aren't copying parent's file descriptor table, set it up like create init process does
    TRY(child->m_fds.with_exclusive([&](auto& fds) -> ErrorOr<void> {
        TRY(fds.try_resize(Process::OpenFileDescriptions::max_open()));

        // NOTE: If Device::base_devices() is returning nullptr, it means the null device is not attached which is a bug.
        VERIFY(Device::base_devices() != nullptr);
        auto& device_to_use_as_tty = tty() ? static_cast<CharacterDevice&>(*tty()) : Device::base_devices()->null_device;
        auto description = TRY(device_to_use_as_tty.open(O_RDWR));
        auto setup_description = [&](int fd) {
            fds.m_fds_metadatas[fd].allocate();
            fds[fd].set(*description);
        };
        setup_description(0);
        setup_description(1);
        setup_description(2);

        return {};
    }));

    // Copy protected data
    with_protected_data([&](auto& my_protected_data) {
        child->with_mutable_protected_data([&](auto& child_protected_data) {
            child_protected_data.umask = my_protected_data.umask;
            child_protected_data.process_group = my_protected_data.process_group;
        });
    });

    dbgln_if(FORK_DEBUG, "posix_spawn: child={}", child);

    // A child created via posix_spawn inherits a copy of its parent's signal mask
    child_first_thread->update_signal_mask(Thread::current()->signal_mask());

    m_scoped_process_list.with([&](auto& list_ptr) {
        if (list_ptr) {
            child->m_scoped_process_list.with([&](auto& child_list_ptr) {
                child_list_ptr = list_ptr;
            });
            list_ptr->attach(*child);
        }
    });

    Thread* new_main_thread = nullptr;
    InterruptsState previous_interrupts_state = InterruptsState::Enabled;
    TRY(child->exec(move(path), move(arguments), move(environment), new_main_thread, previous_interrupts_state));
    thread_finalizer_guard.disarm();

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
