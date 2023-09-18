/*
 * Copyright (c) 2022-2023, Undefine <undefine@undefine.pl>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Time.h>
#include <Kernel/Debug.h>
#include <Kernel/FileSystem/FATFS/Inode.h>
#include <Kernel/Library/KBufferBuilder.h>

namespace Kernel {

ErrorOr<NonnullRefPtr<FATInode>> FATInode::create(FATFS& fs, FATEntry entry, EntryLocation inode_metadata_location, Vector<FATLongFileNameEntry> const& lfn_entries)
{
    auto filename = TRY(compute_filename(entry, lfn_entries));
    auto block_list = TRY(compute_block_list(fs, (((u32)entry.first_cluster_high) << 16) | entry.first_cluster_low));
    return adopt_nonnull_ref_or_enomem(new (nothrow) FATInode(fs, entry, inode_metadata_location, move(filename), block_list));
}

FATInode::FATInode(FATFS& fs, FATEntry entry, EntryLocation inode_metadata_location, NonnullOwnPtr<KString> filename, Vector<BlockBasedFileSystem::BlockIndex> const& block_list)
    : Inode(fs, *reinterpret_cast<u64*>(&inode_metadata_location.block) * fs.m_device_block_size + inode_metadata_location.entry * sizeof(FATEntry))
    , m_entry(entry)
    , m_inode_metadata_location(inode_metadata_location)
    , m_filename(move(filename))
    , m_block_list(block_list)
{
    dbgln_if(FAT_DEBUG, "FATInode[{}]::FATInode(): Created with filename \"{}\" and first cluster {}", identifier(), m_filename, first_cluster());
}

ErrorOr<Vector<BlockBasedFileSystem::BlockIndex>> FATInode::compute_block_list(FATFS& fs, u32 first_cluster)
{
    dbgln_if(FAT_DEBUG, "FATFS: computing block list starting with cluster {}", first_cluster);

    u32 cluster = first_cluster;

    Vector<BlockBasedFileSystem::BlockIndex> block_list;

    if (cluster == 0)
        return block_list;

    while (cluster < no_more_clusters) {
        dbgln_if(FAT_DEBUG, "FATFS: Appending cluster {} to cluster chain starting with {}", cluster, first_cluster);

        BlockBasedFileSystem::BlockIndex first_block = fs.first_block_of_cluster(cluster);
        for (u8 i = 0; i < fs.boot_record()->sectors_per_cluster; i++)
            TRY(block_list.try_append(BlockBasedFileSystem::BlockIndex { first_block.value() + i }));

        cluster = TRY(fs.fat_read(cluster));
    }

    return block_list;
}

ErrorOr<NonnullOwnPtr<KBuffer>> FATInode::read_block_list()
{
    VERIFY(m_inode_lock.is_locked());

    dbgln_if(FAT_DEBUG, "FATFS: reading block list for inode {} ({} blocks)", index(), m_block_list.size());

    auto builder = TRY(KBufferBuilder::try_create());

    u8 buffer[512];
    VERIFY(fs().m_device_block_size <= sizeof(buffer));
    auto buf = UserOrKernelBuffer::for_kernel_buffer(buffer);

    for (BlockBasedFileSystem::BlockIndex block : m_block_list) {
        dbgln_if(FAT_DEBUG, "FATFS: reading block: {}", block);
        TRY(fs().raw_read(block, buf));
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
        } else {
            auto entry_number_bytes = i * sizeof(FATEntry);
            auto block = m_block_list[entry_number_bytes / fs().logical_block_size()];

            auto entries_per_sector = fs().logical_block_size() / sizeof(FATEntry);
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
            // FIXME: These are 16-bit characters, but we are treating them as 8-bit
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
    return (((u32)m_entry.first_cluster_high) << 16) | m_entry.first_cluster_low;
}

ErrorOr<size_t> FATInode::read_bytes_locked(off_t offset, size_t size, UserOrKernelBuffer& buffer, OpenFileDescription*) const
{
    dbgln_if(FAT_DEBUG, "FATFS: Reading inode {}: size: {} offset: {}", identifier().index(), size, offset);
    VERIFY(offset >= 0);
    if (offset >= m_entry.file_size)
        return 0;

    size_t size_to_read = min(size, m_entry.file_size - offset);

    // FIXME: Read only the needed blocks instead of the whole file
    auto blocks = TRY(const_cast<FATInode&>(*this).read_block_list());
    TRY(buffer.write(blocks->data() + offset, size_to_read));

    return size_to_read;
}

InodeMetadata FATInode::metadata() const
{
    dbgln_if(FAT_DEBUG, "FATInode[{}]::metadata(): returning metadata for filename {}", identifier(), m_filename);

    return {
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
        .block_count = m_block_list.size(),
        .block_size = fs().logical_block_size(),
        .major_device = 0,
        .minor_device = 0,
    };
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
