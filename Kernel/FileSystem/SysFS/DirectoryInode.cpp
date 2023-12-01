/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StringView.h>
#include <Kernel/FileSystem/SysFS/DirectoryInode.h>
#include <Kernel/Sections.h>
#include <Kernel/Time/TimeManagement.h>

namespace Kernel {

ErrorOr<NonnullRefPtr<SysFSDirectoryInode>> SysFSDirectoryInode::try_create(SysFS const& sysfs, SysFSComponent const& component)
{
    return adopt_nonnull_ref_or_enomem(new (nothrow) SysFSDirectoryInode(sysfs, component));
}

SysFSDirectoryInode::SysFSDirectoryInode(SysFS const& fs, SysFSComponent const& component)
    : SysFSInode(fs, component)
{
}

SysFSDirectoryInode::~SysFSDirectoryInode() = default;

InodeMetadata SysFSDirectoryInode::metadata() const
{
    // NOTE: No locking required as m_associated_component or its component index will never change during our lifetime.
    InodeMetadata metadata;
    metadata.inode = { fsid(), m_associated_component->component_index() };
    metadata.mode = S_IFDIR | 0755;
    metadata.uid = 0;
    metadata.gid = 0;
    metadata.size = 0;
    metadata.mtime = TimeManagement::boot_time();
    return metadata;
}

ErrorOr<void> SysFSDirectoryInode::traverse_as_directory(Function<ErrorOr<void>(FileSystem::DirectoryEntryView const&)> callback) const
{
    return m_associated_component->traverse_as_directory(fs().fsid(), move(callback));
}

ErrorOr<NonnullRefPtr<Inode>> SysFSDirectoryInode::lookup(StringView name)
{
    auto component = m_associated_component->lookup(name);
    if (!component)
        return ENOENT;
    return TRY(component->to_inode(fs()));
}

}
