/*
 * Copyright (c) 2019-2020, Sergey Bugaev <bugaevc@serenityos.org>
 * Copyright (c) 2022-2024, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/FileSystem/RAMBackedFileType.h>
#include <Kernel/FileSystem/RAMFS/FileSystem.h>
#include <Kernel/FileSystem/RAMFS/Inode.h>

namespace Kernel {

ErrorOr<NonnullRefPtr<FileSystem>> RAMFS::try_create(FileSystemSpecificOptions const& filesystem_specific_options)
{
    Optional<u64> max_size = parse_unsigned_filesystem_specific_option(filesystem_specific_options, "max_size"sv);
    return TRY(adopt_nonnull_ref_or_enomem(new (nothrow) RAMFS(max_size)));
}

ErrorOr<void> RAMFS::validate_mount_unsigned_integer_flag(StringView key, u64 value)
{
    if (key == "max_size"sv) {
        if ((value % RAMFSInode::data_block_size) != 0)
            return EINVAL;
        return {};
    }
    return ENOTSUP;
}

RAMFS::RAMFS(Optional<u64> max_size)
    : m_max_size(max_size)
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

u8 RAMFS::internal_file_type_to_directory_entry_type(DirectoryEntryView const& entry) const
{
    return ram_backed_file_type_to_directory_entry_type(entry);
}

}
