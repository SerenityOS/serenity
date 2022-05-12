/*
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Array.h>
#include <AK/Function.h>
#include <AK/IterationDecision.h>
#include <AK/Stream.h>
#include <AK/String.h>
#include <AK/Vector.h>
#include <string.h>

namespace Archive {

template<size_t fields_size, class T>
static bool read_helper(ReadonlyBytes buffer, T* self)
{
    if (buffer.size() < T::signature.size() + fields_size)
        return false;
    if (buffer.slice(0, T::signature.size()) != T::signature)
        return false;
    memcpy(self, buffer.data() + T::signature.size(), fields_size);
    return true;
}

// NOTE: Due to the format of zip files compression is streamed and decompression is random access.

static constexpr auto signature_length = 4;

struct [[gnu::packed]] EndOfCentralDirectory {
    static constexpr Array<u8, signature_length> signature = { 0x50, 0x4b, 0x05, 0x06 }; // 'PK\x05\x06'

    u16 disk_number;
    u16 central_directory_start_disk;
    u16 disk_records_count;
    u16 total_records_count;
    u32 central_directory_size;
    u32 central_directory_offset;
    u16 comment_length;
    u8 const* comment;

    bool read(ReadonlyBytes buffer)
    {
        constexpr auto fields_size = sizeof(EndOfCentralDirectory) - (sizeof(u8*) * 1);
        if (!read_helper<fields_size>(buffer, this))
            return false;
        if (buffer.size() < signature.size() + fields_size + comment_length)
            return false;
        comment = buffer.data() + signature.size() + fields_size;
        return true;
    }

    void write(OutputStream& stream) const
    {
        stream.write_or_error(signature);
        stream << disk_number;
        stream << central_directory_start_disk;
        stream << disk_records_count;
        stream << total_records_count;
        stream << central_directory_size;
        stream << central_directory_offset;
        stream << comment_length;
        if (comment_length > 0)
            stream.write_or_error({ comment, comment_length });
    }
};

enum class ZipCompressionMethod : u16 {
    Store = 0,
    Shrink = 1,
    Reduce1 = 2,
    Reduce2 = 3,
    Reduce3 = 4,
    Reduce4 = 5,
    Implode = 6,
    Reserved = 7,
    Deflate = 8
};

union ZipGeneralPurposeFlags {
    u16 flags;
    struct {
        u16 encrypted : 1;
        u16 compression_options : 2;
        u16 data_descriptor : 1;
        u16 enhanced_deflation : 1;
        u16 compressed_patched_data : 1;
        u16 strong_encryption : 1;
        u16 : 4;
        u16 language_encoding : 1;
        u16 : 1;
        u16 masked_data_values : 1;
        u16 : 2;
    };
};
static_assert(sizeof(ZipGeneralPurposeFlags) == sizeof(u16));

OutputStream& operator<<(OutputStream& stream, ZipCompressionMethod method);

struct [[gnu::packed]] CentralDirectoryRecord {
    static constexpr Array<u8, signature_length> signature = { 0x50, 0x4b, 0x01, 0x02 }; // 'PK\x01\x02'

    u16 made_by_version;
    u16 minimum_version;
    ZipGeneralPurposeFlags general_purpose_flags;
    ZipCompressionMethod compression_method;
    u16 modification_time;
    u16 modification_date;
    u32 crc32;
    u32 compressed_size;
    u32 uncompressed_size;
    u16 name_length;
    u16 extra_data_length;
    u16 comment_length;
    u16 start_disk;
    u16 internal_attributes;
    u32 external_attributes;
    u32 local_file_header_offset;
    u8 const* name;
    u8 const* extra_data;
    u8 const* comment;

    bool read(ReadonlyBytes buffer)
    {
        constexpr auto fields_size = sizeof(CentralDirectoryRecord) - (sizeof(u8*) * 3);
        if (!read_helper<fields_size>(buffer, this))
            return false;
        if (buffer.size() < size())
            return false;
        name = buffer.data() + signature.size() + fields_size;
        extra_data = name + name_length;
        comment = extra_data + extra_data_length;
        return true;
    }

    void write(OutputStream& stream) const
    {
        stream.write_or_error(signature);
        stream << made_by_version;
        stream << minimum_version;
        stream << general_purpose_flags.flags;
        stream << compression_method;
        stream << modification_time;
        stream << modification_date;
        stream << crc32;
        stream << compressed_size;
        stream << uncompressed_size;
        stream << name_length;
        stream << extra_data_length;
        stream << comment_length;
        stream << start_disk;
        stream << internal_attributes;
        stream << external_attributes;
        stream << local_file_header_offset;
        if (name_length > 0)
            stream.write_or_error({ name, name_length });
        if (extra_data_length > 0)
            stream.write_or_error({ extra_data, extra_data_length });
        if (comment_length > 0)
            stream.write_or_error({ comment, comment_length });
    }

    [[nodiscard]] size_t size() const
    {
        return signature.size() + (sizeof(CentralDirectoryRecord) - (sizeof(u8*) * 3)) + name_length + extra_data_length + comment_length;
    }
};
static constexpr u32 zip_directory_external_attribute = 1 << 4;

struct [[gnu::packed]] LocalFileHeader {
    static constexpr Array<u8, signature_length> signature = { 0x50, 0x4b, 0x03, 0x04 }; // 'PK\x03\x04'

    u16 minimum_version;
    ZipGeneralPurposeFlags general_purpose_flags;
    u16 compression_method;
    u16 modification_time;
    u16 modification_date;
    u32 crc32;
    u32 compressed_size;
    u32 uncompressed_size;
    u16 name_length;
    u16 extra_data_length;
    u8 const* name;
    u8 const* extra_data;
    u8 const* compressed_data;

    bool read(ReadonlyBytes buffer)
    {
        constexpr auto fields_size = sizeof(LocalFileHeader) - (sizeof(u8*) * 3);
        if (!read_helper<fields_size>(buffer, this))
            return false;
        if (buffer.size() < signature.size() + fields_size + name_length + extra_data_length + compressed_size)
            return false;
        name = buffer.data() + signature.size() + fields_size;
        extra_data = name + name_length;
        compressed_data = extra_data + extra_data_length;
        return true;
    }

    void write(OutputStream& stream) const
    {
        stream.write_or_error(signature);
        stream << minimum_version;
        stream << general_purpose_flags.flags;
        stream << compression_method;
        stream << modification_time;
        stream << modification_date;
        stream << crc32;
        stream << compressed_size;
        stream << uncompressed_size;
        stream << name_length;
        stream << extra_data_length;
        if (name_length > 0)
            stream.write_or_error({ name, name_length });
        if (extra_data_length > 0)
            stream.write_or_error({ extra_data, extra_data_length });
        if (compressed_size > 0)
            stream.write_or_error({ compressed_data, compressed_size });
    }
};

struct ZipMember {
    String name;
    ReadonlyBytes compressed_data; // TODO: maybe the decompression/compression should be handled by LibArchive instead of the user?
    ZipCompressionMethod compression_method;
    u32 uncompressed_size;
    u32 crc32;
    bool is_directory;
};

class Zip {
public:
    static Optional<Zip> try_create(ReadonlyBytes buffer);
    bool for_each_member(Function<IterationDecision(ZipMember const&)>);

private:
    static bool find_end_of_central_directory_offset(ReadonlyBytes, size_t& offset);

    Zip(u16 member_count, size_t members_start_offset, ReadonlyBytes input_data)
        : m_member_count { member_count }
        , m_members_start_offset { members_start_offset }
        , m_input_data { input_data }
    {
    }
    u16 m_member_count { 0 };
    size_t m_members_start_offset { 0 };
    ReadonlyBytes m_input_data;
};

class ZipOutputStream {
public:
    ZipOutputStream(OutputStream&);
    void add_member(ZipMember const&);
    void finish();

private:
    OutputStream& m_stream;
    Vector<ZipMember> m_members;

    bool m_finished { false };
};

}
