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
    static NonnullRefPtr<DevPtsFS> create();

    virtual KResult initialize() override;
    virtual StringView class_name() const override { return "DevPtsFS"sv; }

    virtual Inode& root_inode() override;

private:
    DevPtsFS();
    KResultOr<NonnullRefPtr<Inode>> get_inode(InodeIdentifier) const;

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
    virtual KResultOr<size_t> read_bytes(off_t, size_t, UserOrKernelBuffer& buffer, FileDescription*) const override;
    virtual InodeMetadata metadata() const override;
    virtual KResult traverse_as_directory(Function<bool(FileSystem::DirectoryEntryView const&)>) const override;
    virtual KResultOr<NonnullRefPtr<Inode>> lookup(StringView name) override;
    virtual void flush_metadata() override;
    virtual KResultOr<size_t> write_bytes(off_t, size_t, const UserOrKernelBuffer& buffer, FileDescription*) override;
    virtual KResultOr<NonnullRefPtr<Inode>> create_child(StringView name, mode_t, dev_t, UserID, GroupID) override;
    virtual KResult add_child(Inode&, const StringView& name, mode_t) override;
    virtual KResult remove_child(const StringView& name) override;
    virtual KResult chmod(mode_t) override;
    virtual KResult chown(UserID, GroupID) override;

    WeakPtr<SlavePTY> m_pty;
    InodeMetadata m_metadata;
};

}
