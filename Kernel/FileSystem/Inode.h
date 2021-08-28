/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, sin-ack <sin-ack@protonmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

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
#include <Kernel/KResult.h>
#include <Kernel/Library/ListedRefCounted.h>
#include <Kernel/Locking/Mutex.h>

namespace Kernel {

class Inode : public ListedRefCounted<Inode> {
    friend class VirtualFileSystem;
    friend class FileSystem;

public:
    virtual ~Inode();

    virtual void one_ref_left() { }

    FileSystem& fs() { return m_file_system; }
    FileSystem const& fs() const { return m_file_system; }
    unsigned fsid() const { return m_file_system.fsid(); }
    InodeIndex index() const { return m_index; }

    size_t size() const { return metadata().size; }
    bool is_symlink() const { return metadata().is_symlink(); }
    bool is_directory() const { return metadata().is_directory(); }
    bool is_character_device() const { return metadata().is_character_device(); }
    mode_t mode() const { return metadata().mode; }

    InodeIdentifier identifier() const { return { fsid(), index() }; }
    virtual InodeMetadata metadata() const = 0;

    KResultOr<NonnullOwnPtr<KBuffer>> read_entire(FileDescription* = nullptr) const;

    virtual KResult attach(FileDescription&) { return KSuccess; }
    virtual void detach(FileDescription&) { }
    virtual void did_seek(FileDescription&, off_t) { }
    virtual KResultOr<size_t> read_bytes(off_t, size_t, UserOrKernelBuffer& buffer, FileDescription*) const = 0;
    virtual KResult traverse_as_directory(Function<bool(FileSystem::DirectoryEntryView const&)>) const = 0;
    virtual KResultOr<NonnullRefPtr<Inode>> lookup(StringView name) = 0;
    virtual KResultOr<size_t> write_bytes(off_t, size_t, const UserOrKernelBuffer& data, FileDescription*) = 0;
    virtual KResultOr<NonnullRefPtr<Inode>> create_child(StringView name, mode_t, dev_t, UserID, GroupID) = 0;
    virtual KResult add_child(Inode&, const StringView& name, mode_t) = 0;
    virtual KResult remove_child(const StringView& name) = 0;
    virtual KResult chmod(mode_t) = 0;
    virtual KResult chown(UserID, GroupID) = 0;
    virtual KResult truncate(u64) { return KSuccess; }
    virtual KResultOr<NonnullRefPtr<Custody>> resolve_as_link(Custody& base, RefPtr<Custody>* out_parent, int options, int symlink_recursion_level) const;

    virtual KResultOr<int> get_block_address(int) { return ENOTSUP; }

    LocalSocket* socket() { return m_socket.ptr(); }
    const LocalSocket* socket() const { return m_socket.ptr(); }
    bool bind_socket(LocalSocket&);
    bool unbind_socket();

    virtual FileDescription* preopen_fd() { return nullptr; };

    bool is_metadata_dirty() const { return m_metadata_dirty; }

    virtual KResult set_atime(time_t);
    virtual KResult set_ctime(time_t);
    virtual KResult set_mtime(time_t);
    virtual KResult increment_link_count();
    virtual KResult decrement_link_count();

    virtual void flush_metadata() = 0;

    void will_be_destroyed();

    void set_shared_vmobject(Memory::SharedInodeVMObject&);
    RefPtr<Memory::SharedInodeVMObject> shared_vmobject() const;

    static void sync();

    bool has_watchers() const { return !m_watchers.is_empty(); }

    void register_watcher(Badge<InodeWatcher>, InodeWatcher&);
    void unregister_watcher(Badge<InodeWatcher>, InodeWatcher&);

    NonnullRefPtr<FIFO> fifo();

    KResult can_apply_flock(FileDescription const&, flock const&) const;
    KResult apply_flock(Process const&, FileDescription const&, Userspace<flock const*>);
    KResult get_flock(FileDescription const&, Userspace<flock*>) const;
    void remove_flocks_for_description(FileDescription const&);

protected:
    Inode(FileSystem&, InodeIndex);
    void set_metadata_dirty(bool);
    KResult prepare_to_write_data();

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
        short type;
        off_t start;
        off_t len;
        FileDescription const* owner;
        pid_t pid;
    };

    Vector<Flock> m_flocks;

public:
    using AllInstancesList = IntrusiveList<Inode, RawPtr<Inode>, &Inode::m_inode_list_node>;
    static SpinlockProtected<Inode::AllInstancesList>& all_instances();
};

}
