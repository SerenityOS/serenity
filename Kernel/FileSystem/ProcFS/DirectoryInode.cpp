/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Spencer Dixon <spencercdixon@gmail.com>
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/FileSystem/ProcFS/DirectoryInode.h>

namespace Kernel {

ErrorOr<NonnullLockRefPtr<ProcFSDirectoryInode>> ProcFSDirectoryInode::try_create(ProcFS const& procfs, ProcFSExposedComponent const& component)
{
    return adopt_nonnull_lock_ref_or_enomem(new (nothrow) ProcFSDirectoryInode(procfs, component));
}

ProcFSDirectoryInode::ProcFSDirectoryInode(ProcFS const& fs, ProcFSExposedComponent const& component)
    : ProcFSGlobalInode(fs, component)
{
}

ProcFSDirectoryInode::~ProcFSDirectoryInode() = default;

InodeMetadata ProcFSDirectoryInode::metadata() const
{
    MutexLocker locker(m_inode_lock);
    InodeMetadata metadata;
    metadata.inode = { fsid(), m_associated_component->component_index() };
    metadata.mode = S_IFDIR | m_associated_component->required_mode();
    metadata.uid = m_associated_component->owner_user();
    metadata.gid = m_associated_component->owner_group();
    metadata.size = 0;
    metadata.mtime = m_associated_component->modified_time();
    return metadata;
}

ErrorOr<void> ProcFSDirectoryInode::traverse_as_directory(Function<ErrorOr<void>(FileSystem::DirectoryEntryView const&)> callback) const
{
    MutexLocker locker(procfs().m_lock);
    return m_associated_component->traverse_as_directory(procfs().fsid(), move(callback));
}

ErrorOr<NonnullLockRefPtr<Inode>> ProcFSDirectoryInode::lookup(StringView name)
{
    MutexLocker locker(procfs().m_lock);
    auto component = TRY(m_associated_component->lookup(name));
    return component->to_inode(procfs());
}

}
