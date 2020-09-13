/*
 * Copyright (c) 2019-2020, Sergey Bugaev <bugaevc@serenityos.org>
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

#include <AK/HashMap.h>
#include <AK/Optional.h>
#include <Kernel/FileSystem/FileSystem.h>
#include <Kernel/FileSystem/Inode.h>
#include <Kernel/KBuffer.h>

namespace Kernel {

class TmpFSInode;

class TmpFS final : public FS {
    friend class TmpFSInode;

public:
    virtual ~TmpFS() override;
    static NonnullRefPtr<TmpFS> create();
    virtual bool initialize() override;

    virtual const char* class_name() const override { return "TmpFS"; }

    virtual bool supports_watchers() const override { return true; }

    virtual NonnullRefPtr<Inode> root_inode() const override;

private:
    TmpFS();

    RefPtr<TmpFSInode> m_root_inode;

    HashMap<unsigned, NonnullRefPtr<TmpFSInode>> m_inodes;
    RefPtr<Inode> get_inode(InodeIdentifier identifier) const;
    void register_inode(TmpFSInode&);
    void unregister_inode(InodeIdentifier);

    unsigned m_next_inode_index { 1 };
    unsigned next_inode_index();
};

class TmpFSInode final : public Inode {
    friend class TmpFS;

public:
    virtual ~TmpFSInode() override;

    TmpFS& fs() { return static_cast<TmpFS&>(Inode::fs()); }
    const TmpFS& fs() const { return static_cast<const TmpFS&>(Inode::fs()); }

    // ^Inode
    virtual ssize_t read_bytes(off_t, ssize_t, UserOrKernelBuffer& buffer, FileDescription*) const override;
    virtual InodeMetadata metadata() const override;
    virtual KResult traverse_as_directory(Function<bool(const FS::DirectoryEntryView&)>) const override;
    virtual RefPtr<Inode> lookup(StringView name) override;
    virtual void flush_metadata() override;
    virtual ssize_t write_bytes(off_t, ssize_t, const UserOrKernelBuffer& buffer, FileDescription*) override;
    virtual KResultOr<NonnullRefPtr<Inode>> create_child(const String& name, mode_t, dev_t, uid_t, gid_t) override;
    virtual KResult add_child(Inode&, const StringView& name, mode_t) override;
    virtual KResult remove_child(const StringView& name) override;
    virtual KResultOr<size_t> directory_entry_count() const override;
    virtual KResult chmod(mode_t) override;
    virtual KResult chown(uid_t, gid_t) override;
    virtual KResult truncate(u64) override;
    virtual int set_atime(time_t) override;
    virtual int set_ctime(time_t) override;
    virtual int set_mtime(time_t) override;
    virtual void one_ref_left() override;

private:
    TmpFSInode(TmpFS& fs, InodeMetadata metadata, InodeIdentifier parent);
    static NonnullRefPtr<TmpFSInode> create(TmpFS&, InodeMetadata metadata, InodeIdentifier parent);
    static NonnullRefPtr<TmpFSInode> create_root(TmpFS&);

    void notify_watchers();

    InodeMetadata m_metadata;
    InodeIdentifier m_parent;

    Optional<KBuffer> m_content;
    struct Child {
        String name;
        NonnullRefPtr<TmpFSInode> inode;
    };
    HashMap<String, Child> m_children;
};

}
