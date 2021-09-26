/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StringView.h>
#include <Kernel/Debug.h>
#include <Kernel/Devices/BlockDevice.h>
#include <Kernel/FileSystem/FATFileSystem.h>

namespace Kernel {

KResultOr<NonnullRefPtr<FATFS>> FATFS::try_create(OpenFileDescription& file_description)
{
    return adopt_nonnull_ref_or_enomem(new (nothrow) FATFS(file_description));
}

FATFS::FATFS(OpenFileDescription& file_description)
    : BlockBasedFileSystem(file_description)
{
}

FATFS::~FATFS()
{
}

KResult FATFS::initialize()
{
    MutexLocker locker(m_lock);

    set_block_size(m_logical_block_size);
    m_disk_geometry = TRY(KBuffer::try_create_with_size(m_logical_block_size, Memory::Region::Access::Read | Memory::Region::Access::Write, "FATFS: Temporary Boot Table"));
    if (!m_disk_geometry) {
        return ENOMEM;
    }
    auto block_buffer = UserOrKernelBuffer::for_kernel_buffer(m_disk_geometry->data());
    bool success = raw_read(0, block_buffer);
    VERIFY(success);

    size_t fat_size_bytes = m_logical_block_size * disk_geometry()->sectors_per_fat;

    if constexpr (FAT_DEBUG) {
        dbgln("FATFS: bytes_per_sector: {}", disk_geometry()->bytes_per_sector);
        dbgln("FATFS: sectors_per_cluster: {}", disk_geometry()->sectors_per_cluster);
        dbgln("FATFS: num_reserved_sectors: {}", disk_geometry()->num_reserved_sectors);
        dbgln("FATFS: num_fats: {}", disk_geometry()->num_fats);
        dbgln("FATFS: max_root_dir_entries: {}", disk_geometry()->max_root_dir_entries);
        dbgln("FATFS: total_sector_count: {}", disk_geometry()->total_sector_count);
        dbgln("FATFS: num_reserved_sectors: {}", disk_geometry()->num_reserved_sectors);
        dbgln("FATFS: sectors_per_fat: {} {} bytes", disk_geometry()->sectors_per_fat, fat_size_bytes);
        dbgln("FATFS: sectors_per_track: {}", disk_geometry()->sectors_per_track);
        dbgln("FATFS: number_of_heads: {}", disk_geometry()->number_of_heads);
        dbgln("FATFS: fat32_sector_count: {}", disk_geometry()->fat32_sector_count);
    }

    if (auto result = BlockBasedFileSystem::initialize(); result.is_error())
        return result;

    m_fat_type = detect_fat_type();

    m_allocation_table = TRY(KBuffer::try_create_with_size(fat_size_bytes, Memory::Region::Access::Read | Memory::Region::Access::Write, "FATFS: Allocation Table"));
    if (!m_allocation_table)
        return ENOMEM;

    auto uok_at_buffer = UserOrKernelBuffer::for_kernel_buffer(m_allocation_table->data());
    success = raw_read_blocks(1, disk_geometry()->sectors_per_fat, uok_at_buffer);
    VERIFY(success);

    m_root_dir_start_sector = 1 + disk_geometry()->sectors_per_fat * disk_geometry()->num_fats;
    m_root_dir_num_sectors = (disk_geometry()->max_root_dir_entries * sizeof(FATDirectoryEntry)) / m_logical_block_size;
    dbgln_if(FAT_DEBUG, "FATFS: Root has max {} entries ({} sectors), starting at sector {}",
        disk_geometry()->max_root_dir_entries, m_root_dir_num_sectors, m_root_dir_start_sector);

    m_allocation_root_sector = m_root_dir_start_sector + m_root_dir_num_sectors;

    m_root_inode = create_root();
    if (!m_root_inode)
        return ENOMEM;

    return KSuccess;
}

FATType FATFS::detect_fat_type() const
{
    // FIXME: FAT16+ detection logic.
    return FAT12;
}

RefPtr<FATFSInode> FATFSInode::create(FATFS& fs, FATDirectoryEntry entry)
{
    return adopt_ref_if_nonnull(new (nothrow) FATFSInode(fs, entry));
}

RefPtr<FATFSInode> FATFS::create_root()
{
    FATDirectoryEntry root_entry;
    // 0 is a more logical cluster, but it makes the InodeMetadata invalid.
    root_entry.first_logical_cluster = 1;
    root_entry.size = m_root_dir_num_sectors * m_logical_block_size;
    root_entry.attributes = FATAttributeType::Subdirectory;

    auto now = kgettimeofday().to_truncated_seconds();
    root_entry.creation_time = root_entry.last_write_time = FATFSInode::fat_time_from_time(now);
    root_entry.creation_date = root_entry.last_write_date = FATFSInode::fat_date_from_time(now);
    root_entry.last_access_date = FATFSInode::fat_date_from_time(now);

    dbgln_if(FAT_DEBUG, "FATFS: Root first_logical_cluster {}, size {}", root_entry.first_logical_cluster, root_entry.size);
    return FATFSInode::create(*this, root_entry);
}

FATFSInode& FATFS::root_inode()
{
    return *m_root_inode;
}

void FATFS::flush_writes()
{
    // FIXME: Implement.
    BlockBasedFileSystem::flush_writes();
}

BlockBasedFileSystem::BlockIndex FATFS::block_for_cluster(u16 cluster) const
{
    return { u16(m_allocation_root_sector + (cluster - 2) * disk_geometry()->sectors_per_cluster) };
}

Vector<BlockBasedFileSystem::BlockIndex> FATFS::block_chain_from(u16 cluster) const
{
    Vector<BlockBasedFileSystem::BlockIndex> list;

    if (cluster == 1) {
        // Special case for the fixed size root directory
        for (u16 i = 0; i < m_root_dir_num_sectors; i++)
            list.append(BlockBasedFileSystem::BlockIndex { (u16)((u16)m_root_dir_start_sector + i) });

        return list;
    }

    while (true) {
        VERIFY(cluster != 0);
        if (is_end_cluster(cluster)) {
            break;
        }
        BlockBasedFileSystem::BlockIndex block = block_for_cluster(cluster);
        for (u8 i = 0; i < disk_geometry()->sectors_per_cluster; i++)
            list.append(BlockBasedFileSystem::BlockIndex { block.value() + i });
        cluster = next_cluster_for(cluster);
    }

    return list;
}

u16 FATFS::next_cluster_for(u16 cluster) const
{
    VERIFY(!is_end_cluster(cluster));
    VERIFY(cluster >= 2); // First two clusters are reserved.

    if (m_fat_type == FAT12) {
        // Two 12 bit entries packed into 3 bytes.
        int idx = (3 * cluster) / 2;
        u8 a = allocation_table_byte(idx);
        u8 b = allocation_table_byte(idx + 1);

        u16 next;
        if (cluster % 2 == 0) {
            next = a | (0b1111 & b) << 8;
        } else {
            next = (b << 4) | (a >> 4 & 0b1111);
        }
        return next;
    } else {
        // FIXME: Support FAT16/32.
        VERIFY(false);
    }
}

bool FATFS::is_end_cluster(u16 index) const
{
    if (m_fat_type == FAT12) {
        // FIXME account for more than FAT12.
        return index >= 0xFF8 && index <= 0xFFF;
    } else {
        VERIFY(false);
    }
}

FATFSInode::FATFSInode(FATFS& fs, FATDirectoryEntry entry)
    : Inode(fs, entry.first_logical_cluster)
{
    m_raw_entry = entry;

    recalculate_block_list();
}

void FATFSInode::recalculate_block_list()
{
    m_block_list = fs().block_chain_from(m_raw_entry.first_logical_cluster);
}

FATFSInode::~FATFSInode()
{
}

u64 FATFSInode::size() const
{
    if (is_directory()) {
        VERIFY(m_block_list.size() > 0);
        return m_block_list.size() * fs().disk_geometry()->bytes_per_sector;
    } else {
        return m_raw_entry.size;
    }
}

u16 FATFSInode::fat_time_from_time(time_t t)
{
    int seconds = t % 60;
    t -= seconds;
    int minutes = (t / 60) % 60;
    t -= minutes * 60;
    int hours = (t / 3600) % 24;

    return seconds >> 1 | minutes << 5 | hours << 11;
}

u16 FATFSInode::fat_date_from_time(time_t t)
{
    time_t hms = t % (24 * 3600);
    t -= hms;
    time_t day = t / (24 * 3600) % 30;
    t -= day * 24 * 3600;
    time_t month = t / (30 * 24 * 3600) % 12;
    t -= month * 30 * 24 * 3600;
    time_t year = t / (365 * 24 * 3600) - (1980 - 1970);

    return day | month << 5 | year << 9;
}

time_t FATFSInode::time_from_fat_date_time(u16 d, u16 t)
{
    time_t year = (d >> 9) + (1980 - 1970);
    time_t month = (d >> 5) & 0b1111;
    time_t day = d & 0b11111;

    time_t hour = t >> 11;
    time_t minute = (t >> 5) & 0b1111111;
    time_t second = (t & 0b11111) << 1;

    return second + minute * 60 + hour * 3600
        + day + (24 * 3600) + month * (30 * 24 * 3600) + year * (365 * 24 * 3600);
}

InodeMetadata FATFSInode::metadata() const
{
    MutexLocker locker(m_inode_lock);
    InodeMetadata metadata;
    metadata.inode = { fsid(), m_raw_entry.first_logical_cluster };
    metadata.atime = time_from_fat_date_time(m_raw_entry.last_access_date, 0);
    metadata.ctime = FATFSInode::time_from_fat_date_time(m_raw_entry.creation_date, m_raw_entry.creation_time);
    metadata.mtime = FATFSInode::time_from_fat_date_time(m_raw_entry.last_write_date, m_raw_entry.last_write_time);
    metadata.size = size();

    metadata.mode = 0777 | S_ISVTX;
    if (is_directory())
        metadata.mode |= S_IFDIR;
    else
        metadata.mode |= S_IFREG;

    metadata.link_count = 0;
    metadata.block_size = fs().block_size();
    metadata.block_count = m_block_list.size();

    return metadata;
}

void FATFSInode::flush_metadata()
{
    MutexLocker locker(m_inode_lock);
    dbgln_if(FAT_DEBUG, "FATFSInode[{}]::flush_metadata(): Flushing inode", identifier());
    set_metadata_dirty(false);
}

RefPtr<Inode> FATFS::get_inode(InodeIdentifier inode) const
{
    VERIFY_NOT_REACHED();
    MutexLocker locker(m_lock);
    VERIFY(inode.fsid() == fsid());

    return {};
}

KResultOr<size_t> FATFSInode::read_bytes(off_t offset, size_t count, UserOrKernelBuffer& buffer, OpenFileDescription* description) const
{
    MutexLocker inode_locker(m_inode_lock);
    VERIFY(offset >= 0);

    if (static_cast<u64>(offset) >= size())
        return 0;

    size_t nread = 0;
    auto remaining_count = min((off_t)count, (off_t)size() - offset);

    bool allow_cache = !description || !description->is_direct();

    const int block_size = fs().block_size();
    u16 first_block_logical_index = offset / block_size;
    u16 last_block_logical_index = (offset + count) / block_size;
    if (last_block_logical_index >= m_block_list.size())
        last_block_logical_index = m_block_list.size() - 1;

    for (auto bi = first_block_logical_index; remaining_count && bi <= last_block_logical_index; bi++) {
        auto block_index = m_block_list[bi];
        size_t block_offset = (bi == first_block_logical_index) ? offset % block_size : 0;
        size_t num_bytes_to_copy = min((size_t)block_size - block_offset, (size_t)remaining_count);
        auto buffer_offset = buffer.offset(nread);
        if (auto result = fs().read_block(block_index, &buffer_offset, num_bytes_to_copy, block_offset, allow_cache); result.is_error()) {
            dmesgln("FATFSFSInode[{}]::read_bytes(): Failed to read block {} (index {})", identifier(), block_index, bi);
            return result.error();
        }
        remaining_count -= num_bytes_to_copy;
        nread += num_bytes_to_copy;
    }
    return nread;
}

KResult FATFSInode::resize(u64 new_size)
{
    VERIFY_NOT_REACHED();
    auto old_size = size();
    if (old_size == new_size)
        return KSuccess;

    return KSuccess;
}

#pragma GCC diagnostic push
#pragma GCC diagnostic ignored "-Wunused-parameter"
KResultOr<size_t> FATFSInode::write_bytes(off_t offset, size_t count, const UserOrKernelBuffer& data, OpenFileDescription* description)
{
    VERIFY_NOT_REACHED();
    VERIFY(offset >= 0);

    if (count == 0)
        return 0;

    MutexLocker inode_locker(m_inode_lock);
    int nwritten = 0;

    return nwritten;
}
#pragma GCC diagnostic pop

u8 FATFS::internal_file_type_to_directory_entry_type(const DirectoryEntryView& entry) const
{
    if (entry.file_type & FATAttributeType::Subdirectory)
        return DT_DIR;
    return DT_REG;
}

KResult FATFSInode::traverse_as_directory(Function<bool(FileSystem::DirectoryEntryView const&)> callback) const
{
    VERIFY(is_directory());

    u8 buffer[sizeof(FATDirectoryEntry)];
    auto buf = UserOrKernelBuffer::for_kernel_buffer(buffer);

    for (u64 offset = 0; offset < size(); offset += sizeof(FATDirectoryEntry)) {
        if (auto result = read_bytes(offset, sizeof(FATDirectoryEntry), buf, nullptr); result.is_error())
            return result.error();
        auto* entry = reinterpret_cast<FATDirectoryEntry*>(buffer);
        if (entry->filename[0] == 0xE5) {
            // Unused, may have other entries used.
        } else if (entry->filename[0] == 0x00) {
            // Unused, no further entries;
            break;
        } else {
            String filename = dos_filename_from_directory_entry(*entry);
            dbgln_if(FAT_DEBUG, "FATFS: traverse_as_directory entry: [{}] @{}", filename, entry->first_logical_cluster);
            callback({ filename, { fsid(), entry->first_logical_cluster }, entry->attributes });
        }
    }

    return KSuccess;
}
String FATFSInode::dos_filename_from_directory_entry(FATDirectoryEntry& entry)
{
    StringBuilder builder;
    builder.append(space_terminated_filename(entry.filename, 8));
    auto ext = space_terminated_filename(entry.extension, 3);
    if (ext.length() > 0) {
        builder.append(".", 1);
        builder.append(ext);
    }
    return builder.build();
}

StringView FATFSInode::space_terminated_filename(const unsigned char* filename, size_t max_length)
{
    size_t length = 0;
    while (length != max_length) {
        if (filename[length] == ' ') {
            return StringView(filename, length);
        }
        length += 1;
    }
    return StringView(filename, max_length);
}

#pragma GCC diagnostic push
#pragma GCC diagnostic ignored "-Wunused-parameter"
KResult FATFSInode::write_directory(Vector<FATFSDirectoryEntry>& entries)
{
    MutexLocker locker(m_inode_lock);
    VERIFY_NOT_REACHED();

    return KSuccess;
}
#pragma GCC diagnostic pop

#pragma GCC diagnostic push
#pragma GCC diagnostic ignored "-Wunused-parameter"
KResultOr<NonnullRefPtr<Inode>> FATFSInode::create_child(StringView name, mode_t mode, dev_t dev, UserID uid, GroupID gid)
{
    dbgln_if(FAT_DEBUG, "FATFSInode: create_child: {} {} {} {} {}", name, mode, dev, uid, gid);

    VERIFY_NOT_REACHED();

    return ENOENT;
}
#pragma GCC diagnostic pop

#pragma GCC diagnostic push
#pragma GCC diagnostic ignored "-Wunused-parameter"
KResult FATFSInode::add_child(Inode& child, const StringView& name, mode_t mode)
{
    VERIFY_NOT_REACHED();

    MutexLocker locker(m_inode_lock);
    VERIFY(is_directory());
    dbgln_if(FAT_DEBUG, "FATFSInode: add_child {} {}", name, mode);

    return KSuccess;
}
#pragma GCC diagnostic pop

#pragma GCC diagnostic push
#pragma GCC diagnostic ignored "-Wunused-parameter"
KResult FATFSInode::remove_child(const StringView& name)
{
    VERIFY_NOT_REACHED();

    MutexLocker locker(m_inode_lock);
    dbgln_if(FAT_DEBUG, "FATFSInode[{}]::remove_child(): Removing '{}'", identifier(), name);
    VERIFY(is_directory());

    return KSuccess;
}
#pragma GCC diagnostic pop

#pragma GCC diagnostic push
#pragma GCC diagnostic ignored "-Wunused-parameter"
KResult FATFS::create_directory(FATFSInode& parent_inode, const String& name, mode_t mode, uid_t uid, gid_t gid)
{
    VERIFY_NOT_REACHED();

    MutexLocker locker(m_lock);
    VERIFY(is_directory(mode));
    VERIFY_NOT_REACHED();

    return KSuccess;
}
#pragma GCC diagnostic pop

#pragma GCC diagnostic push
#pragma GCC diagnostic ignored "-Wunused-parameter"
KResultOr<NonnullRefPtr<Inode>> FATFS::create_inode(FATFSInode& parent_inode, const String& name, mode_t mode, dev_t dev, uid_t uid, gid_t gid)
{
    VERIFY_NOT_REACHED();

    return ENOENT;
}
#pragma GCC diagnostic pop

KResultOr<NonnullRefPtr<Inode>> FATFSInode::lookup(StringView name)
{
    dbgln_if(FAT_DEBUG, "FATFSInode[{}]:lookup(): Looking up '{}' in dir_size={}", identifier(), name, size());
    VERIFY(is_directory());

    u8 buffer[sizeof(FATDirectoryEntry)];
    auto buf = UserOrKernelBuffer::for_kernel_buffer(buffer);

    // FIXME: We should have a cache built by "traverse_as_directory" of all entries rather than duplicating.
    for (u64 offset = 0; offset < size(); offset += sizeof(FATDirectoryEntry)) {
        if (auto result = read_bytes(offset, sizeof(FATDirectoryEntry), buf, nullptr); result.is_error())
            return result.error();
        auto* entry = reinterpret_cast<FATDirectoryEntry*>(buffer);
        if (entry->filename[0] == 0xE5) {
            // FIXME: Support long filenames.
            continue;
        } else if (entry->filename[0] == 0x00) {
            // Unused, no further entries.
            break;
        } else {
            String filename = dos_filename_from_directory_entry(*entry);
            if (filename.equals_ignoring_case(name)) {
                auto inode = FATFSInode::create(fs(), *entry);
                if (!inode)
                    return ENOENT;
                return inode.release_nonnull();
            }
        }
    }
    return ENOENT;
}

void FATFSInode::one_ref_left()
{
}

KResult FATFSInode::set_atime(time_t t)
{
    MutexLocker locker(m_inode_lock);
    if (fs().is_readonly())
        return EROFS;

    m_raw_entry.last_access_date = FATFSInode::fat_date_from_time(t);

    set_metadata_dirty(true);
    return KSuccess;
}

KResult FATFSInode::set_ctime(time_t t)
{
    MutexLocker locker(m_inode_lock);
    if (fs().is_readonly())
        return EROFS;

    m_raw_entry.creation_time = FATFSInode::fat_time_from_time(t);
    m_raw_entry.creation_date = FATFSInode::fat_date_from_time(t);

    set_metadata_dirty(true);
    return KSuccess;
}

KResult FATFSInode::set_mtime(time_t t)
{
    MutexLocker locker(m_inode_lock);
    if (fs().is_readonly())
        return EROFS;

    m_raw_entry.last_write_time = FATFSInode::fat_time_from_time(t);
    m_raw_entry.last_write_date = FATFSInode::fat_date_from_time(t);

    set_metadata_dirty(true);
    return KSuccess;
}
#pragma GCC diagnostic pop

#pragma GCC diagnostic push
#pragma GCC diagnostic ignored "-Wunused-parameter"
KResult FATFSInode::chmod(mode_t mode)
{
    VERIFY_NOT_REACHED();

    MutexLocker locker(m_inode_lock);

    // FIXME: Implement.
    VERIFY(false);

    set_metadata_dirty(true);
    return KSuccess;
}
#pragma GCC diagnostic pop

#pragma GCC diagnostic push
#pragma GCC diagnostic ignored "-Wunused-parameter"
KResult FATFSInode::chown(UserID uid, GroupID gid)
{
    VERIFY_NOT_REACHED();

    MutexLocker locker(m_inode_lock);

    set_metadata_dirty(true);
    return KSuccess;
}
#pragma GCC diagnostic pop

#pragma GCC diagnostic push
#pragma GCC diagnostic ignored "-Wunused-parameter"
KResult FATFSInode::truncate(u64 size)
{
    VERIFY_NOT_REACHED();

    MutexLocker locker(m_inode_lock);

    set_metadata_dirty(true);
    return KSuccess;
}
#pragma GCC diagnostic pop

unsigned FATFS::total_block_count() const
{
    MutexLocker locker(m_lock);

    return disk_geometry()->total_sector_count;
}

unsigned FATFS::free_block_count() const
{
    MutexLocker locker(m_lock);

    unsigned free_block_count = 0;
    for (u16 block = 2; block < disk_geometry()->total_sector_count - m_allocation_root_sector; block++) {
        if (next_cluster_for(block) == 0)
            free_block_count++;
    }

    return free_block_count;
}

}
