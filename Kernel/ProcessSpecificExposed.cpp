/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/JsonArraySerializer.h>
#include <AK/JsonObjectSerializer.h>
#include <AK/JsonValue.h>
#include <Kernel/FileSystem/Custody.h>
#include <Kernel/FileSystem/ProcFS.h>
#include <Kernel/InterruptDisabler.h>
#include <Kernel/KBufferBuilder.h>
#include <Kernel/Memory/AnonymousVMObject.h>
#include <Kernel/Memory/MemoryManager.h>
#include <Kernel/Process.h>
#include <Kernel/ProcessExposed.h>
#include <Kernel/TTY/TTY.h>

namespace Kernel {

ErrorOr<void> Process::procfs_get_thread_stack(ThreadID thread_id, KBufferBuilder& builder) const
{
    auto array = TRY(JsonArraySerializer<>::try_create(builder));
    auto thread = Thread::from_tid(thread_id);
    if (!thread)
        return ESRCH;
    auto current_process_credentials = Process::current().credentials();
    bool show_kernel_addresses = current_process_credentials->is_superuser();
    bool kernel_address_added = false;
    for (auto address : TRY(Processor::capture_stack_trace(*thread, 1024))) {
        if (!show_kernel_addresses && !Memory::is_user_address(VirtualAddress { address })) {
            if (kernel_address_added)
                continue;
            address = 0xdeadc0de;
            kernel_address_added = true;
        }
        TRY(array.add(address));
    }

    TRY(array.finish());
    return {};
}

ErrorOr<void> Process::traverse_stacks_directory(FileSystemID fsid, Function<ErrorOr<void>(FileSystem::DirectoryEntryView const&)> callback) const
{
    TRY(callback({ "."sv, { fsid, SegmentedProcFSIndex::build_segmented_index_for_main_property(pid(), SegmentedProcFSIndex::ProcessSubDirectory::Stacks, SegmentedProcFSIndex::MainProcessProperty::Reserved) }, 0 }));
    TRY(callback({ ".."sv, { fsid, m_procfs_traits->component_index() }, 0 }));

    return thread_list().with([&](auto& list) -> ErrorOr<void> {
        for (auto const& thread : list) {
            int tid = thread.tid().value();
            InodeIdentifier identifier = { fsid, SegmentedProcFSIndex::build_segmented_index_for_thread_stack(pid(), thread.tid()) };
            auto name = TRY(KString::number(tid));
            TRY(callback({ name->view(), identifier, 0 }));
        }
        return {};
    });
}

ErrorOr<NonnullLockRefPtr<Inode>> Process::lookup_stacks_directory(ProcFS const& procfs, StringView name) const
{
    auto maybe_needle = name.to_uint();
    if (!maybe_needle.has_value())
        return ENOENT;
    auto needle = maybe_needle.release_value();

    ErrorOr<NonnullLockRefPtr<ProcFSProcessPropertyInode>> thread_stack_inode { ENOENT };
    for_each_thread([&](Thread const& thread) {
        int tid = thread.tid().value();
        VERIFY(!(tid < 0));
        if (needle == (unsigned)tid) {
            thread_stack_inode = ProcFSProcessPropertyInode::try_create_for_thread_stack(procfs, thread.tid(), pid());
            return IterationDecision::Break;
        }
        return IterationDecision::Continue;
    });

    if (thread_stack_inode.is_error())
        return thread_stack_inode.release_error();
    return thread_stack_inode.release_value();
}

ErrorOr<void> Process::traverse_children_directory(FileSystemID fsid, Function<ErrorOr<void>(FileSystem::DirectoryEntryView const&)> callback) const
{
    TRY(callback({ "."sv, { fsid, SegmentedProcFSIndex::build_segmented_index_for_sub_directory(pid(), SegmentedProcFSIndex::ProcessSubDirectory::Children) }, 0 }));
    TRY(callback({ ".."sv, { fsid, m_procfs_traits->component_index() }, 0 }));
    return Process::all_instances().with([&](auto& processes) -> ErrorOr<void> {
        for (auto& process : processes) {
            if (process.ppid() == pid()) {
                StringBuilder builder;
                builder.appendff("{}", process.pid());
                TRY(callback({ builder.string_view(), { fsid, SegmentedProcFSIndex::build_segmented_index_for_children(pid(), process.pid()) }, DT_LNK }));
            }
        }
        return {};
    });
}

ErrorOr<NonnullLockRefPtr<Inode>> Process::lookup_children_directory(ProcFS const& procfs, StringView name) const
{
    auto maybe_pid = name.to_uint();
    if (!maybe_pid.has_value())
        return ENOENT;

    auto child_process = Process::from_pid(*maybe_pid);
    if (!child_process || child_process->ppid() != pid())
        return ENOENT;

    return TRY(ProcFSProcessPropertyInode::try_create_for_child_process_link(procfs, *maybe_pid, pid()));
}

ErrorOr<size_t> Process::procfs_get_child_proccess_link(ProcessID child_pid, KBufferBuilder& builder) const
{
    TRY(builder.appendff("/proc/{}", child_pid.value()));
    return builder.length();
}

ErrorOr<size_t> Process::procfs_get_file_description_link(unsigned fd, KBufferBuilder& builder) const
{
    auto file_description = TRY(open_file_description(fd));
    // Note: These links are not guaranteed to point to actual VFS paths, just like in other kernels.
    auto data = TRY(file_description->pseudo_path());
    TRY(builder.append(data->view()));
    return data->length();
}

ErrorOr<void> Process::traverse_file_descriptions_directory(FileSystemID fsid, Function<ErrorOr<void>(FileSystem::DirectoryEntryView const&)> callback) const
{
    TRY(callback({ "."sv, { fsid, m_procfs_traits->component_index() }, 0 }));
    TRY(callback({ ".."sv, { fsid, m_procfs_traits->component_index() }, 0 }));
    size_t count = 0;
    fds().with_shared([&](auto& fds) {
        fds.enumerate([&](auto& file_description_metadata) {
            if (!file_description_metadata.is_valid()) {
                count++;
                return;
            }
            StringBuilder builder;
            builder.appendff("{}", count);
            // FIXME: Propagate errors from callback.
            (void)callback({ builder.string_view(), { fsid, SegmentedProcFSIndex::build_segmented_index_for_file_description(pid(), count) }, DT_LNK });
            count++;
        });
    });
    return {};
}

ErrorOr<NonnullLockRefPtr<Inode>> Process::lookup_file_descriptions_directory(ProcFS const& procfs, StringView name) const
{
    auto maybe_index = name.to_uint();
    if (!maybe_index.has_value())
        return ENOENT;

    if (!m_fds.with_shared([&](auto& fds) { return fds.get_if_valid(*maybe_index); }))
        return ENOENT;

    return TRY(ProcFSProcessPropertyInode::try_create_for_file_description_link(procfs, *maybe_index, pid()));
}

ErrorOr<void> Process::procfs_get_pledge_stats(KBufferBuilder& builder) const
{
    auto obj = TRY(JsonObjectSerializer<>::try_create(builder));
#define __ENUMERATE_PLEDGE_PROMISE(x)              \
    if (has_promised(Pledge::x)) {                 \
        if (!promises_builder.is_empty())          \
            TRY(promises_builder.try_append(' ')); \
        TRY(promises_builder.try_append(#x##sv));  \
    }
    if (has_promises()) {
        StringBuilder promises_builder;
        ENUMERATE_PLEDGE_PROMISES
        TRY(obj.add("promises"sv, promises_builder.string_view()));
    }
#undef __ENUMERATE_PLEDGE_PROMISE
    TRY(obj.finish());
    return {};
}

ErrorOr<void> Process::procfs_get_unveil_stats(KBufferBuilder& builder) const
{
    auto array = TRY(JsonArraySerializer<>::try_create(builder));
    TRY(m_unveil_data.with([&](auto& unveil_data) -> ErrorOr<void> {
        TRY(unveil_data.paths.for_each_node_in_tree_order([&](auto const& unveiled_path) -> ErrorOr<IterationDecision> {
            if (!unveiled_path.was_explicitly_unveiled())
                return IterationDecision::Continue;
            auto obj = TRY(array.add_object());
            TRY(obj.add("path"sv, unveiled_path.path()));
            StringBuilder permissions_builder;
            if (unveiled_path.permissions() & UnveilAccess::Read)
                permissions_builder.append('r');
            if (unveiled_path.permissions() & UnveilAccess::Write)
                permissions_builder.append('w');
            if (unveiled_path.permissions() & UnveilAccess::Execute)
                permissions_builder.append('x');
            if (unveiled_path.permissions() & UnveilAccess::CreateOrRemove)
                permissions_builder.append('c');
            if (unveiled_path.permissions() & UnveilAccess::Browse)
                permissions_builder.append('b');
            TRY(obj.add("permissions"sv, permissions_builder.string_view()));
            TRY(obj.finish());
            return IterationDecision::Continue;
        }));
        return {};
    }));
    TRY(array.finish());
    return {};
}

ErrorOr<void> Process::procfs_get_perf_events(KBufferBuilder& builder) const
{
    InterruptDisabler disabler;
    if (!perf_events()) {
        dbgln("ProcFS: No perf events for {}", pid());
        return Error::from_errno(ENOBUFS);
    }
    return perf_events()->to_json(builder);
}

ErrorOr<void> Process::procfs_get_fds_stats(KBufferBuilder& builder) const
{
    auto array = TRY(JsonArraySerializer<>::try_create(builder));

    return fds().with_shared([&](auto& fds) -> ErrorOr<void> {
        if (fds.open_count() == 0) {
            TRY(array.finish());
            return {};
        }

        size_t count = 0;
        TRY(fds.try_enumerate([&](auto& file_description_metadata) -> ErrorOr<void> {
            if (!file_description_metadata.is_valid()) {
                count++;
                return {};
            }
            bool cloexec = file_description_metadata.flags() & FD_CLOEXEC;
            LockRefPtr<OpenFileDescription> description = file_description_metadata.description();
            auto description_object = TRY(array.add_object());
            TRY(description_object.add("fd"sv, count));
            // TODO: Better OOM handling.
            auto pseudo_path_or_error = description->pseudo_path();
            TRY(description_object.add("absolute_path"sv, pseudo_path_or_error.is_error() ? "???"sv : pseudo_path_or_error.value()->view()));
            TRY(description_object.add("seekable"sv, description->file().is_seekable()));
            TRY(description_object.add("class"sv, description->file().class_name()));
            TRY(description_object.add("offset"sv, description->offset()));
            TRY(description_object.add("cloexec"sv, cloexec));
            TRY(description_object.add("blocking"sv, description->is_blocking()));
            TRY(description_object.add("can_read"sv, description->can_read()));
            TRY(description_object.add("can_write"sv, description->can_write()));
            Inode* inode = description->inode();
            if (inode != nullptr) {
                auto inode_object = TRY(description_object.add_object("inode"sv));
                TRY(inode_object.add("fsid"sv, inode->fsid().value()));
                TRY(inode_object.add("index"sv, inode->index().value()));
                TRY(inode_object.finish());
            }
            TRY(description_object.finish());
            count++;
            return {};
        }));

        TRY(array.finish());
        return {};
    });
}

ErrorOr<void> Process::procfs_get_virtual_memory_stats(KBufferBuilder& builder) const
{
    auto array = TRY(JsonArraySerializer<>::try_create(builder));
    TRY(address_space().with([&](auto& space) -> ErrorOr<void> {
        for (auto const& region : space->region_tree().regions()) {
            auto current_process_credentials = Process::current().credentials();
            if (!region.is_user() && !current_process_credentials->is_superuser())
                continue;
            auto region_object = TRY(array.add_object());
            TRY(region_object.add("readable"sv, region.is_readable()));
            TRY(region_object.add("writable"sv, region.is_writable()));
            TRY(region_object.add("executable"sv, region.is_executable()));
            TRY(region_object.add("stack"sv, region.is_stack()));
            TRY(region_object.add("shared"sv, region.is_shared()));
            TRY(region_object.add("syscall"sv, region.is_syscall_region()));
            TRY(region_object.add("purgeable"sv, region.vmobject().is_anonymous()));
            if (region.vmobject().is_anonymous()) {
                TRY(region_object.add("volatile"sv, static_cast<Memory::AnonymousVMObject const&>(region.vmobject()).is_volatile()));
            }
            TRY(region_object.add("cacheable"sv, region.is_cacheable()));
            TRY(region_object.add("address"sv, region.vaddr().get()));
            TRY(region_object.add("size"sv, region.size()));
            TRY(region_object.add("amount_resident"sv, region.amount_resident()));
            TRY(region_object.add("amount_dirty"sv, region.amount_dirty()));
            TRY(region_object.add("cow_pages"sv, region.cow_pages()));
            TRY(region_object.add("name"sv, region.name()));
            TRY(region_object.add("vmobject"sv, region.vmobject().class_name()));

            StringBuilder pagemap_builder;
            for (size_t i = 0; i < region.page_count(); ++i) {
                auto page = region.physical_page(i);
                if (!page)
                    pagemap_builder.append('N');
                else if (page->is_shared_zero_page() || page->is_lazy_committed_page())
                    pagemap_builder.append('Z');
                else
                    pagemap_builder.append('P');
            }
            TRY(region_object.add("pagemap"sv, pagemap_builder.string_view()));
            TRY(region_object.finish());
        }
        return {};
    }));
    TRY(array.finish());
    return {};
}

ErrorOr<void> Process::procfs_get_current_work_directory_link(KBufferBuilder& builder) const
{
    return builder.append(TRY(const_cast<Process&>(*this).current_directory()->try_serialize_absolute_path())->view());
}

ErrorOr<void> Process::procfs_get_command_line(KBufferBuilder& builder) const
{
    auto array = TRY(JsonArraySerializer<>::try_create(builder));
    for (auto const& arg : arguments()) {
        TRY(array.add(arg.view()));
    }
    TRY(array.finish());
    return {};
}

mode_t Process::binary_link_required_mode() const
{
    if (!executable())
        return 0;
    return m_procfs_traits->required_mode();
}

ErrorOr<void> Process::procfs_get_binary_link(KBufferBuilder& builder) const
{
    auto custody = executable();
    if (!custody)
        return Error::from_errno(ENOEXEC);
    return builder.append(TRY(custody->try_serialize_absolute_path())->view());
}

}
