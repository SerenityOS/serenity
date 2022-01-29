/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/JsonArraySerializer.h>
#include <AK/JsonObjectSerializer.h>
#include <AK/JsonValue.h>
#include <Kernel/Arch/x86/InterruptDisabler.h>
#include <Kernel/FileSystem/Custody.h>
#include <Kernel/FileSystem/ProcFS.h>
#include <Kernel/KBufferBuilder.h>
#include <Kernel/Memory/AnonymousVMObject.h>
#include <Kernel/Memory/MemoryManager.h>
#include <Kernel/Process.h>
#include <Kernel/ProcessExposed.h>
#include <Kernel/TTY/TTY.h>

namespace Kernel {

ErrorOr<void> Process::procfs_get_thread_stack(ThreadID thread_id, KBufferBuilder& builder) const
{
    JsonArraySerializer array { builder };
    auto thread = Thread::from_tid(thread_id);
    if (!thread)
        return ESRCH;
    bool show_kernel_addresses = Process::current().is_superuser();
    bool kernel_address_added = false;
    for (auto address : TRY(Processor::capture_stack_trace(*thread, 1024))) {
        if (!show_kernel_addresses && !Memory::is_user_address(VirtualAddress { address })) {
            if (kernel_address_added)
                continue;
            address = 0xdeadc0de;
            kernel_address_added = true;
        }
        array.add(address);
    }

    array.finish();
    return {};
}

ErrorOr<void> Process::traverse_stacks_directory(FileSystemID fsid, Function<ErrorOr<void>(FileSystem::DirectoryEntryView const&)> callback) const
{
    TRY(callback({ ".", { fsid, SegmentedProcFSIndex::build_segmented_index_for_main_property(pid(), SegmentedProcFSIndex::ProcessSubDirectory::Stacks, SegmentedProcFSIndex::MainProcessProperty::Reserved) }, 0 }));
    TRY(callback({ "..", { fsid, m_procfs_traits->component_index() }, 0 }));

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

ErrorOr<NonnullRefPtr<Inode>> Process::lookup_stacks_directory(const ProcFS& procfs, StringView name) const
{
    ErrorOr<NonnullRefPtr<ProcFSProcessPropertyInode>> thread_stack_inode { ENOENT };

    // FIXME: Try to exit the loop earlier
    for_each_thread([&](const Thread& thread) {
        int tid = thread.tid().value();
        VERIFY(!(tid < 0));
        if (name.to_int() == tid) {
            auto maybe_inode = ProcFSProcessPropertyInode::try_create_for_thread_stack(procfs, thread.tid(), pid());
            if (maybe_inode.is_error()) {
                thread_stack_inode = maybe_inode.release_error();
                return;
            }

            thread_stack_inode = maybe_inode.release_value();
        }
    });

    if (thread_stack_inode.is_error())
        return thread_stack_inode.release_error();
    return thread_stack_inode.release_value();
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
    TRY(callback({ ".", { fsid, m_procfs_traits->component_index() }, 0 }));
    TRY(callback({ "..", { fsid, m_procfs_traits->component_index() }, 0 }));
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

ErrorOr<NonnullRefPtr<Inode>> Process::lookup_file_descriptions_directory(const ProcFS& procfs, StringView name) const
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
    JsonObjectSerializer obj { builder };
#define __ENUMERATE_PLEDGE_PROMISE(x) \
    if (has_promised(Pledge::x)) {    \
        if (!builder.is_empty())      \
            builder.append(' ');      \
        builder.append(#x);           \
    }
    if (has_promises()) {
        StringBuilder builder;
        ENUMERATE_PLEDGE_PROMISES
        obj.add("promises", builder.build());
    }
#undef __ENUMERATE_PLEDGE_PROMISE
    obj.finish();
    return {};
}

ErrorOr<void> Process::procfs_get_unveil_stats(KBufferBuilder& builder) const
{
    JsonArraySerializer array { builder };
    for (auto const& unveiled_path : unveiled_paths()) {
        if (!unveiled_path.was_explicitly_unveiled())
            continue;
        auto obj = array.add_object();
        obj.add("path", unveiled_path.path());
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
        obj.add("permissions", permissions_builder.string_view());
    }
    array.finish();
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
    JsonArraySerializer array { builder };

    return fds().with_shared([&](auto& fds) -> ErrorOr<void> {
        if (fds.open_count() == 0) {
            array.finish();
            return {};
        }

        size_t count = 0;
        fds.enumerate([&](auto& file_description_metadata) {
            if (!file_description_metadata.is_valid()) {
                count++;
                return;
            }
            bool cloexec = file_description_metadata.flags() & FD_CLOEXEC;
            RefPtr<OpenFileDescription> description = file_description_metadata.description();
            auto description_object = array.add_object();
            description_object.add("fd", count);
            // TODO: Better OOM handling.
            auto pseudo_path_or_error = description->pseudo_path();
            description_object.add("absolute_path", pseudo_path_or_error.is_error() ? "???"sv : pseudo_path_or_error.value()->view());
            description_object.add("seekable", description->file().is_seekable());
            description_object.add("class", description->file().class_name());
            description_object.add("offset", description->offset());
            description_object.add("cloexec", cloexec);
            description_object.add("blocking", description->is_blocking());
            description_object.add("can_read", description->can_read());
            description_object.add("can_write", description->can_write());
            Inode* inode = description->inode();
            if (inode != nullptr) {
                auto inode_object = description_object.add_object("inode");
                inode_object.add("fsid", inode->fsid().value());
                inode_object.add("index", inode->index().value());
                inode_object.finish();
            }
            count++;
        });

        array.finish();
        return {};
    });
}

ErrorOr<void> Process::procfs_get_virtual_memory_stats(KBufferBuilder& builder) const
{
    JsonArraySerializer array { builder };
    {
        SpinlockLocker lock(address_space().get_lock());
        for (auto const& region : address_space().regions()) {
            if (!region->is_user() && !Process::current().is_superuser())
                continue;
            auto region_object = array.add_object();
            region_object.add("readable", region->is_readable());
            region_object.add("writable", region->is_writable());
            region_object.add("executable", region->is_executable());
            region_object.add("stack", region->is_stack());
            region_object.add("shared", region->is_shared());
            region_object.add("syscall", region->is_syscall_region());
            region_object.add("purgeable", region->vmobject().is_anonymous());
            if (region->vmobject().is_anonymous()) {
                region_object.add("volatile", static_cast<Memory::AnonymousVMObject const&>(region->vmobject()).is_volatile());
            }
            region_object.add("cacheable", region->is_cacheable());
            region_object.add("address", region->vaddr().get());
            region_object.add("size", region->size());
            region_object.add("amount_resident", region->amount_resident());
            region_object.add("amount_dirty", region->amount_dirty());
            region_object.add("cow_pages", region->cow_pages());
            region_object.add("name", region->name());
            region_object.add("vmobject", region->vmobject().class_name());

            StringBuilder pagemap_builder;
            for (size_t i = 0; i < region->page_count(); ++i) {
                auto const* page = region->physical_page(i);
                if (!page)
                    pagemap_builder.append('N');
                else if (page->is_shared_zero_page() || page->is_lazy_committed_page())
                    pagemap_builder.append('Z');
                else
                    pagemap_builder.append('P');
            }
            region_object.add("pagemap", pagemap_builder.string_view());
        }
    }
    array.finish();
    return {};
}

ErrorOr<void> Process::procfs_get_current_work_directory_link(KBufferBuilder& builder) const
{
    return builder.append(TRY(const_cast<Process&>(*this).current_directory().try_serialize_absolute_path())->view());
}

mode_t Process::binary_link_required_mode() const
{
    if (!executable())
        return 0;
    return m_procfs_traits->required_mode();
}

ErrorOr<void> Process::procfs_get_binary_link(KBufferBuilder& builder) const
{
    auto const* custody = executable();
    if (!custody)
        return Error::from_errno(ENOEXEC);
    return builder.append(TRY(custody->try_serialize_absolute_path())->view());
}

ErrorOr<void> Process::procfs_get_tty_link(KBufferBuilder& builder) const
{
    if (m_tty.is_null())
        return Error::from_errno(ENOENT);
    return builder.append(m_tty->tty_name().view());
}

}
