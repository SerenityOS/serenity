/*
 * Copyright (c) 2019-2020, Sergey Bugaev <bugaevc@serenityos.org>
 * Copyright (c) 2022-2023, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/API/POSIX/sys/limits.h>
#include <Kernel/FileSystem/RAMFS/FileSystem.h>
#include <Kernel/FileSystem/RAMFS/Inode.h>

namespace Kernel {

struct [[gnu::packed]] RAMFSSpecificFlagsBytes {
    u64 max_size;
    u64 max_inode_size;
};

ErrorOr<NonnullRefPtr<FileSystem>> RAMFS::try_create(ReadonlyBytes mount_flags)
{
    auto* ramfs_mount_flags = reinterpret_cast<RAMFSSpecificFlagsBytes const*>(mount_flags.data());
    u64 raw_max_size = ramfs_mount_flags->max_size;
    u64 raw_max_inode_size = ramfs_mount_flags->max_inode_size;
    Optional<u64> max_size = ramfs_mount_flags->max_size != 0 ? raw_max_size : Optional<u64> {};
    Optional<u64> max_inode_size = ramfs_mount_flags->max_inode_size != 0 ? raw_max_inode_size : Optional<u64> {};
    return TRY(adopt_nonnull_ref_or_enomem(new (nothrow) RAMFS(max_size, max_inode_size)));
}

ErrorOr<void> RAMFS::handle_mount_unsigned_integer_flag(Bytes mount_file_specific_flags_buffer, StringView key, u64 value)
{
    auto* ramfs_mount_flags = reinterpret_cast<RAMFSSpecificFlagsBytes*>(mount_file_specific_flags_buffer.data());
    if (key == "fs_max_size") {
        if (ramfs_mount_flags->max_inode_size != 0 && ramfs_mount_flags->max_inode_size > value)
            return ERANGE;
        if ((value % PAGE_SIZE) != 0)
            return EDOM;
        ramfs_mount_flags->max_size = value;
        return {};
    }
    if (key == "inode_max_size") {
        if (ramfs_mount_flags->max_size != 0 && ramfs_mount_flags->max_size < value)
            return ERANGE;
        if ((value % PAGE_SIZE) != 0)
            return EDOM;
        ramfs_mount_flags->max_inode_size = value;
        return {};
    }
    return EINVAL;
}

RAMFS::RAMFS(Optional<u64> max_size, Optional<u64> max_inode_size)
    : m_max_size(max_size)
    , m_max_inode_size(max_inode_size)
{
    m_current_storage_usage_size.with_exclusive([](auto& current_size) {
        current_size = 0;
    });
}

RAMFS::~RAMFS() = default;

ErrorOr<void> RAMFS::initialize()
{
    m_root_inode = TRY(RAMFSInode::try_create_root(*this));
    return {};
}

Inode& RAMFS::root_inode()
{
    VERIFY(!m_root_inode.is_null());
    return *m_root_inode;
}

unsigned RAMFS::next_inode_index()
{
    MutexLocker locker(m_lock);

    return m_next_inode_index++;
}

}
