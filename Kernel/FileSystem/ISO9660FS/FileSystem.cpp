/*
 * Copyright (c) 2021, sin-ack <sin-ack@protonmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Endian.h>
#include <Kernel/FileSystem/ISO9660FS/DirectoryIterator.h>
#include <Kernel/FileSystem/ISO9660FS/FileSystem.h>
#include <Kernel/FileSystem/ISO9660FS/Inode.h>

namespace Kernel {

// NOTE: According to the spec, logical blocks 0 to 15 are system use.
constexpr u32 first_data_area_block = 16;
constexpr u32 logical_sector_size = 2048;
constexpr u32 max_cached_directory_entries = 128;

ErrorOr<NonnullRefPtr<FileSystem>> ISO9660FS::try_create(OpenFileDescription& description, FileSystemSpecificOptions const&)
{
    return TRY(adopt_nonnull_ref_or_enomem(new (nothrow) ISO9660FS(description)));
}

ISO9660FS::ISO9660FS(OpenFileDescription& description)
    : BlockBasedFileSystem(description)
{
    set_logical_block_size(logical_sector_size);
    m_device_block_size = logical_sector_size;
}

ISO9660FS::~ISO9660FS() = default;

bool ISO9660FS::is_initialized_while_locked()
{
    VERIFY(m_lock.is_locked());
    return !m_root_inode.is_null();
}

ErrorOr<void> ISO9660FS::initialize_while_locked()
{
    VERIFY(m_lock.is_locked());
    VERIFY(!is_initialized_while_locked());

    TRY(BlockBasedFileSystem::initialize_while_locked());
    TRY(parse_volume_set());
    TRY(create_root_inode());
    return {};
}

Inode& ISO9660FS::root_inode()
{
    VERIFY(!m_root_inode.is_null());
    return *m_root_inode;
}

ErrorOr<void> ISO9660FS::rename(Inode&, StringView, Inode&, StringView)
{
    return EROFS;
}

unsigned ISO9660FS::total_block_count() const
{
    return LittleEndian { m_primary_volume->volume_space_size.little };
}

unsigned ISO9660FS::total_inode_count() const
{
    if (!m_cached_inode_count) {
        auto result = calculate_inode_count();
        if (result.is_error()) {
            // FIXME: This should be able to return a ErrorOr<void>.
            return 0;
        }
    }

    return m_cached_inode_count;
}

u8 ISO9660FS::internal_file_type_to_directory_entry_type(DirectoryEntryView const& entry) const
{
    if (has_flag(static_cast<ISO::FileFlags>(entry.file_type), ISO::FileFlags::Directory)) {
        return DT_DIR;
    }

    return DT_REG;
}

ErrorOr<void> ISO9660FS::prepare_to_clear_last_mount(Inode&)
{
    // FIXME: Do proper cleaning here.
    return {};
}

ErrorOr<void> ISO9660FS::parse_volume_set()
{
    VERIFY(!m_primary_volume);

    auto block = TRY(KBuffer::try_create_with_size("ISO9660FS: Temporary volume descriptor storage"sv, m_device_block_size, Memory::Region::Access::Read | Memory::Region::Access::Write));
    auto block_buffer = UserOrKernelBuffer::for_kernel_buffer(block->data());

    auto current_block_index = first_data_area_block;
    while (true) {
        auto result = raw_read(BlockIndex { current_block_index }, block_buffer);
        if (result.is_error()) {
            dbgln_if(ISO9660_DEBUG, "Failed to read volume descriptor from ISO file: {}", result.error());
            return result;
        }

        auto const* header = reinterpret_cast<ISO::VolumeDescriptorHeader const*>(block->data());
        if (StringView { header->identifier, 5 } != "CD001"sv) {
            dbgln_if(ISO9660_DEBUG, "Header magic at volume descriptor {} is not valid", current_block_index - first_data_area_block);
            return EIO;
        }

        switch (header->type) {
        case ISO::VolumeDescriptorType::PrimaryVolumeDescriptor: {
            auto const* primary_volume = reinterpret_cast<ISO::PrimaryVolumeDescriptor const*>(header);
            m_primary_volume = adopt_own_if_nonnull(new ISO::PrimaryVolumeDescriptor(*primary_volume));
            break;
        }
        case ISO::VolumeDescriptorType::BootRecord:
        case ISO::VolumeDescriptorType::SupplementaryOrEnhancedVolumeDescriptor:
        case ISO::VolumeDescriptorType::VolumePartitionDescriptor: {
            break;
        }
        case ISO::VolumeDescriptorType::VolumeDescriptorSetTerminator: {
            goto all_headers_read;
        }
        default:
            dbgln_if(ISO9660_DEBUG, "Unexpected volume descriptor type {} in volume set", static_cast<u8>(header->type));
            return EIO;
        }

        current_block_index++;
    }

all_headers_read:
    if (!m_primary_volume) {
        dbgln_if(ISO9660_DEBUG, "Could not find primary volume");
        return EIO;
    }

    m_device_block_size = LittleEndian { m_primary_volume->logical_block_size.little };
    return {};
}

ErrorOr<void> ISO9660FS::create_root_inode()
{
    if (!m_primary_volume) {
        dbgln_if(ISO9660_DEBUG, "Primary volume doesn't exist, can't create root inode");
        return EIO;
    }

    m_root_inode = TRY(ISO9660Inode::try_create_from_directory_record(*this, m_primary_volume->root_directory_record_header, {}));
    return {};
}

ErrorOr<void> ISO9660FS::calculate_inode_count() const
{
    if (!m_primary_volume) {
        dbgln_if(ISO9660_DEBUG, "Primary volume doesn't exist, can't calculate inode count");
        return EIO;
    }

    size_t inode_count = 1;

    TRY(visit_directory_record(m_primary_volume->root_directory_record_header, [&](ISO::DirectoryRecordHeader const* header) {
        if (header == nullptr) {
            return RecursionDecision::Continue;
        }

        inode_count += 1;

        if (has_flag(header->file_flags, ISO::FileFlags::Directory)) {
            if (header->file_identifier_length == 1) {
                auto file_identifier = reinterpret_cast<u8 const*>(header + 1);
                if (file_identifier[0] == '\0' || file_identifier[0] == '\1') {
                    return RecursionDecision::Continue;
                }
            }

            return RecursionDecision::Recurse;
        }

        return RecursionDecision::Continue;
    }));

    m_cached_inode_count = inode_count;
    return {};
}

ErrorOr<void> ISO9660FS::visit_directory_record(ISO::DirectoryRecordHeader const& record, Function<ErrorOr<RecursionDecision>(ISO::DirectoryRecordHeader const*)> const& visitor) const
{
    if (!has_flag(record.file_flags, ISO::FileFlags::Directory)) {
        return {};
    }

    ISO9660DirectoryIterator iterator { const_cast<ISO9660FS&>(*this), record };

    while (!iterator.done()) {
        auto decision = TRY(visitor(*iterator));
        switch (decision) {
        case RecursionDecision::Recurse: {
            auto has_moved = TRY(iterator.next());
            if (!has_moved) {
                // If next() hasn't moved then we have read through all the
                // directories, and can exit.
                return {};
            }

            continue;
        }
        case RecursionDecision::Continue: {
            while (!iterator.done()) {
                if (iterator.skip())
                    break;
                if (!iterator.go_up())
                    return {};
            }

            continue;
        }
        case RecursionDecision::Break:
            return {};
        }
    }

    return {};
}

ErrorOr<NonnullLockRefPtr<ISO9660FSDirectoryEntry>> ISO9660FS::directory_entry_for_record(Badge<ISO9660DirectoryIterator>, ISO::DirectoryRecordHeader const* record)
{
    u32 extent_location = LittleEndian { record->extent_location.little };
    u32 data_length = LittleEndian { record->data_length.little };

    auto key = calculate_directory_entry_cache_key(*record);
    auto it = m_directory_entry_cache.find(key);
    if (it != m_directory_entry_cache.end()) {
        dbgln_if(ISO9660_DEBUG, "Cache hit for dirent @ {}", extent_location);
        return it->value;
    }
    dbgln_if(ISO9660_DEBUG, "Cache miss for dirent @ {} :^(", extent_location);

    if (m_directory_entry_cache.size() == max_cached_directory_entries) {
        // FIXME: A smarter algorithm would probably be nicer.
        m_directory_entry_cache.remove(m_directory_entry_cache.begin());
    }

    if (!(data_length % device_block_size() == 0)) {
        dbgln_if(ISO9660_DEBUG, "Found a directory with non-logical block size aligned data length!");
        return EIO;
    }

    auto blocks = TRY(KBuffer::try_create_with_size("ISO9660FS: Directory traversal buffer"sv, data_length, Memory::Region::Access::Read | Memory::Region::Access::Write));
    auto blocks_buffer = UserOrKernelBuffer::for_kernel_buffer(blocks->data());
    TRY(raw_read_blocks(BlockBasedFileSystem::BlockIndex { extent_location }, data_length / device_block_size(), blocks_buffer));
    auto entry = TRY(ISO9660FSDirectoryEntry::try_create(extent_location, data_length, move(blocks)));
    m_directory_entry_cache.set(key, entry);

    dbgln_if(ISO9660_DEBUG, "Cached dirent @ {}", extent_location);
    return entry;
}

u32 ISO9660FS::calculate_directory_entry_cache_key(ISO::DirectoryRecordHeader const& record)
{
    return LittleEndian { record.extent_location.little };
}

}
