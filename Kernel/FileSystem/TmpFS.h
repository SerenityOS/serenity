/*
 * Copyright (c) 2019-2020, Sergey Bugaev <bugaevc@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
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
    static RefPtr<TmpFS> create();
    virtual bool initialize() override;

    virtual const char* class_name() const override { return "TmpFS"; }

    virtual bool supports_watchers() const override { return true; }

    virtual NonnullRefPtr<Inode> root_inode() const override;

private:
    TmpFS();

    RefPtr<TmpFSInode> m_root_inode;

    HashMap<InodeIndex, NonnullRefPtr<TmpFSInode>> m_inodes;
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
    virtual KResultOr<ssize_t> read_bytes(off_t, ssize_t, UserOrKernelBuffer& buffer, FileDescription*) const override;
    virtual InodeMetadata metadata() const override;
    virtual KResult traverse_as_directory(Function<bool(const FS::DirectoryEntryView&)>) const override;
    virtual RefPtr<Inode> lookup(StringView name) override;
    virtual void flush_metadata() override;
    virtual KResultOr<ssize_t> write_bytes(off_t, ssize_t, const UserOrKernelBuffer& buffer, FileDescription*) override;
    virtual KResultOr<NonnullRefPtr<Inode>> create_child(const String& name, mode_t, dev_t, uid_t, gid_t) override;
    virtual KResult add_child(Inode&, const StringView& name, mode_t) override;
    virtual KResult remove_child(const StringView& name) override;
    virtual KResultOr<size_t> directory_entry_count() const override;
    virtual KResult chmod(mode_t) override;
    virtual KResult chown(uid_t, gid_t) override;
    virtual KResult truncate(u64) override;
    virtual KResult set_atime(time_t) override;
    virtual KResult set_ctime(time_t) override;
    virtual KResult set_mtime(time_t) override;
    virtual void one_ref_left() override;

private:
    TmpFSInode(TmpFS& fs, InodeMetadata metadata, InodeIdentifier parent);
    static RefPtr<TmpFSInode> create(TmpFS&, InodeMetadata metadata, InodeIdentifier parent);
    static RefPtr<TmpFSInode> create_root(TmpFS&);

    void notify_watchers();

    InodeMetadata m_metadata;
    InodeIdentifier m_parent;

    OwnPtr<KBuffer> m_content;
    struct Child {
        String name;
        NonnullRefPtr<TmpFSInode> inode;
    };
    HashMap<String, Child> m_children;
};

}
