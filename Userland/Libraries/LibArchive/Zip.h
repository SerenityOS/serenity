/*
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Function.h>
#include <AK/IterationDecision.h>
#include <AK/Span.h>
#include <AK/Stream.h>
#include <AK/String.h>
#include <AK/Vector.h>
#include <string.h>

namespace Archive {

// NOTE: Due to the format of zip files compression is streamed and decompression is random access.

static constexpr u8 end_of_central_directory_signature[] = { 0x50, 0x4b, 0x05, 0x06 }; // 'PK\x05\x06'
struct [[gnu::packed]] EndOfCentralDirectory {
    u16 disk_number;
    u16 central_directory_start_disk;
    u16 disk_records_count;
    u16 total_records_count;
    u32 central_directory_size;
    u32 central_directory_offset;
    u16 comment_length;
    const u8* comment;

    bool read(ReadonlyBytes buffer)
    {
        auto fields_size = sizeof(EndOfCentralDirectory) - sizeof(u8*);
        if (buffer.size() < sizeof(end_of_central_directory_signature) + fields_size)
            return false;
        if (memcmp(buffer.data(), end_of_central_directory_signature, sizeof(end_of_central_directory_signature)) != 0)
            return false;
        memcpy(reinterpret_cast<void*>(&disk_number), buffer.data() + sizeof(end_of_central_directory_signature), fields_size);
        if (buffer.size() < sizeof(end_of_central_directory_signature) + fields_size + comment_length)
            return false;
        comment = buffer.data() + sizeof(end_of_central_directory_signature) + fields_size;
        return true;
    }

    void write(OutputStream& stream) const
    {
        stream.write_or_error({ end_of_central_directory_signature, sizeof(end_of_central_directory_signature) });
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

static constexpr u8 central_directory_record_signature[] = { 0x50, 0x4b, 0x01, 0x02 }; // 'PK\x01\x02'
struct [[gnu::packed]] CentralDirectoryRecord {
    u16 made_by_version;
    u16 minimum_version;
    u16 general_purpose_flags;
    u16 compression_method;
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
    const u8* name;
    const u8* extra_data;
    const u8* comment;

    bool read(ReadonlyBytes buffer)
    {
        auto fields_size = sizeof(CentralDirectoryRecord) - (sizeof(u8*) * 3);
        if (buffer.size() < sizeof(central_directory_record_signature) + fields_size)
            return false;
        if (memcmp(buffer.data(), central_directory_record_signature, sizeof(central_directory_record_signature)) != 0)
            return false;
        memcpy(reinterpret_cast<void*>(&made_by_version), buffer.data() + sizeof(central_directory_record_signature), fields_size);
        if (buffer.size() < sizeof(end_of_central_directory_signature) + fields_size + comment_length + name_length + extra_data_length)
            return false;
        name = buffer.data() + sizeof(central_directory_record_signature) + fields_size;
        extra_data = name + name_length;
        comment = extra_data + extra_data_length;
        return true;
    }

    void write(OutputStream& stream) const
    {
        stream.write_or_error({ central_directory_record_signature, sizeof(central_directory_record_signature) });
        stream << made_by_version;
        stream << minimum_version;
        stream << general_purpose_flags;
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
        return sizeof(central_directory_record_signature) + (sizeof(CentralDirectoryRecord) - (sizeof(u8*) * 3)) + name_length + extra_data_length + comment_length;
    }
};
static constexpr u32 zip_directory_external_attribute = 1 << 4;

static constexpr u8 local_file_header_signature[] = { 0x50, 0x4b, 0x03, 0x04 }; // 'PK\x03\x04'
struct [[gnu::packed]] LocalFileHeader {
    u16 minimum_version;
    u16 general_purpose_flags;
    u16 compression_method;
    u16 modification_time;
    u16 modification_date;
    u32 crc32;
    u32 compressed_size;
    u32 uncompressed_size;
    u16 name_length;
    u16 extra_data_length;
    const u8* name;
    const u8* extra_data;
    const u8* compressed_data;

    bool read(ReadonlyBytes buffer)
    {
        auto fields_size = sizeof(LocalFileHeader) - (sizeof(u8*) * 3);
        if (buffer.size() < sizeof(local_file_header_signature) + fields_size)
            return false;
        if (memcmp(buffer.data(), local_file_header_signature, sizeof(local_file_header_signature)) != 0)
            return false;
        memcpy(reinterpret_cast<void*>(&minimum_version), buffer.data() + sizeof(local_file_header_signature), fields_size);
        if (buffer.size() < sizeof(end_of_central_directory_signature) + fields_size + name_length + extra_data_length + compressed_size)
            return false;
        name = buffer.data() + sizeof(local_file_header_signature) + fields_size;
        extra_data = name + name_length;
        compressed_data = extra_data + extra_data_length;
        return true;
    }

    void write(OutputStream& stream) const
    {
        stream.write_or_error({ local_file_header_signature, sizeof(local_file_header_signature) });
        stream << minimum_version;
        stream << general_purpose_flags;
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

enum ZipCompressionMethod : u16 {
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
    bool for_each_member(Function<IterationDecision(const ZipMember&)>);

private:
    static bool find_end_of_central_directory_offset(ReadonlyBytes, size_t& offset);

    u16 member_count { 0 };
    size_t members_start_offset { 0 };
    ReadonlyBytes m_input_data;
};

class ZipOutputStream {
public:
    ZipOutputStream(OutputStream&);
    void add_member(const ZipMember&);
    void finish();

private:
    OutputStream& m_stream;
    Vector<ZipMember> m_members;

    bool m_finished { false };
};

}
