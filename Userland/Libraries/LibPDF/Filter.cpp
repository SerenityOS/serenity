/*
 * Copyright (c) 2021, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Hex.h>
#include <LibCompress/Deflate.h>
#include <LibGfx/ImageFormats/JPEGLoader.h>
#include <LibPDF/CommonNames.h>
#include <LibPDF/Filter.h>

namespace PDF {

PDFErrorOr<ByteBuffer> Filter::decode(ReadonlyBytes bytes, DeprecatedFlyString const& encoding_type, RefPtr<DictObject> decode_parms)
{
    int predictor = 1;
    int columns = 1;
    int colors = 1;
    int bits_per_component = 8;

    if (decode_parms) {
        if (decode_parms->contains(CommonNames::Predictor))
            predictor = decode_parms->get_value(CommonNames::Predictor).get<int>();
        if (decode_parms->contains(CommonNames::Columns))
            columns = decode_parms->get_value(CommonNames::Columns).get<int>();
        if (decode_parms->contains(CommonNames::Colors))
            colors = decode_parms->get_value(CommonNames::Colors).get<int>();
        if (decode_parms->contains(CommonNames::BitsPerComponent))
            bits_per_component = decode_parms->get_value(CommonNames::BitsPerComponent).get<int>();
    }

    if (encoding_type == CommonNames::ASCIIHexDecode)
        return decode_ascii_hex(bytes);
    if (encoding_type == CommonNames::ASCII85Decode)
        return decode_ascii85(bytes);
    if (encoding_type == CommonNames::LZWDecode)
        return decode_lzw(bytes);
    if (encoding_type == CommonNames::FlateDecode)
        return decode_flate(bytes, predictor, columns, colors, bits_per_component);
    if (encoding_type == CommonNames::RunLengthDecode)
        return decode_run_length(bytes);
    if (encoding_type == CommonNames::CCITTFaxDecode)
        return decode_ccitt(bytes);
    if (encoding_type == CommonNames::JBIG2Decode)
        return decode_jbig2(bytes);
    if (encoding_type == CommonNames::DCTDecode)
        return decode_dct(bytes);
    if (encoding_type == CommonNames::JPXDecode)
        return decode_jpx(bytes);
    if (encoding_type == CommonNames::Crypt)
        return decode_crypt(bytes);

    return Error::malformed_error("Unrecognized filter encoding {}", encoding_type);
}

PDFErrorOr<ByteBuffer> Filter::decode_ascii_hex(ReadonlyBytes bytes)
{
    ByteBuffer output;

    bool have_read_high_nibble = false;
    u8 high_nibble = 0;
    for (u8 byte : bytes) {
        // 3.3.1 ASCIIHexDecode Filter
        // All white-space characters [...] are ignored.
        // FIXME: Any other characters cause an error.
        if (is_ascii_hex_digit(byte)) {
            u8 hex_digit = decode_hex_digit(byte);
            if (have_read_high_nibble) {
                u8 full_byte = (high_nibble << 4) | hex_digit;
                TRY(output.try_append(full_byte));
                have_read_high_nibble = false;
            } else {
                high_nibble = hex_digit;
                have_read_high_nibble = true;
            }
        }
    }

    // If the filter encounters the EOD marker after reading an odd number
    // of hexadecimal digits, it behaves as if a 0 followed the last digit.
    if (have_read_high_nibble)
        TRY(output.try_append(high_nibble << 4));

    return output;
}

PDFErrorOr<ByteBuffer> Filter::decode_ascii85(ReadonlyBytes bytes)
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

    return TRY(ByteBuffer::copy(buff.span()));
}

PDFErrorOr<ByteBuffer> Filter::decode_png_prediction(Bytes bytes, int bytes_per_row)
{
    int number_of_rows = bytes.size() / bytes_per_row;

    ByteBuffer decoded;
    decoded.ensure_capacity(bytes.size() - number_of_rows);

    auto empty_row = TRY(ByteBuffer::create_zeroed(bytes_per_row));
    auto previous_row = empty_row.data();

    for (int row_index = 0; row_index < number_of_rows; ++row_index) {
        auto row = bytes.data() + row_index * bytes_per_row;

        u8 algorithm_tag = row[0];
        switch (algorithm_tag) {
        case 0:
            break;
        case 1:
            for (int i = 2; i < bytes_per_row; ++i)
                row[i] += row[i - 1];
            break;
        case 2:
            for (int i = 1; i < bytes_per_row; ++i)
                row[i] += previous_row[i];
            break;
        case 3:
            for (int i = 1; i < bytes_per_row; ++i) {
                u8 left = 0;
                if (i > 1)
                    left = row[i - 1];
                u8 above = previous_row[i];
                row[i] += (left + above) / 2;
            }
            break;
        case 4:
            for (int i = 1; i < bytes_per_row; ++i) {
                u8 left = 0;
                u8 upper_left = 0;
                if (i > 1) {
                    left = row[i - 1];
                    upper_left = previous_row[i - 1];
                }
                u8 above = previous_row[i];
                u8 p = left + above - upper_left;

                int left_distance = abs(p - left);
                int above_distance = abs(p - above);
                int upper_left_distance = abs(p - upper_left);

                u8 paeth = min(left_distance, min(above_distance, upper_left_distance));

                row[i] += paeth;
            }
            break;
        default:
            return AK::Error::from_string_literal("Unknown PNG algorithm tag");
        }

        previous_row = row;
        decoded.append(row + 1, bytes_per_row - 1);
    }

    return decoded;
}

PDFErrorOr<ByteBuffer> Filter::decode_lzw(ReadonlyBytes)
{
    return Error::rendering_unsupported_error("LZW Filter is not supported");
}

PDFErrorOr<ByteBuffer> Filter::decode_flate(ReadonlyBytes bytes, int predictor, int columns, int colors, int bits_per_component)
{
    auto buff = Compress::DeflateDecompressor::decompress_all(bytes.slice(2)).value();
    if (predictor == 1)
        return buff;

    // Check if we are dealing with a PNG prediction
    if (predictor == 2)
        return AK::Error::from_string_literal("The TIFF predictor is not supported");
    if (predictor < 10 || predictor > 15)
        return AK::Error::from_string_literal("Invalid predictor value");

    // Rows are always a whole number of bytes long, starting with an algorithm tag
    int bytes_per_row = AK::ceil_div(columns * colors * bits_per_component, 8) + 1;
    if (buff.size() % bytes_per_row)
        return AK::Error::from_string_literal("Flate input data is not divisible into columns");

    return decode_png_prediction(buff, bytes_per_row);
}

PDFErrorOr<ByteBuffer> Filter::decode_run_length(ReadonlyBytes bytes)
{
    constexpr size_t END_OF_DECODING = 128;
    ByteBuffer buffer {};
    while (true) {
        VERIFY(bytes.size() > 0);
        auto length = bytes[0];
        bytes = bytes.slice(1);
        if (length == END_OF_DECODING) {
            VERIFY(bytes.is_empty());
            break;
        }
        if (length < 128) {
            TRY(buffer.try_append(bytes.slice(0, length + 1)));
            bytes = bytes.slice(length + 1);
        } else {
            VERIFY(bytes.size() > 1);
            auto byte_to_append = bytes[0];
            bytes = bytes.slice(1);
            size_t n_chars = 257 - length;
            for (size_t i = 0; i < n_chars; ++i) {
                TRY(buffer.try_append(byte_to_append));
            }
        }
    }
    return buffer;
}

PDFErrorOr<ByteBuffer> Filter::decode_ccitt(ReadonlyBytes)
{
    return Error::rendering_unsupported_error("CCITTFaxDecode Filter is unsupported");
}

PDFErrorOr<ByteBuffer> Filter::decode_jbig2(ReadonlyBytes)
{
    return Error::rendering_unsupported_error("JBIG2 Filter is unsupported");
}

PDFErrorOr<ByteBuffer> Filter::decode_dct(ReadonlyBytes bytes)
{
    if (Gfx::JPEGImageDecoderPlugin::sniff({ bytes.data(), bytes.size() })) {
        auto decoder = TRY(Gfx::JPEGImageDecoderPlugin::create({ bytes.data(), bytes.size() }));
        auto frame = TRY(decoder->frame(0));
        return TRY(frame.image->serialize_to_byte_buffer());
    }
    return AK::Error::from_string_literal("Not a JPEG image!");
}

PDFErrorOr<ByteBuffer> Filter::decode_jpx(ReadonlyBytes)
{
    return Error::rendering_unsupported_error("JPX Filter is not supported");
}

PDFErrorOr<ByteBuffer> Filter::decode_crypt(ReadonlyBytes)
{
    return Error::rendering_unsupported_error("Crypt Filter is not supported");
}

}
