/*
 * Copyright (c) 2019-2020, Sergey Bugaev <bugaevc@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>
#include <Kernel/FileSystem/FileSystem.h>
#include <Kernel/FileSystem/Inode.h>

namespace Kernel {

class SlavePTY;
class DevPtsFSInode;

class DevPtsFS final : public FileSystem {
    friend class DevPtsFSInode;

public:
    virtual ~DevPtsFS() override;
    static ErrorOr<NonnullRefPtr<DevPtsFS>> try_create();

    virtual ErrorOr<void> initialize() override;
    virtual StringView class_name() const override { return "DevPtsFS"sv; }

    virtual Inode& root_inode() override;

private:
    DevPtsFS();
    ErrorOr<NonnullRefPtr<Inode>> get_inode(InodeIdentifier) const;

    RefPtr<DevPtsFSInode> m_root_inode;
};

class DevPtsFSInode final : public Inode {
    friend class DevPtsFS;

public:
    virtual ~DevPtsFSInode() override;

    DevPtsFS& fs() { return static_cast<DevPtsFS&>(Inode::fs()); }
    DevPtsFS const& fs() const { return static_cast<DevPtsFS const&>(Inode::fs()); }

private:
    DevPtsFSInode(DevPtsFS&, InodeIndex, SlavePTY*);

    // ^Inode
    virtual ErrorOr<size_t> read_bytes(off_t, size_t, UserOrKernelBuffer& buffer, OpenFileDescription*) const override;
    virtual InodeMetadata metadata() const override;
    virtual ErrorOr<void> traverse_as_directory(Function<ErrorOr<void>(FileSystem::DirectoryEntryView const&)>) const override;
    virtual ErrorOr<NonnullRefPtr<Inode>> lookup(StringView name) override;
    virtual ErrorOr<void> flush_metadata() override;
    virtual ErrorOr<size_t> write_bytes(off_t, size_t, const UserOrKernelBuffer& buffer, OpenFileDescription*) override;
    virtual ErrorOr<NonnullRefPtr<Inode>> create_child(StringView name, mode_t, dev_t, UserID, GroupID) override;
    virtual ErrorOr<void> add_child(Inode&, StringView name, mode_t) override;
    virtual ErrorOr<void> remove_child(StringView name) override;
    virtual ErrorOr<void> chmod(mode_t) override;
    virtual ErrorOr<void> chown(UserID, GroupID) override;

    WeakPtr<SlavePTY> m_pty;
    InodeMetadata m_metadata;
};

}
