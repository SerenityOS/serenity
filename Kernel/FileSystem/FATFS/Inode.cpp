/*
 * Copyright (c) 2022-2024, Undefine <undefine@undefine.pl>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Time.h>
#include <Kernel/Debug.h>
#include <Kernel/FileSystem/FATFS/Inode.h>
#include <Kernel/Library/KBufferBuilder.h>

namespace Kernel {

ErrorOr<NonnullRefPtr<FATInode>> FATInode::create(FATFS& fs, FATEntry entry, FATEntryLocation inode_metadata_location, Vector<FATLongFileNameEntry> const& lfn_entries)
{
    auto filename = TRY(compute_filename(entry, lfn_entries));
    return adopt_nonnull_ref_or_enomem(new (nothrow) FATInode(fs, entry, inode_metadata_location, move(filename)));
}

FATInode::FATInode(FATFS& fs, FATEntry entry, FATEntryLocation inode_metadata_location, NonnullOwnPtr<KString> filename)
    : Inode(fs, first_cluster(fs.m_fat_version))
    , m_entry(entry)
    , m_inode_metadata_location(inode_metadata_location)
    , m_filename(move(filename))
{
    dbgln_if(FAT_DEBUG, "FATFS: Creating inode {} with filename \"{}\"", index(), m_filename);

    m_metadata = {
        .inode = identifier(),
        .size = m_entry.file_size,
        .mode = static_cast<mode_t>((has_flag(m_entry.attributes, FATAttributes::Directory) ? S_IFDIR : S_IFREG) | 0777),
        .uid = 0,
        .gid = 0,
        .link_count = 0,
        .atime = time_from_packed_dos(m_entry.last_accessed_date, { 0 }),
        .ctime = time_from_packed_dos(m_entry.creation_date, m_entry.creation_time),
        .mtime = time_from_packed_dos(m_entry.modification_date, m_entry.modification_time),
        .dtime = {},
        .block_count = 0,
        .block_size = 0,
        .major_device = 0,
        .minor_device = 0,
    };
}

ErrorOr<Vector<BlockBasedFileSystem::BlockIndex>> FATInode::compute_block_list()
{
    VERIFY(m_inode_lock.is_locked());

    dbgln_if(FAT_DEBUG, "FATFS: computing block list for inode {}", index());

    u32 cluster = first_cluster();

    Vector<BlockBasedFileSystem::BlockIndex> block_list;

    while (cluster < end_of_chain_marker()) {
        dbgln_if(FAT_DEBUG, "FATFS: Appending cluster {} to inode {}'s cluster chain", cluster, index());

        auto first_block_and_length = fs().first_block_of_cluster(cluster);
        for (u8 i = 0; i < first_block_and_length.number_of_sectors; i++)
            TRY(block_list.try_append(BlockBasedFileSystem::BlockIndex { first_block_and_length.start_block.value() + i }));

        // Clusters 0 and 1 are reserved in the FAT, and their entries in the FAT will
        // not point to another valid cluster in the chain (Cluster 0 typically holds
        // the "FAT ID" field with some flags, Cluster 1 should be the end of chain
        // marker).
        // Internally, we use `cluster == 0` to represent the root directory Inode,
        // which is a signal to read the root directory region blocks on FAT12/16
        // file systems. (`fs().first_block_of_cluster` will return the appropriate
        // block/sectors to read given cluster == 0).
        // Therefore, we read one set of sectors for these invalud cluster numbers,
        // and then terminate the loop becuase the FAT entry at `cluster` for these
        // values does not represent the next step in the chain (because there is
        // nothing else to read).
        if (cluster <= 1) {
            break;
        }

        // Look up the next cluster to read, or read End of Chain marker from table.
        cluster = TRY(fs().fat_read(cluster));
    }

    return block_list;
}

u32 FATInode::end_of_chain_marker() const
{
    // Returns the end of chain entry for the given file system.
    // Any FAT entry of this value or greater signifies the end
    // of the chain has been reached for a given entry.
    switch (fs().m_fat_version) {
    case FATVersion::FAT12:
        return 0xFF8;
    case FATVersion::FAT16:
        return 0xFFF8;
    case FATVersion::FAT32:
        return 0x0FFFFFF8;
    default:
        VERIFY_NOT_REACHED();
    }
}

ErrorOr<NonnullOwnPtr<KBuffer>> FATInode::read_block_list()
{
    VERIFY(m_inode_lock.is_locked());

    dbgln_if(FAT_DEBUG, "FATFS: reading block list for inode {} ({} blocks)", index(), m_block_list.size());

    if (m_block_list.is_empty())
        m_block_list = TRY(compute_block_list());

    auto builder = TRY(KBufferBuilder::try_create());

    u8 buffer[512];
    VERIFY(fs().m_device_block_size <= sizeof(buffer));
    auto buf = UserOrKernelBuffer::for_kernel_buffer(buffer);

    for (BlockBasedFileSystem::BlockIndex block : m_block_list) {
        dbgln_if(FAT_DEBUG, "FATFS: reading block: {}", block);
        TRY(fs().read_block(block, &buf, sizeof(buffer)));
        TRY(builder.append((char const*)buffer, fs().m_device_block_size));
    }

    auto blocks = builder.build();
    if (!blocks)
        return ENOMEM;
    return blocks.release_nonnull();
}

ErrorOr<void> FATInode::replace_child(StringView, Inode&)
{
    // TODO: Implement this once we have write support.
    return Error::from_errno(EROFS);
}

ErrorOr<RefPtr<FATInode>> FATInode::traverse(Function<ErrorOr<bool>(RefPtr<FATInode>)> callback)
{
    VERIFY(has_flag(m_entry.attributes, FATAttributes::Directory));

    Vector<FATLongFileNameEntry> lfn_entries;
    auto blocks = TRY(read_block_list());

    for (u32 i = 0; i < blocks->size() / sizeof(FATEntry); i++) {
        auto* entry = reinterpret_cast<FATEntry*>(blocks->data() + i * sizeof(FATEntry));
        if (entry->filename[0] == end_entry_byte) {
            dbgln_if(FAT_DEBUG, "FATFS: Found end entry");
            return nullptr;
        } else if (static_cast<u8>(entry->filename[0]) == unused_entry_byte) {
            dbgln_if(FAT_DEBUG, "FATFS: Found unused entry");
            lfn_entries.clear();
        } else if (entry->attributes == FATAttributes::LongFileName) {
            dbgln_if(FAT_DEBUG, "FATFS: Found LFN entry");
            TRY(lfn_entries.try_append(*reinterpret_cast<FATLongFileNameEntry*>(entry)));
        } else if ((entry->first_cluster_high << 16 | entry->first_cluster_low) <= 1 && entry->file_size > 0) {
            // Because clusters 0 and 1 are reserved, only empty files (size == 0 files)
            // should specify these clusters.
            // This driver uses a cluster number == 0 to represent the root directory inode
            // on FAT12/16 file systems (a signal to look in the root directory region),
            // so we ensure that no entries read off the file system have a cluster number
            // that would also point to this region.
            dbgln_if(FAT_DEBUG, "FATFS: Invalid cluster for entry");
            return EINVAL;
        } else {
            auto entry_number_bytes = i * sizeof(FATEntry);
            auto block = m_block_list[entry_number_bytes / fs().m_device_block_size];

            auto entries_per_sector = fs().m_device_block_size / sizeof(FATEntry);
            u32 block_entry = i % entries_per_sector;

            dbgln_if(FAT_DEBUG, "FATFS: Found 8.3 entry at block {}, entry {}", block, block_entry);
            lfn_entries.reverse();
            auto inode = TRY(FATInode::create(fs(), *entry, { block, block_entry }, lfn_entries));
            if (TRY(callback(inode)))
                return inode;
            lfn_entries.clear();
        }
    }

    return EINVAL;
}

ErrorOr<NonnullOwnPtr<KString>> FATInode::compute_filename(FATEntry& entry, Vector<FATLongFileNameEntry> const& lfn_entries)
{
    if (lfn_entries.is_empty()) {
        StringBuilder filename;
        TRY(filename.try_append(byte_terminated_string(StringView(entry.filename, normal_filename_length), ' ')));
        if (entry.extension[0] != ' ') {
            TRY(filename.try_append('.'));
            TRY(filename.try_append(byte_terminated_string(StringView(entry.extension, normal_extension_length), ' ')));
        }
        return TRY(KString::try_create(filename.string_view()));
    } else {
        StringBuilder filename;
        for (auto& lfn_entry : lfn_entries) {
            TRY(filename.try_append(lfn_entry.characters1[0]));
            TRY(filename.try_append(lfn_entry.characters1[1]));
            TRY(filename.try_append(lfn_entry.characters1[2]));
            TRY(filename.try_append(lfn_entry.characters1[3]));
            TRY(filename.try_append(lfn_entry.characters1[4]));
            TRY(filename.try_append(lfn_entry.characters2[0]));
            TRY(filename.try_append(lfn_entry.characters2[1]));
            TRY(filename.try_append(lfn_entry.characters2[2]));
            TRY(filename.try_append(lfn_entry.characters2[3]));
            TRY(filename.try_append(lfn_entry.characters2[4]));
            TRY(filename.try_append(lfn_entry.characters2[5]));
            TRY(filename.try_append(lfn_entry.characters3[0]));
            TRY(filename.try_append(lfn_entry.characters3[1]));
        }

        // Long Filenames have two terminators:
        // 1. Completely unused "entries" (the `characterN` fields of
        //    `lfn_entry`) are filled with 0xFF (`lfn_entry_unused_byte`).
        // 2. Partially used entries (within `characterN`) are null-padded.
        //
        // `filename` is truncated first to eliminate unused entries, and
        // then further truncated to remove any existing null padding characters.
        //
        // Page 8 of the Long Filename Specification
        // (http://www.osdever.net/documents/LongFileName.pdf)
        // details this encoding ("If the long name does not fill...").
        return TRY(KString::try_create(
            byte_terminated_string(
                byte_terminated_string(filename.string_view(), lfn_entry_unused_byte), lfn_entry_character_termination)));
    }

    VERIFY_NOT_REACHED();
}

StringView FATInode::byte_terminated_string(StringView string, u8 fill_byte)
{
    if (auto index = string.find_last_not(fill_byte); index.has_value())
        return string.substring_view(0, index.value() + 1);
    return string;
}

u32 FATInode::first_cluster() const
{
    return first_cluster(fs().m_fat_version);
}

u32 FATInode::first_cluster(FATVersion const version) const
{
    if (version == FATVersion::FAT32) {
        return (static_cast<u32>(m_entry.first_cluster_high) << 16) | m_entry.first_cluster_low;
    }
    // The space occupied in a directory entry by `first_cluster_high` (0x14)
    // is reserved in FAT12/16, and may be used to store file meta-data.
    // As a result, do not include it on FAT12/16 file systems.
    return m_entry.first_cluster_low;
}

ErrorOr<size_t> FATInode::read_bytes_locked(off_t offset, size_t size, UserOrKernelBuffer& buffer, OpenFileDescription*) const
{
    dbgln_if(FAT_DEBUG, "FATFS: Reading inode {}: size: {} offset: {}", identifier().index(), size, offset);
    VERIFY(offset >= 0);
    if (offset >= m_metadata.size)
        return 0;

    // FIXME: Read only the needed blocks instead of the whole file
    auto blocks = TRY(const_cast<FATInode&>(*this).read_block_list());

    // Take the minimum of the:
    //   1. User-specified size parameter
    //   2. The file size.
    //   3. The number of blocks returned for reading.
    size_t read_size = min(
        min(size, m_metadata.size - offset),
        (m_block_list.size() * fs().m_device_block_size) - offset);
    TRY(buffer.write(blocks->data() + offset, read_size));

    return read_size;
}

InodeMetadata FATInode::metadata() const
{
    return m_metadata;
}

ErrorOr<void> FATInode::traverse_as_directory(Function<ErrorOr<void>(FileSystem::DirectoryEntryView const&)> callback) const
{
    MutexLocker locker(m_inode_lock);

    VERIFY(has_flag(m_entry.attributes, FATAttributes::Directory));

    [[maybe_unused]] auto inode = TRY(const_cast<FATInode&>(*this).traverse([&callback](auto inode) -> ErrorOr<bool> {
        if (inode->m_filename->view() == "" || inode->m_filename->view() == "." || inode->m_filename->view() == "..")
            return false;
        TRY(callback({ inode->m_filename->view(), inode->identifier(), static_cast<u8>(inode->m_entry.attributes) }));
        return false;
    }));

    return {};
}

ErrorOr<NonnullRefPtr<Inode>> FATInode::lookup(StringView name)
{
    MutexLocker locker(m_inode_lock);

    VERIFY(has_flag(m_entry.attributes, FATAttributes::Directory));

    auto inode = TRY(traverse([name](auto child) -> ErrorOr<bool> {
        return child->m_filename->view() == name;
    }));

    if (inode.is_null())
        return ENOENT;
    return inode.release_nonnull();
}

ErrorOr<size_t> FATInode::write_bytes_locked(off_t, size_t, UserOrKernelBuffer const&, OpenFileDescription*)
{
    return EROFS;
}

ErrorOr<NonnullRefPtr<Inode>> FATInode::create_child(StringView, mode_t, dev_t, UserID, GroupID)
{
    return EROFS;
}

ErrorOr<void> FATInode::add_child(Inode&, StringView, mode_t)
{
    return EROFS;
}

ErrorOr<void> FATInode::remove_child(StringView)
{
    return EROFS;
}

ErrorOr<void> FATInode::chmod(mode_t)
{
    // TODO: Linux actually does do some stuff here, like setting the hidden attribute if the file starts with a dot.
    return Error::from_errno(ENOTSUP);
}

ErrorOr<void> FATInode::chown(UserID, GroupID)
{
    return Error::from_errno(ENOTSUP);
}

ErrorOr<void> FATInode::flush_metadata()
{
    return EROFS;
}

}
