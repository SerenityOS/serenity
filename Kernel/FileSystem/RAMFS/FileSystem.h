/*
 * Copyright (c) 2019-2020, Sergey Bugaev <bugaevc@serenityos.org>
 * Copyright (c) 2022-2024, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/FileSystem/FileSystem.h>
#include <Kernel/FileSystem/FileSystemSpecificOption.h>
#include <Kernel/FileSystem/Inode.h>
#include <Kernel/Forward.h>

namespace Kernel {

class RAMFS final : public FileSystem {
    friend class RAMFSInode;

public:
    virtual ~RAMFS() override;
    static ErrorOr<NonnullRefPtr<FileSystem>> try_create(FileSystemSpecificOptions const&);
    virtual ErrorOr<void> initialize() override;

    static ErrorOr<void> validate_mount_unsigned_integer_flag(StringView key, u64);

    virtual StringView class_name() const override { return "RAMFS"sv; }

    virtual bool supports_watchers() const override { return true; }
    virtual bool supports_backing_loop_devices() const override { return true; }

    virtual Inode& root_inode() override;

    virtual ErrorOr<void> rename(Inode& old_parent_inode, StringView old_basename, Inode& new_parent_inode, StringView new_basename) override;

    virtual u8 internal_file_type_to_directory_entry_type(DirectoryEntryView const& entry) const override;

    virtual unsigned total_block_count() const override;
    virtual unsigned free_block_count() const override;

private:
    RAMFS(u64 maximum_size);

    u32 total_block_count_impl() const;

    // This never returns an error for negative values.
    ErrorOr<void> update_size(i64 size);

    RefPtr<RAMFSInode> m_root_inode;

    // NOTE: We start by assigning InodeIndex of 2, because 0 is invalid and 1
    // is reserved for the root directory inode.
    unsigned m_next_inode_index { 2 };
    unsigned next_inode_index();

    // FIXME: All the size computations only consider file content, not
    //        the inodes themselves.
    static constexpr u32 LOGICAL_BLOCK_SIZE = 1024;
    mutable Spinlock<LockRank::FileSystem> m_size_lock {};
    u64 m_current_size = 0; // Both sizes are in bytes.
    u64 m_maximum_size = 0;
};

}
