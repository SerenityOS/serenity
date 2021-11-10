/*
 * Copyright (c) 2019-2020, Sergey Bugaev <bugaevc@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/FileSystem/FileSystem.h>
#include <Kernel/FileSystem/Inode.h>
#include <Kernel/KBuffer.h>

namespace Kernel {

class TmpFSInode;

class TmpFS final : public FileSystem {
    friend class TmpFSInode;

public:
    virtual ~TmpFS() override;
    static ErrorOr<NonnullRefPtr<TmpFS>> try_create();
    virtual ErrorOr<void> initialize() override;

    virtual StringView class_name() const override { return "TmpFS"sv; }

    virtual bool supports_watchers() const override { return true; }

    virtual Inode& root_inode() override;

private:
    TmpFS();

    RefPtr<TmpFSInode> m_root_inode;

    HashMap<InodeIndex, NonnullRefPtr<TmpFSInode>> m_inodes;
    ErrorOr<NonnullRefPtr<Inode>> get_inode(InodeIdentifier identifier) const;
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
    virtual ErrorOr<size_t> read_bytes(off_t, size_t, UserOrKernelBuffer& buffer, OpenFileDescription*) const override;
    virtual InodeMetadata metadata() const override;
    virtual ErrorOr<void> traverse_as_directory(Function<ErrorOr<void>(FileSystem::DirectoryEntryView const&)>) const override;
    virtual ErrorOr<NonnullRefPtr<Inode>> lookup(StringView name) override;
    virtual ErrorOr<void> flush_metadata() override;
    virtual ErrorOr<size_t> write_bytes(off_t, size_t, const UserOrKernelBuffer& buffer, OpenFileDescription*) override;
    virtual ErrorOr<NonnullRefPtr<Inode>> create_child(StringView name, mode_t, dev_t, UserID, GroupID) override;
    virtual ErrorOr<void> add_child(Inode&, StringView name, mode_t) override;
    virtual ErrorOr<void> remove_child(StringView name) override;
    virtual ErrorOr<void> chmod(mode_t) override;
    virtual ErrorOr<void> chown(UserID, GroupID) override;
    virtual ErrorOr<void> truncate(u64) override;
    virtual ErrorOr<void> set_atime(time_t) override;
    virtual ErrorOr<void> set_ctime(time_t) override;
    virtual ErrorOr<void> set_mtime(time_t) override;
    virtual void one_ref_left() override;

private:
    TmpFSInode(TmpFS& fs, const InodeMetadata& metadata, InodeIdentifier parent);
    static ErrorOr<NonnullRefPtr<TmpFSInode>> try_create(TmpFS&, InodeMetadata const& metadata, InodeIdentifier parent);
    static ErrorOr<NonnullRefPtr<TmpFSInode>> try_create_root(TmpFS&);
    void notify_watchers();

    struct Child {
        NonnullOwnPtr<KString> name;
        NonnullRefPtr<TmpFSInode> inode;
        IntrusiveListNode<Child> list_node {};
        using List = IntrusiveList<&Child::list_node>;
    };

    Child* find_child_by_name(StringView);

    InodeMetadata m_metadata;
    InodeIdentifier m_parent;

    OwnPtr<KBuffer> m_content;

    Child::List m_children;
};

}
