/*
 * Copyright (c) 2023, Tim Schumacher <timschumi@gmx.de>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/ByteBuffer.h>
#include <AK/MemoryStream.h>
#include <LibCompress/Lzma2.h>
#include <LibCompress/Xz.h>
#include <LibCrypto/Checksum/CRC32.h>

namespace Compress {

ErrorOr<XzMultibyteInteger> XzMultibyteInteger::read_from_stream(Stream& stream)
{
    // 1.2. Multibyte Integers:
    // "When smaller values are more likely than bigger values (for
    //  example file sizes), multibyte integers are encoded in a
    //  variable-length representation:
    //    - Numbers in the range [0, 127] are copied as is, and take
    //      one byte of space.
    //    - Bigger numbers will occupy two or more bytes. All but the
    //      last byte of the multibyte representation have the highest
    //      (eighth) bit set."

    // 9 * 7 bits is 63 bits, which is the largest that will fit into an u64.
    constexpr size_t maximum_number_of_bytes = 9;

    u64 result = 0;

    for (size_t i = 0; i < maximum_number_of_bytes; i++) {
        u64 next_byte = TRY(stream.read_value<u8>());
        result |= (next_byte & 0x7F) << (i * 7);

        // We should reject numbers that are encoded in too many bytes.
        if (next_byte == 0x00 && i != 0)
            return Error::from_string_literal("XZ multibyte integer has a larger encoding than necessary");

        if ((next_byte & 0x80) == 0)
            break;
    }

    return XzMultibyteInteger { result };
}

ErrorOr<void> XzStreamHeader::validate()
{
    // 2.1.1.1. Header Magic Bytes:
    // "The first six (6) bytes of the Stream are so called Header
    //  Magic Bytes. They can be used to identify the file type.
    //
    //      Using a C array and ASCII:
    //      const uint8_t HEADER_MAGIC[6]
    //              = { 0xFD, '7', 'z', 'X', 'Z', 0x00 };
    //
    //      In plain hexadecimal:
    //      FD 37 7A 58 5A 00
    //
    //  If the Header Magic Bytes don't match, the decoder MUST
    //  indicate an error."
    if (magic[0] != 0xFD || magic[1] != '7' || magic[2] != 'z' || magic[3] != 'X' || magic[4] != 'Z' || magic[5] != 0x00)
        return Error::from_string_literal("XZ stream header has an invalid magic");

    // 2.1.1.2. Stream Flags:
    // "If any reserved bit is set, the decoder MUST indicate an error.
    //  It is possible that there is a new field present which the
    //  decoder is not aware of, and can thus parse the Stream Header
    //  incorrectly."
    if (flags.reserved != 0 || flags.reserved_bits != 0)
        return Error::from_string_literal("XZ stream header has reserved non-null stream flag bits");

    // 2.1.1.3. CRC32:
    // "The CRC32 is calculated from the Stream Flags field. It is
    //  stored as an unsigned 32-bit little endian integer. If the
    //  calculated value does not match the stored one, the decoder
    //  MUST indicate an error."
    if (Crypto::Checksum::CRC32({ &flags, sizeof(flags) }).digest() != flags_crc32)
        return Error::from_string_literal("XZ stream header has an invalid CRC32 checksum");

    return {};
}

ErrorOr<void> XzStreamFooter::validate()
{
    // 2.1.2.1. CRC32:
    // "The CRC32 is calculated from the Backward Size and Stream Flags
    //  fields. It is stored as an unsigned 32-bit little endian
    //  integer. If the calculated value does not match the stored one,
    //  the decoder MUST indicate an error."
    Crypto::Checksum::CRC32 calculated_crc32;
    calculated_crc32.update({ &encoded_backward_size, sizeof(encoded_backward_size) });
    calculated_crc32.update({ &flags, sizeof(flags) });
    if (calculated_crc32.digest() != size_and_flags_crc32)
        return Error::from_string_literal("XZ stream footer has an invalid CRC32 checksum");

    // 2.1.2.4. Footer Magic Bytes:
    // "As the last step of the decoding process, the decoder MUST
    //  verify the existence of Footer Magic Bytes. If they don't
    //  match, an error MUST be indicated.
    //
    //      Using a C array and ASCII:
    //      const uint8_t FOOTER_MAGIC[2] = { 'Y', 'Z' };
    //
    //      In hexadecimal:
    //      59 5A"
    if (magic[0] != 'Y' || magic[1] != 'Z')
        return Error::from_string_literal("XZ stream footer has an invalid magic");

    return {};
}

u32 XzStreamFooter::backward_size()
{
    // 2.1.2.2. Backward Size:
    // "Backward Size is stored as a 32-bit little endian integer,
    //  which indicates the size of the Index field as multiple of
    //  four bytes, minimum value being four bytes:
    //
    //      real_backward_size = (stored_backward_size + 1) * 4;"
    return (encoded_backward_size + 1) * 4;
}

u8 XzBlockFlags::number_of_filters()
{
    // 3.1.2. Block Flags:
    // "Bit(s)  Mask  Description
    //  0-1    0x03  Number of filters (1-4)"
    return encoded_number_of_filters + 1;
}

ErrorOr<void> XzFilterLzma2Properties::validate()
{
    // 5.3.1. LZMA2:
    // "Bits   Mask   Description
    //  6-7    0xC0   Reserved for future use; MUST be zero for now."
    if (reserved != 0)
        return Error::from_string_literal("XZ LZMA2 filter properties contains non-null reserved bits");

    // "    const uint8_t bits = get_dictionary_flags() & 0x3F;
    //      if (bits > 40)
    //          return DICTIONARY_TOO_BIG; // Bigger than 4 GiB"
    if (encoded_dictionary_size > 40)
        return Error::from_string_literal("XZ LZMA2 filter properties contains larger-than-allowed dictionary size");

    return {};
}

u32 XzFilterLzma2Properties::dictionary_size()
{
    // "Dictionary Size is encoded with one-bit mantissa and five-bit
    //  exponent. The smallest dictionary size is 4 KiB and the biggest
    //  is 4 GiB.
    //  Instead of having a table in the decoder, the dictionary size
    //  can be decoded using the following C code:"
    if (encoded_dictionary_size == 40)
        return NumericLimits<u32>::max();

    u32 dictionary_size = 2 | (encoded_dictionary_size & 1);
    dictionary_size <<= encoded_dictionary_size / 2 + 11;
    return dictionary_size;
}

ErrorOr<NonnullOwnPtr<XzDecompressor>> XzDecompressor::create(MaybeOwned<Stream> stream)
{
    auto counting_stream = TRY(try_make<CountingStream>(move(stream)));

    auto decompressor = TRY(adopt_nonnull_own_or_enomem(new (nothrow) XzDecompressor(move(counting_stream))));

    return decompressor;
}

XzDecompressor::XzDecompressor(NonnullOwnPtr<CountingStream> stream)
    : m_stream(move(stream))
{
}

static Optional<size_t> size_for_check_type(XzStreamCheckType check_type)
{
    switch (check_type) {
    case XzStreamCheckType::None:
        return 0;
    case XzStreamCheckType::CRC32:
        return 4;
    case XzStreamCheckType::CRC64:
        return 8;
    case XzStreamCheckType::SHA256:
        return 32;
    default:
        return {};
    }
}

ErrorOr<Bytes> XzDecompressor::read_some(Bytes bytes)
{
    if (m_found_last_stream_footer)
        return bytes.trim(0);

    if (!m_stream_flags.has_value()) {
        // This assumes that we can just read the Stream Header into memory as-is. Check that this still holds up for good measure.
        static_assert(AK::Traits<XzStreamHeader>::is_trivially_serializable());

        XzStreamHeader stream_header {};
        Bytes stream_header_bytes { &stream_header, sizeof(stream_header) };

        if (m_found_first_stream_header) {
            // 2.2. Stream Padding:
            // "Stream Padding MUST contain only null bytes. To preserve the
            //  four-byte alignment of consecutive Streams, the size of Stream
            //  Padding MUST be a multiple of four bytes. Empty Stream Padding
            //  is allowed. If these requirements are not met, the decoder MUST
            //  indicate an error."

            VERIFY(m_stream->read_bytes() % 4 == 0);

            while (true) {
                // Read the first byte until we either get a non-null byte or reach EOF.
                auto byte_or_error = m_stream->read_value<u8>();

                if (byte_or_error.is_error() && m_stream->is_eof())
                    break;

                auto byte = TRY(byte_or_error);

                if (byte != 0) {
                    stream_header_bytes[0] = byte;
                    stream_header_bytes = stream_header_bytes.slice(1);
                    break;
                }
            }

            // If we aren't at EOF we already read the potential first byte of the header, so we need to subtract that.
            auto end_of_padding_offset = m_stream->read_bytes();
            if (!m_stream->is_eof())
                end_of_padding_offset -= 1;

            if (end_of_padding_offset % 4 != 0)
                return Error::from_string_literal("XZ Stream Padding is not aligned to 4 bytes");

            if (m_stream->is_eof()) {
                m_found_last_stream_footer = true;
                return bytes.trim(0);
            }
        }

        TRY(m_stream->read_until_filled(stream_header_bytes));
        TRY(stream_header.validate());

        m_stream_flags = stream_header.flags;
        m_found_first_stream_header = true;
    }

    if (!m_current_block_stream.has_value() || (*m_current_block_stream)->is_eof()) {
        if (m_current_block_stream.has_value()) {
            // We have already processed a block, so we weed to clean up trailing data before the next block starts.

            // 3.3. Block Padding:
            // "Block Padding MUST contain 0-3 null bytes to make the size of
            //  the Block a multiple of four bytes. This can be needed when
            //  the size of Compressed Data is not a multiple of four."
            while (m_stream->read_bytes() % 4 != 0) {
                auto padding_byte = TRY(m_stream->read_value<u8>());

                // "If any of the bytes in Block Padding are not null bytes, the decoder
                //  MUST indicate an error."
                if (padding_byte != 0)
                    return Error::from_string_literal("XZ block contains a non-null padding byte");
            }

            // 3.4. Check:
            // "The type and size of the Check field depends on which bits
            //  are set in the Stream Flags field (see Section 2.1.1.2).
            //
            //  The Check, when used, is calculated from the original
            //  uncompressed data. If the calculated Check does not match the
            //  stored one, the decoder MUST indicate an error. If the selected
            //  type of Check is not supported by the decoder, it SHOULD
            //  indicate a warning or error."
            auto maybe_check_size = size_for_check_type(m_stream_flags->check_type);

            if (!maybe_check_size.has_value())
                return Error::from_string_literal("XZ stream has an unknown check type");

            // TODO: Block content checks are currently unimplemented as a whole, independent of the check type.
            //       For now, we only make sure to remove the correct amount of bytes from the stream.
            TRY(m_stream->discard(*maybe_check_size));
        }

        auto start_of_current_block = m_stream->read_bytes();

        // Ensure that the start of the block is aligned to a multiple of four (in theory, everything in XZ is).
        // This allows us to make sure that the block padding is correct without having to store the block start offset explicitly.
        VERIFY(start_of_current_block % 4 == 0);

        // The first byte between Block Header (3.1.1. Block Header Size) and Index (4.1. Index Indicator) overlap.
        // Block header sizes have valid values in the range of [0x01, 0xFF], the only valid value for an Index Indicator is therefore 0x00.
        auto encoded_block_header_size_or_index_indicator = TRY(m_stream->read_value<u8>());

        if (encoded_block_header_size_or_index_indicator == 0x00) {
            // This is an Index.

            // 4.2. Number of Records:
            // "This field indicates how many Records there are in the List
            //  of Records field, and thus how many Blocks there are in the
            //  Stream. The value is stored using the encoding described in
            //  Section 1.2."
            u64 number_of_records = TRY(m_stream->read_value<XzMultibyteInteger>());

            // 4.3. List of Records:
            // "List of Records consists of as many Records as indicated by the
            //  Number of Records field:"
            for (u64 i = 0; i < number_of_records; i++) {
                // "Each Record contains information about one Block:
                //
                //      +===============+===================+
                //      | Unpadded Size | Uncompressed Size |
                //      +===============+===================+"

                // 4.3.1. Unpadded Size:
                // "This field indicates the size of the Block excluding the Block
                //  Padding field. That is, Unpadded Size is the size of the Block
                //  Header, Compressed Data, and Check fields. Unpadded Size is
                //  stored using the encoding described in Section 1.2."
                u64 unpadded_size = TRY(m_stream->read_value<XzMultibyteInteger>());

                // "The value MUST never be zero; with the current structure of Blocks, the
                //  actual minimum value for Unpadded Size is five."
                if (unpadded_size < 5)
                    return Error::from_string_literal("XZ index contains a record with an unpadded size of less than five");

                // 4.3.2. Uncompressed Size:
                // "This field indicates the Uncompressed Size of the respective
                //  Block as bytes. The value is stored using the encoding
                //  described in Section 1.2."
                u64 uncompressed_size = TRY(m_stream->read_value<XzMultibyteInteger>());

                // 4.3. List of Records:
                // "If the decoder has decoded all the Blocks of the Stream, it
                //  MUST verify that the contents of the Records match the real
                //  Unpadded Size and Uncompressed Size of the respective Blocks."
                // TODO: Validation of unpadded and uncompressed size against the actual blocks is currently unimplemented.
                (void)unpadded_size;
                (void)uncompressed_size;
            }

            // 4.4. Index Padding:
            // "This field MUST contain 0-3 null bytes to pad the Index to
            //  a multiple of four bytes. If any of the bytes are not null
            //  bytes, the decoder MUST indicate an error."
            while ((m_stream->read_bytes() - start_of_current_block) % 4 != 0) {
                auto padding_byte = TRY(m_stream->read_value<u8>());

                if (padding_byte != 0)
                    return Error::from_string_literal("XZ index contains a non-null padding byte");
            }

            // 4.5. CRC32:
            // "The CRC32 is calculated over everything in the Index field
            //  except the CRC32 field itself. The CRC32 is stored as an
            //  unsigned 32-bit little endian integer."
            u32 index_crc32 = TRY(m_stream->read_value<LittleEndian<u32>>());

            // "If the calculated value does not match the stored one, the decoder MUST indicate
            //  an error."
            // TODO: Validation of the index CRC32 is currently unimplemented.
            (void)index_crc32;

            auto size_of_index = m_stream->read_bytes() - start_of_current_block;

            // According to the specification of a stream (2.1. Stream), the index is the last element in a stream,
            // followed by the stream footer (2.1.2. Stream Footer).
            auto stream_footer = TRY(m_stream->read_value<XzStreamFooter>());

            // This handles verifying the CRC32 (2.1.2.1. CRC32) and the magic bytes (2.1.2.4. Footer Magic Bytes).
            TRY(stream_footer.validate());

            // 2.1.2.2. Backward Size:
            // "If the stored value does not match the real size of the Index
            //  field, the decoder MUST indicate an error."
            if (stream_footer.backward_size() != size_of_index)
                return Error::from_string_literal("XZ index size does not match the stored size in the stream footer");

            // 2.1.2.3. Stream Flags:
            // "This is a copy of the Stream Flags field from the Stream
            //  Header. The information stored to Stream Flags is needed
            //  when parsing the Stream backwards. The decoder MUST compare
            //  the Stream Flags fields in both Stream Header and Stream
            //  Footer, and indicate an error if they are not identical."
            if (Bytes { &*m_stream_flags, sizeof(XzStreamFlags) } != Bytes { &stream_footer.flags, sizeof(stream_footer.flags) })
                return Error::from_string_literal("XZ stream header flags don't match the stream footer");

            // Another XZ Stream might follow, so we just unset the current information and continue on the next read.
            m_stream_flags.clear();
            return bytes.trim(0);
        }

        // 3.1.1. Block Header Size:
        // "This field contains the size of the Block Header field,
        //  including the Block Header Size field itself. Valid values are
        //  in the range [0x01, 0xFF], which indicate the size of the Block
        //  Header as multiples of four bytes, minimum size being eight
        //  bytes:
        //
        //      real_header_size = (encoded_header_size + 1) * 4;"
        u64 block_header_size = (encoded_block_header_size_or_index_indicator + 1) * 4;

        // Read the whole header into a buffer to allow calculating the CRC32 later (3.1.7. CRC32).
        auto header = TRY(ByteBuffer::create_uninitialized(block_header_size));
        header[0] = encoded_block_header_size_or_index_indicator;
        TRY(m_stream->read_until_filled(header.span().slice(1)));

        FixedMemoryStream header_stream { header.span().slice(1) };

        // 3.1.2. Block Flags:
        // "If any reserved bit is set, the decoder MUST indicate an error.
        //  It is possible that there is a new field present which the
        //  decoder is not aware of, and can thus parse the Block Header
        //  incorrectly."
        auto flags = TRY(header_stream.read_value<XzBlockFlags>());

        if (flags.reserved != 0)
            return Error::from_string_literal("XZ block header has reserved non-null block flag bits");

        MaybeOwned<Stream> new_block_stream { *m_stream };

        // 3.1.3. Compressed Size:
        // "This field is present only if the appropriate bit is set in
        //  the Block Flags field (see Section 3.1.2)."
        if (flags.compressed_size_present) {
            // "Compressed Size is stored using the encoding described in Section 1.2."
            u64 compressed_size = TRY(header_stream.read_value<XzMultibyteInteger>());

            // "The Compressed Size field contains the size of the Compressed
            //  Data field, which MUST be non-zero."
            if (compressed_size == 0)
                return Error::from_string_literal("XZ block header contains a compressed size of zero");

            new_block_stream = TRY(try_make<ConstrainedStream>(move(new_block_stream), compressed_size));
        }

        // 3.1.4. Uncompressed Size:
        // "This field is present only if the appropriate bit is set in
        //  the Block Flags field (see Section 3.1.2)."
        if (flags.uncompressed_size_present) {
            // "Uncompressed Size is stored using the encoding described in Section 1.2."
            u64 uncompressed_size = TRY(header_stream.read_value<XzMultibyteInteger>());

            m_current_block_uncompressed_size = uncompressed_size;
        } else {
            m_current_block_uncompressed_size.clear();
        }

        // 3.1.5. List of Filter Flags:
        // "The number of Filter Flags fields is stored in the Block Flags
        //  field (see Section 3.1.2)."
        for (size_t i = 0; i < flags.number_of_filters(); i++) {
            // "The format of each Filter Flags field is as follows:
            //  Both Filter ID and Size of Properties are stored using the
            //  encoding described in Section 1.2."
            u64 filter_id = TRY(header_stream.read_value<XzMultibyteInteger>());
            u64 size_of_properties = TRY(header_stream.read_value<XzMultibyteInteger>());

            // "Size of Properties indicates the size of the Filter Properties field as bytes."
            auto filter_properties = TRY(ByteBuffer::create_uninitialized(size_of_properties));
            TRY(header_stream.read_until_filled(filter_properties));

            // 5.3.1. LZMA2
            if (filter_id == 0x21) {
                if (size_of_properties < sizeof(XzFilterLzma2Properties))
                    return Error::from_string_literal("XZ LZMA2 filter has a smaller-than-needed properties size");

                auto properties = reinterpret_cast<XzFilterLzma2Properties*>(filter_properties.data());
                TRY(properties->validate());

                new_block_stream = TRY(Lzma2Decompressor::create_from_raw_stream(move(new_block_stream), properties->dictionary_size()));
                continue;
            }

            return Error::from_string_literal("XZ block header contains unknown filter ID");
        }

        // 3.1.6. Header Padding:
        // "This field contains as many null byte as it is needed to make
        //  the Block Header have the size specified in Block Header Size."
        constexpr size_t size_of_block_header_size = 1;
        constexpr size_t size_of_crc32 = 4;
        while (MUST(header_stream.tell()) < block_header_size - size_of_block_header_size - size_of_crc32) {
            auto padding_byte = TRY(header_stream.read_value<u8>());

            // "If any of the bytes are not null bytes, the decoder MUST
            //  indicate an error."
            if (padding_byte != 0)
                return Error::from_string_literal("XZ block header padding contains non-null bytes");
        }

        // 3.1.7. CRC32:
        // "The CRC32 is calculated over everything in the Block Header
        //  field except the CRC32 field itself.
        Crypto::Checksum::CRC32 calculated_header_crc32 { header.span().trim(block_header_size - size_of_crc32) };
        //  It is stored as an unsigned 32-bit little endian integer.
        u32 stored_header_crc32 = TRY(header_stream.read_value<LittleEndian<u32>>());
        //  If the calculated value does not match the stored one, the decoder MUST indicate
        //  an error."
        if (calculated_header_crc32.digest() != stored_header_crc32)
            return Error::from_string_literal("Stored XZ block header CRC32 does not match the stored CRC32");

        m_current_block_stream = move(new_block_stream);
    }

    return TRY((*m_current_block_stream)->read_some(bytes));
}

ErrorOr<size_t> XzDecompressor::write_some(ReadonlyBytes)
{
    return Error::from_errno(EBADF);
}

bool XzDecompressor::is_eof() const
{
    return m_found_last_stream_footer;
}

bool XzDecompressor::is_open() const
{
    return true;
}

void XzDecompressor::close()
{
}

}
