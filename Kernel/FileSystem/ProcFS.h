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
    static ErrorOr<NonnullRefPtr<ProcFS>> try_create();

    virtual ErrorOr<void> initialize() override;
    virtual StringView class_name() const override { return "ProcFS"sv; }

    virtual Inode& root_inode() override;

private:
    ProcFS();

    RefPtr<ProcFSDirectoryInode> m_root_inode;
};

class ProcFSInode : public Inode {
    friend class ProcFS;

public:
    virtual ~ProcFSInode() override;

protected:
    ProcFSInode(const ProcFS&, InodeIndex);

    ProcFS& procfs() { return static_cast<ProcFS&>(Inode::fs()); }
    ProcFS const& procfs() const { return static_cast<ProcFS const&>(Inode::fs()); }

    // ^Inode
    virtual ErrorOr<void> attach(OpenFileDescription& description) override = 0;
    virtual void did_seek(OpenFileDescription&, off_t) override = 0;
    virtual ErrorOr<void> flush_metadata() override final;
    virtual ErrorOr<NonnullRefPtr<Inode>> create_child(StringView name, mode_t, dev_t, UserID, GroupID) override final;
    virtual ErrorOr<void> add_child(Inode&, StringView name, mode_t) override final;
    virtual ErrorOr<void> remove_child(StringView name) override final;
    virtual ErrorOr<void> chmod(mode_t) override final;
    virtual ErrorOr<void> chown(UserID, GroupID) override final;
};

class ProcFSGlobalInode : public ProcFSInode {
    friend class ProcFS;

public:
    static ErrorOr<NonnullRefPtr<ProcFSGlobalInode>> try_create(const ProcFS&, const ProcFSExposedComponent&);
    virtual ~ProcFSGlobalInode() override {};
    StringView name() const;

protected:
    ProcFSGlobalInode(const ProcFS&, const ProcFSExposedComponent&);

    // ^Inode
    virtual ErrorOr<void> attach(OpenFileDescription& description) override final;
    virtual ErrorOr<size_t> read_bytes(off_t, size_t, UserOrKernelBuffer& buffer, OpenFileDescription*) const override final;
    virtual ErrorOr<size_t> write_bytes(off_t, size_t, const UserOrKernelBuffer& buffer, OpenFileDescription*) override final;
    virtual void did_seek(OpenFileDescription&, off_t) override final;
    virtual InodeMetadata metadata() const override;
    virtual ErrorOr<void> traverse_as_directory(Function<ErrorOr<void>(FileSystem::DirectoryEntryView const&)>) const override;
    virtual ErrorOr<NonnullRefPtr<Inode>> lookup(StringView) override;
    virtual ErrorOr<void> truncate(u64) override final;
    virtual ErrorOr<void> set_mtime(time_t) override final;

    NonnullRefPtr<ProcFSExposedComponent> m_associated_component;
};

class ProcFSLinkInode : public ProcFSGlobalInode {
    friend class ProcFS;

public:
    static ErrorOr<NonnullRefPtr<ProcFSLinkInode>> try_create(const ProcFS&, const ProcFSExposedComponent&);

protected:
    ProcFSLinkInode(const ProcFS&, const ProcFSExposedComponent&);
    virtual InodeMetadata metadata() const override;
};

class ProcFSDirectoryInode final : public ProcFSGlobalInode {
    friend class ProcFS;

public:
    static ErrorOr<NonnullRefPtr<ProcFSDirectoryInode>> try_create(const ProcFS&, const ProcFSExposedComponent&);
    virtual ~ProcFSDirectoryInode() override;

protected:
    ProcFSDirectoryInode(const ProcFS&, const ProcFSExposedComponent&);
    // ^Inode
    virtual InodeMetadata metadata() const override;
    virtual ErrorOr<void> traverse_as_directory(Function<ErrorOr<void>(FileSystem::DirectoryEntryView const&)>) const override;
    virtual ErrorOr<NonnullRefPtr<Inode>> lookup(StringView name) override;
};

