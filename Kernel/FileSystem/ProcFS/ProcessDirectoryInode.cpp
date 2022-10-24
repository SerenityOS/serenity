/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Spencer Dixon <spencercdixon@gmail.com>
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/FileSystem/ProcFS/ProcessDirectoryInode.h>
#include <Kernel/FileSystem/ProcFS/ProcessPropertyInode.h>
#include <Kernel/FileSystem/ProcFS/ProcessSubDirectoryInode.h>
#include <Kernel/Process.h>
#include <Kernel/ProcessExposed.h>

namespace Kernel {

ErrorOr<NonnullLockRefPtr<ProcFSProcessDirectoryInode>> ProcFSProcessDirectoryInode::try_create(ProcFS const& procfs, ProcessID pid)
{
    return adopt_nonnull_lock_ref_or_enomem(new (nothrow) ProcFSProcessDirectoryInode(procfs, pid));
}

ProcFSProcessDirectoryInode::ProcFSProcessDirectoryInode(ProcFS const& procfs, ProcessID pid)
    : ProcFSProcessAssociatedInode(procfs, pid, SegmentedProcFSIndex::build_segmented_index_for_pid_directory(pid))
{
}

ErrorOr<void> ProcFSProcessDirectoryInode::attach(OpenFileDescription&)
{
    return {};
}

InodeMetadata ProcFSProcessDirectoryInode::metadata() const
{
    MutexLocker locker(m_inode_lock);
    auto process = Process::from_pid_in_same_jail(associated_pid());
    if (!process)
        return {};

    auto traits = process->procfs_traits();
    InodeMetadata metadata;
    metadata.inode = { fsid(), traits->component_index() };
    metadata.mode = S_IFDIR | traits->required_mode();
    metadata.uid = traits->owner_user();
    metadata.gid = traits->owner_group();
    metadata.size = 0;
    metadata.mtime = traits->modified_time();
    return metadata;
}

ErrorOr<size_t> ProcFSProcessDirectoryInode::read_bytes_locked(off_t, size_t, UserOrKernelBuffer&, OpenFileDescription*) const
{
    VERIFY_NOT_REACHED();
}

ErrorOr<void> ProcFSProcessDirectoryInode::traverse_as_directory(Function<ErrorOr<void>(FileSystem::DirectoryEntryView const&)> callback) const
{
    MutexLocker locker(procfs().m_lock);
    auto process = Process::from_pid_in_same_jail(associated_pid());
    if (!process)
        return EINVAL;
    return process->procfs_traits()->traverse_as_directory(procfs().fsid(), move(callback));
}

ErrorOr<NonnullLockRefPtr<Inode>> ProcFSProcessDirectoryInode::lookup(StringView name)
{
    MutexLocker locker(procfs().m_lock);
    auto process = Process::from_pid_in_same_jail(associated_pid());
    if (!process)
        return ESRCH;
    if (name == "fd"sv)
        return TRY(ProcFSProcessSubDirectoryInode::try_create(procfs(), SegmentedProcFSIndex::ProcessSubDirectory::OpenFileDescriptions, associated_pid()));
    if (name == "stacks"sv)
        return TRY(ProcFSProcessSubDirectoryInode::try_create(procfs(), SegmentedProcFSIndex::ProcessSubDirectory::Stacks, associated_pid()));
    if (name == "children"sv)
        return TRY(ProcFSProcessSubDirectoryInode::try_create(procfs(), SegmentedProcFSIndex::ProcessSubDirectory::Children, associated_pid()));
    if (name == "unveil"sv)
        return TRY(ProcFSProcessPropertyInode::try_create_for_pid_property(procfs(), SegmentedProcFSIndex::MainProcessProperty::Unveil, associated_pid()));
    if (name == "pledge"sv)
        return TRY(ProcFSProcessPropertyInode::try_create_for_pid_property(procfs(), SegmentedProcFSIndex::MainProcessProperty::Pledge, associated_pid()));
    if (name == "fds"sv)
        return TRY(ProcFSProcessPropertyInode::try_create_for_pid_property(procfs(), SegmentedProcFSIndex::MainProcessProperty::OpenFileDescriptions, associated_pid()));
    if (name == "exe"sv)
        return TRY(ProcFSProcessPropertyInode::try_create_for_pid_property(procfs(), SegmentedProcFSIndex::MainProcessProperty::BinaryLink, associated_pid()));
    if (name == "cwd"sv)
        return TRY(ProcFSProcessPropertyInode::try_create_for_pid_property(procfs(), SegmentedProcFSIndex::MainProcessProperty::CurrentWorkDirectoryLink, associated_pid()));
    if (name == "perf_events"sv)
        return TRY(ProcFSProcessPropertyInode::try_create_for_pid_property(procfs(), SegmentedProcFSIndex::MainProcessProperty::PerformanceEvents, associated_pid()));
    if (name == "vm"sv)
        return TRY(ProcFSProcessPropertyInode::try_create_for_pid_property(procfs(), SegmentedProcFSIndex::MainProcessProperty::VirtualMemoryStats, associated_pid()));
    if (name == "cmdline"sv)
        return TRY(ProcFSProcessPropertyInode::try_create_for_pid_property(procfs(), SegmentedProcFSIndex::MainProcessProperty::CommandLine, associated_pid()));
    return ENOENT;
}

}
