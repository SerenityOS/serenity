/*
 * Copyright (c) 2024, Lucas Chollet <lucas.chollet@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/ConstrainedStream.h>
#include <AK/MemoryStream.h>
#include <LibGfx/ImageFormats/JPEGXLCommon.h>
#include <LibGfx/ImageFormats/JPEGXLEntropyDecoder.h>

namespace Gfx {

namespace {
/// E.4.1 - Data stream
u8 icc_context(u64 i, u8 b1, u8 b2)
{
    u8 p1 = 0;
    u8 p2 = 0;

    if (i <= 128)
        return 0;

    if (b1 >= 'a' && b1 <= 'z')
        p1 = 0;
    else if (b1 >= 'A' && b1 <= 'Z')
        p1 = 0;
    else if (b1 >= '0' && b1 <= '9')
        p1 = 1;
    else if (b1 == '.' || b1 == ',')
        p1 = 1;
    else if (b1 <= 1)
        p1 = 2 + b1;
    else if (b1 > 1 && b1 < 16)
        p1 = 4;
    else if (b1 > 240 && b1 < 255)
        p1 = 5;
    else if (b1 == 255)
        p1 = 6;
    else
        p1 = 7;

    if (b2 >= 'a' && b2 <= 'z')
        p2 = 0;
    else if (b2 >= 'A' && b2 <= 'Z')
        p2 = 0;
    else if (b2 >= '0' && b2 <= '9')
        p2 = 1;
    else if (b2 == '.' || b2 == ',')
        p2 = 1;
    else if (b2 < 16)
        p2 = 2;
    else if (b2 > 240)
        p2 = 3;
    else
        p2 = 4;

    return 1 + p1 + p2 * 8;
}

ErrorOr<ByteBuffer> read_encoded_icc_stream(LittleEndianInputBitStream& stream)
{
    auto const enc_size = TRY(U64(stream));

    auto decoder = TRY(EntropyDecoder::create(stream, 41));

    ByteBuffer uncompressed_icc_stream {};
    TRY(uncompressed_icc_stream.try_resize(enc_size));
    for (u64 index = 0; index < enc_size; ++index) {
        auto const prev_byte = index > 0 ? uncompressed_icc_stream[index - 1] : 0u;
        auto const prev_prev_byte = index > 1 ? uncompressed_icc_stream[index - 2] : 0u;
        auto const context = icc_context(index, prev_byte, prev_prev_byte);
        uncompressed_icc_stream[index] = TRY(decoder.decode_hybrid_uint(stream, context));
    }

    return uncompressed_icc_stream;
}
///

/// E.4.2 - Encoded ICC stream
ErrorOr<u64> read_varint(Stream& stream)
{
    u64 value = 0;
    u8 shift = 0;
    while (1) {
        if (shift >= 56)
            return Error::from_string_literal("JPEGXLImageDecoderPlugin: Invalint shift value in varint");
        auto const b = TRY(stream.read_value<u8>());
        value += (b & 127) << shift;
        if (b <= 127)
            break;
        shift += 7;
    }
    return value;
}
///

/// E.4.3 - ICC header
ErrorOr<void> read_icc_header(Stream& data_stream, ByteBuffer& out)
{
    u8 const header_size = min(128u, out.capacity());

    for (u8 i = 0; i < header_size; ++i) {
        auto const e = TRY(data_stream.read_value<u8>());
        u8 p = 0;

        if (i == 0 || i == 1 || i == 2 || i == 3) {
            // 'output_size[i]' means byte i of output_size encoded as an
            // unsigned 32-bit integer in big endian order
            BigEndian const output_size_as_be { static_cast<u32>(out.capacity()) };
            p = bit_cast<u8 const*>(&output_size_as_be)[i];
        } else if (i == 8) {
            p = 4;
        } else if (i >= 12 && i <= 23) {
            auto s = "mntrRGB XYZ "sv;
            p = s[i - 12];
        } else if (i >= 36 && i <= 39) {
            auto s = "acsp"sv;
            p = s[i - 36];
        } else if ((i == 41 || i == 42) && out[40] == 'A') {
            p = 'P';
        } else if (i == 43 && out[40] == 'A') {
            p = 'L';
        } else if (i == 41 && out[40] == 'M') {
            p = 'S';
        } else if (i == 42 && out[40] == 'M') {
            p = 'F';
        } else if (i == 43 && out[40] == 'M') {
            p = 'T';
        } else if (i == 42 && out[40] == 'S' && out[41] == 'G') {
            p = 'I';
        } else if (i == 43 && out[40] == 'S' && out[41] == 'G') {
            p = 32;
        } else if (i == 42 && out[40] == 'S' && out[41] == 'U') {
            p = 'N';
        } else if (i == 43 && out[40] == 'S' && out[41] == 'U') {
            p = 'W';
        } else if (i == 70) {
            p = 246;
        } else if (i == 71) {
            p = 214;
        } else if (i == 73) {
            p = 1;
        } else if (i == 78) {
            p = 211;
        } else if (i == 79) {
            p = 45;
        } else if (i >= 80 && i < 84) {
            p = out[4 + i - 80];
        }

        out.append((p + e) & 255);
    }

    return {};
}
///

/// E.4.4 - ICC tag list

ErrorOr<void> append_as_u32_be(ByteBuffer& buffer, u32 value)
{
    BigEndian be_value { value };
    return buffer.try_append({ &be_value, sizeof(be_value) });
}
ErrorOr<void> read_tag_list(ConstrainedStream& command_stream, Stream& data_stream, ByteBuffer& out)
{
    auto const v = TRY(read_varint(command_stream));
    if (v == 0)
        return {};
    auto const num_tags = v - 1;

    TRY(append_as_u32_be(out, num_tags));
    u32 previous_tagstart = num_tags * 12 + 128;
    u32 previous_tagsize = 0;

    // Then, the decoder repeatedly reads a tag as specified by the following code until a tag with tagcode
    // equal to 0 is read or until the end of the command stream is reached.
    while (command_stream.remaining() > 0) {
        auto const command = TRY(command_stream.read_value<u8>());
        auto const tagcode = command & 63;
        if (tagcode == 0)
            return {};

        Array<u8, 4> tag;
        if (tagcode == 1) {
            TRY(data_stream.read_until_filled(tag));
        } else if (tagcode == 2) {
            tag = Array<u8, 4>::from_span("rTRC"sv.bytes());
        } else if (tagcode == 3) {
            tag = Array<u8, 4>::from_span("rXYZ"sv.bytes());
        } else if (tagcode >= 4 && tagcode < 21) {
            static constexpr auto strings = to_array(
                { "cprt"sv, "wtpt"sv, "bkpt"sv, "rXYZ"sv, "gXYZ"sv, "bXYZ"sv, "kXYZ"sv, "rTRC"sv, "gTRC"sv,
                    "bTRC"sv, "kTRC"sv, "chad"sv, "desc"sv, "chrm"sv, "dmnd"sv, "dmdd"sv, "lumi"sv });
            tag = Array<u8, 4>::from_span(strings[tagcode - 4].bytes());
        } else {
            return Error::from_string_literal("JPEGXLImageDecoderPlugin: Invalid tagcode in ICC profile");
        }

        auto tagstart = previous_tagstart + previous_tagsize;
        if ((command & 64) != 0)
            tagstart = TRY(read_varint(command_stream));

        u32 tagsize = previous_tagsize;
        if (tag == "rXYZ"sv.bytes() || tag == "gXYZ"sv.bytes() || tag == "bXYZ"sv.bytes()
            || tag == "kXYZ"sv.bytes() || tag == "wtpt"sv.bytes() || tag == "bkpt"sv.bytes()
            || tag == "lumi"sv.bytes())
            tagsize = 20;

        if ((command & 128) != 0)
            tagsize = TRY(read_varint(command_stream));
        previous_tagstart = tagstart;
        previous_tagsize = tagsize;

        // Write tag to output

        TRY(out.try_append(tag));
        TRY(append_as_u32_be(out, tagstart));
        TRY(append_as_u32_be(out, tagsize));
        if (tagcode == 2) {
            out.append("gTRX"sv.bytes());
            TRY(append_as_u32_be(out, tagstart));
            TRY(append_as_u32_be(out, tagsize));

            out.append("bTRC"sv.bytes());
            TRY(append_as_u32_be(out, tagstart));
            TRY(append_as_u32_be(out, tagsize));
        } else if (tagcode == 3) {
            out.append("gXYZ"sv.bytes());
            TRY(append_as_u32_be(out, tagstart + tagsize));
            TRY(append_as_u32_be(out, tagsize));

            out.append("bXYZ"sv.bytes());
            TRY(append_as_u32_be(out, tagstart + 2 * tagsize));
            TRY(append_as_u32_be(out, tagsize));
        }
    }

    return {};
}
///

/// E.4.5 - Main content
ErrorOr<void> shuffle(Bytes bytes, u8 width)
{
    auto temp = TRY(ByteBuffer::create_uninitialized(bytes.size()));

    u64 const height = (bytes.size() + width - 1) / width;

    u64 row_start = 0;
    u64 j = 0;
    for (size_t i = 0; i < bytes.size(); i++) {
        temp[i] = bytes[j];
        j += height;
        if (j >= bytes.size()) {
            ++row_start;
            j = row_start;
        }
    }

    temp.bytes().copy_to(bytes);
    return {};
}

ErrorOr<void> read_icc_main_content(ConstrainedStream& command_stream, Stream& data_stream, ByteBuffer& out)
{
    while (command_stream.remaining() > 0) {
        auto const command = TRY(command_stream.read_value<u8>());
        if (command == 1) {
            auto const num = TRY(read_varint(command_stream));
            auto bytes = TRY(out.get_bytes_for_writing(num));
            TRY(data_stream.read_until_filled(bytes));
        } else if (command == 2 or command == 3) {
            auto const num = TRY(read_varint(command_stream));
            auto bytes = TRY(out.get_bytes_for_writing(num));
            TRY(data_stream.read_until_filled(bytes));
            u8 width = (command == 2) ? 2 : 4;
            TRY(shuffle(bytes, width));
        } else if (command == 4) {
            u8 const flags = TRY(command_stream.read_value<u8>());
            u8 const width = (flags & 3) + 1;
            u8 const order = (flags & 12) >> 2;
            if (width == 3 || order == 3)
                return Error::from_string_literal("JPEGXLImageDecoderPlugin: Invalid width or order value");

            u64 stride = width;
            if ((flags & 16) != 0)
                stride = TRY(read_varint(command_stream));

            if (stride * 4 >= out.size() || stride < width)
                return Error::from_string_literal("JPEGXLImageDecoderPlugin: Invalid stride value");

            auto const num = TRY(read_varint(command_stream));
            ByteBuffer bytes;
            TRY(bytes.try_resize(num));
            TRY(data_stream.read_until_filled(bytes));
            if (width == 2 || width == 4)
                TRY(shuffle(bytes, width));

            for (u64 i = 0; i < num; i += width) {
                // NOTE: 0 <= order <= 2
                u8 const N = order + 1;
                Array<u32, 3> prev {};
                for (u8 j = 0; j < N; ++j) {
                    Array<u8, 4> bytes {};
                    for (u8 k = 0; k < width; ++k)
                        bytes[4 - width + k] = out[stride * (j + 1) + k];
                    prev[j] = BigEndian { *bit_cast<u32*>(bytes.data()) };
                }

                u32 p;
                if (order == 0)
                    p = prev[0];
                else if (order == 1)
                    p = 2 * prev[0] - prev[1];
                else if (order == 2)
                    p = 3 * prev[0] - 3 * prev[1] + prev[2];

                for (u8 j = 0; j < width && i + j < num; ++j) {
                    u8 const val = (bytes[i + j] + (p >> (8 * (width - 1 - j)))) & 255;
                    TRY(out.try_append(val));
                }
            }
        } else if (command == 10) {
            TRY(out.try_append("XYZ "sv.bytes()));
            Array<u8, 4> constexpr zeros {};
            TRY(out.try_append(zeros));
            auto bytes = TRY(out.get_bytes_for_writing(12));
            TRY(data_stream.read_until_filled(bytes));
        } else if (command >= 16 and command < 24) {
            Array constexpr strings = { "XYZ "sv, "desc"sv, "text"sv, "mluc"sv, "para"sv, "curv"sv, "sf32"sv, "gbd "sv };
            TRY(out.try_append(strings[command - 16].bytes()));
            Array<u8, 4> constexpr zeros {};
            TRY(out.try_append(zeros));
        } else {
            return Error::from_string_literal("JPEGXLImageDecoderPlugin: Invalid command in ICC main context");
        }
    }

    return {};
}
///
}

/// E.4 - ICC profile
ErrorOr<ByteBuffer> read_icc(LittleEndianInputBitStream& stream)
{
    auto const encoded_icc = TRY(read_encoded_icc_stream(stream));

    FixedMemoryStream buffer(encoded_icc);
    auto const output_size = TRY(read_varint(buffer));
    auto const commands_size = TRY(read_varint(buffer));

    ConstrainedStream command_stream { MaybeOwned<Stream>(buffer), commands_size };

    auto const data_offset = buffer.offset() + commands_size;
    FixedMemoryStream data_stream(encoded_icc);
    TRY(data_stream.discard(data_offset));

    ByteBuffer out;
    TRY(out.try_ensure_capacity(output_size));

    TRY(read_icc_header(data_stream, out));

    if (output_size <= 128)
        return out;

    TRY(read_tag_list(command_stream, data_stream, out));

    TRY(read_icc_main_content(command_stream, data_stream, out));

    return out;
}
///

}
