/*
 * Copyright (c) 2019-2020, Sergey Bugaev <bugaevc@serenityos.org>
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/FileSystem/TmpFS/FileSystem.h>
#include <Kernel/FileSystem/TmpFS/Inode.h>

namespace Kernel {

struct [[gnu::packed]] TmpFSFlags {
    u64 max_size;
    u64 max_inode_size;
};

ErrorOr<NonnullLockRefPtr<FileSystem>> TmpFS::try_create(Span<u8 const> mount_flags)
{
    auto* tmpfs_mount_flags = reinterpret_cast<TmpFSFlags const*>(mount_flags.data());
    u64 raw_max_size = tmpfs_mount_flags->max_size;
    u64 raw_max_inode_size = tmpfs_mount_flags->max_inode_size;
    Optional<u64> max_size = tmpfs_mount_flags->max_size != 0 ? raw_max_size : Optional<u64> {};
    Optional<u64> max_inode_size = tmpfs_mount_flags->max_inode_size != 0 ? raw_max_inode_size : Optional<u64> {};
    return TRY(adopt_nonnull_lock_ref_or_enomem(new (nothrow) TmpFS(max_size, max_inode_size)));
}

ErrorOr<void> TmpFS::handle_mount_unsigned_integer_flag(Span<u8> mount_file_specific_flags_buffer, StringView key, u64 value)
{
    auto* tmpfs_mount_flags = reinterpret_cast<TmpFSFlags*>(mount_file_specific_flags_buffer.data());
    if (key == "fs_max_size") {
        if (tmpfs_mount_flags->max_inode_size != 0 && tmpfs_mount_flags->max_inode_size > value)
            return ERANGE;
        tmpfs_mount_flags->max_size = value;
        return {};
    }
    if (key == "inode_max_size") {
        if (tmpfs_mount_flags->max_size != 0 && tmpfs_mount_flags->max_size < value)
            return ERANGE;
        tmpfs_mount_flags->max_inode_size = value;
        return {};
    }
    return EINVAL;
}

TmpFS::TmpFS(Optional<u64> max_size, Optional<u64> max_inode_size)
    : m_max_size(max_size)
    , m_max_inode_size(max_inode_size)
{
    m_current_storage_usage_size.with_exclusive([&](auto& current_storage_usage_size) {
        current_storage_usage_size = 0;
    });
}

TmpFS::~TmpFS() = default;

ErrorOr<void> TmpFS::initialize()
{
    m_root_inode = TRY(TmpFSInode::try_create_root(*this));
    return {};
}

Inode& TmpFS::root_inode()
{
    VERIFY(!m_root_inode.is_null());
    return *m_root_inode;
}

unsigned TmpFS::next_inode_index()
{
    MutexLocker locker(m_lock);

    return m_next_inode_index++;
}

}
