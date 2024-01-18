/*
 * Copyright (c) 2022-2024, Undefine <undefine@undefine.pl>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/CharacterTypes.h>
#include <AK/Time.h>
#include <Kernel/Debug.h>
#include <Kernel/FileSystem/FATFS/Inode.h>
#include <Kernel/Library/KBufferBuilder.h>

namespace Kernel {

ErrorOr<NonnullRefPtr<FATInode>> FATInode::create(FATFS& fs, FATEntry entry, FATEntryLocation inode_metadata_location, Vector<FATLongFileNameEntry> const& lfn_entries)
{
    auto filename = TRY(compute_filename(entry, lfn_entries));
    auto cluster_list = TRY(compute_cluster_list(fs, (static_cast<u32>(entry.first_cluster_high) << 16) | entry.first_cluster_low));
    return adopt_nonnull_ref_or_enomem(new (nothrow) FATInode(fs, entry, inode_metadata_location, move(filename), move(cluster_list)));
}

FATInode::FATInode(FATFS& fs, FATEntry entry, FATEntryLocation inode_metadata_location, NonnullOwnPtr<KString> filename, Vector<u32> cluster_list)
    : Inode(fs, first_cluster(fs.m_fat_version))
    , m_cluster_list(move(cluster_list))
    , m_entry(entry)
    , m_inode_metadata_location(inode_metadata_location)
    , m_filename(move(filename))
{
    dbgln_if(FAT_DEBUG, "FATInode[{}]::FATInode(): Creating inode with filename \"{}\"", identifier(), m_filename);
}

ErrorOr<Vector<u32>> FATInode::compute_cluster_list(FATFS& fs, u32 first_cluster)
{
    // FIXME: We should make sure that there is a lock, but as this function is now static, we don't have access to the inode lock.

    dbgln_if(FAT_DEBUG, "FATInode::compute_cluster_list(): computing block list starting with cluster {}", first_cluster);

    u32 cluster = first_cluster;

    Vector<u32> cluster_list;

    if (cluster == 0)
        return cluster_list;

    while (cluster < fs.end_of_chain_marker()) {
        dbgln_if(FAT_DEBUG, "FATInode::compute_cluster_list(): Appending cluster {} to cluster chain starting with {}", cluster, first_cluster);

        TRY(cluster_list.try_append(cluster));

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
        cluster = TRY(fs.fat_read(cluster));
    }

    return cluster_list;
}

u8 FATInode::lfn_entry_checksum(FATEntry const& entry)
{
    u8 checksum = entry.filename[0];
    for (size_t i = 1; i < normal_filename_length; i++)
        checksum = (checksum << 7) + (checksum >> 1) + entry.filename[i];
    for (size_t i = 0; i < normal_extension_length; i++)
        checksum = (checksum << 7) + (checksum >> 1) + entry.extension[i];
    return checksum;
}

void FATInode::create_83_filename_for(FATEntry& entry, StringView name)
{
    // FIXME: Implement the correct algorithm based on 3.2.4 from http://www.osdever.net/documents/LongFileName.pdf
    for (size_t i = 0; i < min(name.length(), normal_filename_length); i++)
        entry.filename[i] = to_ascii_uppercase(name[i]);
}

ErrorOr<Vector<FATLongFileNameEntry>> FATInode::create_lfn_entries(StringView name, u8 checksum)
{
    u32 lfn_entry_count = ceil_div(name.length(), characters_per_lfn_entry);

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
        lfn_entry.checksum = checksum;
        lfn_entry.attributes = FATAttributes::LongFileName;

        lfn_entries.unchecked_append(lfn_entry);
    }

    return lfn_entries;
}

ErrorOr<Vector<BlockBasedFileSystem::BlockIndex>> FATInode::get_block_list()
{
    VERIFY(m_inode_lock.is_locked());

    dbgln_if(FAT_DEBUG, "FATInode[{}]::get_block_list(): getting block list", identifier());

    Vector<BlockBasedFileSystem::BlockIndex> block_list;

    for (auto cluster : m_cluster_list) {
        auto span = fs().first_block_of_cluster(cluster);
        for (size_t i = 0; i < span.number_of_sectors; i++) {
            dbgln_if(FAT_DEBUG, "FATInode[{}]::get_block_list(): Appending block {} to  block list", identifier(), BlockBasedFileSystem::BlockIndex { span.start_block.value() + i });
            TRY(block_list.try_append(BlockBasedFileSystem::BlockIndex { span.start_block.value() + i }));
        }
    }

    return block_list;
}

ErrorOr<NonnullOwnPtr<KBuffer>> FATInode::read_block_list()
{
    VERIFY(m_inode_lock.is_locked());

    auto block_list = TRY(get_block_list());

    dbgln_if(FAT_DEBUG, "FATInode[{}]::read_block_list(): reading block list ({} blocks)", identifier(), block_list.size());

    auto builder = TRY(KBufferBuilder::try_create());

    u8 buffer[512];
    VERIFY(fs().m_device_block_size <= sizeof(buffer));
    auto buf = UserOrKernelBuffer::for_kernel_buffer(buffer);

    for (BlockBasedFileSystem::BlockIndex block : block_list) {
        dbgln_if(FAT_DEBUG, "FATInode[{}]::read_block_list(): reading block: {}", identifier(), block);
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
            dbgln_if(FAT_DEBUG, "FATInode[{}]::traverse(): Found end entry", identifier());
            return nullptr;
        } else if (static_cast<u8>(entry->filename[0]) == unused_entry_byte) {
            dbgln_if(FAT_DEBUG, "FATInode[{}]::traverse(): Found unused entry", identifier());
            lfn_entries.clear();
        } else if (entry->attributes == FATAttributes::LongFileName) {
            dbgln_if(FAT_DEBUG, "FATInode[{}]::traverse(): Found LFN entry", identifier());
            TRY(lfn_entries.try_append(*reinterpret_cast<FATLongFileNameEntry*>(entry)));
        } else if ((entry->first_cluster_high << 16 | entry->first_cluster_low) <= 1 && entry->file_size > 0) {
            // Because clusters 0 and 1 are reserved, only empty files (size == 0 files)
            // should specify these clusters.
            // This driver uses a cluster number == 0 to represent the root directory inode
            // on FAT12/16 file systems (a signal to look in the root directory region),
            // so we ensure that no entries read off the file system have a cluster number
            // that would also point to this region.
            dbgln_if(FAT_DEBUG, "FATInode[{}]::traverse(): Invalid cluster for entry", identifier());
            return EINVAL;
        } else {
            auto entry_number_bytes = i * sizeof(FATEntry);
            auto cluster = m_cluster_list[entry_number_bytes / (fs().m_device_block_size * fs().m_parameter_block->common_bpb()->sectors_per_cluster)];
            auto block = BlockBasedFileSystem::BlockIndex { fs().first_block_of_cluster(cluster).start_block.value() + (entry_number_bytes % (fs().m_device_block_size * fs().m_parameter_block->common_bpb()->sectors_per_cluster)) / fs().m_device_block_size };

            auto entries_per_sector = fs().m_device_block_size / sizeof(FATEntry);
            u32 block_entry = i % entries_per_sector;

            dbgln_if(FAT_DEBUG, "FATInode[{}]::traverse(): Found 8.3 entry at block {}, entry {}", identifier(), block, block_entry);
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

ErrorOr<void> FATInode::allocate_and_add_cluster_to_chain()
{
    VERIFY(m_inode_lock.is_locked());

    u32 allocated_cluster = TRY(fs().allocate_cluster());
    dbgln_if(FAT_DEBUG, "FATInode[{}]::allocate_and_add_cluster_to_chain(): allocated cluster {}", identifier(), allocated_cluster);

    if (m_cluster_list.is_empty()) {
        // This is the first cluster in the chain, so update the inode metadata.
        if (fs().m_fat_version == FATVersion::FAT32) {
            // Only FAT32 uses the `first_cluster_high` field.
            m_entry.first_cluster_high = allocated_cluster >> 16;
        }

        m_entry.first_cluster_low = allocated_cluster & 0xFFFF;

        set_metadata_dirty(true);
    } else {
        // This is not the first cluster in the chain, so we need to update the
        // FAT entry for the last cluster in the chain to point to the newly
        // allocated cluster.
        TRY(fs().fat_write(m_cluster_list[m_cluster_list.size() - 1], allocated_cluster));
    }

    m_cluster_list.append(allocated_cluster);

    return {};
}

ErrorOr<void> FATInode::remove_last_cluster_from_chain()
{
    VERIFY(m_inode_lock.is_locked());
    VERIFY(m_cluster_list.size() > 0);

    u32 last_cluster = m_cluster_list[m_cluster_list.size() - 1];
    TRY(fs().fat_write(last_cluster, 0x0));

    dbgln_if(FAT_DEBUG, "FATInode[{}]::remove_last_cluster_from_chain(): freeing cluster {}", identifier(), last_cluster);

    m_cluster_list.remove(m_cluster_list.size() - 1);

    if (m_cluster_list.is_empty()) {
        // We have removed the last cluster in the chain, so update the inode metadata.
        if (fs().m_fat_version == FATVersion::FAT32) {
            // Only FAT32 uses the `first_cluster_high` field.
            m_entry.first_cluster_high = 0;
        }

        m_entry.first_cluster_low = 0;

        set_metadata_dirty(true);
    } else {
        // We have removed a cluster from the chain, so update the FAT entry for
        // the last cluster in the chain mark it as the end of the chain.
        last_cluster = m_cluster_list[m_cluster_list.size() - 1];
        TRY(fs().fat_write(last_cluster, fs().end_of_chain_marker()));
    }

    return {};
}

ErrorOr<Vector<FATEntryLocation>> FATInode::allocate_entries(u32 count)
{
    // FIXME: This function ignores unused entries, we should make use of them
    // FIXME: If we fail anywhere here, we should make sure the end entry is at the correct location

    auto blocks = TRY(read_block_list());
    auto entries = bit_cast<FATEntry*>(blocks->data());

    auto const entries_per_block = fs().logical_block_size() / sizeof(FATEntry);

    auto block_list = TRY(get_block_list());

    Vector<FATEntryLocation> locations;
    TRY(locations.try_ensure_capacity(count));

    for (u32 current_entry_index = 0; current_entry_index < blocks->size() / sizeof(FATEntry); current_entry_index++) {
        auto& entry = entries[current_entry_index];
        if (entry.filename[0] == end_entry_byte) {
            while (current_entry_index < blocks->size() / sizeof(FATEntry) && locations.size() < count) {
                u32 chosen_block_index = current_entry_index / entries_per_block;
                u32 chosen_entry_index = current_entry_index % entries_per_block;
                locations.unchecked_append({ block_list[chosen_block_index], chosen_entry_index });
                dbgln_if(FAT_DEBUG, "FATInode[{}]::allocate_entries(): allocated new entry at block {}, offset {}", identifier(), block_list[chosen_block_index], chosen_entry_index);
                current_entry_index++;
            }
            if (locations.size() == count) {
                u32 block_index = current_entry_index / entries_per_block;
                u32 entry_index = current_entry_index % entries_per_block;
                dbgln_if(FAT_DEBUG, "FATInode[{}]::allocate_entries(): putting new end entry at block {}, offset {}", identifier(), block_list[block_index], entry_index);

                FATEntry end_entry {};
                end_entry.filename[0] = end_entry_byte;
                TRY(fs().write_block(block_list[block_index], UserOrKernelBuffer::for_kernel_buffer(bit_cast<u8*>(&end_entry)), sizeof(FATEntry), entry_index * sizeof(FATEntry)));
                break;
            }
        }
    }

    if (locations.size() < count) {
        TRY(allocate_and_add_cluster_to_chain());
        u32 new_block_index = block_list.size() - fs().m_parameter_block->common_bpb()->sectors_per_cluster - 1;
        u32 entry_index;
        for (entry_index = 0; entry_index < count - locations.size(); entry_index++) {
            locations.unchecked_append({ block_list[new_block_index], entry_index });
            dbgln_if(FAT_DEBUG, "FATInode[{}]::allocate_entries(): allocated new entry at block {}, offset {}", identifier(), block_list[new_block_index], entry_index);
        }

        dbgln_if(FAT_DEBUG, "FATInode[{}]::allocate_entries(): putting new end entry at block {}, offset {}", identifier(), block_list[new_block_index], entry_index);

        FATEntry end_entry {};
        end_entry.filename[0] = end_entry_byte;
        TRY(fs().write_block(block_list[new_block_index], UserOrKernelBuffer::for_kernel_buffer(bit_cast<u8*>(&end_entry)), sizeof(FATEntry), entry_index * sizeof(FATEntry)));
    }

    return locations;
}

ErrorOr<size_t> FATInode::read_bytes_locked(off_t offset, size_t size, UserOrKernelBuffer& buffer, OpenFileDescription*) const
{
    dbgln_if(FAT_DEBUG, "FATInode[{}]::read_bytes_locked(): Reading {} bytes at offset {}", identifier(), size, offset);
    VERIFY(offset >= 0);
    if (offset >= m_entry.file_size)
        return 0;

    // FIXME: Read only the needed blocks instead of the whole file
    auto blocks = TRY(const_cast<FATInode&>(*this).read_block_list());

    // Take the minimum of the:
    //   1. User-specified size parameter
    //   2. The file size.
    //   3. The number of blocks returned for reading.
    size_t read_size = min(
        min(size, m_entry.file_size - offset),
        (m_cluster_list.size() * fs().m_device_block_size * fs().m_parameter_block->common_bpb()->sectors_per_cluster) - offset);
    TRY(buffer.write(blocks->data() + offset, read_size));

    return read_size;
}

InodeMetadata FATInode::metadata() const
{
    return {
        .inode = identifier(),
        .size = m_entry.file_size,
        // FIXME: Linux also removes the write permission if the file has the read only attribute set.
        .mode = static_cast<mode_t>((has_flag(m_entry.attributes, FATAttributes::Directory) ? S_IFDIR : S_IFREG) | 0777),
        .uid = 0,
        .gid = 0,
        .link_count = 0,
        .atime = time_from_packed_dos(m_entry.last_accessed_date, { 0 }),
        .ctime = time_from_packed_dos(m_entry.creation_date, m_entry.creation_time),
        .mtime = time_from_packed_dos(m_entry.modification_date, m_entry.modification_time),
        .dtime = {},
        .block_count = m_cluster_list.size() * fs().m_parameter_block->common_bpb()->sectors_per_cluster,
        .block_size = fs().m_device_block_size,
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

    while (offset + size > m_cluster_list.size() * fs().m_parameter_block->common_bpb()->sectors_per_cluster * fs().m_device_block_size)
        TRY(allocate_and_add_cluster_to_chain());

    auto block_list = TRY(get_block_list());

    u32 first_block_index = offset % fs().m_device_block_size;
    size_t offset_into_first_block = offset % fs().m_device_block_size;

    for (u32 block_index = first_block_index; block_index * fs().m_device_block_size < size; block_index++) {
        size_t offset_into_block = block_index == first_block_index ? offset_into_first_block : 0;
        TRY(fs().write_block(block_list[block_index], buffer.offset(block_index * fs().m_device_block_size), min(fs().m_device_block_size - offset_into_block, size - block_index * fs().m_device_block_size), offset_into_block));
    }

    return size;
}

ErrorOr<NonnullRefPtr<Inode>> FATInode::create_child(StringView name, mode_t mode, dev_t, UserID, GroupID)
{
    VERIFY(has_flag(m_entry.attributes, FATAttributes::Directory));

    dbgln_if(FAT_DEBUG, "FATInode[{}]::create_child(): creating inode \"{}\"", identifier(), name);

    FATEntry entry {};
    create_83_filename_for(entry, name);

    // TODO: We should set the hidden attribute if the file starts with a dot or read only (the same way Linux does this).
    if (mode & S_IFDIR)
        entry.attributes |= FATAttributes::Directory;

    // FIXME: Set the dates

    // FIXME: For some filenames lfn entries are not necessary
    auto lfn_entries = TRY(create_lfn_entries(name, lfn_entry_checksum(entry)));

    MutexLocker locker(m_inode_lock);

    auto entries = TRY(allocate_entries(lfn_entries.size() + 1));

    // FIXME: If we fail here we should clean up the entries we wrote
    TRY(fs().write_block(entries[lfn_entries.size()].block, UserOrKernelBuffer::for_kernel_buffer(bit_cast<u8*>(&entry)), sizeof(FATEntry), entries[lfn_entries.size()].entry * sizeof(FATEntry)));

    for (u32 i = 0; i < lfn_entries.size(); i++) {
        auto location = entries[lfn_entries.size() - i - 1];
        TRY(fs().write_block(location.block, UserOrKernelBuffer::for_kernel_buffer(bit_cast<u8*>(&lfn_entries[i])), sizeof(FATLongFileNameEntry), location.entry * sizeof(FATLongFileNameEntry)));
    }

    return TRY(FATInode::create(fs(), entry, entries[lfn_entries.size()], lfn_entries));
}

ErrorOr<void> FATInode::add_child(Inode& inode, StringView name, mode_t mode)
{
    VERIFY(has_flag(m_entry.attributes, FATAttributes::Directory));
    VERIFY(inode.fsid() == fsid());

    // FIXME: There's a lot of similar code between this function and create_child, we should try to factor out some of the common code.

    dbgln_if(FAT_DEBUG, "FATInode[{}]::add_child(): appending inode {} as \"{}\"", identifier(), inode.identifier(), name);

    auto entry = bit_cast<FATInode*>(&inode)->m_entry;
    create_83_filename_for(entry, name);

    // TODO: We should set the hidden attribute if the file starts with a dot or read only (the same way Linux does this).
    if (mode & S_IFDIR)
        entry.attributes |= FATAttributes::Directory;

    // FIXME: Set the dates

    // FIXME: For some filenames lfn entries are not necessary
    auto lfn_entries = TRY(create_lfn_entries(name, lfn_entry_checksum(entry)));

    MutexLocker locker(m_inode_lock);

    auto entries = TRY(allocate_entries(lfn_entries.size() + 1));

    // FIXME: If we fail here we should clean up the entries we wrote
    TRY(fs().write_block(entries[lfn_entries.size()].block, UserOrKernelBuffer::for_kernel_buffer(reinterpret_cast<u8*>(&entry)), sizeof(FATEntry), entries[lfn_entries.size()].entry * sizeof(FATEntry)));

    for (u32 i = 0; i < lfn_entries.size(); i++) {
        auto location = entries[lfn_entries.size() - i - 1];
        TRY(fs().write_block(location.block, UserOrKernelBuffer::for_kernel_buffer(reinterpret_cast<u8*>(&lfn_entries[i])), sizeof(FATLongFileNameEntry), location.entry * sizeof(FATLongFileNameEntry)));
    }

    return {};
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

ErrorOr<void> FATInode::truncate(u64 size)
{
    if (m_entry.file_size == size)
        return {};

    dbgln_if(FAT_DEBUG, "FATInode[{}]::truncate(): truncating to {}", identifier(), size);

    MutexLocker locker(m_inode_lock);

    while (fs().m_parameter_block->common_bpb()->sectors_per_cluster * fs().m_device_block_size * m_cluster_list.size() > ceil_div(size, fs().m_parameter_block->common_bpb()->sectors_per_cluster * fs().m_device_block_size))
        TRY(remove_last_cluster_from_chain());

    m_entry.file_size = size;
    set_metadata_dirty(true);

    return {};
}

ErrorOr<void> FATInode::flush_metadata()
{
    if (m_inode_metadata_location.block == 0)
        return {};

    dbgln_if(FAT_DEBUG, "FATInode[{}]::flush_metadata(): Writing entry at block {}, entry {} (size: {}, cluster_low: {}, cluster_high: {})", identifier().index(), m_inode_metadata_location.block, m_inode_metadata_location.entry, m_entry.file_size, m_entry.first_cluster_low, m_entry.first_cluster_high);

    TRY(fs().write_block(m_inode_metadata_location.block, UserOrKernelBuffer::for_kernel_buffer(bit_cast<u8*>(&m_entry)), sizeof(FATEntry), m_inode_metadata_location.entry * sizeof(FATEntry)));

    set_metadata_dirty(false);
    return {};
}

ErrorOr<void> FATInode::update_timestamps(Optional<UnixDateTime>, Optional<UnixDateTime>, Optional<UnixDateTime>)
{
    // FIXME: Implement FATInode::update_timestamps
    return {};
}

}
