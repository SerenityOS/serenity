/*
 * Copyright (c) 2021, sin-ack <sin-ack@protonmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/CharacterTypes.h>
#include <AK/Endian.h>
#include <Kernel/FileSystem/ISO9660FS/Inode.h>

namespace Kernel {

ErrorOr<size_t> ISO9660Inode::read_bytes_locked(off_t offset, size_t size, UserOrKernelBuffer& buffer, OpenFileDescription*) const
{
    VERIFY(m_inode_lock.is_locked());

    u32 data_length = LittleEndian { m_record.data_length.little };
    u32 extent_location = LittleEndian { m_record.extent_location.little };

    if (static_cast<u64>(offset) >= data_length)
        return 0;

    auto block = TRY(KBuffer::try_create_with_size("ISO9660FS: Inode read buffer"sv, fs().device_block_size()));
    auto block_buffer = UserOrKernelBuffer::for_kernel_buffer(block->data());

    size_t total_bytes = min(size, data_length - offset);
    size_t nread = 0;
    size_t blocks_already_read = offset / fs().device_block_size();
    size_t initial_offset = offset % fs().device_block_size();

    auto current_block_index = BlockBasedFileSystem::BlockIndex { extent_location + blocks_already_read };
    while (nread != total_bytes) {
        size_t bytes_to_read = min(total_bytes - nread, fs().device_block_size() - initial_offset);
        auto buffer_offset = buffer.offset(nread);
        dbgln_if(ISO9660_VERY_DEBUG, "ISO9660Inode::read_bytes: Reading {} bytes into buffer offset {}/{}, logical block index: {}", bytes_to_read, nread, total_bytes, current_block_index.value());

        TRY(const_cast<ISO9660FS&>(fs()).raw_read(current_block_index, block_buffer));
        TRY(buffer_offset.write(block->data() + initial_offset, bytes_to_read));

        nread += bytes_to_read;
        initial_offset = 0;
        current_block_index = BlockBasedFileSystem::BlockIndex { current_block_index.value() + 1 };
    }

    return nread;
}

InodeMetadata ISO9660Inode::metadata() const
{
    return m_metadata;
}

ErrorOr<void> ISO9660Inode::traverse_as_directory(Function<ErrorOr<void>(FileSystem::DirectoryEntryView const&)> visitor) const
{
    Array<u8, max_file_identifier_length> file_identifier_buffer;
    ErrorOr<void> result;

    return fs().visit_directory_record(m_record, [&](ISO::DirectoryRecordHeader const* record) {
        StringView filename = get_normalized_filename(*record, file_identifier_buffer);
        dbgln_if(ISO9660_VERY_DEBUG, "traverse_as_directory(): Found {}", filename);

        InodeIdentifier id { fsid(), get_inode_index(*record, filename) };
        auto entry = FileSystem::DirectoryEntryView(filename, id, static_cast<u8>(record->file_flags));

        result = visitor(entry);
        if (result.is_error())
            return RecursionDecision::Break;

        return RecursionDecision::Continue;
    });
}

ErrorOr<NonnullRefPtr<Inode>> ISO9660Inode::lookup(StringView name)
{
    RefPtr<Inode> inode;
    Array<u8, max_file_identifier_length> file_identifier_buffer;

    TRY(fs().visit_directory_record(m_record, [&](ISO::DirectoryRecordHeader const* record) {
        StringView filename = get_normalized_filename(*record, file_identifier_buffer);

        if (filename == name) {
            auto maybe_inode = ISO9660Inode::try_create_from_directory_record(fs(), *record, filename);
            if (maybe_inode.is_error()) {
                // FIXME: The Inode API does not handle allocation failures very
                //        well... we can't return a ErrorOr from here. It
                //        would be nice if we could return a ErrorOr<void>(Or) from
                //        any place where allocation may happen.
                dbgln("Could not allocate inode for lookup!");
            } else {
                inode = maybe_inode.release_value();
            }
            return RecursionDecision::Break;
        }

        return RecursionDecision::Continue;
    }));

    if (!inode)
        return ENOENT;
    return inode.release_nonnull();
}

ErrorOr<void> ISO9660Inode::flush_metadata()
{
    return {};
}

ErrorOr<size_t> ISO9660Inode::write_bytes_locked(off_t, size_t, UserOrKernelBuffer const&, OpenFileDescription*)
{
    return EROFS;
}

ErrorOr<NonnullRefPtr<Inode>> ISO9660Inode::create_child(StringView, mode_t, dev_t, UserID, GroupID)
{
    return EROFS;
}

ErrorOr<void> ISO9660Inode::add_child(Inode&, StringView, mode_t)
{
    return EROFS;
}

ErrorOr<void> ISO9660Inode::remove_child(StringView)
{
    return EROFS;
}

ErrorOr<void> ISO9660Inode::chmod(mode_t)
{
    return EROFS;
}

ErrorOr<void> ISO9660Inode::chown(UserID, GroupID)
{
    return EROFS;
}

ErrorOr<void> ISO9660Inode::truncate_locked(u64)
{
    return EROFS;
}

ErrorOr<void> ISO9660Inode::update_timestamps(Optional<UnixDateTime>, Optional<UnixDateTime>, Optional<UnixDateTime>)
{
    return EROFS;
}

ISO9660Inode::ISO9660Inode(ISO9660FS& fs, ISO::DirectoryRecordHeader const& record, StringView name)
    : Inode(fs, get_inode_index(record, name))
    , m_record(record)
{
    dbgln_if(ISO9660_VERY_DEBUG, "Creating inode #{}", index());
    create_metadata();
}

ISO9660Inode::~ISO9660Inode() = default;

ErrorOr<NonnullRefPtr<ISO9660Inode>> ISO9660Inode::try_create_from_directory_record(ISO9660FS& fs, ISO::DirectoryRecordHeader const& record, StringView name)
{
    return adopt_nonnull_ref_or_enomem(new (nothrow) ISO9660Inode(fs, record, name));
}

void ISO9660Inode::create_metadata()
{
    u32 data_length = LittleEndian { m_record.data_length.little };
    bool is_directory = has_flag(m_record.file_flags, ISO::FileFlags::Directory);
    auto recorded_at = parse_numerical_date_time(m_record.recording_date_and_time);

    m_metadata = {
        .inode = identifier(),
        .size = data_length,
        .mode = static_cast<mode_t>((is_directory ? S_IFDIR : S_IFREG) | (is_directory ? 0555 : 0444)),
        .uid = 0,
        .gid = 0,
        .link_count = 1,
        .atime = recorded_at,
        .ctime = recorded_at,
        .mtime = recorded_at,
        .dtime = {},
        .block_count = 0,
        .block_size = 0,
        .major_device = 0,
        .minor_device = 0,
    };
}

UnixDateTime ISO9660Inode::parse_numerical_date_time(ISO::NumericalDateAndTime const& date)
{
    i32 year_offset = date.years_since_1900 - 70;

    // FIXME: This ignores timezone information in date.
    return UnixDateTime::from_unix_time_parts(year_offset, date.month, date.day, date.hour, date.minute, date.second, 0);
}

StringView ISO9660Inode::get_normalized_filename(ISO::DirectoryRecordHeader const& record, Bytes buffer)
{
    auto const* file_identifier = reinterpret_cast<u8 const*>(&record + 1);
    auto filename = StringView { file_identifier, record.file_identifier_length };

    if (filename.length() == 1) {
        if (filename[0] == '\0') {
            filename = "."sv;
        }

        if (filename[0] == '\1') {
            filename = ".."sv;
        }
    }

    if (!has_flag(record.file_flags, ISO::FileFlags::Directory)) {
        // FIXME: We currently strip the file version from the filename,
        //        but that may be used later down the line if the file actually
        //        has multiple versions on the disk.
        Optional<size_t> semicolon = filename.find(';');
        if (semicolon.has_value()) {
            filename = filename.substring_view(0, semicolon.value());
        }

        if (filename[filename.length() - 1] == '.') {
            filename = filename.substring_view(0, filename.length() - 1);
        }
    }

    if (filename.length() > buffer.size()) {
        // FIXME: Rock Ridge allows filenames up to 255 characters, so we should
        //        probably support that instead of truncating.
        filename = filename.substring_view(0, buffer.size());
    }

    for (size_t i = 0; i < filename.length(); i++) {
        buffer[i] = to_ascii_lowercase(filename[i]);
    }

    return { buffer.data(), filename.length() };
}

InodeIndex ISO9660Inode::get_inode_index(ISO::DirectoryRecordHeader const& record, StringView name)
{
    if (name.is_null()) {
        // NOTE: This is the index of the root inode.
        return 1;
    }

    return { pair_int_hash(LittleEndian { record.extent_location.little }, string_hash(name.characters_without_null_termination(), name.length())) };
}

}
