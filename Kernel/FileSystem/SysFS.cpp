/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StringView.h>
#include <Kernel/FileSystem/SysFS.h>
#include <Kernel/FileSystem/SysFS/Registry.h>
#include <Kernel/Sections.h>
#include <Kernel/Time/TimeManagement.h>

namespace Kernel {

ErrorOr<NonnullLockRefPtr<FileSystem>> SysFS::try_create()
{
    return TRY(adopt_nonnull_lock_ref_or_enomem(new (nothrow) SysFS));
}

SysFS::SysFS() = default;
SysFS::~SysFS() = default;

ErrorOr<void> SysFS::initialize()
{
    m_root_inode = TRY(SysFSComponentRegistry::the().root_directory().to_inode(*this));
    return {};
}

Inode& SysFS::root_inode()
{
    return *m_root_inode;
}

ErrorOr<NonnullLockRefPtr<SysFSInode>> SysFSInode::try_create(SysFS const& fs, SysFSComponent const& component)
{
    return adopt_nonnull_lock_ref_or_enomem(new (nothrow) SysFSInode(fs, component));
}

SysFSInode::SysFSInode(SysFS const& fs, SysFSComponent const& component)
    : Inode(const_cast<SysFS&>(fs), component.component_index())
    , m_associated_component(component)
{
}

void SysFSInode::did_seek(OpenFileDescription& description, off_t new_offset)
{
    if (new_offset != 0)
        return;
    auto result = m_associated_component->refresh_data(description);
    if (result.is_error()) {
        // Subsequent calls to read will return EIO!
        dbgln("SysFS: Could not refresh contents: {}", result.error());
    }
}

ErrorOr<void> SysFSInode::attach(OpenFileDescription& description)
{
    return m_associated_component->refresh_data(description);
}

ErrorOr<size_t> SysFSInode::read_bytes(off_t offset, size_t count, UserOrKernelBuffer& buffer, OpenFileDescription* fd) const
{
    return m_associated_component->read_bytes(offset, count, buffer, fd);
}

ErrorOr<void> SysFSInode::traverse_as_directory(Function<ErrorOr<void>(FileSystem::DirectoryEntryView const&)>) const
{
    VERIFY_NOT_REACHED();
}

ErrorOr<NonnullLockRefPtr<Inode>> SysFSInode::lookup(StringView)
{
    VERIFY_NOT_REACHED();
}

InodeMetadata SysFSInode::metadata() const
{
    // NOTE: No locking required as m_associated_component or its component index will never change during our lifetime.
    InodeMetadata metadata;
    metadata.inode = { fsid(), m_associated_component->component_index() };
    metadata.mode = S_IFREG | m_associated_component->permissions();
    metadata.uid = 0;
    metadata.gid = 0;
    metadata.size = m_associated_component->size();
    metadata.mtime = TimeManagement::boot_time();
    return metadata;
}

ErrorOr<void> SysFSInode::flush_metadata()
{
    return {};
}

ErrorOr<size_t> SysFSInode::write_bytes(off_t offset, size_t count, UserOrKernelBuffer const& buffer, OpenFileDescription* fd)
{
    return m_associated_component->write_bytes(offset, count, buffer, fd);
}

ErrorOr<NonnullLockRefPtr<Inode>> SysFSInode::create_child(StringView, mode_t, dev_t, UserID, GroupID)
{
    return EROFS;
}

ErrorOr<void> SysFSInode::add_child(Inode&, StringView, mode_t)
{
    return EROFS;
}

ErrorOr<void> SysFSInode::remove_child(StringView)
{
    return EROFS;
}

ErrorOr<void> SysFSInode::chmod(mode_t)
{
    return EPERM;
}

ErrorOr<void> SysFSInode::chown(UserID, GroupID)
{
    return EPERM;
}

ErrorOr<void> SysFSInode::set_mtime(time_t time)
{
    return m_associated_component->set_mtime(time);
}

ErrorOr<void> SysFSInode::truncate(u64 size)
{
    return m_associated_component->truncate(size);
}

ErrorOr<NonnullLockRefPtr<SysFSLinkInode>> SysFSLinkInode::try_create(SysFS const& sysfs, SysFSComponent const& component)
{
    return adopt_nonnull_lock_ref_or_enomem(new (nothrow) SysFSLinkInode(sysfs, component));
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

ErrorOr<NonnullLockRefPtr<SysFSDirectoryInode>> SysFSDirectoryInode::try_create(SysFS const& sysfs, SysFSComponent const& component)
{
    return adopt_nonnull_lock_ref_or_enomem(new (nothrow) SysFSDirectoryInode(sysfs, component));
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
    metadata.mode = S_IFDIR | S_IRUSR | S_IRGRP | S_IROTH | S_IXOTH;
    metadata.uid = 0;
    metadata.gid = 0;
    metadata.size = 0;
    metadata.mtime = TimeManagement::boot_time();
    return metadata;
}
ErrorOr<void> SysFSDirectoryInode::traverse_as_directory(Function<ErrorOr<void>(FileSystem::DirectoryEntryView const&)> callback) const
{
    MutexLocker locker(fs().m_lock);
    return m_associated_component->traverse_as_directory(fs().fsid(), move(callback));
}

ErrorOr<NonnullLockRefPtr<Inode>> SysFSDirectoryInode::lookup(StringView name)
{
    MutexLocker locker(fs().m_lock);
    auto component = m_associated_component->lookup(name);
    if (!component)
        return ENOENT;
    return TRY(component->to_inode(fs()));
}

}
