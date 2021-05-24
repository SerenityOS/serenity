/*
 * Copyright (c) 2021, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Hex.h>
#include <LibCompress/Deflate.h>
#include <LibPDF/Filter.h>

namespace PDF {

Optional<ByteBuffer> Filter::decode(const ReadonlyBytes& bytes, const FlyString& encoding_type)
{
    if (encoding_type == "ASCIIHexDecode")
        return decode_ascii_hex(bytes);
    if (encoding_type == "ASCII85Decode")
        return decode_ascii85(bytes);
    if (encoding_type == "LZWDecode")
        return decode_lzw(bytes);
    if (encoding_type == "FlateDecode")
        return decode_flate(bytes);
    if (encoding_type == "RunLengthDecode")
        return decode_run_length(bytes);
    if (encoding_type == "CCITTFaxDecode")
        return decode_ccitt(bytes);
    if (encoding_type == "JBIG2Decode")
        return decode_jbig2(bytes);
    if (encoding_type == "DCTDecode")
        return decode_dct(bytes);
    if (encoding_type == "JPXDecode")
        return decode_jpx(bytes);
    if (encoding_type == "Crypt")
        return decode_crypt(bytes);

    return {};
}

Optional<ByteBuffer> Filter::decode_ascii_hex(const ReadonlyBytes& bytes)
{
    if (bytes.size() % 2 == 0)
        return decode_hex(bytes);

    // FIXME: Integrate this padding into AK/Hex?

    auto output = ByteBuffer::create_zeroed(bytes.size() / 2 + 1);

    for (size_t i = 0; i < bytes.size() / 2; ++i) {
        const auto c1 = decode_hex_digit(static_cast<char>(bytes[i * 2]));
        if (c1 >= 16)
            return {};

        const auto c2 = decode_hex_digit(static_cast<char>(bytes[i * 2 + 1]));
        if (c2 >= 16)
            return {};

        output[i] = (c1 << 4) + c2;
    }

    // Process last byte with a padded zero
    output[output.size() - 1] = decode_hex_digit(static_cast<char>(bytes[bytes.size() - 1])) * 16;

    return output;
};

Optional<ByteBuffer> Filter::decode_ascii85(const ReadonlyBytes& bytes)
{
    Vector<u8> buff;
    buff.ensure_capacity(bytes.size());

    size_t byte_index = 0;

    while (byte_index < bytes.size()) {
        if (bytes[byte_index] == ' ') {
            byte_index++;
            continue;
        }

        if (bytes[byte_index] == 'z') {
            byte_index++;
            for (int i = 0; i < 4; i++)
                buff.append(0);
            continue;
        }

        u32 number = 0;

        if (byte_index + 5 >= bytes.size()) {
            auto to_write = bytes.size() - byte_index;
            for (int i = 0; i < 5; i++) {
                auto byte = byte_index >= bytes.size() ? 'u' : bytes[byte_index++];
                if (byte == ' ') {
                    i--;
                    continue;
                }
                number = number * 85 + byte - 33;
            }

            for (size_t i = 0; i < to_write - 1; i++)
                buff.append(reinterpret_cast<u8*>(&number)[3 - i]);

            break;
        } else {
            for (int i = 0; i < 5; i++) {
                auto byte = bytes[byte_index++];
                if (byte == ' ') {
                    i--;
                    continue;
                }
                number = number * 85 + byte - 33;
            }
        }

        for (int i = 0; i < 4; i++)
            buff.append(reinterpret_cast<u8*>(&number)[3 - i]);
    }

    return ByteBuffer::copy(buff.span());
};

Optional<ByteBuffer> Filter::decode_lzw(const ReadonlyBytes&)
{
    dbgln("LZW decoding is not supported");
    VERIFY_NOT_REACHED();
};

Optional<ByteBuffer> Filter::decode_flate(const ReadonlyBytes& bytes)
{
    // FIXME: The spec says Flate decoding is "based on" zlib, does that mean they
    // aren't exactly the same?

    auto buff = Compress::DeflateDecompressor::decompress_all(bytes.slice(2));
    VERIFY(buff.has_value());
    return buff.value();
};

Optional<ByteBuffer> Filter::decode_run_length(const ReadonlyBytes&)
{
    // FIXME: Support RunLength decoding
    TODO();
};

Optional<ByteBuffer> Filter::decode_ccitt(const ReadonlyBytes&)
{
    // FIXME: Support CCITT decoding
    TODO();
};

Optional<ByteBuffer> Filter::decode_jbig2(const ReadonlyBytes&)
{
    // FIXME: Support JBIG2 decoding
    TODO();
};

Optional<ByteBuffer> Filter::decode_dct(const ReadonlyBytes&)
{
    // FIXME: Support dct decoding
    TODO();
};

Optional<ByteBuffer> Filter::decode_jpx(const ReadonlyBytes&)
{
    // FIXME: Support JPX decoding
    TODO();
};

Optional<ByteBuffer> Filter::decode_crypt(const ReadonlyBytes&)
{
    // FIXME: Support Crypt decoding
    TODO();
};

}
