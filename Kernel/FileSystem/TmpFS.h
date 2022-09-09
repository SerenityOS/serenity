/*
 * Copyright (c) 2019-2020, Sergey Bugaev <bugaevc@serenityos.org>
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/FileSystem/FileSystem.h>
#include <Kernel/FileSystem/Inode.h>
#include <Kernel/KBuffer.h>
#include <Kernel/Locking/MutexProtected.h>
#include <Kernel/Memory/AnonymousVMObject.h>

namespace Kernel {

class TmpFSInode;

class TmpFS final : public FileSystem {
    friend class TmpFSInode;

public:
    virtual ~TmpFS() override;
    static ErrorOr<NonnullLockRefPtr<FileSystem>> try_create();
    virtual ErrorOr<void> initialize() override;

    virtual StringView class_name() const override { return "TmpFS"sv; }

    virtual bool supports_watchers() const override { return true; }

    virtual Inode& root_inode() override;

private:
    TmpFS();

    LockRefPtr<TmpFSInode> m_root_inode;

    unsigned m_next_inode_index { 1 };
    unsigned next_inode_index();
};

class TmpFSInode final : public Inode {
    friend class TmpFS;

public:
    virtual ~TmpFSInode() override;

    TmpFS& fs() { return static_cast<TmpFS&>(Inode::fs()); }
    TmpFS const& fs() const { return static_cast<TmpFS const&>(Inode::fs()); }

    // ^Inode
    virtual InodeMetadata metadata() const override;
    virtual ErrorOr<void> traverse_as_directory(Function<ErrorOr<void>(FileSystem::DirectoryEntryView const&)>) const override;
    virtual ErrorOr<NonnullLockRefPtr<Inode>> lookup(StringView name) override;
    virtual ErrorOr<void> flush_metadata() override;
    virtual ErrorOr<NonnullLockRefPtr<Inode>> create_child(StringView name, mode_t, dev_t, UserID, GroupID) override;
    virtual ErrorOr<void> add_child(Inode&, StringView name, mode_t) override;
    virtual ErrorOr<void> remove_child(StringView name) override;
    virtual ErrorOr<void> chmod(mode_t) override;
    virtual ErrorOr<void> chown(UserID, GroupID) override;
    virtual ErrorOr<void> truncate(u64) override;
    virtual ErrorOr<void> update_timestamps(Optional<time_t> atime, Optional<time_t> ctime, Optional<time_t> mtime) override;

private:
    TmpFSInode(TmpFS& fs, InodeMetadata const& metadata, LockWeakPtr<TmpFSInode> parent);
    static ErrorOr<NonnullLockRefPtr<TmpFSInode>> try_create(TmpFS&, InodeMetadata const& metadata, LockWeakPtr<TmpFSInode> parent);
    static ErrorOr<NonnullLockRefPtr<TmpFSInode>> try_create_root(TmpFS&);

    // ^Inode
    virtual ErrorOr<size_t> read_bytes_locked(off_t, size_t, UserOrKernelBuffer& buffer, OpenFileDescription*) const override;
    virtual ErrorOr<size_t> write_bytes_locked(off_t, size_t, UserOrKernelBuffer const& buffer, OpenFileDescription*) override;

    ErrorOr<size_t> do_io_on_content_space(Memory::Region& mapping_region, size_t offset, size_t io_size, UserOrKernelBuffer& buffer, bool write);

    struct Child {
        NonnullOwnPtr<KString> name;
        NonnullLockRefPtr<TmpFSInode> inode;
        IntrusiveListNode<Child> list_node {};
        using List = IntrusiveList<&Child::list_node>;
    };

    Child* find_child_by_name(StringView);

    InodeMetadata m_metadata;
    LockWeakPtr<TmpFSInode> m_parent;

    ErrorOr<void> ensure_allocated_blocks(size_t offset, size_t io_size);
    ErrorOr<void> truncate_to_block_index(size_t block_index);
    ErrorOr<size_t> read_bytes_from_content_space(size_t offset, size_t io_size, UserOrKernelBuffer& buffer) const;
    ErrorOr<size_t> write_bytes_to_content_space(size_t offset, size_t io_size, UserOrKernelBuffer const& buffer);

    struct DataBlock {
    public:
        using List = Vector<OwnPtr<DataBlock>>;

        static ErrorOr<NonnullOwnPtr<DataBlock>> create();

        constexpr static size_t block_size = 128 * KiB;

        Memory::AnonymousVMObject& vmobject() { return *m_content_buffer_vmobject; }
        Memory::AnonymousVMObject const& vmobject() const { return *m_content_buffer_vmobject; }

    private:
        explicit DataBlock(NonnullLockRefPtr<Memory::AnonymousVMObject> content_buffer_vmobject)
            : m_content_buffer_vmobject(move(content_buffer_vmobject))
        {
        }

        NonnullLockRefPtr<Memory::AnonymousVMObject> m_content_buffer_vmobject;
    };

    DataBlock::List m_blocks;
    Child::List m_children;
};

}
