/*
 * Copyright (c) 2022-2024, Undefine <undefine@undefine.pl>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Optional.h>
#include <AK/OwnPtr.h>
#include <AK/RefPtr.h>
#include <Kernel/FileSystem/FATFS/Definitions.h>
#include <Kernel/FileSystem/FATFS/FileSystem.h>
#include <Kernel/FileSystem/FATFS/SFNUtilities.h>
#include <Kernel/FileSystem/Inode.h>
#include <Kernel/Library/KBuffer.h>
#include <Kernel/Library/KString.h>

namespace Kernel {

struct FATEntryLocation {
    BlockBasedFileSystem::BlockIndex block;
    u32 entry;
};

class FATInode final : public Inode {
    friend FATFS;

public:
    virtual ~FATInode() override = default;

    static ErrorOr<NonnullRefPtr<FATInode>> create(FATFS&, FATEntry, FATEntryLocation inode_metadata_location, Vector<FATLongFileNameEntry> const& = {});

    FATFS& fs() { return static_cast<FATFS&>(Inode::fs()); }
    FATFS const& fs() const { return static_cast<FATFS const&>(Inode::fs()); }

private:
    FATInode(FATFS&, FATEntry, FATEntryLocation inode_metadata_location, NonnullOwnPtr<KString> filename);

    static constexpr u8 end_entry_byte = 0x00;
    static constexpr u8 unused_entry_byte = 0xE5;

    static constexpr u8 lfn_entry_character_termination = 0x00;
    static constexpr u8 lfn_entry_unused_byte = 0xFF;

    static constexpr u8 normal_filename_length = 8;
    static constexpr u8 normal_extension_length = 3;

    static constexpr size_t lfn_entry_characters_part_1_length = 5;
    static constexpr size_t lfn_entry_characters_part_2_length = 6;
    static constexpr size_t lfn_entry_characters_part_3_length = 2;

    static constexpr size_t characters_per_lfn_entry = lfn_entry_characters_part_1_length + lfn_entry_characters_part_2_length + lfn_entry_characters_part_3_length;
    static constexpr u8 last_lfn_entry_mask = 0x40;

    static constexpr size_t max_filename_length = 255;

    static ErrorOr<NonnullOwnPtr<KString>> compute_filename(FATEntry&, Vector<FATLongFileNameEntry> const& = {});
    static StringView byte_terminated_string(StringView, u8);
    static u8 lfn_entry_checksum(FATEntry const& entry);
    static ErrorOr<void> create_unique_sfn_for(FATEntry& entry, NonnullRefPtr<SFNUtils::SFN> sfn, Vector<ByteBuffer> existing_sfns);
    static ErrorOr<void> encode_known_good_sfn_for(FATEntry& entry, StringView name);
    static ErrorOr<Vector<FATLongFileNameEntry>> create_lfn_entries(StringView name, u8 checksum);

    ErrorOr<RawPtr<Vector<u32>>> get_cluster_list();
    ErrorOr<Vector<u32>> compute_cluster_list(FATFS&, u32 first_cluster);
    ErrorOr<Vector<BlockBasedFileSystem::BlockIndex>> get_block_list();
    ErrorOr<NonnullOwnPtr<KBuffer>> read_block_list();
    ErrorOr<RefPtr<FATInode>> traverse(Function<ErrorOr<bool>(RefPtr<FATInode>)> callback);
    u32 first_cluster() const;
    // This overload of `first_cluster` does not rely on the base Inode
    // already being created to determine the FAT version. It is used
    // during FATInode creation (create()).
    static u32 first_cluster(FATVersion const version, u16 first_cluster_low, u16 first_cluster_high);
    ErrorOr<void> allocate_and_add_cluster_to_chain();
    ErrorOr<void> remove_last_cluster_from_chain();
    ErrorOr<Vector<FATEntryLocation>> allocate_entries(u32 count);

    ErrorOr<void> zero_data(u64 offset, u64 count);
    ErrorOr<void> resize(u64 size, Optional<u64> clear_from, Optional<u64> to_clear);

    ErrorOr<Vector<ByteBuffer>> collect_sfns();

    enum class FreeClusters {
        Yes,
        No,
    };

    ErrorOr<void> remove_child_impl(StringView name, FreeClusters free_clusters);

    // ^Inode
    virtual ErrorOr<size_t> write_bytes_locked(off_t, size_t, UserOrKernelBuffer const& data, OpenFileDescription*) override;
    virtual ErrorOr<size_t> read_bytes_locked(off_t, size_t, UserOrKernelBuffer& buffer, OpenFileDescription*) const override;

    virtual InodeMetadata metadata() const override;
    virtual ErrorOr<void> traverse_as_directory(Function<ErrorOr<void>(FileSystem::DirectoryEntryView const&)>) const override;
    virtual ErrorOr<NonnullRefPtr<Inode>> lookup(StringView name) override;
    virtual ErrorOr<NonnullRefPtr<Inode>> create_child(StringView name, mode_t, dev_t, UserID, GroupID) override;
    virtual ErrorOr<void> add_child(Inode&, StringView name, mode_t) override;
    virtual ErrorOr<void> remove_child(StringView name) override;
    virtual ErrorOr<void> chmod(mode_t) override;
    virtual ErrorOr<void> chown(UserID, GroupID) override;
    virtual ErrorOr<void> truncate_locked(u64) override;
    virtual ErrorOr<void> flush_metadata() override;
    virtual ErrorOr<void> update_timestamps(Optional<UnixDateTime> atime, Optional<UnixDateTime> ctime, Optional<UnixDateTime> mtime) override;

    Optional<Vector<u32>> m_cluster_list;
    FATEntry m_entry;
    FATEntryLocation m_inode_metadata_location;
    NonnullOwnPtr<KString> m_filename;
};

}
