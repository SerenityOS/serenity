/*
 * Copyright (c) 2021, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/BitStream.h>
#include <AK/Hex.h>
#include <LibCompress/Deflate.h>
#include <LibCompress/LZWDecoder.h>
#include <LibCompress/PackBitsDecoder.h>
#include <LibGfx/ImageFormats/JPEGLoader.h>
#include <LibGfx/ImageFormats/PNGLoader.h>
#include <LibPDF/CommonNames.h>
#include <LibPDF/Filter.h>
#include <LibPDF/Reader.h>

namespace PDF {

PDFErrorOr<ByteBuffer> Filter::decode(ReadonlyBytes bytes, DeprecatedFlyString const& encoding_type, RefPtr<DictObject> decode_parms)
{
    int predictor = 1;
    int columns = 1;
    int colors = 1;
    int bits_per_component = 8;
    int early_change = 1;

    if (decode_parms) {
        if (decode_parms->contains(CommonNames::Predictor))
            predictor = decode_parms->get_value(CommonNames::Predictor).get<int>();
        if (decode_parms->contains(CommonNames::Columns))
            columns = decode_parms->get_value(CommonNames::Columns).get<int>();
        if (decode_parms->contains(CommonNames::Colors))
            colors = decode_parms->get_value(CommonNames::Colors).get<int>();
        if (decode_parms->contains(CommonNames::BitsPerComponent))
            bits_per_component = decode_parms->get_value(CommonNames::BitsPerComponent).get<int>();
        if (decode_parms->contains(CommonNames::EarlyChange))
            early_change = decode_parms->get_value(CommonNames::EarlyChange).get<int>();
    }

    if (encoding_type == CommonNames::ASCIIHexDecode)
        return decode_ascii_hex(bytes);
    if (encoding_type == CommonNames::ASCII85Decode)
        return decode_ascii85(bytes);
    if (encoding_type == CommonNames::LZWDecode)
        return decode_lzw(bytes, predictor, columns, colors, bits_per_component, early_change);
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

    dbgln_if(PDF_DEBUG, "Unrecognized filter encoding {}", encoding_type);
    return Error::malformed_error("Unrecognized filter encoding");
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
    // 3.3.2 ASCII85Decode Filter

    ByteBuffer buffer;
    TRY(buffer.try_ensure_capacity(bytes.size()));

    size_t byte_index = 0;

    while (byte_index < bytes.size()) {
        if (Reader::is_whitespace(bytes[byte_index])) {
            byte_index++;
            continue;
        }

        if (bytes[byte_index] == 'z') {
            byte_index++;
            for (int i = 0; i < 4; i++)
                buffer.append(0);
            continue;
        }

        u32 number = 0;

        auto to_write = byte_index + 5 >= bytes.size() ? bytes.size() - byte_index : 5;

        Optional<u32> end_of_data_index {};

        for (int i = 0; i < 5; i++) {
            // We check for the EOD sequence '~>', but as '~' can only appear in
            // this sequence, there is no need to check for '>'.
            if (!end_of_data_index.has_value() && bytes[byte_index] == '~') {
                end_of_data_index = i;
                to_write = i + 1;
            }

            bool const should_fake_end = byte_index >= bytes.size() || end_of_data_index.has_value();
            auto const byte = should_fake_end ? 'u' : bytes[byte_index++];

            if (Reader::is_whitespace(byte)) {
                i--;
                continue;
            }
            number = number * 85 + byte - 33;
        }

        for (size_t i = 0; i < to_write - 1; i++)
            buffer.append(reinterpret_cast<u8*>(&number)[3 - i]);

        if (end_of_data_index.has_value())
            break;
    }

    return buffer;
}

PDFErrorOr<ByteBuffer> Filter::decode_png_prediction(Bytes bytes, size_t bytes_per_row, size_t bytes_per_pixel)
{
    int number_of_rows = bytes.size() / bytes_per_row;

    ByteBuffer decoded;
    decoded.ensure_capacity(bytes.size() - number_of_rows);

    auto empty_row = TRY(ByteBuffer::create_zeroed(bytes_per_row - 1));
    auto previous_row = empty_row.bytes();

    for (int row_index = 0; row_index < number_of_rows; ++row_index) {
        auto row = Bytes { bytes.data() + row_index * bytes_per_row, bytes_per_row };

        auto filter = TRY(Gfx::PNG::filter_type(row[0]));
        row = row.slice(1);

        Gfx::PNGImageDecoderPlugin::unfilter_scanline(filter, row, previous_row, bytes_per_pixel);

        previous_row = row;
        decoded.append(row);
    }

    return decoded;
}

PDFErrorOr<ByteBuffer> Filter::handle_lzw_and_flate_parameters(ByteBuffer buffer, int predictor, int columns, int colors, int bits_per_component)
{
    // Table 3.7 Optional parameters for LZWDecode and FlateDecode filters

    if (predictor == 1)
        return buffer;

    // Check if we are dealing with a PNG prediction
    if (predictor == 2)
        return AK::Error::from_string_literal("The TIFF predictor is not supported");
    if (predictor < 10 || predictor > 15)
        return AK::Error::from_string_literal("Invalid predictor value");

    // Rows are always a whole number of bytes long, starting with an algorithm tag
    size_t const bytes_per_row = ceil_div(columns * colors * bits_per_component, 8) + 1;
    if (buffer.size() % bytes_per_row)
        return AK::Error::from_string_literal("Flate input data is not divisible into columns");

    size_t bytes_per_pixel = ceil_div(colors * bits_per_component, 8);
    return decode_png_prediction(buffer, bytes_per_row, bytes_per_pixel);
}

PDFErrorOr<ByteBuffer> Filter::decode_lzw(ReadonlyBytes bytes, int predictor, int columns, int colors, int bits_per_component, int early_change)
{
    auto decoded = TRY(Compress::LZWDecoder<BigEndianInputBitStream>::decode_all(bytes, 8, -early_change));
    return handle_lzw_and_flate_parameters(move(decoded), predictor, columns, colors, bits_per_component);
}

PDFErrorOr<ByteBuffer> Filter::decode_flate(ReadonlyBytes bytes, int predictor, int columns, int colors, int bits_per_component)
{
    auto buff = TRY(Compress::DeflateDecompressor::decompress_all(bytes.slice(2)));
    return handle_lzw_and_flate_parameters(move(buff), predictor, columns, colors, bits_per_component);
}

PDFErrorOr<ByteBuffer> Filter::decode_run_length(ReadonlyBytes bytes)
{
    return TRY(Compress::PackBits::decode_all(bytes, OptionalNone {}, Compress::PackBits::CompatibilityMode::PDF));
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
    if (!Gfx::JPEGImageDecoderPlugin::sniff({ bytes.data(), bytes.size() }))
        return AK::Error::from_string_literal("Not a JPEG image!");

    auto decoder = TRY(Gfx::JPEGImageDecoderPlugin::create_with_options({ bytes.data(), bytes.size() }, { .cmyk = Gfx::JPEGDecoderOptions::CMYK::PDF }));
    auto internal_format = decoder->natural_frame_format();

    if (internal_format == Gfx::NaturalFrameFormat::CMYK) {
        auto bitmap = TRY(decoder->cmyk_frame());
        // FIXME: Could give CMYKBitmap a method to steal its internal ByteBuffer.
        auto size = bitmap->size().width() * bitmap->size().height() * 4;
        auto buffer = TRY(ByteBuffer::create_uninitialized(size));
        buffer.overwrite(0, bitmap->scanline(0), size);
        return buffer;
    }

    auto bitmap = TRY(decoder->frame(0)).image;
    auto size = bitmap->size().width() * bitmap->size().height() * (internal_format == Gfx::NaturalFrameFormat::Grayscale ? 1 : 3);
    ByteBuffer buffer;
    TRY(buffer.try_ensure_capacity(size));

    for (auto& pixel : *bitmap) {
        Color color = Color::from_argb(pixel);
        if (internal_format == Gfx::NaturalFrameFormat::Grayscale) {
            // Either channel is fine, they're all the same.
            buffer.append(color.red());
        } else {
            buffer.append(color.red());
            buffer.append(color.green());
            buffer.append(color.blue());
        }
    }
    return buffer;
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
