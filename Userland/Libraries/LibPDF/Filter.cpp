/*
 * Copyright (c) 2021, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/BitStream.h>
#include <AK/FixedArray.h>
#include <AK/Hex.h>
#include <LibCompress/Deflate.h>
#include <LibCompress/Lzw.h>
#include <LibCompress/PackBitsDecoder.h>
#include <LibGfx/ImageFormats/CCITTDecoder.h>
#include <LibGfx/ImageFormats/JBIG2Loader.h>
#include <LibGfx/ImageFormats/JPEG2000Loader.h>
#include <LibGfx/ImageFormats/JPEGLoader.h>
#include <LibGfx/ImageFormats/PNGLoader.h>
#include <LibGfx/ImageFormats/TIFFLoader.h>
#include <LibPDF/CommonNames.h>
#include <LibPDF/Filter.h>
#include <LibPDF/Reader.h>

namespace PDF {

PDFErrorOr<ByteBuffer> Filter::decode(Document* document, ReadonlyBytes bytes, DeprecatedFlyString const& encoding_type, RefPtr<DictObject> decode_parms)
{
    if (encoding_type == CommonNames::ASCIIHexDecode)
        return decode_ascii_hex(bytes);
    if (encoding_type == CommonNames::ASCII85Decode)
        return decode_ascii85(bytes);
    if (encoding_type == CommonNames::LZWDecode)
        return decode_lzw(bytes, decode_parms);
    if (encoding_type == CommonNames::FlateDecode)
        return decode_flate(bytes, decode_parms);
    if (encoding_type == CommonNames::RunLengthDecode)
        return decode_run_length(bytes);
    if (encoding_type == CommonNames::CCITTFaxDecode)
        return decode_ccitt(bytes, decode_parms);
    if (encoding_type == CommonNames::JBIG2Decode)
        return decode_jbig2(document, bytes, decode_parms);
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
    TRY(decoded.try_ensure_capacity(bytes.size() - number_of_rows));

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

PDFErrorOr<ByteBuffer> Filter::decode_tiff_prediction(Bytes bytes, int columns, int colors, int bits_per_component)
{
    auto bits_per_sample = TRY(FixedArray<u32>::create(colors));
    bits_per_sample.fill_with(bits_per_component);
    return TRY(Gfx::TIFFImageDecoderPlugin::invert_horizontal_differencing(bytes, columns, bits_per_sample));
}

PDFErrorOr<ByteBuffer> Filter::handle_lzw_and_flate_parameters(ByteBuffer buffer, RefPtr<DictObject> decode_parms)
{
    // Table 3.7 Optional parameters for LZWDecode and FlateDecode filters
    int predictor = 1;
    int colors = 1;
    int bits_per_component = 8;
    int columns = 1;

    if (decode_parms) {
        if (decode_parms->contains(CommonNames::Predictor))
            predictor = decode_parms->get_value(CommonNames::Predictor).get<int>();
        if (decode_parms->contains(CommonNames::Colors))
            colors = decode_parms->get_value(CommonNames::Colors).get<int>();
        if (decode_parms->contains(CommonNames::BitsPerComponent))
            bits_per_component = decode_parms->get_value(CommonNames::BitsPerComponent).get<int>();
        if (decode_parms->contains(CommonNames::Columns))
            columns = decode_parms->get_value(CommonNames::Columns).get<int>();
    }

    if (predictor == 1)
        return buffer;

    // Check if we are dealing with a TIFF or PNG prediction.
    if (predictor != 2 && (predictor < 10 || predictor > 15))
        return AK::Error::from_string_literal("Invalid predictor value");

    // Rows are always a whole number of bytes long, for PNG starting with an algorithm tag.
    auto buffer_bytes = buffer.bytes();
    size_t bytes_per_row = ceil_div(columns * colors * bits_per_component, 8);
    if (predictor != 2)
        bytes_per_row++;
    if (buffer.size() % bytes_per_row) {
        // Rarely, there is some trailing data after the image data. Ignore the part of it that doesn't fit into a row.
        dbgln_if(PDF_DEBUG, "Predictor input data length {} is not divisible into columns {}, dropping {} bytes", buffer.size(), bytes_per_row, buffer.size() % bytes_per_row);
        buffer_bytes = buffer_bytes.slice(0, buffer.size() - buffer.size() % bytes_per_row);
    }

    if (predictor == 2)
        return decode_tiff_prediction(buffer_bytes, columns, colors, bits_per_component);

    size_t bytes_per_pixel = ceil_div(colors * bits_per_component, 8);
    return decode_png_prediction(buffer_bytes, bytes_per_row, bytes_per_pixel);
}

PDFErrorOr<ByteBuffer> Filter::decode_lzw(ReadonlyBytes bytes, RefPtr<DictObject> decode_parms)
{
    // Table 3.7 Optional parameters for LZWDecode and FlateDecode filters
    int early_change = 1;
    if (decode_parms && decode_parms->contains(CommonNames::EarlyChange))
        early_change = decode_parms->get_value(CommonNames::EarlyChange).get<int>();

    auto decoded = TRY(Compress::LzwDecompressor<BigEndianInputBitStream>::decompress_all(bytes, 8, -early_change));
    return handle_lzw_and_flate_parameters(move(decoded), decode_parms);
}

PDFErrorOr<ByteBuffer> Filter::decode_flate(ReadonlyBytes bytes, RefPtr<DictObject> decode_parms)
{
    auto buff = TRY(Compress::DeflateDecompressor::decompress_all(bytes.slice(2)));
    return handle_lzw_and_flate_parameters(move(buff), decode_parms);
}

PDFErrorOr<ByteBuffer> Filter::decode_run_length(ReadonlyBytes bytes)
{
    return TRY(Compress::PackBits::decode_all(bytes, OptionalNone {}, Compress::PackBits::CompatibilityMode::PDF));
}

static void invert_bits(ByteBuffer& decoded)
{
    for (u8& byte : decoded.bytes())
        byte = ~byte;
}

PDFErrorOr<ByteBuffer> Filter::decode_ccitt(ReadonlyBytes bytes, RefPtr<DictObject> decode_parms)
{
    // Table 3.9 Optional parameters for the CCITTFaxDecode filter
    int k = 0;
    bool require_end_of_line = false;
    bool encoded_byte_align = false;
    int columns = 1728;
    int rows = 0;
    bool end_of_block = true;
    bool black_is_1 = false;
    int damaged_rows_before_error = 0;
    if (decode_parms) {
        if (decode_parms->contains(CommonNames::K))
            k = decode_parms->get_value(CommonNames::K).get<int>();
        if (decode_parms->contains(CommonNames::EndOfLine))
            require_end_of_line = decode_parms->get_value(CommonNames::EndOfLine).get<bool>();
        if (decode_parms->contains(CommonNames::EncodedByteAlign))
            encoded_byte_align = decode_parms->get_value(CommonNames::EncodedByteAlign).get<bool>();
        if (decode_parms->contains(CommonNames::Columns))
            columns = decode_parms->get_value(CommonNames::Columns).get<int>();
        if (decode_parms->contains(CommonNames::Rows))
            rows = decode_parms->get_value(CommonNames::Rows).get<int>();
        if (decode_parms->contains(CommonNames::EndOfBlock))
            end_of_block = decode_parms->get_value(CommonNames::EndOfBlock).get<bool>();
        if (decode_parms->contains(CommonNames::BlackIs1))
            black_is_1 = decode_parms->get_value(CommonNames::BlackIs1).get<bool>();
        if (decode_parms->contains(CommonNames::DamagedRowsBeforeError))
            damaged_rows_before_error = decode_parms->get_value(CommonNames::DamagedRowsBeforeError).get<int>();
    }

    if (require_end_of_line || damaged_rows_before_error > 0)
        return Error::rendering_unsupported_error("Unimplemented option for the CCITTFaxDecode Filter");

    ByteBuffer decoded {};
    if (k < 0) {
        Gfx::CCITT::Group4Options options {
            .has_end_of_block = end_of_block ? Gfx::CCITT::Group4Options::HasEndOfBlock::Yes : Gfx::CCITT::Group4Options::HasEndOfBlock::No,
            .encoded_byte_aligned = encoded_byte_align ? Gfx::CCITT::EncodedByteAligned::Yes : Gfx::CCITT::EncodedByteAligned::No,
        };
        decoded = TRY(Gfx::CCITT::decode_ccitt_group4(bytes, columns, rows, options));
    } else if (k == 0) {
        Gfx::CCITT::Group3Options options {
            .require_end_of_line = require_end_of_line ? Gfx::CCITT::Group3Options::RequireEndOfLine::Yes : Gfx::CCITT::Group3Options::RequireEndOfLine::No,
            .encoded_byte_aligned = encoded_byte_align ? Gfx::CCITT::EncodedByteAligned::Yes : Gfx::CCITT::EncodedByteAligned::No,
        };

        decoded = TRY(Gfx::CCITT::decode_ccitt_group3(bytes, columns, rows, options));
    } else {
        return Error::rendering_unsupported_error("CCITTFaxDecode Filter Group 3, 2-D is unsupported");
    }

    if (!black_is_1)
        invert_bits(decoded);

    return decoded;
}

PDFErrorOr<ByteBuffer> Filter::decode_jbig2(Document* document, ReadonlyBytes bytes, RefPtr<DictObject> decode_parms)
{
    // 3.3.6 JBIG2Decode Filter
    Vector<ReadonlyBytes> segments;
    if (decode_parms) {
        if (decode_parms->contains(CommonNames::JBIG2Globals)) {
            auto globals = TRY(decode_parms->get_stream(document, CommonNames::JBIG2Globals));
            segments.append(globals->bytes());
        }
    }

    segments.append(bytes);
    auto decoded = TRY(Gfx::JBIG2ImageDecoderPlugin::decode_embedded(segments));

    // JBIG2 treats `1` as "ink present" (black) and `0` as "no ink" (white).
    // PDF treats `1` as "light present" (white) and `1` as "no light" (black).
    // So we have to invert.
    invert_bits(decoded);

    return decoded;
}

PDFErrorOr<ByteBuffer> Filter::decode_dct(ReadonlyBytes bytes)
{
    if (!Gfx::JPEGImageDecoderPlugin::sniff(bytes))
        return AK::Error::from_string_literal("Not a JPEG image!");

    auto decoder = TRY(Gfx::JPEGImageDecoderPlugin::create_with_options(bytes, { .cmyk = Gfx::JPEGDecoderOptions::CMYK::PDF }));
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

PDFErrorOr<ByteBuffer> Filter::decode_jpx(ReadonlyBytes bytes)
{
    if (!Gfx::JPEG2000ImageDecoderPlugin::sniff(bytes))
        return AK::Error::from_string_literal("Not a JPEG2000 image!");

    auto decoder = TRY(Gfx::JPEG2000ImageDecoderPlugin::create_with_options(bytes, { .palette_handling = Gfx::JPEG2000DecoderOptions::PaletteHandling::PaletteIndicesAsGrayscale }));
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

PDFErrorOr<ByteBuffer> Filter::decode_crypt(ReadonlyBytes)
{
    return Error::rendering_unsupported_error("Crypt Filter is not supported");
}

}
