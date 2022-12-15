/*
 * Copyright (c) 2019-2020, Sergey Bugaev <bugaevc@serenityos.org>
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Badge.h>
#include <Kernel/FileSystem/FileSystem.h>
#include <Kernel/FileSystem/Inode.h>
#include <Kernel/Forward.h>
#include <Kernel/Locking/MutexProtected.h>

namespace Kernel {

class TmpFS final : public FileSystem {
    friend class TmpFSInode;

public:
    virtual ~TmpFS() override;
    static ErrorOr<NonnullLockRefPtr<FileSystem>> try_create(Span<u8 const>);

    static ErrorOr<void> handle_mount_unsigned_integer_flag(Span<u8>, StringView key, u64);

    virtual ErrorOr<void> initialize() override;

    virtual StringView class_name() const override { return "TmpFS"sv; }

    virtual bool supports_watchers() const override { return true; }

    virtual Inode& root_inode() override;

    MutexProtected<u64>& current_storage_usage_size(Badge<TmpFSInode>) { return m_current_storage_usage_size; }
    Optional<u64> max_size() const { return m_max_size; }
    Optional<u64> max_inode_size() const { return m_max_inode_size; }

private:
    TmpFS(Optional<u64>, Optional<u64>);

    LockRefPtr<TmpFSInode> m_root_inode;

    Optional<u64> const m_max_size;
    Optional<u64> const m_max_inode_size;

    MutexProtected<u64> m_current_storage_usage_size;

    // NOTE: We start by assigning InodeIndex of 2, because 0 is invalid and 1
    // is reserved for the root directory inode.
    unsigned m_next_inode_index { 2 };
    unsigned next_inode_index();
};

}
