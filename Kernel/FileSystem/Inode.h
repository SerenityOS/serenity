/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, sin-ack <sin-ack@protonmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Error.h>
#include <AK/Function.h>
#include <AK/HashTable.h>
#include <AK/IntrusiveList.h>
#include <Kernel/FileSystem/CustodyBase.h>
#include <Kernel/FileSystem/FIFO.h>
#include <Kernel/FileSystem/FileSystem.h>
#include <Kernel/FileSystem/InodeIdentifier.h>
#include <Kernel/FileSystem/InodeMetadata.h>
#include <Kernel/Forward.h>
#include <Kernel/Library/ListedRefCounted.h>
#include <Kernel/Library/LockWeakPtr.h>
#include <Kernel/Locking/Mutex.h>
#include <Kernel/Memory/SharedInodeVMObject.h>

namespace Kernel {

enum class ShouldBlock {
    No = 0,
    Yes = 1
};

class Inode : public ListedRefCounted<Inode, LockType::Spinlock>
    , public LockWeakable<Inode> {
    friend class FileSystem;
    friend class InodeFile;

public:
    virtual ~Inode();

    virtual void remove_from_secondary_lists() { }

    FileSystem& fs() { return m_file_system; }
    FileSystem const& fs() const { return m_file_system; }
    FileSystemID fsid() const { return m_file_system.fsid(); }
    InodeIndex index() const { return m_index; }

    size_t size() const { return metadata().size; }
    bool is_symlink() const { return metadata().is_symlink(); }
    bool is_directory() const { return metadata().is_directory(); }
    bool is_character_device() const { return metadata().is_character_device(); }
    mode_t mode() const { return metadata().mode; }

    InodeIdentifier identifier() const { return { fsid(), index() }; }
    virtual InodeMetadata metadata() const = 0;

    ErrorOr<size_t> write_bytes(off_t, size_t, UserOrKernelBuffer const& data, OpenFileDescription*);
    ErrorOr<size_t> read_bytes(off_t, size_t, UserOrKernelBuffer& buffer, OpenFileDescription*) const;
    ErrorOr<size_t> read_until_filled_or_end(off_t, size_t, UserOrKernelBuffer buffer, OpenFileDescription*) const;
    ErrorOr<void> truncate(u64);

    virtual ErrorOr<void> attach(OpenFileDescription&) { return {}; }
    virtual void detach(OpenFileDescription&) { }
    virtual void did_seek(OpenFileDescription&, off_t) { }
    virtual ErrorOr<void> traverse_as_directory(Function<ErrorOr<void>(FileSystem::DirectoryEntryView const&)>) const = 0;
    virtual ErrorOr<NonnullRefPtr<Inode>> lookup(StringView name) = 0;
    virtual ErrorOr<NonnullRefPtr<Inode>> create_child(StringView name, mode_t, dev_t, UserID, GroupID) = 0;
    virtual ErrorOr<void> add_child(Inode&, StringView name, mode_t) = 0;
    virtual ErrorOr<void> remove_child(StringView name) = 0;
    virtual ErrorOr<void> chmod(mode_t) = 0;
    virtual ErrorOr<void> chown(UserID, GroupID) = 0;

    ErrorOr<NonnullRefPtr<Custody>> resolve_as_link(VFSRootContext const&, Credentials const&, CustodyBase const& base, RefPtr<Custody>* out_parent, int options, int symlink_recursion_level) const;

    virtual ErrorOr<int> get_block_address(int) { return ENOTSUP; }

    LockRefPtr<LocalSocket> bound_socket() const;
    bool bind_socket(LocalSocket&);
    bool unbind_socket();

    bool is_metadata_dirty() const { return m_metadata_dirty; }

    virtual ErrorOr<void> update_timestamps(Optional<UnixDateTime> atime, Optional<UnixDateTime> ctime, Optional<UnixDateTime> mtime);
    virtual ErrorOr<void> increment_link_count();
    virtual ErrorOr<void> decrement_link_count();

    virtual ErrorOr<void> flush_metadata() = 0;

    void will_be_destroyed();

    ErrorOr<void> set_shared_vmobject(Memory::SharedInodeVMObject&);
    LockRefPtr<Memory::SharedInodeVMObject> shared_vmobject() const;

    static void sync_all();
    void sync();

    bool has_watchers() const;

    ErrorOr<void> register_watcher(Badge<InodeWatcher>, InodeWatcher&);
    void unregister_watcher(Badge<InodeWatcher>, InodeWatcher&);

    ErrorOr<NonnullRefPtr<FIFO>> fifo();

    bool can_apply_flock(flock const&, Optional<OpenFileDescription const&> = {}) const;
    ErrorOr<void> apply_flock(Process const&, OpenFileDescription const&, Userspace<flock const*>, ShouldBlock);
    ErrorOr<void> get_flock(OpenFileDescription const&, Userspace<flock*>) const;
    void remove_flocks_for_description(OpenFileDescription const&);
    Thread::FlockBlockerSet& flock_blocker_set() { return m_flock_blocker_set; }

protected:
    Inode(FileSystem&, InodeIndex);
    void set_metadata_dirty(bool);
    ErrorOr<void> prepare_to_write_data();

    void did_add_child(InodeIdentifier child_id, StringView);
    void did_remove_child(InodeIdentifier child_id, StringView);
    void did_modify_contents();
    void did_delete_self();

    mutable Mutex m_inode_lock { "Inode"sv };

    ErrorOr<size_t> prepare_and_write_bytes_locked(off_t, size_t, UserOrKernelBuffer const& data, OpenFileDescription*);

    virtual ErrorOr<size_t> write_bytes_locked(off_t, size_t, UserOrKernelBuffer const& data, OpenFileDescription*) = 0;
    virtual ErrorOr<size_t> read_bytes_locked(off_t, size_t, UserOrKernelBuffer& buffer, OpenFileDescription*) const = 0;
    virtual ErrorOr<void> truncate_locked(u64) { return {}; }

private:
    ErrorOr<bool> try_apply_flock(Process const&, OpenFileDescription const&, flock const&);

    FileSystem& m_file_system;
    InodeIndex m_index { 0 };
    LockWeakPtr<Memory::SharedInodeVMObject> m_shared_vmobject;
    LockWeakPtr<LocalSocket> m_bound_socket;
    SpinlockProtected<HashTable<InodeWatcher*>, LockRank::None> m_watchers {};
    bool m_metadata_dirty { false };
    RefPtr<FIFO> m_fifo;
    IntrusiveListNode<Inode> m_inode_list_node;

    struct Flock {
        off_t start;
        off_t len;
        OpenFileDescription const* owner;
        pid_t pid;
        short type;
    };

    Thread::FlockBlockerSet m_flock_blocker_set;
    SpinlockProtected<Vector<Flock>, LockRank::None> m_flocks {};

public:
    using AllInstancesList = IntrusiveList<&Inode::m_inode_list_node>;
    static SpinlockProtected<Inode::AllInstancesList, LockRank::None>& all_instances();
};

}
