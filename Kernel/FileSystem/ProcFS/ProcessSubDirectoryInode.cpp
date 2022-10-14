/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Spencer Dixon <spencercdixon@gmail.com>
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/FileSystem/ProcFS/ProcessSubDirectoryInode.h>
#include <Kernel/Process.h>
#include <Kernel/ProcessExposed.h>

namespace Kernel {

ErrorOr<NonnullLockRefPtr<ProcFSProcessSubDirectoryInode>> ProcFSProcessSubDirectoryInode::try_create(ProcFS const& procfs, SegmentedProcFSIndex::ProcessSubDirectory sub_directory_type, ProcessID pid)
{
    return adopt_nonnull_lock_ref_or_enomem(new (nothrow) ProcFSProcessSubDirectoryInode(procfs, sub_directory_type, pid));
}

ProcFSProcessSubDirectoryInode::ProcFSProcessSubDirectoryInode(ProcFS const& procfs, SegmentedProcFSIndex::ProcessSubDirectory sub_directory_type, ProcessID pid)
    : ProcFSProcessAssociatedInode(procfs, pid, SegmentedProcFSIndex::build_segmented_index_for_sub_directory(pid, sub_directory_type))
    , m_sub_directory_type(sub_directory_type)
{
}

ErrorOr<size_t> ProcFSProcessSubDirectoryInode::read_bytes_locked(off_t, size_t, UserOrKernelBuffer&, OpenFileDescription*) const
{
    VERIFY_NOT_REACHED();
}

ErrorOr<void> ProcFSProcessSubDirectoryInode::attach(OpenFileDescription&)
{
    return {};
}

void ProcFSProcessSubDirectoryInode::did_seek(OpenFileDescription&, off_t)
{
    VERIFY_NOT_REACHED();
}

InodeMetadata ProcFSProcessSubDirectoryInode::metadata() const
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

ErrorOr<void> ProcFSProcessSubDirectoryInode::traverse_as_directory(Function<ErrorOr<void>(FileSystem::DirectoryEntryView const&)> callback) const
{
    MutexLocker locker(procfs().m_lock);
    auto process = Process::from_pid_in_same_jail(associated_pid());
    if (!process)
        return EINVAL;
    switch (m_sub_directory_type) {
    case SegmentedProcFSIndex::ProcessSubDirectory::OpenFileDescriptions:
        return process->traverse_file_descriptions_directory(procfs().fsid(), move(callback));
    case SegmentedProcFSIndex::ProcessSubDirectory::Stacks:
        return process->traverse_stacks_directory(procfs().fsid(), move(callback));
    case SegmentedProcFSIndex::ProcessSubDirectory::Children:
        return process->traverse_children_directory(procfs().fsid(), move(callback));
    default:
        VERIFY_NOT_REACHED();
    }
    VERIFY_NOT_REACHED();
}

ErrorOr<NonnullLockRefPtr<Inode>> ProcFSProcessSubDirectoryInode::lookup(StringView name)
{
    MutexLocker locker(procfs().m_lock);
    auto process = Process::from_pid_in_same_jail(associated_pid());
    if (!process)
        return ESRCH;
    switch (m_sub_directory_type) {
    case SegmentedProcFSIndex::ProcessSubDirectory::OpenFileDescriptions:
        return process->lookup_file_descriptions_directory(procfs(), name);
    case SegmentedProcFSIndex::ProcessSubDirectory::Stacks:
        return process->lookup_stacks_directory(procfs(), name);
    case SegmentedProcFSIndex::ProcessSubDirectory::Children:
        return process->lookup_children_directory(procfs(), name);
    default:
        VERIFY_NOT_REACHED();
    }
}

}
