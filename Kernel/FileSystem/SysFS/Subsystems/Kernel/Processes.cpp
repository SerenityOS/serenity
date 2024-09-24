/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/JsonObjectSerializer.h>
#include <AK/Try.h>
#include <Kernel/Devices/TTY/TTY.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Kernel/Processes.h>
#include <Kernel/Sections.h>
#include <Kernel/Tasks/Process.h>
#include <Kernel/Tasks/Scheduler.h>

namespace Kernel {

UNMAP_AFTER_INIT SysFSOverallProcesses::SysFSOverallProcesses(SysFSDirectory const& parent_directory)
    : SysFSGlobalInformation(parent_directory)
{
}

UNMAP_AFTER_INIT NonnullRefPtr<SysFSOverallProcesses> SysFSOverallProcesses::must_create(SysFSDirectory const& parent_directory)
{
    return adopt_ref_if_nonnull(new (nothrow) SysFSOverallProcesses(parent_directory)).release_nonnull();
}

ErrorOr<void> SysFSOverallProcesses::try_generate(KBufferBuilder& builder)
{
    auto json = TRY(JsonObjectSerializer<>::try_create(builder));

    // Keep this in sync with CProcessStatistics.
    auto build_process = [&](JsonArraySerializer<KBufferBuilder>& array, Process const& process) -> ErrorOr<void> {
        auto process_object = TRY(array.add_object());

        if (process.is_user_process()) {
            StringBuilder pledge_builder;

#define __ENUMERATE_PLEDGE_PROMISE(promise)    \
    if (process.has_promised(Pledge::promise)) \
        TRY(pledge_builder.try_append(#promise " "sv));
            ENUMERATE_PLEDGE_PROMISES
#undef __ENUMERATE_PLEDGE_PROMISE

            TRY(process_object.add("pledge"sv, pledge_builder.string_view()));

            switch (process.veil_state()) {
            case VeilState::None:
                TRY(process_object.add("veil"sv, "None"));
                break;
            case VeilState::Dropped:
                TRY(process_object.add("veil"sv, "Dropped"));
                break;
            case VeilState::Locked:
                TRY(process_object.add("veil"sv, "Locked"));
                break;
            case VeilState::LockedInherited:
                // Note: We don't reveal if the locked state is either by our choice
                // or someone else applied it.
                TRY(process_object.add("veil"sv, "Locked"));
                break;
            }
        } else {
            TRY(process_object.add("pledge"sv, ""sv));
            TRY(process_object.add("veil"sv, ""sv));
        }

        TRY(process_object.add("pid"sv, process.pid().value()));
        ProcessGroupID tty_pgid = 0;
        if (auto tty = process.tty())
            tty_pgid = tty->pgid();
        TRY(process_object.add("pgid"sv, tty_pgid.value()));
        TRY(process_object.add("pgp"sv, process.pgid().value()));
        TRY(process_object.add("sid"sv, process.sid().value()));
        auto credentials = process.credentials();
        TRY(process_object.add("uid"sv, credentials->uid().value()));
        TRY(process_object.add("gid"sv, credentials->gid().value()));
        TRY(process_object.add("ppid"sv, process.ppid().value()));
        if (process.tty()) {
            auto tty_pseudo_name = TRY(process.tty()->pseudo_name());
            TRY(process_object.add("tty"sv, tty_pseudo_name->view()));
        } else {
            TRY(process_object.add("tty"sv, ""));
        }
        TRY(process.name().with([&](auto& process_name) { return process_object.add("name"sv, process_name.representable_view()); }));
        TRY(process_object.add("executable"sv, process.executable() ? TRY(process.executable()->try_serialize_absolute_path())->view() : ""sv));
        TRY(process_object.add("creation_time"sv, process.creation_time().nanoseconds_since_epoch()));

        size_t amount_virtual = 0;
        size_t amount_resident = 0;
        size_t amount_dirty_private = 0;
        size_t amount_clean_inode = 0;
        size_t amount_shared = 0;
        size_t amount_purgeable_volatile = 0;
        size_t amount_purgeable_nonvolatile = 0;

        TRY(process.address_space().with([&](auto& space) -> ErrorOr<void> {
            amount_virtual = space->amount_virtual();
            amount_resident = space->amount_resident();
            amount_dirty_private = space->amount_dirty_private();
            amount_clean_inode = TRY(space->amount_clean_inode());
            amount_shared = space->amount_shared();
            amount_purgeable_volatile = space->amount_purgeable_volatile();
            amount_purgeable_nonvolatile = space->amount_purgeable_nonvolatile();
            return {};
        }));

        TRY(process_object.add("amount_virtual"sv, amount_virtual));
        TRY(process_object.add("amount_resident"sv, amount_resident));
        TRY(process_object.add("amount_dirty_private"sv, amount_dirty_private));
        TRY(process_object.add("amount_clean_inode"sv, amount_clean_inode));
        TRY(process_object.add("amount_shared"sv, amount_shared));
        TRY(process_object.add("amount_purgeable_volatile"sv, amount_purgeable_volatile));
        TRY(process_object.add("amount_purgeable_nonvolatile"sv, amount_purgeable_nonvolatile));
        TRY(process_object.add("dumpable"sv, process.is_dumpable()));
        TRY(process_object.add("kernel"sv, process.is_kernel_process()));
        auto thread_array = TRY(process_object.add_array("threads"sv));
        TRY(process.try_for_each_thread([&](Thread const& thread) -> ErrorOr<void> {
            SpinlockLocker locker(thread.get_lock());
            auto thread_object = TRY(thread_array.add_object());
#if LOCK_DEBUG
            TRY(thread_object.add("lock_count"sv, thread.lock_count()));
#endif
            TRY(thread_object.add("tid"sv, thread.tid().value()));
            TRY(thread.name().with([&](auto& thread_name) { return thread_object.add("name"sv, thread_name.representable_view()); }));
            TRY(thread_object.add("times_scheduled"sv, thread.times_scheduled()));
            TRY(thread_object.add("time_user"sv, thread.time_in_user()));
            TRY(thread_object.add("time_kernel"sv, thread.time_in_kernel()));
            TRY(thread_object.add("state"sv, thread.state_string()));
            TRY(thread_object.add("cpu"sv, thread.cpu()));
            TRY(thread_object.add("priority"sv, thread.priority()));
            TRY(thread_object.add("syscall_count"sv, thread.syscall_count()));
            TRY(thread_object.add("inode_faults"sv, thread.inode_faults()));
            TRY(thread_object.add("zero_faults"sv, thread.zero_faults()));
            TRY(thread_object.add("cow_faults"sv, thread.cow_faults()));
            TRY(thread_object.add("file_read_bytes"sv, thread.file_read_bytes()));
            TRY(thread_object.add("file_write_bytes"sv, thread.file_write_bytes()));
            TRY(thread_object.add("unix_socket_read_bytes"sv, thread.unix_socket_read_bytes()));
            TRY(thread_object.add("unix_socket_write_bytes"sv, thread.unix_socket_write_bytes()));
            TRY(thread_object.add("ipv4_socket_read_bytes"sv, thread.ipv4_socket_read_bytes()));
            TRY(thread_object.add("ipv4_socket_write_bytes"sv, thread.ipv4_socket_write_bytes()));

            TRY(thread_object.finish());
            return {};
        }));
        TRY(thread_array.finish());
        TRY(process_object.finish());
        return {};
    };

    {
        auto array = TRY(json.add_array("processes"sv));
        if (!Process::current().is_jailed())
            TRY(build_process(array, *Scheduler::colonel()));
        TRY(Process::for_each_in_same_process_list([&](Process& process) -> ErrorOr<void> {
            TRY(build_process(array, process));
            return {};
        }));
        TRY(array.finish());
    }

    auto total_time_scheduled = Scheduler::get_total_time_scheduled();
    TRY(json.add("total_time"sv, total_time_scheduled.total));
    TRY(json.add("total_time_kernel"sv, total_time_scheduled.total_kernel));
    TRY(json.finish());
    return {};
}

}
