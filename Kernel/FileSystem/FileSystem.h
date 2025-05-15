/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/AtomicRefCounted.h>
#include <AK/Error.h>
#include <AK/StringView.h>
#include <Kernel/FileSystem/InodeIdentifier.h>
#include <Kernel/Forward.h>
#include <Kernel/Library/LockRefPtr.h>
#include <Kernel/Library/UserOrKernelBuffer.h>
#include <Kernel/Locking/Mutex.h>
#include <Kernel/UnixTypes.h>

namespace Kernel {

class FileSystem : public AtomicRefCounted<FileSystem> {
    friend class Inode;

public:
    virtual ~FileSystem();

    FileSystemID fsid() const { return m_fsid; }
    static void sync();

    virtual ErrorOr<void> initialize() = 0;
    virtual StringView class_name() const = 0;
    virtual Inode& root_inode() = 0;
    virtual bool supports_watchers() const { return false; }

    virtual ErrorOr<void> rename(Inode& old_parent_inode, StringView old_basename, Inode& new_parent_inode, StringView new_basename) = 0;

    // FIXME: We should aim to provide more concise mechanism to ensure
    // that backing Inodes from the FileSystem are kept intact so we can
    // attach them to a loop device.
    virtual bool supports_backing_loop_devices() const { return false; }

    bool is_readonly() const { return m_readonly; }

    virtual unsigned total_block_count() const { return 0; }
    virtual unsigned free_block_count() const { return 0; }
    virtual unsigned total_inode_count() const { return 0; }
    virtual unsigned free_inode_count() const { return 0; }

    ErrorOr<void> prepare_to_unmount(Inode& mount_guest_inode);

    struct DirectoryEntryView {
        DirectoryEntryView(StringView name, InodeIdentifier, u8 file_type);

        StringView name;
        InodeIdentifier inode;
        u8 file_type { 0 };
    };

    virtual ErrorOr<void> flush_writes() { return {}; }

    u64 logical_block_size() const { return m_logical_block_size; }
    size_t fragment_size() const { return m_fragment_size; }

    virtual bool is_file_backed() const { return false; }

    // Converts file types that are used internally by the filesystem to DT_* types
    virtual u8 internal_file_type_to_directory_entry_type(DirectoryEntryView const& entry) const { return entry.file_type; }

    SpinlockProtected<size_t, LockRank::FileSystem>& mounted_count() { return m_attach_count; }

protected:
    FileSystem();

    void set_logical_block_size(u64 size) { m_logical_block_size = size; }
    void set_fragment_size(size_t size) { m_fragment_size = size; }

    virtual ErrorOr<void> prepare_to_clear_last_mount([[maybe_unused]] Inode& mount_guest_inode) { return {}; }

    mutable Mutex m_lock { "FS"sv };

private:
    FileSystemID m_fsid;
    u64 m_logical_block_size { 0 };
    size_t m_fragment_size { 0 };
    bool m_readonly { false };

    SpinlockProtected<size_t, LockRank::FileSystem> m_attach_count { 0 };
    IntrusiveListNode<FileSystem> m_file_system_node;

public:
    using List = IntrusiveList<&FileSystem::m_file_system_node>;

    // NOTE: This method is implemented in Kernel/FileSystem/VirtualFileSystem.cpp
    static SpinlockProtected<FileSystem::List, LockRank::FileSystem>& all_file_systems_list();
};

}

namespace AK {

template<>
struct Traits<Kernel::InodeIdentifier> : public DefaultTraits<Kernel::InodeIdentifier> {
    static unsigned hash(Kernel::InodeIdentifier const& inode) { return pair_int_hash(inode.fsid().value(), inode.index().value()); }
};

}
