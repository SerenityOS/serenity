/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/FileSystem/SysFS/LinkInode.h>
#include <Kernel/Time/TimeManagement.h>

namespace Kernel {

ErrorOr<NonnullRefPtr<SysFSLinkInode>> SysFSLinkInode::try_create(SysFS const& sysfs, SysFSComponent const& component)
{
    return adopt_nonnull_ref_or_enomem(new (nothrow) SysFSLinkInode(sysfs, component));
}

SysFSLinkInode::SysFSLinkInode(SysFS const& fs, SysFSComponent const& component)
    : SysFSInode(fs, component)
{
}

SysFSLinkInode::~SysFSLinkInode() = default;

InodeMetadata SysFSLinkInode::metadata() const
{
    // NOTE: No locking required as m_associated_component or its component index will never change during our lifetime.
    InodeMetadata metadata;
    metadata.inode = { fsid(), m_associated_component->component_index() };
    metadata.mode = S_IFLNK | S_IRUSR | S_IRGRP | S_IROTH | S_IXOTH;
    metadata.uid = 0;
    metadata.gid = 0;
    metadata.size = 0;
    metadata.mtime = TimeManagement::boot_time();
    return metadata;
}

}
