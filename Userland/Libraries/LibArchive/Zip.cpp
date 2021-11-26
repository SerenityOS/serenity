/*
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibArchive/Zip.h>

namespace Archive {

bool Zip::find_end_of_central_directory_offset(ReadonlyBytes buffer, size_t& offset)
{
    for (size_t backwards_offset = 0; backwards_offset <= UINT16_MAX; backwards_offset++) // the file may have a trailing comment of an arbitrary 16 bit length
    {
        if (buffer.size() < (sizeof(EndOfCentralDirectory) - sizeof(u8*)) + backwards_offset)
            return false;

        auto signature_offset = (buffer.size() - (sizeof(EndOfCentralDirectory) - sizeof(u8*)) - backwards_offset);
        if (memcmp(buffer.data() + signature_offset, end_of_central_directory_signature, sizeof(end_of_central_directory_signature)) == 0) {
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
        if (central_directory_record.general_purpose_flags & 1)
            return {}; // TODO: support encrypted zip members
        if (central_directory_record.general_purpose_flags & 3)
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

    Zip zip;
    zip.m_input_data = buffer;
    zip.member_count = end_of_central_directory.total_records_count;
    zip.members_start_offset = end_of_central_directory.central_directory_offset;
    return zip;
}

bool Zip::for_each_member(Function<IterationDecision(const ZipMember&)> callback)
{
    size_t member_offset = members_start_offset;
    for (size_t i = 0; i < member_count; i++) {
        CentralDirectoryRecord central_directory_record {};
        VERIFY(central_directory_record.read(m_input_data.slice(member_offset)));
        LocalFileHeader local_file_header {};
        VERIFY(local_file_header.read(m_input_data.slice(central_directory_record.local_file_header_offset)));

        ZipMember member;
        char null_terminated_name[central_directory_record.name_length + 1];
        memcpy(null_terminated_name, central_directory_record.name, central_directory_record.name_length);
        null_terminated_name[central_directory_record.name_length] = 0;
        member.name = String { null_terminated_name };
        member.compressed_data = { local_file_header.compressed_data, central_directory_record.compressed_size };
        member.compression_method = static_cast<ZipCompressionMethod>(central_directory_record.compression_method);
        member.uncompressed_size = central_directory_record.uncompressed_size;
        member.crc32 = central_directory_record.crc32;
        member.is_directory = central_directory_record.external_attributes & zip_directory_external_attribute || member.name.ends_with('/'); // FIXME: better directory detection

        if (callback(member) == IterationDecision::Break)
            return false;

        member_offset += central_directory_record.size();
    }
    return true;
}

ZipOutputStream::ZipOutputStream(OutputStream& stream)
    : m_stream(stream)
{
}

void ZipOutputStream::add_member(const ZipMember& member)
{
    VERIFY(!m_finished);
    VERIFY(member.name.length() <= UINT16_MAX);
    VERIFY(member.compressed_data.size() <= UINT32_MAX);
    m_members.append(member);

    LocalFileHeader local_file_header {};
    local_file_header.minimum_version = member.compression_method == ZipCompressionMethod::Deflate ? 20 : 10; // Deflate was added in PKZip 2.0
    local_file_header.general_purpose_flags = 0;
    local_file_header.compression_method = static_cast<u16>(member.compression_method);
    local_file_header.modification_time = 0; // TODO: support modification time
    local_file_header.modification_date = 0;
    local_file_header.crc32 = member.crc32;
    local_file_header.compressed_size = member.compressed_data.size();
    local_file_header.uncompressed_size = member.uncompressed_size;
    local_file_header.name_length = member.name.length();
    local_file_header.extra_data_length = 0;
    local_file_header.name = (const u8*)(member.name.characters());
    local_file_header.extra_data = nullptr;
    local_file_header.compressed_data = member.compressed_data.data();
    local_file_header.write(m_stream);
}

void ZipOutputStream::finish()
{
    VERIFY(!m_finished);
    m_finished = true;

    auto file_header_offset = 0;
    auto central_directory_size = 0;
    for (const ZipMember& member : m_members) {
        CentralDirectoryRecord central_directory_record {};
        auto zip_version = member.compression_method == ZipCompressionMethod::Deflate ? 20 : 10; // Deflate was added in PKZip 2.0
        central_directory_record.made_by_version = zip_version;
        central_directory_record.minimum_version = zip_version;
        central_directory_record.general_purpose_flags = 0;
        central_directory_record.compression_method = static_cast<u16>(member.compression_method);
        central_directory_record.modification_time = 0; // TODO: support modification time
        central_directory_record.modification_date = 0;
        central_directory_record.crc32 = member.crc32;
        central_directory_record.compressed_size = member.compressed_data.size();
        central_directory_record.uncompressed_size = member.uncompressed_size;
        central_directory_record.name_length = member.name.length();
        central_directory_record.extra_data_length = 0;
        central_directory_record.comment_length = 0;
        central_directory_record.start_disk = 0;
        central_directory_record.internal_attributes = 0;
        central_directory_record.external_attributes = member.is_directory ? zip_directory_external_attribute : 0;
        central_directory_record.local_file_header_offset = file_header_offset; // FIXME: we assume the wrapped output stream was never written to before us
        file_header_offset += sizeof(local_file_header_signature) + (sizeof(LocalFileHeader) - (sizeof(u8*) * 3)) + member.name.length() + member.compressed_data.size();
        central_directory_record.name = (const u8*)(member.name.characters());
        central_directory_record.extra_data = nullptr;
        central_directory_record.comment = nullptr;
        central_directory_record.write(m_stream);
        central_directory_size += central_directory_record.size();
    }

    EndOfCentralDirectory end_of_central_directory {};
    end_of_central_directory.disk_number = 0;
    end_of_central_directory.central_directory_start_disk = 0;
    end_of_central_directory.disk_records_count = m_members.size();
    end_of_central_directory.total_records_count = m_members.size();
    end_of_central_directory.central_directory_size = central_directory_size;
    end_of_central_directory.central_directory_offset = file_header_offset;
    end_of_central_directory.comment_length = 0;
    end_of_central_directory.comment = nullptr;
    end_of_central_directory.write(m_stream);
}

}
