/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#pragma once

#include <AK/String.h>
#include <AK/Function.h>
#include <AK/InlineLinkedList.h>
#include <AK/RefCounted.h>
#include <AK/WeakPtr.h>
#include <Kernel/FileSystem/FileSystem.h>
#include <Kernel/FileSystem/InodeIdentifier.h>
#include <Kernel/FileSystem/InodeMetadata.h>
#include <Kernel/KResult.h>
#include <Kernel/Lock.h>

class FileDescription;
class InodeVMObject;
class InodeWatcher;
class LocalSocket;
class Custody;

class Inode : public RefCounted<Inode>
    , public Weakable<Inode>
    , public InlineLinkedListNode<Inode> {
    friend class VFS;
    friend class FS;

public:
    virtual ~Inode();

    virtual void one_ref_left() {}

    FS& fs() { return m_fs; }
    const FS& fs() const { return m_fs; }
    unsigned fsid() const;
    unsigned index() const { return m_index; }

    size_t size() const { return metadata().size; }
    bool is_symlink() const { return metadata().is_symlink(); }
    bool is_directory() const { return metadata().is_directory(); }
    bool is_character_device() const { return metadata().is_character_device(); }
    mode_t mode() const { return metadata().mode; }

    InodeIdentifier identifier() const { return { fsid(), index() }; }
    virtual InodeMetadata metadata() const = 0;

    ByteBuffer read_entire(FileDescription* = nullptr) const;

    virtual ssize_t read_bytes(off_t, ssize_t, u8* buffer, FileDescription*) const = 0;
    virtual bool traverse_as_directory(Function<bool(const FS::DirectoryEntry&)>) const = 0;
    virtual RefPtr<Inode> lookup(StringView name) = 0;
    virtual ssize_t write_bytes(off_t, ssize_t, const u8* data, FileDescription*) = 0;
    virtual KResult add_child(InodeIdentifier child_id, const StringView& name, mode_t) = 0;
    virtual KResult remove_child(const StringView& name) = 0;
    virtual size_t directory_entry_count() const = 0;
    virtual KResult chmod(mode_t) = 0;
    virtual KResult chown(uid_t, gid_t) = 0;
    virtual KResult truncate(u64) { return KSuccess; }
    virtual KResultOr<NonnullRefPtr<Custody>> resolve_as_link(Custody& base, RefPtr<Custody>* out_parent = nullptr, int options = 0, int symlink_recursion_level = 0) const;

    LocalSocket* socket() { return m_socket.ptr(); }
    const LocalSocket* socket() const { return m_socket.ptr(); }
    bool bind_socket(LocalSocket&);
    bool unbind_socket();

    virtual FileDescription* preopen_fd() { return nullptr; };

    bool is_metadata_dirty() const { return m_metadata_dirty; }

    virtual int set_atime(time_t);
    virtual int set_ctime(time_t);
    virtual int set_mtime(time_t);
    virtual KResult increment_link_count();
    virtual KResult decrement_link_count();

    virtual void flush_metadata() = 0;

    void will_be_destroyed();

    void set_vmobject(VMObject&);
    InodeVMObject* vmobject() { return m_vmobject.ptr(); }
    const InodeVMObject* vmobject() const { return m_vmobject.ptr(); }

    static void sync();

    bool has_watchers() const { return !m_watchers.is_empty(); }

    void register_watcher(Badge<InodeWatcher>, InodeWatcher&);
    void unregister_watcher(Badge<InodeWatcher>, InodeWatcher&);

    // For InlineLinkedListNode.
    Inode* m_next { nullptr };
    Inode* m_prev { nullptr };

protected:
    Inode(FS& fs, unsigned index);
    void set_metadata_dirty(bool);
    void inode_contents_changed(off_t, ssize_t, const u8*);
    void inode_size_changed(size_t old_size, size_t new_size);

    mutable Lock m_lock { "Inode" };

private:
    FS& m_fs;
    unsigned m_index { 0 };
    WeakPtr<InodeVMObject> m_vmobject;
    RefPtr<LocalSocket> m_socket;
    HashTable<InodeWatcher*> m_watchers;
    bool m_metadata_dirty { false };
};
