/*
 * Copyright (c) 2019-2020, Sergey Bugaev <bugaevc@serenityos.org>
 * Copyright (c) 2022-2024, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Badge.h>
#include <Kernel/FileSystem/FileSystem.h>
#include <Kernel/FileSystem/FileSystemSpecificOption.h>
#include <Kernel/FileSystem/Inode.h>
#include <Kernel/Forward.h>
#include <Kernel/Locking/MutexProtected.h>

namespace Kernel {

class RAMFS final : public FileSystem {
    friend class RAMFSInode;

public:
    virtual ~RAMFS() override;
    static ErrorOr<NonnullRefPtr<FileSystem>> try_create(FileSystemSpecificOptions const&);

    static ErrorOr<void> validate_mount_unsigned_integer_flag(StringView key, u64);
    virtual ErrorOr<void> initialize() override;

    virtual StringView class_name() const override { return "RAMFS"sv; }

    virtual bool supports_watchers() const override { return true; }
    virtual bool supports_backing_loop_devices() const override { return true; }

    virtual Inode& root_inode() override;

    virtual u8 internal_file_type_to_directory_entry_type(DirectoryEntryView const& entry) const override;
    MutexProtected<u64>& current_storage_usage_size(Badge<RAMFSInode>) { return m_current_storage_usage_size; }
    Optional<u64> max_size() const { return m_max_size; }

private:
    RAMFS(Optional<u64>);

    RefPtr<RAMFSInode> m_root_inode;

    Optional<u64> const m_max_size;
    MutexProtected<u64> m_current_storage_usage_size;

    // NOTE: We start by assigning InodeIndex of 2, because 0 is invalid and 1
    // is reserved for the root directory inode.
    unsigned m_next_inode_index { 2 };
    unsigned next_inode_index();
};

}
