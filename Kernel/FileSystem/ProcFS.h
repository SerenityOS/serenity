/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/HashMap.h>
#include <AK/Types.h>
#include <Kernel/FileSystem/FileSystem.h>
#include <Kernel/FileSystem/Inode.h>
#include <Kernel/Forward.h>
#include <Kernel/KBufferBuilder.h>
#include <Kernel/Locking/Mutex.h>
#include <Kernel/ProcessExposed.h>
#include <Kernel/UnixTypes.h>

namespace Kernel {

class ProcFS final : public FileSystem {
    friend class ProcFSInode;
    friend class ProcFSDirectoryInode;
    friend class ProcFSProcessDirectoryInode;
    friend class ProcFSGlobalInode;
    friend class ProcFSAssociatedProcessInode;
    friend class ProcFSProcessSubDirectoryInode;

public:
    virtual ~ProcFS() override;
    static ErrorOr<NonnullLockRefPtr<FileSystem>> try_create();

    virtual ErrorOr<void> initialize() override;
    virtual StringView class_name() const override { return "ProcFS"sv; }

    virtual Inode& root_inode() override;

private:
    ProcFS();

    LockRefPtr<ProcFSDirectoryInode> m_root_inode;
};

class ProcFSInode : public Inode {
    friend class ProcFS;

public:
    virtual ~ProcFSInode() override;

protected:
    ProcFSInode(ProcFS const&, InodeIndex);

    ProcFS& procfs() { return static_cast<ProcFS&>(Inode::fs()); }
    ProcFS const& procfs() const { return static_cast<ProcFS const&>(Inode::fs()); }

    // ^Inode
    virtual ErrorOr<void> attach(OpenFileDescription& description) override = 0;
    virtual void did_seek(OpenFileDescription&, off_t) override = 0;
    virtual ErrorOr<void> flush_metadata() override final;
    virtual ErrorOr<NonnullLockRefPtr<Inode>> create_child(StringView name, mode_t, dev_t, UserID, GroupID) override final;
    virtual ErrorOr<void> add_child(Inode&, StringView name, mode_t) override final;
    virtual ErrorOr<void> remove_child(StringView name) override final;
    virtual ErrorOr<void> chmod(mode_t) override final;
    virtual ErrorOr<void> chown(UserID, GroupID) override final;
};

class ProcFSGlobalInode : public ProcFSInode {
    friend class ProcFS;

public:
    static ErrorOr<NonnullLockRefPtr<ProcFSGlobalInode>> try_create(ProcFS const&, ProcFSExposedComponent const&);
    virtual ~ProcFSGlobalInode() override {};
    StringView name() const;

protected:
    ProcFSGlobalInode(ProcFS const&, ProcFSExposedComponent const&);

    // ^Inode
    virtual ErrorOr<void> attach(OpenFileDescription& description) override final;
    virtual ErrorOr<size_t> read_bytes_locked(off_t, size_t, UserOrKernelBuffer& buffer, OpenFileDescription*) const override final;
    virtual ErrorOr<size_t> write_bytes_locked(off_t, size_t, UserOrKernelBuffer const& buffer, OpenFileDescription*) override final;
    virtual void did_seek(OpenFileDescription&, off_t) override final;
    virtual InodeMetadata metadata() const override;
    virtual ErrorOr<void> traverse_as_directory(Function<ErrorOr<void>(FileSystem::DirectoryEntryView const&)>) const override;
    virtual ErrorOr<NonnullLockRefPtr<Inode>> lookup(StringView) override;
    virtual ErrorOr<void> truncate(u64) override final;
    virtual ErrorOr<void> update_timestamps(Optional<time_t> atime, Optional<time_t> ctime, Optional<time_t> mtime) override;

    NonnullLockRefPtr<ProcFSExposedComponent> m_associated_component;
};

class ProcFSLinkInode : public ProcFSGlobalInode {
    friend class ProcFS;

public:
    static ErrorOr<NonnullLockRefPtr<ProcFSLinkInode>> try_create(ProcFS const&, ProcFSExposedComponent const&);

protected:
    ProcFSLinkInode(ProcFS const&, ProcFSExposedComponent const&);
    virtual InodeMetadata metadata() const override;
};

class ProcFSDirectoryInode final : public ProcFSGlobalInode {
    friend class ProcFS;

public:
    static ErrorOr<NonnullLockRefPtr<ProcFSDirectoryInode>> try_create(ProcFS const&, ProcFSExposedComponent const&);
    virtual ~ProcFSDirectoryInode() override;

protected:
    ProcFSDirectoryInode(ProcFS const&, ProcFSExposedComponent const&);
    // ^Inode
    virtual InodeMetadata metadata() const override;
    virtual ErrorOr<void> traverse_as_directory(Function<ErrorOr<void>(FileSystem::DirectoryEntryView const&)>) const override;
    virtual ErrorOr<NonnullLockRefPtr<Inode>> lookup(StringView name) override;
};

class ProcFSProcessAssociatedInode : public ProcFSInode {
    friend class ProcFS;

protected:
    ProcFSProcessAssociatedInode(ProcFS const&, ProcessID, InodeIndex);
    ProcessID associated_pid() const { return m_pid; }

