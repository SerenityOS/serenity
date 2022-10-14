/*
 * Copyright (c) 2020-2022, the SerenityOS developers.
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCompress/Gzip.h>

#include <AK/DeprecatedString.h>
#include <AK/MemoryStream.h>
#include <LibCore/DateTime.h>
#include <LibCore/MemoryStream.h>

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

GzipDecompressor::GzipDecompressor(NonnullOwnPtr<Core::Stream::Stream> stream)
    : m_input_stream(move(stream))
{
}

GzipDecompressor::~GzipDecompressor()
{
    m_current_member.clear();
}

ErrorOr<Bytes> GzipDecompressor::read(Bytes bytes)
{
    size_t total_read = 0;
    while (total_read < bytes.size()) {
        if (is_eof())
            break;

        auto slice = bytes.slice(total_read);

        if (m_current_member.has_value()) {
            auto current_slice = TRY(current_member().m_stream.read(slice));
            current_member().m_checksum.update(current_slice);
            current_member().m_nread += current_slice.size();

            if (current_slice.size() < slice.size()) {
                LittleEndian<u32> crc32, input_size;
                TRY(m_input_stream->read(crc32.bytes()));
                TRY(m_input_stream->read(input_size.bytes()));

                if (crc32 != current_member().m_checksum.digest())
                    return Error::from_string_literal("Stored CRC32 does not match the calculated CRC32 of the current member");

                if (input_size != current_member().m_nread)
                    return Error::from_string_literal("Input size does not match the number of read bytes");

                m_current_member.clear();

                total_read += current_slice.size();
                continue;
            }

            total_read += current_slice.size();
            continue;
        } else {
            auto current_partial_header_slice = Bytes { m_partial_header, sizeof(BlockHeader) }.slice(m_partial_header_offset);
            auto current_partial_header_data = TRY(m_input_stream->read(current_partial_header_slice));
            m_partial_header_offset += current_partial_header_data.size();

            if (is_eof())
                break;

            if (m_partial_header_offset < sizeof(BlockHeader)) {
                break; // partial header read
            }
            m_partial_header_offset = 0;

            BlockHeader header = *(reinterpret_cast<BlockHeader*>(m_partial_header));

            if (!header.valid_magic_number())
                return Error::from_string_literal("Header does not have a valid magic number");

            if (!header.supported_by_implementation())
                return Error::from_string_literal("Header is not supported by implementation");

            if (header.flags & Flags::FEXTRA) {
                LittleEndian<u16> subfield_id, length;
                TRY(m_input_stream->read(subfield_id.bytes()));
                TRY(m_input_stream->read(length.bytes()));
                TRY(m_input_stream->discard(length));
            }

            auto discard_string = [&]() -> ErrorOr<void> {
                char next_char;
                do {
                    TRY(m_input_stream->read({ &next_char, sizeof(next_char) }));
                } while (next_char);

                return {};
            };

            if (header.flags & Flags::FNAME)
                TRY(discard_string());

            if (header.flags & Flags::FCOMMENT)
                TRY(discard_string());

            if (header.flags & Flags::FHCRC) {
                LittleEndian<u16> crc16;
                TRY(m_input_stream->read(crc16.bytes()));
                // FIXME: we should probably verify this instead of just assuming it matches
            }

            m_current_member.emplace(header, *m_input_stream);
            continue;
        }
    }
    return bytes.slice(0, total_read);
}

Optional<DeprecatedString> GzipDecompressor::describe_header(ReadonlyBytes bytes)
{
    if (bytes.size() < sizeof(BlockHeader))
        return {};

    auto& header = *(reinterpret_cast<BlockHeader const*>(bytes.data()));
    if (!header.valid_magic_number() || !header.supported_by_implementation())
        return {};

    LittleEndian<u32> original_size = *reinterpret_cast<u32 const*>(bytes.offset(bytes.size() - sizeof(u32)));
    return DeprecatedString::formatted("last modified: {}, original size {}", Core::DateTime::from_timestamp(header.modification_time).to_deprecated_string(), (u32)original_size);
}

ErrorOr<ByteBuffer> GzipDecompressor::decompress_all(ReadonlyBytes bytes)
{
    auto memory_stream = TRY(Core::Stream::FixedMemoryStream::construct(bytes));
    auto gzip_stream = make<GzipDecompressor>(move(memory_stream));
    DuplexMemoryStream output_stream;

    auto buffer = TRY(ByteBuffer::create_uninitialized(4096));
    while (!gzip_stream->is_eof()) {
        auto const data = TRY(gzip_stream->read(buffer));
        output_stream.write_or_error(data);
    }

    return output_stream.copy_into_contiguous_buffer();
}

bool GzipDecompressor::is_eof() const { return m_input_stream->is_eof(); }

ErrorOr<size_t> GzipDecompressor::write(ReadonlyBytes)
{
    VERIFY_NOT_REACHED();
}

GzipCompressor::GzipCompressor(OutputStream& stream)
    : m_output_stream(stream)
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
