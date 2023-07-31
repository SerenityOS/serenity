/*
 * Copyright (c) 2022, Undefine <undefine@undefine.pl>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/OwnPtr.h>
#include <AK/RefPtr.h>
#include <Kernel/FileSystem/FATFS/Definitions.h>
#include <Kernel/FileSystem/FATFS/FileSystem.h>
#include <Kernel/FileSystem/Inode.h>
#include <Kernel/Library/KBuffer.h>
#include <Kernel/Library/KString.h>

namespace Kernel {

class FATInode final : public Inode {
    friend FATFS;

public:
    virtual ~FATInode() override = default;

    static ErrorOr<NonnullRefPtr<FATInode>> create(FATFS&, FATEntry, Vector<FATLongFileNameEntry> const& = {});

    FATFS& fs() { return static_cast<FATFS&>(Inode::fs()); }
    FATFS const& fs() const { return static_cast<FATFS const&>(Inode::fs()); }

private:
    FATInode(FATFS&, FATEntry, NonnullOwnPtr<KString> filename);

    size_t fat_offset_for_cluster(u32 cluster) const;

    // Returns cluster number value that indicates the end of the chain
    // has been reached. Any cluster value >= this value indicates this
    // is the last cluster.
    u32 end_of_chain_marker() const;

    // Reads the cluster number located at the offset within the table.
    u32 cluster_number(KBuffer const& fat_sector, u32 entry_cluster_number, u32 entry_offset) const;

    static constexpr u8 end_entry_byte = 0x00;
    static constexpr u8 unused_entry_byte = 0xE5;

    static constexpr u8 lfn_entry_character_termination = 0x00;
    static constexpr u8 lfn_entry_unused_byte = 0xFF;

    static constexpr u8 normal_filename_length = 8;
    static constexpr u8 normal_extension_length = 3;

    static ErrorOr<NonnullOwnPtr<KString>> compute_filename(FATEntry&, Vector<FATLongFileNameEntry> const& = {});
    static StringView byte_terminated_string(StringView, u8);

    ErrorOr<Vector<BlockBasedFileSystem::BlockIndex>> compute_block_list();
    ErrorOr<NonnullOwnPtr<KBuffer>> read_block_list();
    ErrorOr<RefPtr<FATInode>> traverse(Function<ErrorOr<bool>(RefPtr<FATInode>)> callback);
    u32 first_cluster() const;
    // This overload of `first_cluster` does not rely on the base Inode
    // already being created to determine the FAT version. It is used
    // during FATInode creation (create()).
    u32 first_cluster(FATVersion const version) const;

    // ^Inode
    virtual ErrorOr<size_t> write_bytes_locked(off_t, size_t, UserOrKernelBuffer const& data, OpenFileDescription*) override;
    virtual ErrorOr<size_t> read_bytes_locked(off_t, size_t, UserOrKernelBuffer& buffer, OpenFileDescription*) const override;

    virtual InodeMetadata metadata() const override;
    virtual ErrorOr<void> traverse_as_directory(Function<ErrorOr<void>(FileSystem::DirectoryEntryView const&)>) const override;
    virtual ErrorOr<NonnullRefPtr<Inode>> lookup(StringView name) override;
    virtual ErrorOr<NonnullRefPtr<Inode>> create_child(StringView name, mode_t, dev_t, UserID, GroupID) override;
    virtual ErrorOr<void> add_child(Inode&, StringView name, mode_t) override;
    virtual ErrorOr<void> remove_child(StringView name) override;
    virtual ErrorOr<void> replace_child(StringView name, Inode& child) override;
    virtual ErrorOr<void> chmod(mode_t) override;
    virtual ErrorOr<void> chown(UserID, GroupID) override;
    virtual ErrorOr<void> flush_metadata() override;

    Vector<BlockBasedFileSystem::BlockIndex> m_block_list;
    FATEntry m_entry;
    NonnullOwnPtr<KString> m_filename;
    InodeMetadata m_metadata;
};

}
