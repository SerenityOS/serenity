/*
 * Copyright (c) 2025, Lucas Chollet <lucas.chollet@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "CCITTEncoder.h"
#include <AK/BitStream.h>
#include <AK/Debug.h>
#include <AK/Math.h>
#include <LibGfx/ImageFormats/CCITTCommon.h>

namespace Gfx::CCITT {

// This file is using the T-REC-T.6 spec as a reference.
// See comments in CCITTDecoder.h for a spec link.

namespace {

ErrorOr<ReferenceLine> get_reference_line(Bitmap const& bitmap, u32 line)
{
    auto* scan_line = bitmap.scanline(line);
    ReferenceLine reference_line;

    Color last_color = ccitt_white;

    for (u32 x = 0; x < static_cast<u32>(bitmap.width()); ++x) {
        if (Color::from_argb(scan_line[x]) != last_color) {
            last_color = invert(last_color);
            TRY(reference_line.try_empend(last_color, x));
        }
    }

    TRY(reference_line.try_empend(invert(last_color), static_cast<u32>(bitmap.width())));
    TRY(reference_line.try_empend(invert(last_color), static_cast<u32>(bitmap.width())));

    return reference_line;
}

struct EncodingStatus {
    Optional<Change> a0;
    Change a1;
    Change b1;
    Optional<Change> b2;
};

void update_status(EncodingStatus& status, Span<Change>& last_line, Span<Change>& current_line)
{
    // a0 was already updated at the end of each encoding mode.
    // If this is the first call:
    // 2.2.5.1 Processing the first picture element
    // "The first starting picture element a0 on each coding line is imaginarily
    //  set at a position just before the first picture element, and is regarded
    //  as a white picture element (see § 2.2.2)."

    // Remove everything before a0.
    if (status.a0.has_value()) {
        while (current_line[0].column <= status.a0->column)
            current_line = current_line.slice(1);
        while (last_line[0].column <= status.a0->column)
            last_line = last_line.slice(1);
    }

    status.a1 = current_line[0];

    u32 b_offset = last_line[0].color == status.a0.map([](auto const& change) { return change.color; }).value_or(ccitt_white) ? 1 : 0;
    status.b1 = last_line[b_offset + 0];
    if (last_line.size() > b_offset + 1)
        status.b2 = last_line[b_offset + 1];
    else
        status.b2.clear();
}

ErrorOr<void> encode_mode(BigEndianOutputBitStream& bit_stream, Mode mode)
{
    auto mode_code = mode_codes[to_underlying(mode)];
    return TRY(bit_stream.write_bits(mode_code.code, mode_code.code_length));
}

ErrorOr<void> encode_pass_mode(BigEndianOutputBitStream& bit_stream, EncodingStatus& status, Span<Change> current_line)
{
    TRY(encode_mode(bit_stream, Mode::Pass));

    // "Put a0 just under b2."
    status.a0 = status.b2;
    // Note that with pass mode, the new a0 is not aligned with an actual change.
    // Thus, we need to retrieve the correct color.
    for (auto change : current_line) {
        if (change.column > status.a0->column) {
            status.a0->color = invert(change.color);
            break;
        }
    };

    dbg_if(CCITT_DEBUG, "pass {}, ", status.b2->column - (status.a0.has_value() ? status.a0->column : 0));
    return {};
}

ErrorOr<void> encode_vertical_mode(BigEndianOutputBitStream& bit_stream, EncodingStatus& status)
{
    int distance = status.a1.column - status.b1.column;

    switch (distance) {
    case 0:
        TRY(encode_mode(bit_stream, Mode::Vertical_0));
        break;
    case 1:
        TRY(encode_mode(bit_stream, Mode::Vertical_R1));
        break;
    case 2:
        TRY(encode_mode(bit_stream, Mode::Vertical_R2));
        break;
    case 3:
        TRY(encode_mode(bit_stream, Mode::Vertical_R3));
        break;
    case -1:
        TRY(encode_mode(bit_stream, Mode::Vertical_L1));
        break;
    case -2:
        TRY(encode_mode(bit_stream, Mode::Vertical_L2));
        break;
    case -3:
        TRY(encode_mode(bit_stream, Mode::Vertical_L3));
        break;
    default:
        VERIFY_NOT_REACHED();
    }

    // "Put a0 on a1."
    status.a0 = status.a1;

    dbg_if(CCITT_DEBUG, "vertical {},", distance);
    return {};
}

template<size_t Size1, size_t Size2>
ErrorOr<void> encode_from_arrays(BigEndianOutputBitStream& bit_stream, u32 length,
    Array<Code, Size1> make_up_codes, Array<Code, Size2> terminating_codes)
{
    // FIXME: Stop iterating the whole array for each code.
    for (auto code : common_make_up_codes.in_reverse()) {
        while (length >= code.run_length) {
            TRY(bit_stream.write_bits(code.code, code.code_length));
            length -= code.run_length;
        }
    }

    for (auto code : make_up_codes.in_reverse()) {
        if (length >= code.run_length && code != EOL) {
            TRY(bit_stream.write_bits(code.code, code.code_length));
            length -= code.run_length;
            break;
        }
    }

    for (auto code : terminating_codes.in_reverse()) {
        if (length >= code.run_length) {
            TRY(bit_stream.write_bits(code.code, code.code_length));
            length -= code.run_length;
            break;
        }
    }

    return {};
}

ErrorOr<void> encode_white_length(BigEndianOutputBitStream& bit_stream, u32 length)
{
    return encode_from_arrays(bit_stream, length, white_make_up_codes, white_terminating_codes);
}

ErrorOr<void> encode_black_length(BigEndianOutputBitStream& bit_stream, u32 length)
{
    return encode_from_arrays(bit_stream, length, black_make_up_codes, black_terminating_codes);
}

ErrorOr<void> encode_horizontal_mode(BigEndianOutputBitStream& bit_stream, EncodingStatus& status, Span<Change> current_line)
{
    // "Detect a2."
    auto const a2 = current_line[1];

    TRY(encode_mode(bit_stream, Mode::Horizontal));

    if (!status.a0.has_value() || status.a0->color == ccitt_white) {
        auto a0_column = status.a0.map([](auto const& change) { return change.column; }).value_or(0);
        TRY(encode_white_length(bit_stream, status.a1.column - a0_column));
        TRY(encode_black_length(bit_stream, a2.column - status.a1.column));
        dbg_if(CCITT_DEBUG, "horizontal white({}) black({}),", status.a1.column - a0_column, a2.column - status.a1.column);
    } else {
        TRY(encode_black_length(bit_stream, status.a1.column - status.a0->column));
        TRY(encode_white_length(bit_stream, a2.column - status.a1.column));
        dbg_if(CCITT_DEBUG, "horizontal black({}) white({}),", status.a1.column - status.a0->column, a2.column - status.a1.column);
    }

    // "Put a0 on a2."
    status.a0 = a2;
    return {};
}

}

ErrorOr<void> Group4Encoder::encode(Stream& stream, Bitmap const& bitmap, Group4EncodingOptions const& options)
{
    auto bit_stream = make<BigEndianOutputBitStream>(MaybeOwned<Stream>(stream));

    // 2.2.4 Coding procedure

    ReferenceLine last_line;
    // "White reference line."
    TRY(last_line.try_empend(ccitt_black, static_cast<u32>(bitmap.width())));
    TRY(last_line.try_empend(ccitt_white, static_cast<u32>(bitmap.width())));

    for (u32 y = 0; y < static_cast<u32>(bitmap.height()); ++y) {
        auto current_line = TRY(get_reference_line(bitmap, y));

        auto last_line_span = last_line.span();
        auto current_line_span = current_line.span();

        EncodingStatus status;

        dbg_if(CCITT_DEBUG, "Line {}:", y);

        while (true) {
            // "Detect a1, b1, b2."
            update_status(status, last_line_span, current_line_span);

            // "b2 to the left of a1."
            if (status.b2.has_value() && status.b2->column < status.a1.column) {
                // "Pass mode coding."
                TRY(encode_pass_mode(*bit_stream, status, current_line_span));
            } else {
                // "|a1b1| ≤ 3"
                if (abs(static_cast<int>(status.a1.column) - static_cast<int>(status.b1.column)) <= 3) {
                    // "Vertical mode coding."
                    TRY(encode_vertical_mode(*bit_stream, status));
                } else {
                    // "Horizontal mode coding."
                    TRY(encode_horizontal_mode(*bit_stream, status, current_line_span));
                }
            }

            // "End of line?"
            if (status.a0->column >= static_cast<u32>(bitmap.width()))
                break;
        }

        // "Reference line for next coding line."
        last_line = move(current_line);

        dbgln_if(CCITT_DEBUG, "");
    }

    // EOFB.
    if (options.append_eofb == Group4EncodingOptions::AppendEOFB::Yes)
        TRY(bit_stream->write_bits(EOFB, 24));

    // Pad bits.
    TRY(bit_stream->align_to_byte_boundary());

    return {};
}

}