class ProcFSProcessAssociatedInode : public ProcFSInode {
    friend class ProcFS;

protected:
    ProcFSProcessAssociatedInode(const ProcFS&, ProcessID, InodeIndex);
    ProcessID associated_pid() const { return m_pid; }

    // ^Inode
    virtual ErrorOr<size_t> write_bytes(off_t, size_t, const UserOrKernelBuffer& buffer, OpenFileDescription*) override final;

private:
    const ProcessID m_pid;
};

class ProcFSProcessDirectoryInode final : public ProcFSProcessAssociatedInode {
    friend class ProcFS;

public:
    static ErrorOr<NonnullRefPtr<ProcFSProcessDirectoryInode>> try_create(const ProcFS&, ProcessID);

private:
    ProcFSProcessDirectoryInode(const ProcFS&, ProcessID);
    // ^Inode
    virtual ErrorOr<void> attach(OpenFileDescription& description) override;
    virtual void did_seek(OpenFileDescription&, off_t) override { }
    virtual InodeMetadata metadata() const override;
    virtual ErrorOr<void> traverse_as_directory(Function<ErrorOr<void>(FileSystem::DirectoryEntryView const&)>) const override;
    virtual ErrorOr<size_t> read_bytes(off_t, size_t, UserOrKernelBuffer& buffer, OpenFileDescription*) const override final;
    virtual ErrorOr<NonnullRefPtr<Inode>> lookup(StringView name) override;
};

class ProcFSProcessSubDirectoryInode final : public ProcFSProcessAssociatedInode {
    friend class ProcFS;

public:
    static ErrorOr<NonnullRefPtr<ProcFSProcessSubDirectoryInode>> try_create(const ProcFS&, SegmentedProcFSIndex::ProcessSubDirectory, ProcessID);

private:
    ProcFSProcessSubDirectoryInode(const ProcFS&, SegmentedProcFSIndex::ProcessSubDirectory, ProcessID);
    // ^Inode
    virtual ErrorOr<void> attach(OpenFileDescription& description) override;
    virtual void did_seek(OpenFileDescription&, off_t) override;
    virtual InodeMetadata metadata() const override;
    virtual ErrorOr<void> traverse_as_directory(Function<ErrorOr<void>(FileSystem::DirectoryEntryView const&)>) const override;
    virtual ErrorOr<size_t> read_bytes(off_t, size_t, UserOrKernelBuffer& buffer, OpenFileDescription*) const override final;
    virtual ErrorOr<NonnullRefPtr<Inode>> lookup(StringView name) override;

    const SegmentedProcFSIndex::ProcessSubDirectory m_sub_directory_type;
};

class ProcFSProcessPropertyInode final : public ProcFSProcessAssociatedInode {
    friend class ProcFS;

public:
    static ErrorOr<NonnullRefPtr<ProcFSProcessPropertyInode>> try_create_for_file_description_link(const ProcFS&, unsigned, ProcessID);
    static ErrorOr<NonnullRefPtr<ProcFSProcessPropertyInode>> try_create_for_thread_stack(const ProcFS&, ThreadID, ProcessID);
    static ErrorOr<NonnullRefPtr<ProcFSProcessPropertyInode>> try_create_for_pid_property(const ProcFS&, SegmentedProcFSIndex::MainProcessProperty, ProcessID);

private:
    ProcFSProcessPropertyInode(const ProcFS&, SegmentedProcFSIndex::MainProcessProperty, ProcessID);
    ProcFSProcessPropertyInode(const ProcFS&, ThreadID, ProcessID);
    ProcFSProcessPropertyInode(const ProcFS&, unsigned, ProcessID);
    // ^Inode
    virtual ErrorOr<void> attach(OpenFileDescription& description) override;
    virtual void did_seek(OpenFileDescription&, off_t) override;
    virtual InodeMetadata metadata() const override;
    virtual ErrorOr<void> traverse_as_directory(Function<ErrorOr<void>(FileSystem::DirectoryEntryView const&)>) const override;
    virtual ErrorOr<size_t> read_bytes(off_t, size_t, UserOrKernelBuffer& buffer, OpenFileDescription*) const override final;
    virtual ErrorOr<NonnullRefPtr<Inode>> lookup(StringView name) override final;

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
