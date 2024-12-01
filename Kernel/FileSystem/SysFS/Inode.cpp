/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StringView.h>
#include <Kernel/FileSystem/SysFS/Inode.h>
#include <Kernel/Time/TimeManagement.h>

namespace Kernel {

ErrorOr<NonnullRefPtr<SysFSInode>> SysFSInode::try_create(SysFS const& fs, SysFSComponent const& component)
{
    return adopt_nonnull_ref_or_enomem(new (nothrow) SysFSInode(fs, component));
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

ErrorOr<size_t> SysFSInode::read_bytes_locked(off_t offset, size_t count, UserOrKernelBuffer& buffer, OpenFileDescription* fd) const
{
    return m_associated_component->read_bytes(offset, count, buffer, fd);
}

ErrorOr<void> SysFSInode::traverse_as_directory(Function<ErrorOr<void>(FileSystem::DirectoryEntryView const&)>) const
{
    VERIFY_NOT_REACHED();
}

ErrorOr<NonnullRefPtr<Inode>> SysFSInode::lookup(StringView)
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

ErrorOr<size_t> SysFSInode::write_bytes_locked(off_t offset, size_t count, UserOrKernelBuffer const& buffer, OpenFileDescription* fd)
{
    return m_associated_component->write_bytes(offset, count, buffer, fd);
}

ErrorOr<NonnullRefPtr<Inode>> SysFSInode::create_child(StringView, mode_t, dev_t, UserID, GroupID)
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

ErrorOr<void> SysFSInode::truncate_locked(u64 size)
{
    VERIFY(m_inode_lock.is_locked());
    return m_associated_component->truncate(size);
}

ErrorOr<void> SysFSInode::update_timestamps(Optional<UnixDateTime>, Optional<UnixDateTime>, Optional<UnixDateTime>)
{
    return {};
}

}
