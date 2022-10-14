/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Spencer Dixon <spencercdixon@gmail.com>
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/FileSystem/ProcFS/GlobalInode.h>

namespace Kernel {

ErrorOr<NonnullLockRefPtr<ProcFSGlobalInode>> ProcFSGlobalInode::try_create(ProcFS const& fs, ProcFSExposedComponent const& component)
{
    return adopt_nonnull_lock_ref_or_enomem(new (nothrow) ProcFSGlobalInode(fs, component));
}

ProcFSGlobalInode::ProcFSGlobalInode(ProcFS const& fs, ProcFSExposedComponent const& component)
    : ProcFSInode(fs, component.component_index())
    , m_associated_component(component)
{
}

void ProcFSGlobalInode::did_seek(OpenFileDescription& description, off_t new_offset)
{
    if (new_offset != 0)
        return;
    auto result = m_associated_component->refresh_data(description);
    if (result.is_error()) {
        // Subsequent calls to read will return EIO!
        dbgln("ProcFS: Could not refresh contents: {}", result.error());
    }
}

ErrorOr<void> ProcFSGlobalInode::attach(OpenFileDescription& description)
{
    return m_associated_component->refresh_data(description);
}

ErrorOr<size_t> ProcFSGlobalInode::read_bytes_locked(off_t offset, size_t count, UserOrKernelBuffer& buffer, OpenFileDescription* fd) const
{
    return m_associated_component->read_bytes(offset, count, buffer, fd);
}

StringView ProcFSGlobalInode::name() const
{
    return m_associated_component->name();
}

ErrorOr<void> ProcFSGlobalInode::traverse_as_directory(Function<ErrorOr<void>(FileSystem::DirectoryEntryView const&)>) const
{
    VERIFY_NOT_REACHED();
}

ErrorOr<NonnullLockRefPtr<Inode>> ProcFSGlobalInode::lookup(StringView)
{
    VERIFY_NOT_REACHED();
}

ErrorOr<void> ProcFSGlobalInode::truncate(u64 size)
{
    return m_associated_component->truncate(size);
}

ErrorOr<void> ProcFSGlobalInode::update_timestamps(Optional<Time>, Optional<Time>, Optional<Time>)
{
    return {};
}

InodeMetadata ProcFSGlobalInode::metadata() const
{
    MutexLocker locker(m_inode_lock);
    InodeMetadata metadata;
    metadata.inode = { fsid(), m_associated_component->component_index() };
    metadata.mode = S_IFREG | m_associated_component->required_mode();
    metadata.uid = m_associated_component->owner_user();
    metadata.gid = m_associated_component->owner_group();
    metadata.size = 0;
    metadata.mtime = m_associated_component->modified_time();
    return metadata;
}

ErrorOr<size_t> ProcFSGlobalInode::write_bytes_locked(off_t offset, size_t count, UserOrKernelBuffer const& buffer, OpenFileDescription* fd)
{
    return m_associated_component->write_bytes(offset, count, buffer, fd);
}

}
