/*
 * Copyright (c) 2020, the SerenityOS developers.
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCompress/Gzip.h>

#include <AK/MemoryStream.h>
#include <AK/String.h>
#include <LibCore/DateTime.h>

namespace Compress {

bool GzipDecompressor::is_likely_compressed(ReadonlyBytes bytes)
{
    return bytes.size() >= 2 && bytes[0] == gzip_magic_1 && bytes[1] == gzip_magic_2;
}

bool BlockHeader::valid_magic_number() const
{
    return identification_1 == gzip_magic_1 && identification_2 == gzip_magic_2;
}

bool BlockHeader::supported_by_implementation() const
{
    if (compression_method != 0x08) {
        // RFC 1952 does not define any compression methods other than deflate.
        return false;
    }

    if (flags > Flags::MAX) {
        // RFC 1952 does not define any more flags.
        return false;
    }

    return true;
}

GzipDecompressor::GzipDecompressor(InputStream& stream)
    : m_input_stream(stream)
{
}

GzipDecompressor::~GzipDecompressor()
{
    m_current_member.clear();
}

// FIXME: Again, there are surely a ton of bugs because the code doesn't check for read errors.
size_t GzipDecompressor::read(Bytes bytes)
{
    size_t total_read = 0;
    while (total_read < bytes.size()) {
        if (has_any_error() || m_eof)
            break;

        auto slice = bytes.slice(total_read);

        if (m_current_member.has_value()) {
            size_t nread = current_member().m_stream.read(slice);
            current_member().m_checksum.update(slice.trim(nread));
            current_member().m_nread += nread;

            if (current_member().m_stream.handle_any_error()) {
                set_fatal_error();
                break;
            }

            if (nread < slice.size()) {
                LittleEndian<u32> crc32, input_size;
                m_input_stream >> crc32 >> input_size;

                if (crc32 != current_member().m_checksum.digest()) {
                    // FIXME: Somehow the checksum is incorrect?

                    set_fatal_error();
                    break;
                }

                if (input_size != current_member().m_nread) {
                    set_fatal_error();
                    break;
                }

                m_current_member.clear();

                total_read += nread;
                continue;
            }

            total_read += nread;
            continue;
        } else {
            m_partial_header_offset += m_input_stream.read(Bytes { m_partial_header, sizeof(BlockHeader) }.slice(m_partial_header_offset));

            if (m_input_stream.handle_any_error() || m_input_stream.unreliable_eof()) {
                m_eof = true;
                break;
            }

            if (m_partial_header_offset < sizeof(BlockHeader)) {
                break; // partial header read
            }
            m_partial_header_offset = 0;

            BlockHeader header = *(reinterpret_cast<BlockHeader*>(m_partial_header));

            if (!header.valid_magic_number() || !header.supported_by_implementation()) {
                set_fatal_error();
                break;
            }

            if (header.flags & Flags::FEXTRA) {
                LittleEndian<u16> subfield_id, length;
                m_input_stream >> subfield_id >> length;
                m_input_stream.discard_or_error(length);
            }

            auto discard_string = [&]() {
                char next_char;
                do {
                    m_input_stream >> next_char;
                    if (m_input_stream.has_any_error()) {
                        set_fatal_error();
                        break;
                    }
                } while (next_char);
            };

            if (header.flags & Flags::FNAME) {
                discard_string();
                if (has_any_error())
                    break;
            }

            if (header.flags & Flags::FCOMMENT) {
                discard_string();
                if (has_any_error())
                    break;
            }

            if (header.flags & Flags::FHCRC) {
                LittleEndian<u16> crc16;
                m_input_stream >> crc16;
                // FIXME: we should probably verify this instead of just assuming it matches
            }

            m_current_member.emplace(header, m_input_stream);
            continue;
        }
    }
    return total_read;
}

Optional<String> GzipDecompressor::describe_header(ReadonlyBytes bytes)
{
    if (bytes.size() < sizeof(BlockHeader))
        return {};

    auto& header = *(reinterpret_cast<const BlockHeader*>(bytes.data()));
    if (!header.valid_magic_number() || !header.supported_by_implementation())
        return {};

    LittleEndian<u32> original_size = *reinterpret_cast<const u32*>(bytes.offset(bytes.size() - sizeof(u32)));
    return String::formatted("last modified: {}, original size {}", Core::DateTime::from_timestamp(header.modification_time).to_string(), (u32)original_size);
}

bool GzipDecompressor::read_or_error(Bytes bytes)
{
    if (read(bytes) < bytes.size()) {
        set_fatal_error();
        return false;
    }

    return true;
}

bool GzipDecompressor::discard_or_error(size_t count)
{
    u8 buffer[4096];

    size_t ndiscarded = 0;
    while (ndiscarded < count) {
        if (unreliable_eof()) {
            set_fatal_error();
            return false;
        }

        ndiscarded += read({ buffer, min<size_t>(count - ndiscarded, sizeof(buffer)) });
    }

    return true;
}

Optional<ByteBuffer> GzipDecompressor::decompress_all(ReadonlyBytes bytes)
{
    InputMemoryStream memory_stream { bytes };
    GzipDecompressor gzip_stream { memory_stream };
    DuplexMemoryStream output_stream;

    u8 buffer[4096];
    while (!gzip_stream.has_any_error() && !gzip_stream.unreliable_eof()) {
        const auto nread = gzip_stream.read({ buffer, sizeof(buffer) });
        output_stream.write_or_error({ buffer, nread });
    }

    if (gzip_stream.handle_any_error())
        return {};

    return output_stream.copy_into_contiguous_buffer();
}

bool GzipDecompressor::unreliable_eof() const { return m_eof; }

bool GzipDecompressor::handle_any_error()
{
    bool handled_errors = m_input_stream.handle_any_error();
    return Stream::handle_any_error() || handled_errors;
}

GzipCompressor::GzipCompressor(OutputStream& stream)
    : m_output_stream(stream)
{
}

GzipCompressor::~GzipCompressor()
{
}

size_t GzipCompressor::write(ReadonlyBytes bytes)
{
    BlockHeader header;
    header.identification_1 = 0x1f;
    header.identification_2 = 0x8b;
    header.compression_method = 0x08;
    header.flags = 0;
    header.modification_time = 0;
    header.extra_flags = 3;      // DEFLATE sets 2 for maximum compression and 4 for minimum compression
    header.operating_system = 3; // unix
    m_output_stream << Bytes { &header, sizeof(header) };
    DeflateCompressor compressed_stream { m_output_stream };
    VERIFY(compressed_stream.write_or_error(bytes));
    compressed_stream.final_flush();
    Crypto::Checksum::CRC32 crc32;
    crc32.update(bytes);
    LittleEndian<u32> digest = crc32.digest();
    LittleEndian<u32> size = bytes.size();
    m_output_stream << digest << size;
    return bytes.size();
}

bool GzipCompressor::write_or_error(ReadonlyBytes bytes)
{
    if (write(bytes) < bytes.size()) {
        set_fatal_error();
        return false;
    }

    return true;
}

Optional<ByteBuffer> GzipCompressor::compress_all(ReadonlyBytes bytes)
{
    DuplexMemoryStream output_stream;
    GzipCompressor gzip_stream { output_stream };

    gzip_stream.write_or_error(bytes);

    if (gzip_stream.handle_any_error())
        return {};

    return output_stream.copy_into_contiguous_buffer();
}

}
