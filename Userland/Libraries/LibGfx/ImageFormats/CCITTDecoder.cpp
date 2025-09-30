/*
 * Copyright (c) 2023, Lucas Chollet <lucas.chollet@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Array.h>
#include <AK/BitStream.h>
#include <AK/MemoryStream.h>
#include <LibGfx/Color.h>
#include <LibGfx/ImageFormats/CCITTCommon.h>
#include <LibGfx/ImageFormats/CCITTDecoder.h>

namespace Gfx::CCITT {

namespace {

template<typename T, size_t Size>
Optional<T> get_code_from_table(Array<T, Size> const& array, u16 code_word, u8 code_size)
{
    // FIXME: Use an approach that doesn't require a full scan for every bit. See Compress::CanonicalCodes.
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

enum class ShouldAcceptEOL : u8 {
    No = 0,
    Yes = 1,
};

ErrorOr<u32> read_run_length(BigEndianInputBitStream& input_bit_stream, Optional<ReferenceLine&> reference_line, Color current_color, u32 image_width, u32 column, ShouldAcceptEOL should_accept_eol = ShouldAcceptEOL::No)
{
    u8 size {};
    u16 potential_code {};
    u32 run_length {};
    while (size < 14) {
        potential_code <<= 1;
        potential_code |= TRY(input_bit_stream.read_bit());
        size++;

        if (auto const maybe_markup = get_markup_code(current_color, potential_code, size); maybe_markup.has_value()) {
            if (*maybe_markup == EOL) {
                if (should_accept_eol == ShouldAcceptEOL::No || column != 0 || run_length != 0)
                    return Error::from_string_literal("CCITTDecoder: Invalid EndOfLine code");
                should_accept_eol = ShouldAcceptEOL::No;
            }

            run_length += maybe_markup->run_length;
            // Let's reset the loop to read a new code.
            size = 0;
            potential_code = 0;
        } else if (auto const maybe_terminal = get_terminal_code(current_color, potential_code, size); maybe_terminal.has_value()) {
            run_length += maybe_terminal->run_length;
            if (reference_line.has_value())
                TRY(reference_line->try_append({ invert(current_color), column + run_length }));
            break;
        }
    }

    if (size == 14)
        return Error::from_string_literal("TIFFImageDecoderPlugin: Invalid CCITT code");

    if (column + run_length > image_width)
        return Error::from_string_literal("TIFFImageDecoderPlugin: CCITT codes encode for more than a line");

    return run_length;
}

ErrorOr<ReferenceLine> decode_single_ccitt3_1d_line(BigEndianInputBitStream& input_bit_stream, BigEndianOutputBitStream& decoded_bits, u32 image_width, ShouldAcceptEOL should_accept_eol)
{
    // This is only useful for the 2D decoder.
    ReferenceLine reference_line;

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

        current_color = invert(current_color);

        run_length += TRY(read_run_length(input_bit_stream, reference_line, current_color, image_width, column, should_accept_eol));
    }

    TRY(decoded_bits.align_to_byte_boundary());

    return reference_line;
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

using InvalidResult = u8;

ErrorOr<Variant<ModeCode, InvalidResult>> read_mode(BigEndianInputBitStream& input_bit_stream)
{
    u8 size {};
    u8 potential_code {};
    while (size < 7) {
        potential_code <<= 1;
        potential_code |= TRY(input_bit_stream.read_bit());
        ++size;

        if (auto const maybe_mode = get_code_from_table(mode_codes, potential_code, size); maybe_mode.has_value())
            return *maybe_mode;
    }

    return Variant<ModeCode, InvalidResult>(InvalidResult { potential_code });
}

enum class Search : u8 {
    B1,
    B2,
};

struct CCITTStatus {
    ReferenceLine current_line {};
    bool has_reached_eol { false };
};

ErrorOr<void> ensure_invalid_result_is_actually_eol(BigEndianInputBitStream& input_bit_stream, InvalidResult partially_read_eol, Group4Options const& options)
{
    if (partially_read_eol != 0)
        return Error::from_string_literal("CCITTDecoder: Unable to find the correct mode");

    auto const remaining_eol = TRY(input_bit_stream.read_bits(5));
    if (options.has_end_of_block == Group4Options::HasEndOfBlock::Yes
        && remaining_eol == 0) {
        // Some PDF like 00000337.pdf ends with an EOFB [1] that is byte aligned. This is
        // what we are trying to detect/read here. As we already read 12 bytes from
        // partially_read_eol and remaining_eol, we need to realign ourselves first.
        // [1] 2.4.1.1 End-of-facsimile block

        u8 fill_bits_length = (12 + input_bit_stream.bits_until_next_byte_boundary()) % 8;
        u8 to_read = fill_bits_length + 12;
        auto potential_eofb = TRY(input_bit_stream.read_bits(to_read));

        // We already checked that the 12 first bits were zeroes, so here we check that the
        // last to_read bits end with EOFB.
        if (potential_eofb != EOFB)
            return Error::from_string_literal("CCITTDecoder: Unable to find the correct mode");
    } else if (remaining_eol != 1) {
        return Error::from_string_literal("CCITTDecoder: Unable to find the correct mode");
    }

    return {};
}

ErrorOr<CCITTStatus> decode_single_ccitt_2d_line(
    BigEndianInputBitStream& input_bit_stream,
    BigEndianOutputBitStream& decoded_bits,
    ReferenceLine&& reference_line,
    u32 image_width,
    Group4Options const& options = {})
{
    CCITTStatus status {};

    // status.current_line stores the color changes in the line. In the worst case scenario,
    // the image is a checkerboard and there is a color change at every pixel (+1 for the
    // right edge), so let's pre-allocate for this scenario.
    TRY(status.current_line.try_ensure_capacity(image_width + 1));

    Color current_color { ccitt_white };
    u32 column {};
    u32 remainder_from_pass_mode {};

    auto reference_line_span = reference_line.span();
    auto const next_change_on_reference_line = [&]() -> ErrorOr<Change> {
        // 4.2.1.3.1 Definition of changing picture elements
        Optional<Change> next_change {}; // This is referred to as b1 in the spec.
        u32 offset {};
        while (!next_change.has_value()) {
            if (reference_line_span.is_empty() || reference_line_span.size() <= offset)
                return Error::from_string_literal("CCITTDecoder: Corrupted stream");
            auto const change = reference_line_span[0 + offset];
            // 4.2.1.3.4 Processing the first and last picture elements in a line
            // "The first starting picture element a0 on each coding line is imaginarily set at a position just
            // before the first picture element, and is regarded as a white picture element."
            // To emulate this behavior we check for column == 0 here.
            if (change.column <= column && column != 0) {
                reference_line_span = reference_line_span.slice(1);
                continue;
            }
            if (change.color != current_color || change.column == image_width)
                next_change = change;
            else
                offset++;
        }
        return *next_change;
    };

    auto const encode_for = [&](Change change, i8 offset = 0) -> ErrorOr<void> {
        i32 const to_encode = remainder_from_pass_mode + change.column - column + offset;
        if (to_encode < 0)
            return Error::from_string_literal("CCITTDecoder: Corrupted stream");
        for (i32 i {}; i < to_encode; ++i)
            TRY(decoded_bits.write_bits(current_color == ccitt_white ? 0u : 1u, 1));

        column = change.column + offset;
        current_color = invert(current_color);
        remainder_from_pass_mode = 0;

        status.current_line.unchecked_append({ current_color, column });
        return {};
    };

    while (column < image_width) {
        auto const maybe_mode = TRY(read_mode(input_bit_stream));

        if (maybe_mode.has<InvalidResult>()) {
            TRY(ensure_invalid_result_is_actually_eol(input_bit_stream, maybe_mode.get<InvalidResult>(), options));

            // We reached EOL
            status.has_reached_eol = true;
            break;
        }

        // Behavior are described here 4.2.1.3.2 Coding modes.
        switch (maybe_mode.get<ModeCode>().mode) {
        case Mode::Pass: {
            auto const column_before = column;
            // We search for b1.
            auto change = TRY(next_change_on_reference_line());
            current_color = change.color;
            column = change.column;

            // We search for b2, which is the same as searching for b1 after updating the state.
            change = TRY(next_change_on_reference_line());
            current_color = change.color;
            column = change.column;

            remainder_from_pass_mode += column - column_before;
            break;
        }
        case Mode::Horizontal: {
            // a0a1
            auto run_length = TRY(read_run_length(input_bit_stream, OptionalNone {}, current_color, image_width, column));
            TRY(encode_for({ invert(current_color), column + run_length }));

            // a1a2
            run_length = TRY(read_run_length(input_bit_stream, OptionalNone {}, current_color, image_width, column));
            TRY(encode_for({ invert(current_color), column + run_length }));
            break;
        }
        case Mode::Vertical_0:
            TRY(encode_for(TRY(next_change_on_reference_line())));
            break;
        case Mode::Vertical_R1:
            TRY(encode_for(TRY(next_change_on_reference_line()), 1));
            break;
        case Mode::Vertical_R2:
            TRY(encode_for(TRY(next_change_on_reference_line()), 2));
            break;
        case Mode::Vertical_R3:
            TRY(encode_for(TRY(next_change_on_reference_line()), 3));
            break;
        case Mode::Vertical_L1:
            TRY(encode_for(TRY(next_change_on_reference_line()), -1));
            break;
        case Mode::Vertical_L2:
            TRY(encode_for(TRY(next_change_on_reference_line()), -2));
            break;
        case Mode::Vertical_L3:
            TRY(encode_for(TRY(next_change_on_reference_line()), -3));
            break;
        default:
            return Error::from_string_literal("CCITTDecoder: Unsupported mode for 2D decoding");
        }
    }

    TRY(decoded_bits.align_to_byte_boundary());

    return status;
}

ErrorOr<void> decode_single_ccitt3_2d_block(BigEndianInputBitStream& input_bit_stream, BigEndianOutputBitStream& decoded_bits, u32 image_width, u32 image_height, Group3Options::UseFillBits use_fill_bits)
{
    ReferenceLine reference_line;
    for (u32 i = 0; i < image_height; ++i) {
        TRY(read_eol(input_bit_stream, use_fill_bits));
        bool const next_is_1D = TRY(input_bit_stream.read_bit()) == 1;

        if (next_is_1D)
            reference_line = TRY(decode_single_ccitt3_1d_line(input_bit_stream, decoded_bits, image_width, ShouldAcceptEOL::No));
        else
            reference_line = TRY(decode_single_ccitt_2d_line(input_bit_stream, decoded_bits, move(reference_line), image_width)).current_line;
    }

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
        TRY(decode_single_ccitt3_1d_line(*bit_stream, *decoded_bits, image_width, ShouldAcceptEOL::No));

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

        bool const require_end_of_line = options.require_end_of_line == Group3Options::RequireEndOfLine::Yes;

        for (u32 i = 0; i < image_height; ++i) {
            if (require_end_of_line)
                TRY(read_eol(*bit_stream, options.use_fill_bits));
            TRY(decode_single_ccitt3_1d_line(*bit_stream, *decoded_bits, image_width, require_end_of_line ? ShouldAcceptEOL::No : ShouldAcceptEOL::Yes));
            if (options.encoded_byte_aligned == EncodedByteAligned::Yes)
                bit_stream->align_to_byte_boundary();
        }

        return decoded_bytes;
    }

    if (options.require_end_of_line == Group3Options::RequireEndOfLine::No || options.encoded_byte_aligned == EncodedByteAligned::Yes)
        return Error::from_string_literal("CCITTDecoder: Unsupported option for CCITT3 2D decoding");

    TRY(decode_single_ccitt3_2d_block(*bit_stream, *decoded_bits, image_width, image_height, options.use_fill_bits));
    return decoded_bytes;
}

ErrorOr<ByteBuffer> decode_ccitt_group4(ReadonlyBytes bytes, u32 image_width, u32 image_height, Group4Options const& options)
{
    auto stream = make<FixedMemoryStream>(bytes);
    return decode_ccitt_group4(*stream, image_width, image_height, options);
}

ErrorOr<ByteBuffer> decode_ccitt_group4(Stream& stream, u32 image_width, u32 image_height, Group4Options const& options)
{
    auto bit_stream = make<BigEndianInputBitStream>(MaybeOwned<Stream>(stream));

    auto output_stream = make<AllocatingMemoryStream>();
    auto decoded_bits = make<BigEndianOutputBitStream>(MaybeOwned<Stream>(*output_stream));

    // T.6 2.2.1 Principle of the coding scheme
    // The reference line for the first coding line in a page is an imaginary white line.
    CCITTStatus status;
    TRY(status.current_line.try_empend(ccitt_black, image_width));

    for (u32 i = 0; !status.has_reached_eol && (image_height == 0 || i < image_height); ++i) {
        status = TRY(decode_single_ccitt_2d_line(*bit_stream, *decoded_bits, move(status.current_line), image_width, options));
        if (options.encoded_byte_aligned == EncodedByteAligned::Yes)
            bit_stream->align_to_byte_boundary();
    }

    if (!status.has_reached_eol && options.has_end_of_block == Group4Options::HasEndOfBlock::Yes) {
        auto potential_eofb = TRY(bit_stream->read_bits(24));
        if (potential_eofb != EOFB)
            return Error::from_string_literal("CCITTDecoder: Missing EOFB");
    }

    return output_stream->read_until_eof();
}

}
