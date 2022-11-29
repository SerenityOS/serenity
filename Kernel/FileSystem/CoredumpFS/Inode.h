/*
 * Copyright (c) 2019-2020, Sergey Bugaev <bugaevc@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Optional.h>
#include <AK/Types.h>
#include <Kernel/FileSystem/CoredumpFS/FileSystem.h>
#include <Kernel/FileSystem/Inode.h>

namespace Kernel {

class CoredumpFSInode final : public Inode {
    friend class CoredumpFS;

public:
    virtual ~CoredumpFSInode() override;

    using CoredumpPID = u64;

private:
    CoredumpFS& fs() { return static_cast<CoredumpFS&>(Inode::fs()); }
    CoredumpFS const& fs() const { return static_cast<CoredumpFS const&>(Inode::fs()); }

    CoredumpFSInode(CoredumpFS&, InodeIndex);

    // ^Inode
    virtual ErrorOr<size_t> read_bytes_locked(off_t, size_t, UserOrKernelBuffer& buffer, OpenFileDescription*) const override;
    virtual InodeMetadata metadata() const override;
    virtual ErrorOr<void> traverse_as_directory(Function<ErrorOr<void>(FileSystem::DirectoryEntryView const&)>) const override;
    virtual ErrorOr<NonnullLockRefPtr<Inode>> lookup(StringView name) override;
    virtual ErrorOr<void> flush_metadata() override;
    virtual ErrorOr<size_t> write_bytes_locked(off_t, size_t, UserOrKernelBuffer const& buffer, OpenFileDescription*) override;
    virtual ErrorOr<NonnullLockRefPtr<Inode>> create_child(StringView name, mode_t, dev_t, UserID, GroupID) override;
    virtual ErrorOr<void> add_child(Inode&, StringView name, mode_t) override;
    virtual ErrorOr<void> remove_child(StringView name) override;
    virtual ErrorOr<void> replace_child(StringView name, Inode& child) override;
    virtual ErrorOr<void> chmod(mode_t) override;
    virtual ErrorOr<void> chown(UserID, GroupID) override;
    virtual ErrorOr<void> truncate(u64 size) override;

    Optional<CoredumpPID> const m_associated_coredump_pid;
};

}
