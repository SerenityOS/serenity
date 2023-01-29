/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/FileSystem/Inode.h>
#include <Kernel/FileSystem/OpenFileDescription.h>
#include <Kernel/FileSystem/ProcFS/FileSystem.h>
#include <Kernel/ProcessExposed.h>
#include <Kernel/UnixTypes.h>

namespace Kernel {

class ProcFSExposedDirectory;
class ProcFSExposedLink;
class ProcFSInode final : public Inode {
    friend class ProcFS;

public:
    enum class Type {
        GlobalLink,
        GlobalDirectory,
        FileDescriptionLink,
        ThreadStack,
        ProcessProperty,
        ChildProcessLink,
        ProcessDirectory,
        ProcessSubdirectory,
    };

    static ErrorOr<NonnullLockRefPtr<ProcFSInode>> try_create_as_file_description_link_inode(ProcFS const&, unsigned, ProcessID);
    static ErrorOr<NonnullLockRefPtr<ProcFSInode>> try_create_as_thread_stack_inode(ProcFS const&, ThreadID, ProcessID);
    static ErrorOr<NonnullLockRefPtr<ProcFSInode>> try_create_as_pid_property_inode(ProcFS const&, SegmentedProcFSIndex::MainProcessProperty, ProcessID);
    static ErrorOr<NonnullLockRefPtr<ProcFSInode>> try_create_as_child_process_link_inode(ProcFS const&, ProcessID, ProcessID);

    static ErrorOr<NonnullLockRefPtr<ProcFSInode>> try_create_as_process_directory_inode(ProcFS const&, ProcessID);
    static ErrorOr<NonnullLockRefPtr<ProcFSInode>> try_create_as_process_subdirectory_inode(ProcFS const&, SegmentedProcFSIndex::ProcessSubDirectory, ProcessID);

    static ErrorOr<NonnullLockRefPtr<ProcFSInode>> try_create_as_directory_inode(ProcFS const&, ProcFSExposedDirectory const&);
    static ErrorOr<NonnullLockRefPtr<ProcFSInode>> try_create_as_global_link_inode(ProcFS const&, ProcFSExposedLink const&);

    virtual ~ProcFSInode() override;

private:
    // ProcFS PID property inode (/proc/PID/PROPERTY)
    ProcFSInode(ProcFS const&, SegmentedProcFSIndex::MainProcessProperty, ProcessID);
    // ProcFS Thread stack inode (/proc/PID/stacks/TID)
    ProcFSInode(ProcFS const&, ThreadID, ProcessID);
    // ProcFS File description link inode (/proc/PID/fd/FD)
    ProcFSInode(ProcFS const&, unsigned, ProcessID);
    // ProcFS Child process link inode (/proc/PID/children/CHILD_PID)
    ProcFSInode(ProcFS const&, ProcessID, ProcessID);
    // ProcFS Process directory inode (/proc/PID/)
    ProcFSInode(ProcFS const&, ProcessID);
    // ProcFS Process sub directory inode (/proc/PID/SUBDIRECTORY)
    ProcFSInode(ProcFS const&, SegmentedProcFSIndex::ProcessSubDirectory, ProcessID);

    ProcFSInode(ProcFS const&, ProcFSExposedLink const&);
    ProcFSInode(ProcFS const&, ProcFSExposedDirectory const&);

    ProcFS& procfs() { return static_cast<ProcFS&>(Inode::fs()); }
    ProcFS const& procfs() const { return static_cast<ProcFS const&>(Inode::fs()); }

    // ^Inode
    virtual ErrorOr<void> attach(OpenFileDescription& description) override;
    virtual void did_seek(OpenFileDescription&, off_t) override;
    virtual ErrorOr<void> flush_metadata() override final;
    virtual ErrorOr<NonnullLockRefPtr<Inode>> create_child(StringView name, mode_t, dev_t, UserID, GroupID) override;
    virtual ErrorOr<void> add_child(Inode&, StringView name, mode_t) override final;
    virtual ErrorOr<void> remove_child(StringView name) override final;
    virtual ErrorOr<void> replace_child(StringView name, Inode& child) override final;
    virtual ErrorOr<void> chmod(mode_t) override final;
    virtual ErrorOr<void> chown(UserID, GroupID) override final;
    virtual InodeMetadata metadata() const override;
    virtual ErrorOr<void> traverse_as_directory(Function<ErrorOr<void>(FileSystem::DirectoryEntryView const&)>) const override;
    virtual ErrorOr<size_t> read_bytes_locked(off_t, size_t, UserOrKernelBuffer& buffer, OpenFileDescription*) const override;
    virtual ErrorOr<size_t> write_bytes_locked(off_t, size_t, UserOrKernelBuffer const& buffer, OpenFileDescription*) override;
    virtual ErrorOr<NonnullLockRefPtr<Inode>> lookup(StringView name) override final;
    virtual ErrorOr<void> truncate(u64) override final;
    virtual ErrorOr<void> update_timestamps(Optional<Time> atime, Optional<Time> ctime, Optional<Time> mtime) override;

    ErrorOr<void> refresh_process_property_data(OpenFileDescription& description);
    ErrorOr<void> try_fetch_process_property_data(NonnullLockRefPtr<Process>, KBufferBuilder& builder) const;

    Type m_type;

    union {
        SegmentedProcFSIndex::MainProcessProperty property_type;
        unsigned property_index;
    } m_possible_data;
    Optional<SegmentedProcFSIndex::ProcessSubDirectory> const m_parent_subdirectory_type {};
    Optional<SegmentedProcFSIndex::ProcessSubDirectory> const m_subdirectory_type {};

    Optional<ProcessID> const m_associated_pid {};

    LockRefPtr<ProcFSExposedComponent> m_associated_component;
    mutable Mutex m_refresh_lock;
};

}
