/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Spencer Dixon <spencercdixon@gmail.com>
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/FileSystem/ProcFS/LinkInode.h>

namespace Kernel {

ErrorOr<NonnullLockRefPtr<ProcFSLinkInode>> ProcFSLinkInode::try_create(ProcFS const& procfs, ProcFSExposedComponent const& component)
{
    return adopt_nonnull_lock_ref_or_enomem(new (nothrow) ProcFSLinkInode(procfs, component));
}

ProcFSLinkInode::ProcFSLinkInode(ProcFS const& fs, ProcFSExposedComponent const& component)
    : ProcFSGlobalInode(fs, component)
{
}

InodeMetadata ProcFSLinkInode::metadata() const
{
    MutexLocker locker(m_inode_lock);
    InodeMetadata metadata;
    metadata.inode = { fsid(), m_associated_component->component_index() };
    metadata.mode = S_IFLNK | m_associated_component->required_mode();
    metadata.uid = m_associated_component->owner_user();
    metadata.gid = m_associated_component->owner_group();
    metadata.size = 0;
    metadata.mtime = m_associated_component->modified_time();
    return metadata;
}

}
