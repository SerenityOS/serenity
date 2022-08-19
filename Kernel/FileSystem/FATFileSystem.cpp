/*
 * Copyright (c) 2022, Undefine <undefine@undefine.pl>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Time.h>
#include <Kernel/Debug.h>
#include <Kernel/Devices/BlockDevice.h>
#include <Kernel/FileSystem/FATFileSystem.h>
#include <Kernel/Memory/Region.h>

namespace Kernel {

ErrorOr<NonnullLockRefPtr<FileSystem>> FATFS::try_create(OpenFileDescription& file_description)
{
    return TRY(adopt_nonnull_lock_ref_or_enomem(new (nothrow) FATFS(file_description)));
}

FATFS::FATFS(OpenFileDescription& file_description)
    : BlockBasedFileSystem(file_description)
{
}

bool FATFS::is_initialized_while_locked()
{
    VERIFY(m_lock.is_locked());
    return !m_root_inode.is_null();
}

ErrorOr<void> FATFS::initialize_while_locked()
{
    VERIFY(m_lock.is_locked());
    VERIFY(!is_initialized_while_locked());

    m_boot_record = TRY(KBuffer::try_create_with_size("FATFS: Boot Record"sv, m_logical_block_size));
    auto boot_record_buffer = UserOrKernelBuffer::for_kernel_buffer(m_boot_record->data());
    TRY(raw_read(0, boot_record_buffer));

    if constexpr (FAT_DEBUG) {
        dbgln("FATFS: oem_identifier: {}", boot_record()->oem_identifier);
        dbgln("FATFS: bytes_per_sector: {}", boot_record()->bytes_per_sector);
        dbgln("FATFS: sectors_per_cluster: {}", boot_record()->sectors_per_cluster);
        dbgln("FATFS: reserved_sector_count: {}", boot_record()->reserved_sector_count);
        dbgln("FATFS: fat_count: {}", boot_record()->fat_count);
        dbgln("FATFS: root_directory_entry_count: {}", boot_record()->root_directory_entry_count);
        dbgln("FATFS: media_descriptor_type: {}", boot_record()->media_descriptor_type);
        dbgln("FATFS: sectors_per_track: {}", boot_record()->sectors_per_track);
        dbgln("FATFS: head_count: {}", boot_record()->head_count);
        dbgln("FATFS: hidden_sector_count: {}", boot_record()->hidden_sector_count);
        dbgln("FATFS: sector_count: {}", boot_record()->sector_count);
        dbgln("FATFS: sectors_per_fat: {}", boot_record()->sectors_per_fat);
        dbgln("FATFS: flags: {}", boot_record()->flags);
        dbgln("FATFS: fat_version: {}", boot_record()->fat_version);
        dbgln("FATFS: root_directory_cluster: {}", boot_record()->root_directory_cluster);
        dbgln("FATFS: fs_info_sector: {}", boot_record()->fs_info_sector);
        dbgln("FATFS: backup_boot_sector: {}", boot_record()->backup_boot_sector);
        dbgln("FATFS: drive_number: {}", boot_record()->drive_number);
        dbgln("FATFS: volume_id: {}", boot_record()->volume_id);
    }

    if (boot_record()->signature != signature_1 && boot_record()->signature != signature_2) {
        dbgln("FATFS: Invalid signature");
        return EINVAL;
    }

    m_logical_block_size = boot_record()->bytes_per_sector;
    set_block_size(m_logical_block_size);

    u32 root_directory_sectors = ((boot_record()->root_directory_entry_count * sizeof(FATEntry)) + (m_logical_block_size - 1)) / m_logical_block_size;
    m_first_data_sector = boot_record()->reserved_sector_count + (boot_record()->fat_count * boot_record()->sectors_per_fat) + root_directory_sectors;

    TRY(BlockBasedFileSystem::initialize_while_locked());

    FATEntry root_entry {};

    root_entry.first_cluster_low = boot_record()->root_directory_cluster & 0xFFFF;
    root_entry.first_cluster_high = boot_record()->root_directory_cluster >> 16;

    root_entry.attributes = FATAttributes::Directory;
    m_root_inode = TRY(FATInode::create(*this, root_entry));

    return {};
}

Inode& FATFS::root_inode()
{
    return *m_root_inode;
}

BlockBasedFileSystem::BlockIndex FATFS::first_block_of_cluster(u32 cluster) const
{
    return ((cluster - first_data_cluster) * boot_record()->sectors_per_cluster) + m_first_data_sector;
}

ErrorOr<NonnullLockRefPtr<FATInode>> FATInode::create(FATFS& fs, FATEntry entry, Vector<FATLongFileNameEntry> const& lfn_entries)
{
    auto filename = TRY(compute_filename(entry, lfn_entries));
    return adopt_nonnull_lock_ref_or_enomem(new (nothrow) FATInode(fs, entry, move(filename)));
}

FATInode::FATInode(FATFS& fs, FATEntry entry, NonnullOwnPtr<KString> filename)
    : Inode(fs, first_cluster())
    , m_entry(entry)
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
        .atime = fat_date_time(m_entry.last_accessed_date, { 0 }),
        .ctime = fat_date_time(m_entry.creation_date, m_entry.creation_time),
        .mtime = fat_date_time(m_entry.modification_date, m_entry.modification_time),
        .dtime = 0,
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

    auto fat_sector = TRY(KBuffer::try_create_with_size("FATFS: FAT read buffer"sv, fs().m_logical_block_size));
    auto fat_sector_buffer = UserOrKernelBuffer::for_kernel_buffer(fat_sector->data());

    while (cluster < no_more_clusters) {
        dbgln_if(FAT_DEBUG, "FATFS: Appending cluster {} to inode {}'s cluster chain", cluster, index());

        BlockBasedFileSystem::BlockIndex first_block = fs().first_block_of_cluster(cluster);
        for (u8 i = 0; i < fs().boot_record()->sectors_per_cluster; i++)
            block_list.append(BlockBasedFileSystem::BlockIndex { first_block.value() + i });

        u32 fat_offset = cluster * sizeof(u32);
        u32 fat_sector_index = fs().boot_record()->reserved_sector_count + (fat_offset / fs().m_logical_block_size);
        u32 entry_offset = fat_offset % fs().m_logical_block_size;

        TRY(fs().raw_read(fat_sector_index, fat_sector_buffer));

        cluster = *reinterpret_cast<u32*>(&fat_sector->data()[entry_offset]);
        cluster &= cluster_number_mask;
    }

    return block_list;
}

ErrorOr<NonnullOwnPtr<KBuffer>> FATInode::read_block_list()
{
    VERIFY(m_inode_lock.is_locked());

    dbgln_if(FAT_DEBUG, "FATFS: reading block list for inode {} ({} blocks)", index(), m_block_list.size());

    if (m_block_list.is_empty())
        m_block_list = TRY(compute_block_list());

    auto builder = TRY(KBufferBuilder::try_create());

    u8 buffer[512];
    VERIFY(fs().m_logical_block_size <= sizeof(buffer));
    auto buf = UserOrKernelBuffer::for_kernel_buffer(buffer);

    for (BlockBasedFileSystem::BlockIndex block : m_block_list) {
        dbgln_if(FAT_DEBUG, "FATFS: reading block: {}", block);
        TRY(fs().raw_read(block, buf));
        TRY(builder.append((char const*)buffer, fs().m_logical_block_size));
    }

    auto blocks = builder.build();
    if (!blocks)
        return ENOMEM;
    return blocks.release_nonnull();
}

ErrorOr<LockRefPtr<FATInode>> FATInode::traverse(Function<ErrorOr<bool>(LockRefPtr<FATInode>)> callback)
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
            dbgln_if(FAT_DEBUG, "FATFS: Found 8.3 entry");
            lfn_entries.reverse();
            auto inode = TRY(FATInode::create(fs(), *entry, lfn_entries));
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
        filename.append(byte_terminated_string(StringView(entry.filename, normal_filename_length), ' '));
        if (entry.extension[0] != ' ') {
            filename.append('.');
            filename.append(byte_terminated_string(StringView(entry.extension, normal_extension_length), ' '));
        }
        return TRY(KString::try_create(filename.string_view()));
    } else {
        StringBuilder filename;
        for (auto& lfn_entry : lfn_entries) {
            filename.append(lfn_entry.characters1[0]);
            filename.append(lfn_entry.characters1[1]);
            filename.append(lfn_entry.characters1[2]);
            filename.append(lfn_entry.characters1[3]);
            filename.append(lfn_entry.characters1[4]);
            filename.append(lfn_entry.characters2[0]);
            filename.append(lfn_entry.characters2[1]);
            filename.append(lfn_entry.characters2[2]);
            filename.append(lfn_entry.characters2[3]);
            filename.append(lfn_entry.characters2[4]);
            filename.append(lfn_entry.characters2[5]);
            filename.append(lfn_entry.characters3[0]);
            filename.append(lfn_entry.characters3[1]);
        }

        return TRY(KString::try_create(byte_terminated_string(filename.string_view(), lfn_entry_text_termination)));
    }

    VERIFY_NOT_REACHED();
}

time_t FATInode::fat_date_time(FATPackedDate date, FATPackedTime time)
{
    if (date.value == 0)
        return 0;

    return Time::from_timestamp(first_fat_year + date.year, date.month, date.day, time.hour, time.minute, time.second * 2, 0).to_seconds();
}

StringView FATInode::byte_terminated_string(StringView string, u8 fill_byte)
{
    if (auto index = string.find_last_not(fill_byte); index.has_value())
        return string.substring_view(0, index.value());
    return string;
}

u32 FATInode::first_cluster() const
{
    return (((u32)m_entry.first_cluster_high) << 16) | m_entry.first_cluster_low;
}

ErrorOr<size_t> FATInode::read_bytes_locked(off_t offset, size_t size, UserOrKernelBuffer& buffer, OpenFileDescription*) const
{
    dbgln_if(FAT_DEBUG, "FATFS: Reading inode {}: size: {} offset: {}", identifier().index(), size, offset);

    // FIXME: Read only the needed blocks instead of the whole file
    auto blocks = TRY(const_cast<FATInode&>(*this).read_block_list());
    TRY(buffer.write(blocks->data() + offset, min(size, m_block_list.size() * fs().m_logical_block_size - offset)));

    return min(size, m_block_list.size() * fs().m_logical_block_size - offset);
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

ErrorOr<NonnullLockRefPtr<Inode>> FATInode::lookup(StringView name)
{
    MutexLocker locker(m_inode_lock);

    VERIFY(has_flag(m_entry.attributes, FATAttributes::Directory));

    auto inode = TRY(traverse([name](auto child) -> ErrorOr<bool> {
        return child->m_filename->view() == name;
    }));

    if (inode.is_null())
        return ENOENT;
    else
        return inode.release_nonnull();
}

ErrorOr<size_t> FATInode::write_bytes_locked(off_t, size_t, UserOrKernelBuffer const&, OpenFileDescription*)
{
    return EROFS;
}

ErrorOr<NonnullLockRefPtr<Inode>> FATInode::create_child(StringView, mode_t, dev_t, UserID, GroupID)
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
    return EROFS;
}

ErrorOr<void> FATInode::chown(UserID, GroupID)
{
    return EROFS;
}

ErrorOr<void> FATInode::flush_metadata()
{
    return EROFS;
}

}
