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
#include <AK/String.h>
#include <AK/WeakPtr.h>
#include <Kernel/FileSystem/FIFO.h>
#include <Kernel/FileSystem/FileSystem.h>
#include <Kernel/FileSystem/InodeIdentifier.h>
#include <Kernel/FileSystem/InodeMetadata.h>
#include <Kernel/Forward.h>
#include <Kernel/Library/ListedRefCounted.h>
#include <Kernel/Locking/Mutex.h>

namespace Kernel {

class Inode : public ListedRefCounted<Inode>
    , public Weakable<Inode> {
    friend class VirtualFileSystem;
    friend class FileSystem;

public:
    virtual ~Inode();

    virtual void one_ref_left() { }

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

    ErrorOr<NonnullOwnPtr<KBuffer>> read_entire(OpenFileDescription* = nullptr) const;

    virtual ErrorOr<void> attach(OpenFileDescription&) { return {}; }
    virtual void detach(OpenFileDescription&) { }
    virtual void did_seek(OpenFileDescription&, off_t) { }
    virtual ErrorOr<size_t> read_bytes(off_t, size_t, UserOrKernelBuffer& buffer, OpenFileDescription*) const = 0;
    virtual ErrorOr<void> traverse_as_directory(Function<ErrorOr<void>(FileSystem::DirectoryEntryView const&)>) const = 0;
    virtual ErrorOr<NonnullRefPtr<Inode>> lookup(StringView name) = 0;
    virtual ErrorOr<size_t> write_bytes(off_t, size_t, const UserOrKernelBuffer& data, OpenFileDescription*) = 0;
    virtual ErrorOr<NonnullRefPtr<Inode>> create_child(StringView name, mode_t, dev_t, UserID, GroupID) = 0;
    virtual ErrorOr<void> add_child(Inode&, StringView name, mode_t) = 0;
    virtual ErrorOr<void> remove_child(StringView name) = 0;
    virtual ErrorOr<void> chmod(mode_t) = 0;
    virtual ErrorOr<void> chown(UserID, GroupID) = 0;
    virtual ErrorOr<void> truncate(u64) { return {}; }
    virtual ErrorOr<NonnullRefPtr<Custody>> resolve_as_link(Custody& base, RefPtr<Custody>* out_parent, int options, int symlink_recursion_level) const;

    virtual ErrorOr<int> get_block_address(int) { return ENOTSUP; }

    LocalSocket* socket() { return m_socket.ptr(); }
    const LocalSocket* socket() const { return m_socket.ptr(); }
    bool bind_socket(LocalSocket&);
    bool unbind_socket();

    bool is_metadata_dirty() const { return m_metadata_dirty; }

    virtual ErrorOr<void> set_atime(time_t);
    virtual ErrorOr<void> set_ctime(time_t);
    virtual ErrorOr<void> set_mtime(time_t);
    virtual ErrorOr<void> increment_link_count();
    virtual ErrorOr<void> decrement_link_count();

    virtual ErrorOr<void> flush_metadata() = 0;

    void will_be_destroyed();

    void set_shared_vmobject(Memory::SharedInodeVMObject&);
    RefPtr<Memory::SharedInodeVMObject> shared_vmobject() const;

    static void sync_all();
    void sync();

    bool has_watchers() const { return !m_watchers.is_empty(); }

    void register_watcher(Badge<InodeWatcher>, InodeWatcher&);
    void unregister_watcher(Badge<InodeWatcher>, InodeWatcher&);

    ErrorOr<NonnullRefPtr<FIFO>> fifo();

    ErrorOr<void> can_apply_flock(OpenFileDescription const&, flock const&) const;
    ErrorOr<void> apply_flock(Process const&, OpenFileDescription const&, Userspace<flock const*>);
    ErrorOr<void> get_flock(OpenFileDescription const&, Userspace<flock*>) const;
    void remove_flocks_for_description(OpenFileDescription const&);

protected:
    Inode(FileSystem&, InodeIndex);
    void set_metadata_dirty(bool);
    ErrorOr<void> prepare_to_write_data();

    void did_add_child(InodeIdentifier const& child_id, String const& name);
    void did_remove_child(InodeIdentifier const& child_id, String const& name);
    void did_modify_contents();
    void did_delete_self();

    mutable Mutex m_inode_lock { "Inode" };

private:
    FileSystem& m_file_system;
    InodeIndex m_index { 0 };
    WeakPtr<Memory::SharedInodeVMObject> m_shared_vmobject;
    RefPtr<LocalSocket> m_socket;
    HashTable<InodeWatcher*> m_watchers;
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

    Vector<Flock> m_flocks;

public:
    using AllInstancesList = IntrusiveList<&Inode::m_inode_list_node>;
    static SpinlockProtected<Inode::AllInstancesList>& all_instances();
};

}
