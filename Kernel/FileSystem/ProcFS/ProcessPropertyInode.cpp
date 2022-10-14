/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Spencer Dixon <spencercdixon@gmail.com>
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/API/POSIX/errno.h>
#include <Kernel/Debug.h>
#include <Kernel/FileSystem/OpenFileDescription.h>
#include <Kernel/FileSystem/ProcFS/ProcessPropertyInode.h>
#include <Kernel/Process.h>
#include <Kernel/ProcessExposed.h>

namespace Kernel {

ErrorOr<NonnullLockRefPtr<ProcFSProcessPropertyInode>> ProcFSProcessPropertyInode::try_create_for_file_description_link(ProcFS const& procfs, unsigned file_description_index, ProcessID pid)
{
    return adopt_nonnull_lock_ref_or_enomem(new (nothrow) ProcFSProcessPropertyInode(procfs, file_description_index, pid));
}

ErrorOr<NonnullLockRefPtr<ProcFSProcessPropertyInode>> ProcFSProcessPropertyInode::try_create_for_thread_stack(ProcFS const& procfs, ThreadID stack_thread_index, ProcessID pid)
{
    return adopt_nonnull_lock_ref_or_enomem(new (nothrow) ProcFSProcessPropertyInode(procfs, stack_thread_index, pid));
}

ErrorOr<NonnullLockRefPtr<ProcFSProcessPropertyInode>> ProcFSProcessPropertyInode::try_create_for_pid_property(ProcFS const& procfs, SegmentedProcFSIndex::MainProcessProperty main_property_type, ProcessID pid)
{
    return adopt_nonnull_lock_ref_or_enomem(new (nothrow) ProcFSProcessPropertyInode(procfs, main_property_type, pid));
}

ErrorOr<NonnullLockRefPtr<ProcFSProcessPropertyInode>> ProcFSProcessPropertyInode::try_create_for_child_process_link(ProcFS const& procfs, ProcessID child_pid, ProcessID pid)
{
    return adopt_nonnull_lock_ref_or_enomem(new (nothrow) ProcFSProcessPropertyInode(procfs, child_pid, pid));
}

ProcFSProcessPropertyInode::ProcFSProcessPropertyInode(ProcFS const& procfs, SegmentedProcFSIndex::MainProcessProperty main_property_type, ProcessID pid)
    : ProcFSProcessAssociatedInode(procfs, pid, SegmentedProcFSIndex::build_segmented_index_for_main_property_in_pid_directory(pid, main_property_type))
    , m_parent_sub_directory_type(SegmentedProcFSIndex::ProcessSubDirectory::Reserved)
{
    m_possible_data.property_type = main_property_type;
}

ProcFSProcessPropertyInode::ProcFSProcessPropertyInode(ProcFS const& procfs, unsigned file_description_index, ProcessID pid)
    : ProcFSProcessAssociatedInode(procfs, pid, SegmentedProcFSIndex::build_segmented_index_for_file_description(pid, file_description_index))
    , m_parent_sub_directory_type(SegmentedProcFSIndex::ProcessSubDirectory::OpenFileDescriptions)
{
    m_possible_data.property_index = file_description_index;
}

ProcFSProcessPropertyInode::ProcFSProcessPropertyInode(ProcFS const& procfs, ThreadID thread_stack_index, ProcessID pid)
    : ProcFSProcessAssociatedInode(procfs, pid, SegmentedProcFSIndex::build_segmented_index_for_thread_stack(pid, thread_stack_index))
    , m_parent_sub_directory_type(SegmentedProcFSIndex::ProcessSubDirectory::Stacks)
{
    m_possible_data.property_index = thread_stack_index.value();
}

ProcFSProcessPropertyInode::ProcFSProcessPropertyInode(ProcFS const& procfs, ProcessID child_pid, ProcessID pid)
    : ProcFSProcessAssociatedInode(procfs, pid, SegmentedProcFSIndex::build_segmented_index_for_children(pid, child_pid))
    , m_parent_sub_directory_type(SegmentedProcFSIndex::ProcessSubDirectory::Children)
{
    m_possible_data.property_index = child_pid.value();
}

ErrorOr<void> ProcFSProcessPropertyInode::attach(OpenFileDescription& description)
{
    return refresh_data(description);
}

void ProcFSProcessPropertyInode::did_seek(OpenFileDescription& description, off_t offset)
{
    if (offset != 0)
        return;
    (void)refresh_data(description);
}

static mode_t determine_procfs_process_inode_mode(SegmentedProcFSIndex::ProcessSubDirectory parent_sub_directory_type, SegmentedProcFSIndex::MainProcessProperty main_property)
{
    if (parent_sub_directory_type == SegmentedProcFSIndex::ProcessSubDirectory::OpenFileDescriptions)
        return S_IFLNK | 0400;
    if (parent_sub_directory_type == SegmentedProcFSIndex::ProcessSubDirectory::Stacks)
        return S_IFREG | 0400;
    if (parent_sub_directory_type == SegmentedProcFSIndex::ProcessSubDirectory::Children)
        return S_IFLNK | 0400;
    VERIFY(parent_sub_directory_type == SegmentedProcFSIndex::ProcessSubDirectory::Reserved);
    if (main_property == SegmentedProcFSIndex::MainProcessProperty::BinaryLink)
        return S_IFLNK | 0777;
    if (main_property == SegmentedProcFSIndex::MainProcessProperty::CurrentWorkDirectoryLink)
        return S_IFLNK | 0777;
    return S_IFREG | 0400;
}

InodeMetadata ProcFSProcessPropertyInode::metadata() const
{
    MutexLocker locker(m_inode_lock);
    auto process = Process::from_pid_in_same_jail(associated_pid());
    if (!process)
        return {};

    auto traits = process->procfs_traits();
    InodeMetadata metadata;
    metadata.inode = { fsid(), traits->component_index() };
    metadata.mode = determine_procfs_process_inode_mode(m_parent_sub_directory_type, m_possible_data.property_type);
    metadata.uid = traits->owner_user();
    metadata.gid = traits->owner_group();
    metadata.size = 0;
    metadata.mtime = traits->modified_time();
    return metadata;
}

ErrorOr<void> ProcFSProcessPropertyInode::traverse_as_directory(Function<ErrorOr<void>(FileSystem::DirectoryEntryView const&)>) const
{
    VERIFY_NOT_REACHED();
}

ErrorOr<size_t> ProcFSProcessPropertyInode::read_bytes_locked(off_t offset, size_t count, UserOrKernelBuffer& buffer, OpenFileDescription* description) const
{
    dbgln_if(PROCFS_DEBUG, "ProcFS ProcessInformation: read_bytes_locked offset: {} count: {}", offset, count);

    VERIFY(offset >= 0);
    VERIFY(buffer.user_or_kernel_ptr());

    if (!description) {
        auto builder = TRY(KBufferBuilder::try_create());
        auto process = Process::from_pid_in_same_jail(associated_pid());
        if (!process)
            return Error::from_errno(ESRCH);
        TRY(try_to_acquire_data(*process, builder));
        auto data_buffer = builder.build();
        if (!data_buffer)
            return Error::from_errno(EFAULT);
        ssize_t nread = min(static_cast<off_t>(data_buffer->size() - offset), static_cast<off_t>(count));
        TRY(buffer.write(data_buffer->data() + offset, nread));
        return nread;
    }
    if (!description->data()) {
        dbgln("ProcFS Process Information: Do not have cached data!");
        return Error::from_errno(EIO);
    }

    MutexLocker locker(m_refresh_lock);

    auto& typed_cached_data = static_cast<ProcFSInodeData&>(*description->data());
    auto& data_buffer = typed_cached_data.buffer;

    if (!data_buffer || (size_t)offset >= data_buffer->size())
        return 0;

    ssize_t nread = min(static_cast<off_t>(data_buffer->size() - offset), static_cast<off_t>(count));
    TRY(buffer.write(data_buffer->data() + offset, nread));

    return nread;
}

ErrorOr<NonnullLockRefPtr<Inode>> ProcFSProcessPropertyInode::lookup(StringView)
{
    return EINVAL;
}

static ErrorOr<void> build_from_cached_data(KBufferBuilder& builder, ProcFSInodeData& cached_data)
{
    cached_data.buffer = builder.build();
    if (!cached_data.buffer)
        return ENOMEM;
    return {};
}

ErrorOr<void> ProcFSProcessPropertyInode::try_to_acquire_data(Process& process, KBufferBuilder& builder) const
{
    // FIXME: Verify process is already ref-counted
    if (m_parent_sub_directory_type == SegmentedProcFSIndex::ProcessSubDirectory::OpenFileDescriptions) {
        TRY(process.procfs_get_file_description_link(m_possible_data.property_index, builder));
        return {};
    }
    if (m_parent_sub_directory_type == SegmentedProcFSIndex::ProcessSubDirectory::Stacks) {
        TRY(process.procfs_get_thread_stack(m_possible_data.property_index, builder));
        return {};
    }
    if (m_parent_sub_directory_type == SegmentedProcFSIndex::ProcessSubDirectory::Children) {
        TRY(process.procfs_get_child_proccess_link(m_possible_data.property_index, builder));
        return {};
    }

    VERIFY(m_parent_sub_directory_type == SegmentedProcFSIndex::ProcessSubDirectory::Reserved);
    switch (m_possible_data.property_type) {
    case SegmentedProcFSIndex::MainProcessProperty::Unveil:
        return process.procfs_get_unveil_stats(builder);
    case SegmentedProcFSIndex::MainProcessProperty::Pledge:
        return process.procfs_get_pledge_stats(builder);
    case SegmentedProcFSIndex::MainProcessProperty::OpenFileDescriptions:
        return process.procfs_get_fds_stats(builder);
    case SegmentedProcFSIndex::MainProcessProperty::BinaryLink:
        return process.procfs_get_binary_link(builder);
    case SegmentedProcFSIndex::MainProcessProperty::CurrentWorkDirectoryLink:
        return process.procfs_get_current_work_directory_link(builder);
    case SegmentedProcFSIndex::MainProcessProperty::PerformanceEvents:
        return process.procfs_get_perf_events(builder);
    case SegmentedProcFSIndex::MainProcessProperty::VirtualMemoryStats:
        return process.procfs_get_virtual_memory_stats(builder);
    case SegmentedProcFSIndex::MainProcessProperty::CommandLine:
        return process.procfs_get_command_line(builder);
    default:
        VERIFY_NOT_REACHED();
    }
}

ErrorOr<void> ProcFSProcessPropertyInode::refresh_data(OpenFileDescription& description)
{
    // For process-specific inodes, hold the process's ptrace lock across refresh
    // and refuse to load data if the process is not dumpable.
    // Without this, files opened before a process went non-dumpable could still be used for dumping.
    auto process = Process::from_pid_in_same_jail(associated_pid());
    if (!process)
        return Error::from_errno(ESRCH);
    process->ptrace_lock().lock();
    if (!process->is_dumpable()) {
        process->ptrace_lock().unlock();
        return EPERM;
    }
    ScopeGuard guard = [&] {
        process->ptrace_lock().unlock();
    };
    MutexLocker locker(m_refresh_lock);
    auto& cached_data = description.data();
    if (!cached_data) {
        cached_data = adopt_own_if_nonnull(new (nothrow) ProcFSInodeData);
        if (!cached_data)
            return ENOMEM;
    }
    auto builder = TRY(KBufferBuilder::try_create());
    TRY(try_to_acquire_data(*process, builder));
    return build_from_cached_data(builder, static_cast<ProcFSInodeData&>(*cached_data));
}

}
