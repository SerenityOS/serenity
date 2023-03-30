/*
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@serenityos.org>
 * Copyright (c) 2022-2023, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/LexicalPath.h>
#include <LibArchive/Zip.h>
#include <LibCompress/Deflate.h>
#include <LibCore/DateTime.h>
#include <LibCore/DirIterator.h>
#include <LibCore/System.h>
#include <LibCrypto/Checksum/CRC32.h>
#include <LibFileSystem/FileSystem.h>

namespace Archive {

bool Zip::find_end_of_central_directory_offset(ReadonlyBytes buffer, size_t& offset)
{
    for (size_t backwards_offset = 0; backwards_offset <= UINT16_MAX; backwards_offset++) // the file may have a trailing comment of an arbitrary 16 bit length
    {
        if (buffer.size() < (sizeof(EndOfCentralDirectory) - sizeof(u8*)) + backwards_offset)
            return false;

        auto const signature_offset = (buffer.size() - (sizeof(EndOfCentralDirectory) - sizeof(u8*)) - backwards_offset);
        if (auto signature = ReadonlyBytes { buffer.data() + signature_offset, EndOfCentralDirectory::signature.size() };
            signature == EndOfCentralDirectory::signature) {
            offset = signature_offset;
            return true;
        }
    }
    return false;
}

Optional<Zip> Zip::try_create(ReadonlyBytes buffer)
{
    size_t end_of_central_directory_offset;
    if (!find_end_of_central_directory_offset(buffer, end_of_central_directory_offset))
        return {};

    EndOfCentralDirectory end_of_central_directory {};
    if (!end_of_central_directory.read(buffer.slice(end_of_central_directory_offset)))
        return {};

    if (end_of_central_directory.disk_number != 0 || end_of_central_directory.central_directory_start_disk != 0 || end_of_central_directory.disk_records_count != end_of_central_directory.total_records_count)
        return {}; // TODO: support multi-volume zip archives

    size_t member_offset = end_of_central_directory.central_directory_offset;
    for (size_t i = 0; i < end_of_central_directory.total_records_count; i++) {
        CentralDirectoryRecord central_directory_record {};
        if (member_offset > buffer.size())
            return {};
        if (!central_directory_record.read(buffer.slice(member_offset)))
            return {};
        if (central_directory_record.general_purpose_flags.encrypted)
            return {}; // TODO: support encrypted zip members
        if (central_directory_record.general_purpose_flags.data_descriptor)
            return {}; // TODO: support zip data descriptors
        if (central_directory_record.compression_method != ZipCompressionMethod::Store && central_directory_record.compression_method != ZipCompressionMethod::Deflate)
            return {}; // TODO: support obsolete zip compression methods
        if (central_directory_record.compression_method == ZipCompressionMethod::Store && central_directory_record.uncompressed_size != central_directory_record.compressed_size)
            return {};
        if (central_directory_record.start_disk != 0)
            return {}; // TODO: support multi-volume zip archives
        if (memchr(central_directory_record.name, 0, central_directory_record.name_length) != nullptr)
            return {};
        LocalFileHeader local_file_header {};
        if (central_directory_record.local_file_header_offset > buffer.size())
            return {};
        if (!local_file_header.read(buffer.slice(central_directory_record.local_file_header_offset)))
            return {};
        if (buffer.size() - (local_file_header.compressed_data - buffer.data()) < central_directory_record.compressed_size)
            return {};
        member_offset += central_directory_record.size();
    }

    return Zip {
        end_of_central_directory.total_records_count,
        end_of_central_directory.central_directory_offset,
        buffer,
    };
}

ErrorOr<bool> Zip::for_each_member(Function<IterationDecision(ZipMember const&)> callback)
{
    size_t member_offset = m_members_start_offset;
    for (size_t i = 0; i < m_member_count; i++) {
        CentralDirectoryRecord central_directory_record {};
        VERIFY(central_directory_record.read(m_input_data.slice(member_offset)));
        LocalFileHeader local_file_header {};
        VERIFY(local_file_header.read(m_input_data.slice(central_directory_record.local_file_header_offset)));

        ZipMember member;
        member.name = TRY(String::from_utf8({ central_directory_record.name, central_directory_record.name_length }));
        member.compressed_data = { local_file_header.compressed_data, central_directory_record.compressed_size };
        member.compression_method = central_directory_record.compression_method;
        member.uncompressed_size = central_directory_record.uncompressed_size;
        member.crc32 = central_directory_record.crc32;
        member.modification_time = central_directory_record.modification_time;
        member.modification_date = central_directory_record.modification_date;
        member.is_directory = central_directory_record.external_attributes & zip_directory_external_attribute || member.name.bytes_as_string_view().ends_with('/'); // FIXME: better directory detection

        if (callback(member) == IterationDecision::Break)
            return false;

        member_offset += central_directory_record.size();
    }
    return true;
}

ZipOutputStream::ZipOutputStream(NonnullOwnPtr<Stream> stream)
    : m_stream(move(stream))
{
}

static u16 minimum_version_needed(ZipCompressionMethod method)
{
    // Deflate was added in PKZip 2.0
    return method == ZipCompressionMethod::Deflate ? 20 : 10;
}

ErrorOr<void> ZipOutputStream::add_member(ZipMember const& member)
{
    VERIFY(!m_finished);
    VERIFY(member.name.bytes_as_string_view().length() <= UINT16_MAX);
    VERIFY(member.compressed_data.size() <= UINT32_MAX);
    TRY(m_members.try_append(member));

    LocalFileHeader local_file_header {
        .minimum_version = minimum_version_needed(member.compression_method),
        .general_purpose_flags = { .flags = 0 },
        .compression_method = static_cast<u16>(member.compression_method),
        .modification_time = member.modification_time,
        .modification_date = member.modification_date,
        .crc32 = member.crc32,
        .compressed_size = static_cast<u32>(member.compressed_data.size()),
        .uncompressed_size = member.uncompressed_size,
        .name_length = static_cast<u16>(member.name.bytes_as_string_view().length()),
        .extra_data_length = 0,
        .name = reinterpret_cast<u8 const*>(member.name.bytes_as_string_view().characters_without_null_termination()),
        .extra_data = nullptr,
        .compressed_data = member.compressed_data.data(),
    };
    return local_file_header.write(*m_stream);
}

ErrorOr<void> ZipOutputStream::add_member_from_path(StringView path, RecurseThroughDirectories recurse_through_directories, AddMemberCallback const& callback)
{
    if (FileSystem::is_directory(path)) {
        return add_directory_from_path(path, recurse_through_directories, callback);
    }

    auto result = TRY(add_file_from_path(path));
    callback(result);

    return {};
}

ErrorOr<ZipMemberFromFileResult> ZipOutputStream::add_file_from_path(StringView path)
{
    auto file = TRY(Core::File::open(path, Core::File::OpenMode::Read));
    auto file_buffer = TRY(file->read_until_eof());
    auto canonicalized_path = TRY(String::from_deprecated_string(LexicalPath::canonicalized_path(path)));

    auto stat = TRY(Core::System::fstat(file->fd()));
    auto modification_date = Core::DateTime::from_timestamp(stat.st_mtime);
    auto checksum = Crypto::Checksum::CRC32 { file_buffer.bytes() };

    ZipMember member = {};
    member.name = canonicalized_path;
    member.uncompressed_size = (u32)file_buffer.size();
    member.crc32 = checksum.digest();
    member.is_directory = false;
    member.modification_time = to_packed_dos_time(modification_date.hour(), modification_date.minute(), modification_date.second());
    member.modification_date = to_packed_dos_date(modification_date.year(), modification_date.month(), modification_date.day());

    auto deflated_amount = 0;
    auto deflate_buffer = Compress::DeflateCompressor::compress_all(file_buffer);
    if (!deflate_buffer.is_error() && deflate_buffer.value().size() < file_buffer.size()) {
        member.compressed_data = deflate_buffer.value().bytes();
        member.compression_method = ZipCompressionMethod::Deflate;

        auto compression_ratio = (double)deflate_buffer.value().size() / (double)file_buffer.size();
        deflated_amount = (int)(compression_ratio * 100);
    } else {
        member.compressed_data = file_buffer.bytes();
        member.compression_method = ZipCompressionMethod::Store;
    }

    TRY(add_member(member));
    return ZipMemberFromFileResult {
        .canonicalized_path = canonicalized_path,
        .deflated_amount = deflated_amount
    };
}

ErrorOr<void> ZipOutputStream::add_directory_from_path(StringView path, RecurseThroughDirectories recurse_through_directories, AddMemberCallback const& callback)
{
    auto canonicalized_path = TRY(String::formatted("{}/", LexicalPath::canonicalized_path(path)));
    auto stat = TRY(Core::System::stat(canonicalized_path));
    auto modification_date = Core::DateTime::from_timestamp(stat.st_mtime);

    auto member = ZipMember {
        .name = canonicalized_path,
        .compressed_data = {},
        .compression_method = ZipCompressionMethod::Store,
        .uncompressed_size = 0,
        .crc32 = 0,
        .is_directory = true,
        .modification_time = to_packed_dos_time(modification_date.hour(), modification_date.minute(), modification_date.second()),
        .modification_date = to_packed_dos_date(modification_date.year(), modification_date.month(), modification_date.day()),
    };

    TRY(add_member(member));
    callback(ZipMemberFromFileResult { .canonicalized_path = canonicalized_path, .deflated_amount = 0 });

    // Some users of this utility may not want us to automatically handle directories, so we can return early here.
    if (recurse_through_directories == RecurseThroughDirectories::No)
        return {};

    auto iterator = Core::DirIterator(path, Core::DirIterator::Flags::SkipParentAndBaseDir);
    while (iterator.has_next()) {
        auto child_path = iterator.next_full_path();
        if (FileSystem::is_link(child_path))
            continue;

        TRY(add_member_from_path(child_path, recurse_through_directories, callback));
    }

    return {};
}

ErrorOr<void> ZipOutputStream::finish()
{
    VERIFY(!m_finished);
    m_finished = true;

    auto file_header_offset = 0u;
    auto central_directory_size = 0u;
    for (ZipMember const& member : m_members) {
        auto zip_version = minimum_version_needed(member.compression_method);
        CentralDirectoryRecord central_directory_record {
            .made_by_version = zip_version,
            .minimum_version = zip_version,
            .general_purpose_flags = { .flags = 0 },
            .compression_method = member.compression_method,
            .modification_time = member.modification_time,
            .modification_date = member.modification_date,
            .crc32 = member.crc32,
            .compressed_size = static_cast<u32>(member.compressed_data.size()),
            .uncompressed_size = member.uncompressed_size,
            .name_length = static_cast<u16>(member.name.bytes_as_string_view().length()),
            .extra_data_length = 0,
            .comment_length = 0,
            .start_disk = 0,
            .internal_attributes = 0,
            .external_attributes = member.is_directory ? zip_directory_external_attribute : 0,
            .local_file_header_offset = file_header_offset, // FIXME: we assume the wrapped output stream was never written to before us
            .name = reinterpret_cast<u8 const*>(member.name.bytes_as_string_view().characters_without_null_termination()),
            .extra_data = nullptr,
            .comment = nullptr,
        };
        file_header_offset += sizeof(LocalFileHeader::signature) + (sizeof(LocalFileHeader) - (sizeof(u8*) * 3)) + member.name.bytes_as_string_view().length() + member.compressed_data.size();
        TRY(central_directory_record.write(*m_stream));
        central_directory_size += central_directory_record.size();
    }

    EndOfCentralDirectory end_of_central_directory {
        .disk_number = 0,
        .central_directory_start_disk = 0,
        .disk_records_count = static_cast<u16>(m_members.size()),
        .total_records_count = static_cast<u16>(m_members.size()),
        .central_directory_size = central_directory_size,
        .central_directory_offset = file_header_offset,
        .comment_length = 0,
        .comment = nullptr,
    };
    return end_of_central_directory.write(*m_stream);
}

}
