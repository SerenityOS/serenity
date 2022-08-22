/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/FileSystem/FileSystem.h>
#include <Kernel/FileSystem/Inode.h>
#include <Kernel/FileSystem/SysFS/Component.h>
#include <Kernel/Locking/MutexProtected.h>

namespace Kernel {

class SysFS final : public FileSystem {
    friend class SysFSInode;
    friend class SysFSDirectoryInode;

public:
    virtual ~SysFS() override;
    static ErrorOr<NonnullLockRefPtr<FileSystem>> try_create();

    virtual ErrorOr<void> initialize() override;
    virtual StringView class_name() const override { return "SysFS"sv; }

    virtual Inode& root_inode() override;

private:
    SysFS();

    LockRefPtr<SysFSInode> m_root_inode;
};

class SysFSInode : public Inode {
    friend class SysFS;
    friend class SysFSDirectoryInode;

public:
    static ErrorOr<NonnullLockRefPtr<SysFSInode>> try_create(SysFS const&, SysFSComponent const&);
    StringView name() const { return m_associated_component->name(); }

protected:
    SysFSInode(SysFS const&, SysFSComponent const&);
    virtual ErrorOr<size_t> read_bytes(off_t, size_t, UserOrKernelBuffer& buffer, OpenFileDescription*) const override;
    virtual ErrorOr<void> traverse_as_directory(Function<ErrorOr<void>(FileSystem::DirectoryEntryView const&)>) const override;
    virtual ErrorOr<NonnullLockRefPtr<Inode>> lookup(StringView name) override;
    virtual ErrorOr<void> flush_metadata() override;
    virtual InodeMetadata metadata() const override;
    virtual ErrorOr<size_t> write_bytes(off_t, size_t, UserOrKernelBuffer const&, OpenFileDescription*) override;
    virtual ErrorOr<NonnullLockRefPtr<Inode>> create_child(StringView name, mode_t, dev_t, UserID, GroupID) override;
    virtual ErrorOr<void> add_child(Inode&, StringView name, mode_t) override;
    virtual ErrorOr<void> remove_child(StringView name) override;
    virtual ErrorOr<void> chmod(mode_t) override;
    virtual ErrorOr<void> chown(UserID, GroupID) override;
    virtual ErrorOr<void> truncate(u64) override;
    virtual ErrorOr<void> update_timestamps(Optional<time_t> atime, Optional<time_t> ctime, Optional<time_t> mtime) override;

    virtual ErrorOr<void> attach(OpenFileDescription& description) override final;
    virtual void did_seek(OpenFileDescription&, off_t) override final;

    NonnullLockRefPtr<SysFSComponent> m_associated_component;
};

class SysFSLinkInode : public SysFSInode {
    friend class SysFS;

public:
    static ErrorOr<NonnullLockRefPtr<SysFSLinkInode>> try_create(SysFS const&, SysFSComponent const&);
    virtual ~SysFSLinkInode() override;

protected:
    SysFSLinkInode(SysFS const&, SysFSComponent const&);
    // ^Inode
    virtual InodeMetadata metadata() const override;
};

class SysFSDirectoryInode : public SysFSInode {
    friend class SysFS;

public:
    static ErrorOr<NonnullLockRefPtr<SysFSDirectoryInode>> try_create(SysFS const&, SysFSComponent const&);
    virtual ~SysFSDirectoryInode() override;

    SysFS& fs() { return static_cast<SysFS&>(Inode::fs()); }
    SysFS const& fs() const { return static_cast<SysFS const&>(Inode::fs()); }

protected:
    SysFSDirectoryInode(SysFS const&, SysFSComponent const&);
    // ^Inode
    virtual InodeMetadata metadata() const override;
    virtual ErrorOr<void> traverse_as_directory(Function<ErrorOr<void>(FileSystem::DirectoryEntryView const&)>) const override;
    virtual ErrorOr<NonnullLockRefPtr<Inode>> lookup(StringView name) override;
};

}
