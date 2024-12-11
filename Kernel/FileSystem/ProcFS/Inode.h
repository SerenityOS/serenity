/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022-2023, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/FileSystem/Inode.h>
#include <Kernel/FileSystem/OpenFileDescription.h>
#include <Kernel/FileSystem/ProcFS/Definitions.h>
#include <Kernel/FileSystem/ProcFS/FileSystem.h>
#include <Kernel/Library/KBufferBuilder.h>
#include <Kernel/UnixTypes.h>

namespace Kernel {
struct ProcFSInodeData : public OpenFileDescriptionData {
    OwnPtr<KBuffer> buffer;
};

class ProcFSInode final : public Inode {
    friend class ProcFS;

public:
    enum class Type {
        RootDirectory,
        SelfProcessLink,
        ProcessProperty,
        ProcessDirectory,
        ProcessSubdirectory,
    };

    static InodeIndex create_index_from_global_directory_entry(segmented_global_inode_index entry);
    static InodeIndex create_index_from_process_directory_entry(ProcessID pid, segmented_process_directory_entry entry);

    virtual ~ProcFSInode() override;

private:
    ProcFSInode(ProcFS const&, InodeIndex);

    ProcFS& procfs() { return static_cast<ProcFS&>(Inode::fs()); }
    ProcFS const& procfs() const { return static_cast<ProcFS const&>(Inode::fs()); }

    // ^Inode (EROFS handling)
    virtual ErrorOr<NonnullRefPtr<Inode>> create_child(StringView, mode_t, dev_t, UserID, GroupID) override { return EROFS; }
    virtual ErrorOr<void> add_child(Inode&, StringView, mode_t) override { return EROFS; }
    virtual ErrorOr<void> remove_child(StringView) override { return EROFS; }
    virtual ErrorOr<void> chmod(mode_t) override { return EROFS; }
    virtual ErrorOr<void> chown(UserID, GroupID) override { return EROFS; }
    virtual ErrorOr<size_t> write_bytes_locked(off_t, size_t, UserOrKernelBuffer const&, OpenFileDescription*) override { return EROFS; }
    virtual ErrorOr<void> truncate_locked(u64) override { return EROFS; }

    // ^Inode (Silent ignore handling)
    virtual ErrorOr<void> flush_metadata() override { return {}; }
    virtual ErrorOr<void> update_timestamps(Optional<UnixDateTime>, Optional<UnixDateTime>, Optional<UnixDateTime>) override { return {}; }

    // ^Inode
    virtual ErrorOr<void> attach(OpenFileDescription& description) override;
    virtual void did_seek(OpenFileDescription&, off_t) override;
    ErrorOr<void> traverse_as_root_directory(Function<ErrorOr<void>(FileSystem::DirectoryEntryView const&)>) const;
    virtual ErrorOr<void> traverse_as_directory(Function<ErrorOr<void>(FileSystem::DirectoryEntryView const&)>) const override;

    virtual InodeMetadata metadata() const override;
    virtual ErrorOr<size_t> read_bytes_locked(off_t, size_t, UserOrKernelBuffer& buffer, OpenFileDescription*) const override;

    ErrorOr<NonnullRefPtr<Inode>> lookup_as_root_directory(StringView name);
    virtual ErrorOr<NonnullRefPtr<Inode>> lookup(StringView name) override final;

    ErrorOr<void> refresh_process_property_data(OpenFileDescription& description);
    ErrorOr<void> try_fetch_process_property_data(NonnullRefPtr<Process>, KBufferBuilder& builder) const;

    Type m_type;
    Optional<ProcessID> const m_associated_pid {};
    u16 const m_subdirectory { 0 };
    u32 const m_property { 0 };

    mutable Mutex m_refresh_lock;
};

}