    // ^Inode
    virtual ErrorOr<size_t> write_bytes_locked(off_t, size_t, UserOrKernelBuffer const& buffer, OpenFileDescription*) override final;

private:
    const ProcessID m_pid;
};

class ProcFSProcessDirectoryInode final : public ProcFSProcessAssociatedInode {
    friend class ProcFS;

public:
    static ErrorOr<NonnullLockRefPtr<ProcFSProcessDirectoryInode>> try_create(ProcFS const&, ProcessID);

private:
    ProcFSProcessDirectoryInode(ProcFS const&, ProcessID);
    // ^Inode
    virtual ErrorOr<void> attach(OpenFileDescription& description) override;
    virtual void did_seek(OpenFileDescription&, off_t) override { }
    virtual InodeMetadata metadata() const override;
    virtual ErrorOr<void> traverse_as_directory(Function<ErrorOr<void>(FileSystem::DirectoryEntryView const&)>) const override;
    virtual ErrorOr<size_t> read_bytes_locked(off_t, size_t, UserOrKernelBuffer& buffer, OpenFileDescription*) const override final;
    virtual ErrorOr<NonnullLockRefPtr<Inode>> lookup(StringView name) override;
};

class ProcFSProcessSubDirectoryInode final : public ProcFSProcessAssociatedInode {
    friend class ProcFS;

public:
    static ErrorOr<NonnullLockRefPtr<ProcFSProcessSubDirectoryInode>> try_create(ProcFS const&, SegmentedProcFSIndex::ProcessSubDirectory, ProcessID);

private:
    ProcFSProcessSubDirectoryInode(ProcFS const&, SegmentedProcFSIndex::ProcessSubDirectory, ProcessID);
    // ^Inode
    virtual ErrorOr<void> attach(OpenFileDescription& description) override;
    virtual void did_seek(OpenFileDescription&, off_t) override;
    virtual InodeMetadata metadata() const override;
    virtual ErrorOr<void> traverse_as_directory(Function<ErrorOr<void>(FileSystem::DirectoryEntryView const&)>) const override;
    virtual ErrorOr<size_t> read_bytes_locked(off_t, size_t, UserOrKernelBuffer& buffer, OpenFileDescription*) const override final;
    virtual ErrorOr<NonnullLockRefPtr<Inode>> lookup(StringView name) override;

    const SegmentedProcFSIndex::ProcessSubDirectory m_sub_directory_type;
};

class ProcFSProcessPropertyInode final : public ProcFSProcessAssociatedInode {
    friend class ProcFS;

public:
    static ErrorOr<NonnullLockRefPtr<ProcFSProcessPropertyInode>> try_create_for_file_description_link(ProcFS const&, unsigned, ProcessID);
    static ErrorOr<NonnullLockRefPtr<ProcFSProcessPropertyInode>> try_create_for_thread_stack(ProcFS const&, ThreadID, ProcessID);
    static ErrorOr<NonnullLockRefPtr<ProcFSProcessPropertyInode>> try_create_for_pid_property(ProcFS const&, SegmentedProcFSIndex::MainProcessProperty, ProcessID);
    static ErrorOr<NonnullLockRefPtr<ProcFSProcessPropertyInode>> try_create_for_child_process_link(ProcFS const&, ProcessID, ProcessID);

private:
    ProcFSProcessPropertyInode(ProcFS const&, SegmentedProcFSIndex::MainProcessProperty, ProcessID);
    ProcFSProcessPropertyInode(ProcFS const&, ThreadID, ProcessID);
    ProcFSProcessPropertyInode(ProcFS const&, unsigned, ProcessID);
    ProcFSProcessPropertyInode(ProcFS const&, ProcessID, ProcessID);

    // ^Inode
    virtual ErrorOr<void> attach(OpenFileDescription& description) override;
    virtual void did_seek(OpenFileDescription&, off_t) override;
    virtual InodeMetadata metadata() const override;
    virtual ErrorOr<void> traverse_as_directory(Function<ErrorOr<void>(FileSystem::DirectoryEntryView const&)>) const override;
    virtual ErrorOr<size_t> read_bytes_locked(off_t, size_t, UserOrKernelBuffer& buffer, OpenFileDescription*) const override final;
    virtual ErrorOr<NonnullLockRefPtr<Inode>> lookup(StringView name) override final;

    ErrorOr<void> refresh_data(OpenFileDescription& description);
    ErrorOr<void> try_to_acquire_data(Process& process, KBufferBuilder& builder) const;

    const SegmentedProcFSIndex::ProcessSubDirectory m_parent_sub_directory_type;
    union {
        SegmentedProcFSIndex::MainProcessProperty property_type;
        unsigned property_index;
    } m_possible_data;
    mutable Mutex m_refresh_lock;
};
}
