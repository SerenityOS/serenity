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

static constexpr StringView MAXIMUM_SIZE_OPTION = "maximum_size"sv;

ErrorOr<void> RAMFS::validate_mount_unsigned_integer_flag(StringView key, u64)
{
    if (key == MAXIMUM_SIZE_OPTION)
        return {};
    return EINVAL;
}

ErrorOr<NonnullRefPtr<FileSystem>> RAMFS::try_create(FileSystemSpecificOptions const& options)
{
    u64 maximum_size = parse_unsigned_filesystem_specific_option(options, MAXIMUM_SIZE_OPTION).value_or(0);
    return TRY(adopt_nonnull_ref_or_enomem(new (nothrow) RAMFS(maximum_size)));
}

RAMFS::RAMFS(u64 maximum_size)
    : m_maximum_size(maximum_size)
{
}

RAMFS::~RAMFS() = default;

ErrorOr<void> RAMFS::initialize()
{
    set_logical_block_size(LOGICAL_BLOCK_SIZE);
    set_fragment_size(LOGICAL_BLOCK_SIZE);
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

ErrorOr<void> RAMFS::update_size(i64 delta)
{
    SpinlockLocker locker(m_size_lock);

    auto would_overflow = Checked<decltype(m_current_size)>::addition_would_overflow(m_current_size, delta);
    if (m_maximum_size > 0 && (would_overflow || m_current_size + delta > m_maximum_size))
        return Error::from_errno(ENOSPC);

    m_current_size += delta;
    return {};
}

u32 RAMFS::total_block_count_impl() const
{
    VERIFY(m_size_lock.is_locked());
    if (m_maximum_size == 0)
        return ceil_div(m_current_size, LOGICAL_BLOCK_SIZE);
    return ceil_div(m_maximum_size, LOGICAL_BLOCK_SIZE);
}

u64 RAMFS::total_block_count() const
{
    SpinlockLocker locker(m_size_lock);
    return total_block_count_impl();
}

u64 RAMFS::free_block_count() const
{
    SpinlockLocker locker(m_size_lock);
    if (m_maximum_size == 0)
        return 0;
    return total_block_count_impl() - ceil_div(m_current_size, LOGICAL_BLOCK_SIZE);
}

}
