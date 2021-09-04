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
    static KResultOr<NonnullRefPtr<ProcFS>> try_create();

    virtual KResult initialize() override;
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
    ProcFSInode(ProcFS const&, InodeIndex);

    ProcFS& procfs() { return static_cast<ProcFS&>(Inode::fs()); }
    ProcFS const& procfs() const { return static_cast<ProcFS const&>(Inode::fs()); }

    // ^Inode
    virtual KResult attach(FileDescription& description) = 0;
    virtual void did_seek(FileDescription&, off_t) = 0;
    virtual void flush_metadata() override final;
    virtual KResultOr<NonnullRefPtr<Inode>> create_child(StringView name, mode_t, dev_t, UserID, GroupID) override final;
    virtual KResult add_child(Inode&, StringView const& name, mode_t) override final;
    virtual KResult remove_child(StringView const& name) override final;
    virtual KResult chmod(mode_t) override final;
    virtual KResult chown(UserID, GroupID) override final;
    virtual KResult truncate(u64) override final;
};

class ProcFSGlobalInode : public ProcFSInode {
    friend class ProcFS;

public:
    static KResultOr<NonnullRefPtr<ProcFSGlobalInode>> try_create(ProcFS const&, ProcFSExposedComponent const&);
    virtual ~ProcFSGlobalInode() override {};
    StringView name() const;

protected:
    ProcFSGlobalInode(ProcFS const&, ProcFSExposedComponent const&);

    // ^Inode
    virtual KResult attach(FileDescription& description) override final;
    virtual KResultOr<size_t> read_bytes(off_t, size_t, UserOrKernelBuffer& buffer, FileDescription*) const override final;
    virtual KResultOr<size_t> write_bytes(off_t, size_t, UserOrKernelBuffer const& buffer, FileDescription*) override final;
    virtual void did_seek(FileDescription&, off_t) override final;
    virtual InodeMetadata metadata() const override;
    virtual KResult traverse_as_directory(Function<bool(FileSystem::DirectoryEntryView const&)>) const override;
    virtual KResultOr<NonnullRefPtr<Inode>> lookup(StringView) override;

    NonnullRefPtr<ProcFSExposedComponent> m_associated_component;
};

class ProcFSLinkInode : public ProcFSGlobalInode {
    friend class ProcFS;

public:
    static KResultOr<NonnullRefPtr<ProcFSLinkInode>> try_create(ProcFS const&, ProcFSExposedComponent const&);

protected:
    ProcFSLinkInode(ProcFS const&, ProcFSExposedComponent const&);
    virtual InodeMetadata metadata() const override;
};

class ProcFSDirectoryInode final : public ProcFSGlobalInode {
    friend class ProcFS;

public:
    static KResultOr<NonnullRefPtr<ProcFSDirectoryInode>> try_create(ProcFS const&, ProcFSExposedComponent const&);
    virtual ~ProcFSDirectoryInode() override;

protected:
    ProcFSDirectoryInode(ProcFS const&, ProcFSExposedComponent const&);
    // ^Inode
    virtual InodeMetadata metadata() const override;
    virtual KResult traverse_as_directory(Function<bool(FileSystem::DirectoryEntryView const&)>) const override;
    virtual KResultOr<NonnullRefPtr<Inode>> lookup(StringView name) override;
};

class ProcFSProcessAssociatedInode : public ProcFSInode {
    friend class ProcFS;

protected:
    ProcFSProcessAssociatedInode(ProcFS const&, ProcessID, InodeIndex);
    ProcessID associated_pid() const { return m_pid; }

    // ^Inode
    virtual KResultOr<size_t> write_bytes(off_t, size_t, UserOrKernelBuffer const& buffer, FileDescription*) override final;

private:
    const ProcessID m_pid;
};

class ProcFSProcessDirectoryInode final : public ProcFSProcessAssociatedInode {
    friend class ProcFS;

public:
    static KResultOr<NonnullRefPtr<ProcFSProcessDirectoryInode>> try_create(ProcFS const&, ProcessID);

private:
    ProcFSProcessDirectoryInode(ProcFS const&, ProcessID);
    // ^Inode
    virtual KResult attach(FileDescription& description) override;
    virtual void did_seek(FileDescription&, off_t) override { }
    virtual InodeMetadata metadata() const override;
    virtual KResult traverse_as_directory(Function<bool(FileSystem::DirectoryEntryView const&)>) const override;
    virtual KResultOr<size_t> read_bytes(off_t, size_t, UserOrKernelBuffer& buffer, FileDescription*) const override final;
    virtual KResultOr<NonnullRefPtr<Inode>> lookup(StringView name) override;
};

class ProcFSProcessSubDirectoryInode final : public ProcFSProcessAssociatedInode {
    friend class ProcFS;

public:
    static KResultOr<NonnullRefPtr<ProcFSProcessSubDirectoryInode>> try_create(ProcFS const&, SegmentedProcFSIndex::ProcessSubDirectory, ProcessID);

private:
    ProcFSProcessSubDirectoryInode(ProcFS const&, SegmentedProcFSIndex::ProcessSubDirectory, ProcessID);
    // ^Inode
    virtual KResult attach(FileDescription& description) override;
    virtual void did_seek(FileDescription&, off_t) override;
    virtual InodeMetadata metadata() const override;
    virtual KResult traverse_as_directory(Function<bool(FileSystem::DirectoryEntryView const&)>) const override;
    virtual KResultOr<size_t> read_bytes(off_t, size_t, UserOrKernelBuffer& buffer, FileDescription*) const override final;
    virtual KResultOr<NonnullRefPtr<Inode>> lookup(StringView name) override;

    const SegmentedProcFSIndex::ProcessSubDirectory m_sub_directory_type;
};

class ProcFSProcessPropertyInode final : public ProcFSProcessAssociatedInode {
    friend class ProcFS;

public:
    static KResultOr<NonnullRefPtr<ProcFSProcessPropertyInode>> try_create_for_file_description_link(ProcFS const&, unsigned, ProcessID);
    static KResultOr<NonnullRefPtr<ProcFSProcessPropertyInode>> try_create_for_thread_stack(ProcFS const&, ThreadID, ProcessID);
    static KResultOr<NonnullRefPtr<ProcFSProcessPropertyInode>> try_create_for_pid_property(ProcFS const&, SegmentedProcFSIndex::MainProcessProperty, ProcessID);

private:
    ProcFSProcessPropertyInode(ProcFS const&, SegmentedProcFSIndex::MainProcessProperty, ProcessID);
    ProcFSProcessPropertyInode(ProcFS const&, ThreadID, ProcessID);
    ProcFSProcessPropertyInode(ProcFS const&, unsigned, ProcessID);
    // ^Inode
    virtual KResult attach(FileDescription& description) override;
    virtual void did_seek(FileDescription&, off_t) override;
    virtual InodeMetadata metadata() const override;
    virtual KResult traverse_as_directory(Function<bool(FileSystem::DirectoryEntryView const&)>) const override;
    virtual KResultOr<size_t> read_bytes(off_t, size_t, UserOrKernelBuffer& buffer, FileDescription*) const override final;
    virtual KResultOr<NonnullRefPtr<Inode>> lookup(StringView name) override final;

    KResult refresh_data(FileDescription& description);
    KResult try_to_acquire_data(Process& process, KBufferBuilder& builder) const;

    const SegmentedProcFSIndex::ProcessSubDirectory m_parent_sub_directory_type;
    union {
        SegmentedProcFSIndex::MainProcessProperty property_type;
        unsigned property_index;
    } m_possible_data;
    mutable Mutex m_refresh_lock;
};
}
