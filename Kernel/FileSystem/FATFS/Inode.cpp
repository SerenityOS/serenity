/*
 * Copyright (c) 2022-2023, Undefine <undefine@undefine.pl>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/CharacterTypes.h>
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

void FATInode::create_83_filename_for(FATEntry& entry, StringView name)
{
    // FIXME: Implement the correct algorithm based on 3.2.4 from http://www.osdever.net/documents/LongFileName.pdf
    for (size_t i = 0; i < min(name.length(), normal_filename_length); i++)
        entry.filename[i] = to_ascii_uppercase(name[i]);
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

    if (blocks->size() == 0)
        return nullptr;

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

u8 FATInode::lfn_entry_checksum(FATEntry const& entry)
{
    u8 checksum = entry.filename[0];
    checksum = (checksum << 7) + (checksum >> 1) + entry.filename[1];
    checksum = (checksum << 7) + (checksum >> 1) + entry.filename[2];
    checksum = (checksum << 7) + (checksum >> 1) + entry.filename[3];
    checksum = (checksum << 7) + (checksum >> 1) + entry.filename[4];
    checksum = (checksum << 7) + (checksum >> 1) + entry.filename[5];
    checksum = (checksum << 7) + (checksum >> 1) + entry.filename[6];
    checksum = (checksum << 7) + (checksum >> 1) + entry.filename[7];
    checksum = (checksum << 7) + (checksum >> 1) + entry.extension[0];
    checksum = (checksum << 7) + (checksum >> 1) + entry.extension[1];
    checksum = (checksum << 7) + (checksum >> 1) + entry.extension[2];
    return checksum;
}

u32 FATInode::first_cluster() const
{
    return (((u32)m_entry.first_cluster_high) << 16) | m_entry.first_cluster_low;
}

ErrorOr<void> FATInode::allocate_and_add_cluster_to_chain()
{
    VERIFY(m_inode_lock.is_locked());

    u32 allocated_cluster = TRY(fs().allocate_cluster());
    dbgln_if(FAT_DEBUG, "FATInode[{}]::allocate_and_add_cluster_to_chain(): allocated cluster {}", identifier(), allocated_cluster);

    if (m_block_list.is_empty()) {
        m_entry.first_cluster_low = allocated_cluster & 0xFFFF;
        m_entry.first_cluster_high = allocated_cluster >> 16;

        set_metadata_dirty(true);
    } else {
        u32 last_cluster = fs().block_to_cluster(m_block_list[m_block_list.size() - 1]);
        TRY(fs().fat_write(last_cluster, allocated_cluster));
    }

    BlockBasedFileSystem::BlockIndex first_block = fs().first_block_of_cluster(allocated_cluster);
    for (u8 i = 0; i < fs().boot_record()->sectors_per_cluster; i++)
        TRY(m_block_list.try_append(BlockBasedFileSystem::BlockIndex { first_block.value() + i }));

    return {};
}

ErrorOr<void> FATInode::remove_last_cluster_from_chain()
{
    VERIFY(m_inode_lock.is_locked());
    VERIFY(m_block_list.size() > 0);

    u32 last_cluster = fs().block_to_cluster(m_block_list[m_block_list.size() - 1]);
    TRY(fs().fat_write(last_cluster, cluster_free));

    dbgln_if(FAT_DEBUG, "FATInode[{}]::remove_last_cluster_from_chain(): freeing cluster {}", identifier(), last_cluster);

    for (u32 i = 0; i < fs().boot_record()->sectors_per_cluster; i++)
        m_block_list.remove(m_block_list.size() - 1);

    if (m_block_list.is_empty()) {
        m_entry.first_cluster_low = 0;
        m_entry.first_cluster_high = 0;

        set_metadata_dirty(true);
    } else {
        last_cluster = fs().block_to_cluster(m_block_list[m_block_list.size() - 1]);
        TRY(fs().fat_write(last_cluster, no_more_clusters));
    }

    return {};
}

ErrorOr<Vector<FATInode::EntryLocation>> FATInode::allocate_entries(u32 count)
{
    // FIXME: This function ignores unused entries, we should make use of them
    // FIXME: If we fail anywhere here, we should make sure the end entry is at the correct location

    auto blocks = TRY(read_block_list());
    auto entries = reinterpret_cast<FATEntry*>(blocks->data());

    auto const entries_per_block = fs().logical_block_size() / sizeof(FATEntry);

    Vector<EntryLocation> locations;
    TRY(locations.try_ensure_capacity(count));

    for (u32 current_entry_index = 0; current_entry_index < blocks->size() / sizeof(FATEntry); current_entry_index++) {
        auto& entry = entries[current_entry_index];
        if (entry.filename[0] == end_entry_byte) {
            while (current_entry_index < blocks->size() / sizeof(FATEntry) && locations.size() < count) {
                u32 chosen_block_index = current_entry_index / entries_per_block;
                u32 chosen_entry_index = current_entry_index % entries_per_block;
                locations.unchecked_append({ m_block_list[chosen_block_index], chosen_entry_index });
                dbgln_if(FAT_DEBUG, "FATInode[{}]::allocate_entries(): allocated new entry at block {}, offset {}", identifier(), m_block_list[chosen_block_index], chosen_entry_index);
                current_entry_index++;
            }
            if (locations.size() == count) {
                u32 block_index = current_entry_index / entries_per_block;
                u32 entry_index = current_entry_index % entries_per_block;
                dbgln_if(FAT_DEBUG, "FATInode[{}]::allocate_entries(): putting new end entry at block {}, offset {}", identifier(), m_block_list[block_index], entry_index);

                FATEntry end_entry {};
                end_entry.filename[0] = end_entry_byte;
                TRY(fs().write_block(m_block_list[block_index], UserOrKernelBuffer::for_kernel_buffer(reinterpret_cast<u8*>(&end_entry)), sizeof(FATEntry), entry_index * sizeof(FATEntry)));
                break;
            }
        }
    }

    if (locations.size() < count) {
        TRY(allocate_and_add_cluster_to_chain());
        u32 new_block_index = m_block_list.size() - fs().boot_record()->sectors_per_cluster - 1;
        u32 entry_index;
        for (entry_index = 0; entry_index < count - locations.size(); entry_index++) {
            locations.unchecked_append({ m_block_list[new_block_index], entry_index });
            dbgln_if(FAT_DEBUG, "FATInode[{}]::allocate_entries(): allocated new entry at block {}, offset {}", identifier(), m_block_list[new_block_index], entry_index);
        }

        dbgln_if(FAT_DEBUG, "FATInode[{}]::allocate_entries(): putting new end entry at block {}, offset {}", identifier(), m_block_list[new_block_index], entry_index);

        FATEntry end_entry {};
        end_entry.filename[0] = end_entry_byte;
        TRY(fs().write_block(m_block_list[new_block_index], UserOrKernelBuffer::for_kernel_buffer(reinterpret_cast<u8*>(&end_entry)), sizeof(FATEntry), entry_index * sizeof(FATEntry)));
    }

    return locations;
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

ErrorOr<size_t> FATInode::write_bytes_locked(off_t offset, size_t size, UserOrKernelBuffer const& buffer, OpenFileDescription*)
{
    dbgln_if(FAT_DEBUG, "FATInode[{}]::write_bytes_locked(): Writing size: {} offset: {}", identifier(), size, offset);

    m_entry.file_size = max(m_entry.file_size, offset + size);
    set_metadata_dirty(true);

    while (offset + size > m_block_list.size() * fs().m_device_block_size)
        TRY(allocate_and_add_cluster_to_chain());

    u32 first_block_index = offset % fs().logical_block_size();

    size_t offset_into_first_block = offset % fs().logical_block_size();

    for (u32 block_index = first_block_index; block_index * fs().logical_block_size() < size; block_index++) {
        size_t offset_into_block = block_index == first_block_index ? offset_into_first_block : 0;
        TRY(fs().write_block(m_block_list[block_index], buffer.offset(block_index * fs().logical_block_size()), min(fs().logical_block_size() - offset_into_block, size - block_index * fs().logical_block_size()), offset_into_block));
    }

    return size;
}

ErrorOr<NonnullRefPtr<Inode>> FATInode::create_child(StringView name, mode_t mode, dev_t, UserID, GroupID)
{
    VERIFY(has_flag(m_entry.attributes, FATAttributes::Directory));

    MutexLocker locker(m_inode_lock);

    dbgln_if(FAT_DEBUG, "FATInode[{}]::create_child(): creating inode \"{}\"", identifier(), name);

    // FIXME: For some filenames lfn entries are not necessary
    u32 lfn_entry_count = ceil_div(name.length(), characters_per_lfn_entry);

    auto entries = TRY(allocate_entries(lfn_entry_count + 1));

    FATEntry entry {};
    create_83_filename_for(entry, name);

    if (mode & S_IFDIR)
        entry.attributes |= FATAttributes::Directory;
    // FIXME: Set the dates

    TRY(fs().write_block(entries[lfn_entry_count].block, UserOrKernelBuffer::for_kernel_buffer(reinterpret_cast<u8*>(&entry)), sizeof(FATEntry), entries[lfn_entry_count].entry * sizeof(FATEntry)));

    u8 lfn_checksum = lfn_entry_checksum(entry);

    Vector<FATLongFileNameEntry> lfn_entries;
    TRY(lfn_entries.try_ensure_capacity(lfn_entry_count));

    auto characters_left = name.length();

    for (u32 i = 0; i < lfn_entry_count; i++) {
        FATLongFileNameEntry lfn_entry {};

        size_t characters_in_part = min(characters_left, lfn_entry_characters_part_1_length);

        for (size_t j = 0; j < characters_in_part; j++) {
            lfn_entry.characters1[j] = name[name.length() - characters_left];
            characters_left--;
        }

        if (characters_left > 0) {
            characters_in_part = min(characters_left, lfn_entry_characters_part_2_length);

            for (size_t j = 0; j < characters_in_part; j++) {
                lfn_entry.characters2[j] = name[name.length() - characters_left];
                characters_left--;
            }
        }

        if (characters_left > 0) {
            characters_in_part = min(characters_left, lfn_entry_characters_part_3_length);

            for (size_t j = 0; j < characters_in_part; j++) {
                lfn_entry.characters3[j] = name[name.length() - characters_left];
                characters_left--;
            }
        }

        lfn_entry.entry_index = (i + 1) | (i + 1 == lfn_entry_count ? last_lfn_entry_mask : 0);
        lfn_entry.checksum = lfn_checksum;
        lfn_entry.attributes = FATAttributes::LongFileName;

        auto location = entries[lfn_entry_count - i - 1];

        // FIXME: If we fail here, we should clean up the entries we already wrote
        TRY(fs().write_block(location.block, UserOrKernelBuffer::for_kernel_buffer(reinterpret_cast<u8*>(&lfn_entry)), sizeof(FATLongFileNameEntry), location.entry * sizeof(FATLongFileNameEntry)));

        lfn_entries.unchecked_append(lfn_entry);
    }

    return TRY(FATInode::create(fs(), entry, entries[lfn_entry_count], lfn_entries));
}

ErrorOr<void> FATInode::add_child(Inode&, StringView, mode_t)
{
    return EROFS;
}

ErrorOr<void> FATInode::remove_child(StringView name)
{
    MutexLocker locker(m_inode_lock);

    dbgln_if(FAT_DEBUG, "FATInode[{}]::remove_child(): removing inode \"{}\"", identifier(), name);

    VERIFY(has_flag(m_entry.attributes, FATAttributes::Directory));

    Vector<FATLongFileNameEntry> lfn_entries;
    TRY(lfn_entries.try_ensure_capacity(ceil_div(max_filename_length, characters_per_lfn_entry)));
    Vector<EntryLocation> lfn_entry_locations;
    TRY(lfn_entry_locations.try_ensure_capacity(ceil_div(max_filename_length, characters_per_lfn_entry)));
    auto blocks = TRY(read_block_list());

    for (u32 i = 0; i < blocks->size() / sizeof(FATEntry); i++) {
        auto* entry = reinterpret_cast<FATEntry*>(blocks->data() + i * sizeof(FATEntry));

        auto entry_number_bytes = i * sizeof(FATEntry);
        auto block = m_block_list[entry_number_bytes / fs().logical_block_size()];

        auto entries_per_sector = fs().logical_block_size() / sizeof(FATEntry);
        u32 block_entry = i % entries_per_sector;

        if (entry->filename[0] == end_entry_byte) {
            dbgln_if(FAT_DEBUG, "FATInode[{}]::remove_child(): Found end entry", identifier());
            return ENOENT;
        } else if (static_cast<u8>(entry->filename[0]) == unused_entry_byte) {
            dbgln_if(FAT_DEBUG, "FATInode[{}]::remove_child(): Found unused entry", identifier());
            lfn_entries.clear();
            lfn_entry_locations.clear();
        } else if (entry->attributes == FATAttributes::LongFileName) {
            dbgln_if(FAT_DEBUG, "FATInode[{}]::remove_child(): Found LFN entry", identifier());
            lfn_entries.unchecked_append(*reinterpret_cast<FATLongFileNameEntry*>(entry));
            lfn_entry_locations.unchecked_append({ block, block_entry });
        } else {
            dbgln_if(FAT_DEBUG, "FATInode[{}]::remove_child(): Found 8.3 entry at block {}, entry {}", identifier(), block, block_entry);
            lfn_entries.reverse();
            auto filename = TRY(compute_filename(*entry, lfn_entries));
            if (*filename == name) {
                // FIXME: If it's the last entry move the end entry instead of unused entries
                FATEntry unused_entry {};
                unused_entry.filename[0] = unused_entry_byte;
                TRY(fs().write_block(block, UserOrKernelBuffer::for_kernel_buffer(reinterpret_cast<u8*>(&unused_entry)), sizeof(FATEntry), block_entry * sizeof(FATEntry)));

                for (auto const& lfn_entry_location : lfn_entry_locations)
                    TRY(fs().write_block(lfn_entry_location.block, UserOrKernelBuffer::for_kernel_buffer(reinterpret_cast<u8*>(&unused_entry)), sizeof(FATEntry), lfn_entry_location.entry * sizeof(FATEntry)));

                return {};
            }
            lfn_entries.clear_with_capacity();
            lfn_entry_locations.clear_with_capacity();
        }
    }

    return EINVAL;
}

ErrorOr<void> FATInode::chmod(mode_t)
{
    return Error::from_errno(ENOTSUP);
}

ErrorOr<void> FATInode::chown(UserID, GroupID)
{
    return Error::from_errno(ENOTSUP);
}

ErrorOr<void> FATInode::truncate(u64 size)
{
    MutexLocker locker(m_inode_lock);

    if (m_entry.file_size == size)
        return {};

    dbgln_if(FAT_DEBUG, "FATInode[{}]::truncate(): truncating to {}", identifier(), size);

    while (fs().boot_record()->sectors_per_cluster * m_block_list.size() > ceil_div(size, fs().boot_record()->sectors_per_cluster * fs().logical_block_size()))
        TRY(remove_last_cluster_from_chain());

    m_entry.file_size = size;
    set_metadata_dirty(true);

    return {};
}

ErrorOr<void> FATInode::flush_metadata()
{
    if (m_inode_metadata_location.block == 0)
        return {};

    dbgln_if(FAT_DEBUG, "FATInode[{}]::flush_metadata(): Writing entry at block {}, entry {} (size: {})", identifier().index(), m_inode_metadata_location.block, m_inode_metadata_location.entry, m_entry.file_size);

    TRY(fs().write_block(m_inode_metadata_location.block, UserOrKernelBuffer::for_kernel_buffer(reinterpret_cast<u8*>(&m_entry)), sizeof(FATEntry), m_inode_metadata_location.entry * sizeof(FATEntry)));

    set_metadata_dirty(false);
    return {};
}

ErrorOr<void> FATInode::update_timestamps(Optional<UnixDateTime>, Optional<UnixDateTime>, Optional<UnixDateTime>)
{
    // FIXME: Implement FATInode::update_timestamps
    return {};
}

}
