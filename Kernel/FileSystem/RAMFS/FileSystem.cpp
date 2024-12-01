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

ErrorOr<NonnullRefPtr<FileSystem>> RAMFS::try_create(FileSystemSpecificOptions const&)
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

ErrorOr<void> RAMFS::rename(Inode& old_parent_inode, StringView old_basename, Inode& new_parent_inode, StringView new_basename)
{
    MutexLocker locker(m_lock);

    if (auto maybe_inode_to_be_replaced = new_parent_inode.lookup(new_basename); !maybe_inode_to_be_replaced.is_error()) {
        VERIFY(!maybe_inode_to_be_replaced.value()->is_directory());
        TRY(new_parent_inode.remove_child(new_basename));
    }

    auto old_inode = TRY(old_parent_inode.lookup(old_basename));

    TRY(new_parent_inode.add_child(old_inode, new_basename, old_inode->mode()));
    TRY(old_parent_inode.remove_child(old_basename));

    return {};
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
