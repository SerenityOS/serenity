/*
 * Copyright (c) 2023, Lucas Chollet <lucas.chollet@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Array.h>
#include <AK/BitStream.h>
#include <AK/MemoryStream.h>
#include <LibGfx/Color.h>
#include <LibGfx/ImageFormats/CCITTDecoder.h>

namespace Gfx::CCITT {

namespace {

struct Code {
    u16 run_length {};
    u8 code_length {};
    u16 code {};
};

// Table 2/T.4 – Terminating codes
constexpr Array white_terminating_codes = {
    Code { 0, 8, 0b00110101 },
    Code { 1, 6, 0b000111 },
    Code { 2, 4, 0b0111 },
    Code { 3, 4, 0b1000 },
    Code { 4, 4, 0b1011 },
    Code { 5, 4, 0b1100 },
    Code { 6, 4, 0b1110 },
    Code { 7, 4, 0b1111 },
    Code { 8, 5, 0b10011 },
    Code { 9, 5, 0b10100 },
    Code { 10, 5, 0b00111 },
    Code { 11, 5, 0b01000 },
    Code { 12, 6, 0b001000 },
    Code { 13, 6, 0b000011 },
    Code { 14, 6, 0b110100 },
    Code { 15, 6, 0b110101 },
    Code { 16, 6, 0b101010 },
    Code { 17, 6, 0b101011 },
    Code { 18, 7, 0b0100111 },
    Code { 19, 7, 0b0001100 },
    Code { 20, 7, 0b0001000 },
    Code { 21, 7, 0b0010111 },
    Code { 22, 7, 0b0000011 },
    Code { 23, 7, 0b0000100 },
    Code { 24, 7, 0b0101000 },
    Code { 25, 7, 0b0101011 },
    Code { 26, 7, 0b0010011 },
    Code { 27, 7, 0b0100100 },
    Code { 28, 7, 0b0011000 },
    Code { 29, 8, 0b00000010 },
    Code { 30, 8, 0b00000011 },
    Code { 31, 8, 0b00011010 },
    Code { 32, 8, 0b00011011 },
    Code { 33, 8, 0b00010010 },
    Code { 34, 8, 0b00010011 },
    Code { 35, 8, 0b00010100 },
    Code { 36, 8, 0b00010101 },
    Code { 37, 8, 0b00010110 },
    Code { 38, 8, 0b00010111 },
    Code { 39, 8, 0b00101000 },
    Code { 40, 8, 0b00101001 },
    Code { 41, 8, 0b00101010 },
    Code { 42, 8, 0b00101011 },
    Code { 43, 8, 0b00101100 },
    Code { 44, 8, 0b00101101 },
    Code { 45, 8, 0b00000100 },
    Code { 46, 8, 0b00000101 },
    Code { 47, 8, 0b00001010 },
    Code { 48, 8, 0b00001011 },
    Code { 49, 8, 0b01010010 },
    Code { 50, 8, 0b01010011 },
    Code { 51, 8, 0b01010100 },
    Code { 52, 8, 0b01010101 },
    Code { 53, 8, 0b00100100 },
    Code { 54, 8, 0b00100101 },
    Code { 55, 8, 0b01011000 },
    Code { 56, 8, 0b01011001 },
    Code { 57, 8, 0b01011010 },
    Code { 58, 8, 0b01011011 },
    Code { 59, 8, 0b01001010 },
    Code { 60, 8, 0b01001011 },
    Code { 61, 8, 0b00110010 },
    Code { 62, 8, 0b00110011 },
    Code { 63, 8, 0b00110100 },
};

// Table 2/T.4 – Terminating codes
constexpr Array black_terminating_codes = {
    Code { 0, 10, 0b0000110111 },
    Code { 1, 3, 0b010 },
    Code { 2, 2, 0b11 },
    Code { 3, 2, 0b10 },
    Code { 4, 3, 0b011 },
    Code { 5, 4, 0b0011 },
    Code { 6, 4, 0b0010 },
    Code { 7, 5, 0b00011 },
    Code { 8, 6, 0b000101 },
    Code { 9, 6, 0b000100 },
    Code { 10, 7, 0b0000100 },
    Code { 11, 7, 0b0000101 },
    Code { 12, 7, 0b0000111 },
    Code { 13, 8, 0b00000100 },
    Code { 14, 8, 0b00000111 },
    Code { 15, 9, 0b000011000 },
    Code { 16, 10, 0b0000010111 },
    Code { 17, 10, 0b0000011000 },
    Code { 18, 10, 0b0000001000 },
    Code { 19, 11, 0b00001100111 },
    Code { 20, 11, 0b00001101000 },
    Code { 21, 11, 0b00001101100 },
    Code { 22, 11, 0b00000110111 },
    Code { 23, 11, 0b00000101000 },
    Code { 24, 11, 0b00000010111 },
    Code { 25, 11, 0b00000011000 },
    Code { 26, 12, 0b000011001010 },
    Code { 27, 12, 0b000011001011 },
    Code { 28, 12, 0b000011001100 },
    Code { 29, 12, 0b000011001101 },
    Code { 30, 12, 0b000001101000 },
    Code { 31, 12, 0b000001101001 },
    Code { 32, 12, 0b000001101010 },
    Code { 33, 12, 0b000001101011 },
    Code { 34, 12, 0b000011010010 },
    Code { 35, 12, 0b000011010011 },
    Code { 36, 12, 0b000011010100 },
    Code { 37, 12, 0b000011010101 },
    Code { 38, 12, 0b000011010110 },
    Code { 39, 12, 0b000011010111 },
    Code { 40, 12, 0b000001101100 },
    Code { 41, 12, 0b000001101101 },
    Code { 42, 12, 0b000011011010 },
    Code { 43, 12, 0b000011011011 },
    Code { 44, 12, 0b000001010100 },
    Code { 45, 12, 0b000001010101 },
    Code { 46, 12, 0b000001010110 },
    Code { 47, 12, 0b000001010111 },
    Code { 48, 12, 0b000001100100 },
    Code { 49, 12, 0b000001100101 },
    Code { 50, 12, 0b000001010010 },
    Code { 51, 12, 0b000001010011 },
    Code { 52, 12, 0b000000100100 },
    Code { 53, 12, 0b000000110111 },
    Code { 54, 12, 0b000000111000 },
    Code { 55, 12, 0b000000100111 },
    Code { 56, 12, 0b000000101000 },
    Code { 57, 12, 0b000001011000 },
    Code { 58, 12, 0b000001011001 },
    Code { 59, 12, 0b000000101011 },
    Code { 60, 12, 0b000000101100 },
    Code { 61, 12, 0b000001011010 },
    Code { 62, 12, 0b000001100110 },
    Code { 63, 12, 0b000001100111 },
};

// Table 3a/T.4 – Make-up codes
constexpr Array white_make_up_codes = {
    Code { 64, 5, 0b11011 },
    Code { 128, 5, 0b10010 },
    Code { 192, 6, 0b010111 },
    Code { 256, 7, 0b0110111 },
    Code { 320, 8, 0b00110110 },
    Code { 384, 8, 0b00110111 },
    Code { 448, 8, 0b01100100 },
    Code { 512, 8, 0b01100101 },
    Code { 576, 8, 0b01101000 },
    Code { 640, 8, 0b01100111 },
    Code { 704, 9, 0b011001100 },
    Code { 768, 9, 0b011001101 },
    Code { 832, 9, 0b011010010 },
    Code { 896, 9, 0b011010011 },
    Code { 960, 9, 0b011010100 },
    Code { 1024, 9, 0b011010101 },
    Code { 1088, 9, 0b011010110 },
    Code { 1152, 9, 0b011010111 },
    Code { 1216, 9, 0b011011000 },
    Code { 1280, 9, 0b011011001 },
    Code { 1344, 9, 0b011011010 },
    Code { 1408, 9, 0b011011011 },
    Code { 1472, 9, 0b010011000 },
    Code { 1536, 9, 0b010011001 },
    Code { 1600, 9, 0b010011010 },
    Code { 1664, 6, 0b011000 },
    Code { 1728, 9, 0b010011011 },
    Code { 0xE0L, 12, 0b000000000001 },
};

// Table 3a/T.4 – Make-up codes
constexpr Array black_make_up_codes = {
    Code { 64, 10, 0b0000001111 },
    Code { 128, 12, 0b000011001000 },
    Code { 192, 12, 0b000011001001 },
    Code { 256, 12, 0b000001011011 },
    Code { 320, 12, 0b000000110011 },
    Code { 384, 12, 0b000000110100 },
    Code { 448, 12, 0b000000110101 },
    Code { 512, 13, 0b0000001101100 },
    Code { 576, 13, 0b0000001101101 },
    Code { 640, 13, 0b0000001001010 },
    Code { 704, 13, 0b0000001001011 },
    Code { 768, 13, 0b0000001001100 },
    Code { 832, 13, 0b0000001001101 },
    Code { 896, 13, 0b0000001110010 },
    Code { 960, 13, 0b0000001110011 },
    Code { 1024, 13, 0b0000001110100 },
    Code { 1088, 13, 0b0000001110101 },
    Code { 1152, 13, 0b0000001110110 },
    Code { 1216, 13, 0b0000001110111 },
    Code { 1280, 13, 0b0000001010010 },
    Code { 1344, 13, 0b0000001010011 },
    Code { 1408, 13, 0b0000001010100 },
    Code { 1472, 13, 0b0000001010101 },
    Code { 1536, 13, 0b0000001011010 },
    Code { 1600, 13, 0b0000001011011 },
    Code { 1664, 13, 0b0000001100100 },
    Code { 1728, 13, 0b0000001100101 },
    Code { 0xE0L, 11, 0b00000000000 },
};

// Table 3b/T.4 – Make-up codes
constexpr Array common_make_up_codes = {
    Code { 1792, 11, 0b00000001000 },
    Code { 1856, 11, 0b00000001100 },
    Code { 1920, 11, 0b00000001101 },
    Code { 1984, 12, 0b000000010010 },
    Code { 2048, 12, 0b000000010011 },
    Code { 2112, 12, 0b000000010100 },
    Code { 2176, 12, 0b000000010101 },
    Code { 2240, 12, 0b000000010110 },
    Code { 2304, 12, 0b000000010111 },
    Code { 2368, 12, 0b000000011100 },
    Code { 2432, 12, 0b000000011101 },
    Code { 2496, 12, 0b000000011110 },
    Code { 2560, 12, 0b000000011111 },
};

template<size_t Size>
Optional<Code> get_code_from_table(Array<Code, Size> const& array, u16 code_word, u8 code_size)
{
    for (auto const& code : array) {
        if (code.code_length == code_size && code.code == code_word)
            return code;
    }
    return OptionalNone {};
}

Optional<Code> get_markup_code(Color color, u16 code_word, u8 code_size)
{
    if (auto maybe_value = get_code_from_table(common_make_up_codes, code_word, code_size); maybe_value.has_value())
        return maybe_value.value();

    if (color == Color::NamedColor::White)
        return get_code_from_table(white_make_up_codes, code_word, code_size);
    return get_code_from_table(black_make_up_codes, code_word, code_size);
}

Optional<Code> get_terminal_code(Color color, u16 code_word, u8 code_size)
{
    if (color == Color::NamedColor::White)
        return get_code_from_table(white_terminating_codes, code_word, code_size);
    return get_code_from_table(black_terminating_codes, code_word, code_size);
}

constexpr auto const ccitt_white = Color::NamedColor::White;
constexpr auto const ccitt_black = Color::NamedColor::Black;

ErrorOr<void> decode_single_ccitt3_1d_line(BigEndianInputBitStream& input_bit_stream, BigEndianOutputBitStream& decoded_bits, u32 image_width)
{
    // We always flip the color when entering the loop, so let's initialize the
    // color with black to make the first marker actually be white.
    Color current_color { ccitt_black };
    u32 run_length = 0;
    u32 column = 0;

    while (column < image_width) {
        if (run_length > 0) {
            run_length--;
            TRY(decoded_bits.write_bits(current_color == ccitt_white ? 0u : 1u, 1));

            ++column;
            continue;
        }

        current_color = current_color == ccitt_white ? ccitt_black : ccitt_white;

        u8 size {};
        u16 potential_code {};
        while (size < 14) {
            potential_code <<= 1;
            potential_code |= TRY(input_bit_stream.read_bit());
            size++;

            if (auto const maybe_markup = get_markup_code(current_color, potential_code, size); maybe_markup.has_value()) {
                run_length += maybe_markup->run_length;
                // OK, let's reset the loop to read a terminal code now
                size = 0;
                potential_code = 0;
            } else if (auto const maybe_terminal = get_terminal_code(current_color, potential_code, size); maybe_terminal.has_value()) {
                run_length += maybe_terminal->run_length;
                break;
            }
        }

        if (size == 14)
            return Error::from_string_literal("TIFFImageDecoderPlugin: Invalid CCITT code");

        if (column + run_length > image_width)
            return Error::from_string_literal("TIFFImageDecoderPlugin: CCITT codes encode for more that a line");
    }

    TRY(decoded_bits.align_to_byte_boundary());

    return {};
}

static ErrorOr<void> read_eol(BigEndianInputBitStream& bit_stream, Group3Options::UseFillBits use_fill_bits)
{
    constexpr u16 EOL = 0b0000'0000'0001;

    if (use_fill_bits == Group3Options::UseFillBits::Yes) {
        // TIFF specification, description of the T4Options tag:
        // "Fill bits have been added as necessary before EOL codes such that
        // EOL always ends on a byte boundary, thus ensuring an EOL-sequence of 1 byte
        // preceded by a zero nibble: xxxx-0000 0000-0001."
        auto const to_skip = (12 + bit_stream.bits_until_next_byte_boundary()) % 8;
        TRY(bit_stream.read_bits(to_skip));
    }

    auto const read = TRY(bit_stream.read_bits<u16>(12));
    if (read != EOL)
        return Error::from_string_literal("CCITTDecoder: Invalid EndOfLine code");

    return {};
}

}

ErrorOr<ByteBuffer> decode_ccitt_rle(ReadonlyBytes bytes, u32 image_width, u32 image_height)
{
    auto strip_stream = make<FixedMemoryStream>(bytes);
    auto bit_stream = make<BigEndianInputBitStream>(MaybeOwned<Stream>(*strip_stream));

    // Note: We put image_height extra-space to handle at most one alignment to byte boundary per line.
    ByteBuffer decoded_bytes = TRY(ByteBuffer::create_zeroed(ceil_div(image_width * image_height, 8) + image_height));
    auto output_stream = make<FixedMemoryStream>(decoded_bytes.bytes());
    auto decoded_bits = make<BigEndianOutputBitStream>(MaybeOwned<Stream>(*output_stream));

    while (!bit_stream->is_eof()) {
        TRY(decode_single_ccitt3_1d_line(*bit_stream, *decoded_bits, image_width));

        bit_stream->align_to_byte_boundary();
    }

    return decoded_bytes;
}

ErrorOr<ByteBuffer> decode_ccitt_group3(ReadonlyBytes bytes, u32 image_width, u32 image_height, Group3Options const& options)
{
    auto strip_stream = make<FixedMemoryStream>(bytes);
    auto bit_stream = make<BigEndianInputBitStream>(MaybeOwned<Stream>(*strip_stream));

    // Note: We put image_height extra-space to handle at most one alignment to byte boundary per line.
    ByteBuffer decoded_bytes = TRY(ByteBuffer::create_zeroed(ceil_div(image_width * image_height, 8) + image_height));
    auto output_stream = make<FixedMemoryStream>(decoded_bytes.bytes());
    auto decoded_bits = make<BigEndianOutputBitStream>(MaybeOwned<Stream>(*output_stream));

    if (options.dimensions == Group3Options::Mode::OneDimension) {
        // 4.1.2 End-of-line (EOL)
        // This code word follows each line of data. It is a unique code word that can never be found within a
        // valid line of data; therefore, resynchronization after an error burst is possible.
        // In addition, this signal will occur prior to the first data line of a page.
        // ---
        // NOTE: For whatever reason, the last EOL doesn't seem to be included

        for (u32 i = 0; i < image_height; ++i) {
            TRY(read_eol(*bit_stream, options.use_fill_bits));
            TRY(decode_single_ccitt3_1d_line(*bit_stream, *decoded_bits, image_width));
        }

        return decoded_bytes;
    }

    return Error::from_string_literal("CCITT3 2D is not implemented yet :^(");
}

}
