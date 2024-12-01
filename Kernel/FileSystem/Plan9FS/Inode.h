/*
 * Copyright (c) 2020, Sergey Bugaev <bugaevc@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Atomic.h>
#include <Kernel/FileSystem/Inode.h>
#include <Kernel/FileSystem/Plan9FS/FileSystem.h>
#include <Kernel/FileSystem/Plan9FS/Message.h>
#include <Kernel/Library/KBufferBuilder.h>

namespace Kernel {

class Plan9FSInode final : public Inode {
    friend class Plan9FS;

public:
    virtual ~Plan9FSInode() override;

    u32 fid() const { return index().value(); }

    // ^Inode
    virtual InodeMetadata metadata() const override;
    virtual ErrorOr<void> flush_metadata() override;
    virtual ErrorOr<void> traverse_as_directory(Function<ErrorOr<void>(FileSystem::DirectoryEntryView const&)>) const override;
    virtual ErrorOr<NonnullRefPtr<Inode>> lookup(StringView name) override;
    virtual ErrorOr<NonnullRefPtr<Inode>> create_child(StringView name, mode_t, dev_t, UserID, GroupID) override;
    virtual ErrorOr<void> add_child(Inode&, StringView name, mode_t) override;
    virtual ErrorOr<void> remove_child(StringView name) override;
    virtual ErrorOr<void> chmod(mode_t) override;
    virtual ErrorOr<void> chown(UserID, GroupID) override;
    virtual ErrorOr<void> truncate_locked(u64) override;

private:
    // ^Inode
    virtual ErrorOr<size_t> read_bytes_locked(off_t, size_t, UserOrKernelBuffer& buffer, OpenFileDescription*) const override;
    virtual ErrorOr<size_t> write_bytes_locked(off_t, size_t, UserOrKernelBuffer const& data, OpenFileDescription*) override;

    Plan9FSInode(Plan9FS&, u32 fid);
    static ErrorOr<NonnullRefPtr<Plan9FSInode>> try_create(Plan9FS&, u32 fid);

    enum class GetAttrMask : u64 {
        Mode = 0x1,
        NLink = 0x2,
        UID = 0x4,
        GID = 0x8,
        RDev = 0x10,
        ATime = 0x20,
        MTime = 0x40,
        CTime = 0x80,
        Ino = 0x100,
        Size = 0x200,
        Blocks = 0x400,

        BTime = 0x800,
        Gen = 0x1000,
        DataVersion = 0x2000,

        Basic = 0x7ff,
        All = 0x3fff
    };

    enum class SetAttrMask : u64 {
        Mode = 0x1,
        UID = 0x2,
        GID = 0x4,
        Size = 0x8,
        ATime = 0x10,
        MTime = 0x20,
        CTime = 0x40,
        ATimeSet = 0x80,
        MTimeSet = 0x100
    };

    // Mode in which the file is already open, using SerenityOS constants.
    int m_open_mode { 0 };
    ErrorOr<void> ensure_open_for_mode(int mode);

    Plan9FS& fs() { return reinterpret_cast<Plan9FS&>(Inode::fs()); }
    Plan9FS& fs() const
    {
        return const_cast<Plan9FS&>(reinterpret_cast<Plan9FS const&>(Inode::fs()));
    }
};

}
