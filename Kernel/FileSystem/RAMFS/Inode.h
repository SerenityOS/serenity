/*
 * Copyright (c) 2019-2020, Sergey Bugaev <bugaevc@serenityos.org>
 * Copyright (c) 2022-2023, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/FileSystem/Inode.h>
#include <Kernel/FileSystem/RAMFS/FileSystem.h>
#include <Kernel/Forward.h>
#include <Kernel/Memory/AnonymousVMObject.h>

namespace Kernel {

class RAMFSInode final : public Inode {
    friend class RAMFS;

public:
    virtual ~RAMFSInode() override;

    RAMFS& fs() { return static_cast<RAMFS&>(Inode::fs()); }
    RAMFS const& fs() const { return static_cast<RAMFS const&>(Inode::fs()); }

    // ^Inode
    virtual InodeMetadata metadata() const override;
    virtual ErrorOr<void> traverse_as_directory(Function<ErrorOr<void>(FileSystem::DirectoryEntryView const&)>) const override;
    virtual ErrorOr<NonnullLockRefPtr<Inode>> lookup(StringView name) override;
    virtual ErrorOr<void> flush_metadata() override;
    virtual ErrorOr<NonnullLockRefPtr<Inode>> create_child(StringView name, mode_t, dev_t, UserID, GroupID) override;
    virtual ErrorOr<void> add_child(Inode&, StringView name, mode_t) override;
    virtual ErrorOr<void> remove_child(StringView name) override;
    virtual ErrorOr<void> replace_child(StringView name, Inode& child) override;
    virtual ErrorOr<void> chmod(mode_t) override;
    virtual ErrorOr<void> chown(UserID, GroupID) override;
    virtual ErrorOr<void> truncate(u64) override;
    virtual ErrorOr<void> update_timestamps(Optional<Time> atime, Optional<Time> ctime, Optional<Time> mtime) override;

private:
    // Note: These methods are being used only during early boot when extracting the initramfs archive to a ramfs instance.
    static ErrorOr<NonnullLockRefPtr<RAMFSInode>> try_create_with_content(RAMFS&, InodeMetadata const& metadata, PhysicalAddress content_blocks_offset, size_t blocks_count, LockWeakPtr<RAMFSInode> parent);
    static ErrorOr<NonnullLockRefPtr<RAMFSInode>> try_create_with_empty_content(RAMFS&, InodeMetadata const& metadata, LockWeakPtr<RAMFSInode> parent);
    static ErrorOr<NonnullLockRefPtr<RAMFSInode>> try_create_as_directory(RAMFS&, InodeMetadata const& metadata, LockWeakPtr<RAMFSInode> parent);

    RAMFSInode(RAMFS& fs, InodeMetadata const& metadata, LockWeakPtr<RAMFSInode> parent);
    explicit RAMFSInode(RAMFS& fs);
    static ErrorOr<NonnullLockRefPtr<RAMFSInode>> try_create(RAMFS&, InodeMetadata const& metadata, LockWeakPtr<RAMFSInode> parent);
    static ErrorOr<NonnullLockRefPtr<RAMFSInode>> try_create_root(RAMFS&);

    // ^Inode
    virtual ErrorOr<size_t> read_bytes_locked(off_t, size_t, UserOrKernelBuffer& buffer, OpenFileDescription*) const override;
    virtual ErrorOr<size_t> write_bytes_locked(off_t, size_t, UserOrKernelBuffer const& buffer, OpenFileDescription*) override;

    ErrorOr<size_t> do_io_on_content_space(Memory::Region& mapping_region, size_t offset, size_t io_size, UserOrKernelBuffer& buffer, bool write);
    ErrorOr<void> attach_initramfs_data_block(PhysicalAddress data_block_paddr);

    struct Child {
        NonnullOwnPtr<KString> name;
        NonnullLockRefPtr<RAMFSInode> inode;
        IntrusiveListNode<Child> list_node {};
        using List = IntrusiveList<&Child::list_node>;
    };

    Child* find_child_by_name(StringView);

    InodeMetadata m_metadata;
    LockWeakPtr<RAMFSInode> m_parent;

    ErrorOr<void> ensure_allocated_blocks(size_t offset, size_t io_size);
    ErrorOr<void> truncate_to_block_index(size_t block_index);
    ErrorOr<size_t> read_bytes_from_content_space(size_t offset, size_t io_size, UserOrKernelBuffer& buffer) const;
    ErrorOr<size_t> write_bytes_to_content_space(size_t offset, size_t io_size, UserOrKernelBuffer const& buffer);

    struct DataBlock {
    public:
        using List = Vector<OwnPtr<DataBlock>>;

        static ErrorOr<NonnullOwnPtr<DataBlock>> create();
        static ErrorOr<NonnullOwnPtr<DataBlock>> create_with_known_paddr(PhysicalAddress data_block_paddr);

        constexpr static size_t block_size = 4 * KiB;

        Memory::AnonymousVMObject& vmobject() { return *m_content_buffer_vmobject; }
        Memory::AnonymousVMObject const& vmobject() const { return *m_content_buffer_vmobject; }

    private:
        explicit DataBlock(NonnullLockRefPtr<Memory::AnonymousVMObject> content_buffer_vmobject)
            : m_content_buffer_vmobject(move(content_buffer_vmobject))
        {
        }

        NonnullLockRefPtr<Memory::AnonymousVMObject> m_content_buffer_vmobject;
    };

    bool const m_root_directory_inode { false };

    DataBlock::List m_blocks;
    Child::List m_children;
};

}
