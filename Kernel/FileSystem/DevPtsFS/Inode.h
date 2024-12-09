/*
 * Copyright (c) 2019-2020, Sergey Bugaev <bugaevc@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>
#include <Kernel/Devices/TTY/SlavePTY.h>
#include <Kernel/FileSystem/DevPtsFS/FileSystem.h>
#include <Kernel/FileSystem/Inode.h>

namespace Kernel {

class DevPtsFSInode final : public Inode {
    friend class DevPtsFS;

public:
    virtual ~DevPtsFSInode() override;

    DevPtsFS& fs() { return static_cast<DevPtsFS&>(Inode::fs()); }
    DevPtsFS const& fs() const { return static_cast<DevPtsFS const&>(Inode::fs()); }

private:
    DevPtsFSInode(DevPtsFS&, InodeIndex, SlavePTY&);

    // NOTE: This constructor is used for the root inode only.
    DevPtsFSInode(DevPtsFS&);

    // ^Inode
    virtual ErrorOr<size_t> read_bytes_locked(off_t, size_t, UserOrKernelBuffer& buffer, OpenFileDescription*) const override;
    virtual InodeMetadata metadata() const override;
    virtual ErrorOr<void> traverse_as_directory(Function<ErrorOr<void>(FileSystem::DirectoryEntryView const&)>) const override;
    virtual ErrorOr<NonnullRefPtr<Inode>> lookup(StringView name) override;
    virtual ErrorOr<void> flush_metadata() override;
    virtual ErrorOr<size_t> write_bytes_locked(off_t, size_t, UserOrKernelBuffer const& buffer, OpenFileDescription*) override;
    virtual ErrorOr<NonnullRefPtr<Inode>> create_child(StringView name, mode_t, dev_t, UserID, GroupID) override;
    virtual ErrorOr<void> add_child(Inode&, StringView name, mode_t) override;
    virtual ErrorOr<void> remove_child(StringView name) override;
    virtual ErrorOr<void> chmod(mode_t) override;
    virtual ErrorOr<void> chown(UserID, GroupID) override;

    LockWeakPtr<SlavePTY> m_pty;
    InodeMetadata m_metadata;
};

}
