/*
 * Copyright (c) 2019-2020, Sergey Bugaev <bugaevc@serenityos.org>
 * Copyright (c) 2022-2023, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/FileSystem/RAMFS/FileSystem.h>
#include <Kernel/FileSystem/RAMFS/Inode.h>

namespace Kernel {

ErrorOr<NonnullRefPtr<FileSystem>> RAMFS::try_create(ReadonlyBytes)
{
    return TRY(adopt_nonnull_ref_or_enomem(new (nothrow) RAMFS));
}

RAMFS::RAMFS() = default;
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
    switch (static_cast<FileType>(entry.file_type)) {
    case FileType::Directory:
        return DT_DIR;
    case FileType::Character:
        return DT_CHR;
    case FileType::Block:
        return DT_BLK;
    case FileType::Regular:
        return DT_REG;
    case FileType::FIFO:
        return DT_FIFO;
    case FileType::Link:
        return DT_LNK;
    case FileType::Socket:
        return DT_SOCK;
    case FileType::Unknown:
    default:
        return DT_UNKNOWN;
    }
}

}
