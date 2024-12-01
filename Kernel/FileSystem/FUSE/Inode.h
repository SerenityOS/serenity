/*
 * Copyright (c) 2024, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>
#include <Kernel/FileSystem/FUSE/FileSystem.h>
#include <Kernel/FileSystem/Inode.h>

namespace Kernel {

class FUSEInode final : public Inode {
    friend class FUSE;

public:
    virtual ~FUSEInode() override;

    FUSE& fs() { return static_cast<FUSE&>(Inode::fs()); }
    FUSE const& fs() const { return static_cast<FUSE const&>(Inode::fs()); }

private:
    FUSEInode(FUSE&, InodeIndex);
    FUSEInode(FUSE&);

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
    virtual ErrorOr<void> truncate_locked(u64) override;
    virtual ErrorOr<void> update_timestamps(Optional<UnixDateTime> atime, Optional<UnixDateTime> ctime, Optional<UnixDateTime> mtime) override;

    ErrorOr<u64> try_open(bool directory, u32 flags) const;
    ErrorOr<void> try_flush(u64 id) const;
    ErrorOr<void> try_release(u64 id, bool directory) const;

    InodeMetadata m_metadata;
};

}
