/*
 * Copyright (c) 2024, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>
#include <Kernel/Devices/Loop/LoopDevice.h>
#include <Kernel/FileSystem/DevLoopFS/FileSystem.h>
#include <Kernel/FileSystem/Inode.h>

namespace Kernel {

class DevLoopFSInode final : public Inode {
    friend class DevLoopFS;

public:
    virtual ~DevLoopFSInode() override;

    DevLoopFS& fs() { return static_cast<DevLoopFS&>(Inode::fs()); }
    DevLoopFS const& fs() const { return static_cast<DevLoopFS const&>(Inode::fs()); }

private:
    DevLoopFSInode(DevLoopFS&, InodeIndex, LoopDevice&);

    // NOTE: This constructor is used for the root inode only.
    DevLoopFSInode(DevLoopFS&);

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

    LockWeakPtr<LoopDevice> m_loop_device;
    InodeMetadata m_metadata;
};

}
