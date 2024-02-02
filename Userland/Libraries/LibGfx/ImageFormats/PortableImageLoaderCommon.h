/*
 * Copyright (c) 2020, Hüseyin Aslıtürk <asliturk@hotmail.com>
 * Copyright (c) 2020-2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Debug.h>
#include <AK/Endian.h>
#include <AK/ScopeGuard.h>
#include <AK/String.h>
#include <AK/StringBuilder.h>
#include <AK/Types.h>
#include <AK/Vector.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/Color.h>
#include <LibGfx/ImageFormats/ImageDecoder.h>

namespace Gfx {

static constexpr Color adjust_color(u16 max_val, Color color)
{
    color.set_red((color.red() * 255) / max_val);
    color.set_green((color.green() * 255) / max_val);
    color.set_blue((color.blue() * 255) / max_val);

    return color;
}

inline ErrorOr<String> read_token(SeekableStream& stream)
{
    StringBuilder sb {};
    u8 byte {};

    for (auto buffer = TRY(stream.read_some({ &byte, 1 })); !buffer.is_empty(); buffer = TRY(stream.read_some({ &byte, 1 }))) {
        if (byte == ' ' || byte == '\t' || byte == '\n' || byte == '\r') {
            TRY(stream.seek(-1, SeekMode::FromCurrentPosition));
            break;
        }

        sb.append(byte);
    }

    return TRY(sb.to_string());
}

static inline ErrorOr<u16> read_number(SeekableStream& stream)
{
    auto const maybe_value = TRY(read_token(stream)).to_number<u16>();
    if (!maybe_value.has_value())
        return Error::from_string_literal("Can't convert bytes to a number");

    return *maybe_value;
}

template<typename TContext>
static ErrorOr<void> read_comment(TContext& context)
{
    auto& stream = *context.stream;
    bool is_first_char = true;
    u8 byte {};

    while ((byte = TRY(stream.template read_value<u8>()))) {
        if (is_first_char) {
            if (byte != '#')
                return Error::from_string_literal("Can't read comment from stream");
            is_first_char = false;
        } else if (byte == '\t' || byte == '\n') {
            break;
        }
    }

    return {};
}

template<typename TContext>
static ErrorOr<void> read_magic_number(TContext& context)
{
    if (TRY(context.stream->size()) < 2) {
        dbgln_if(PORTABLE_IMAGE_LOADER_DEBUG, "There is no enough data for {}", TContext::FormatDetails::image_type);
        return Error::from_string_literal("There is no enough data to read magic number.");
    }

    Array<u8, 2> magic_number {};
    TRY(context.stream->read_until_filled(Bytes { magic_number }));

    if constexpr (requires { TContext::FormatDetails::ascii_magic_number; }) {
        if (magic_number[0] == 'P' && magic_number[1] == TContext::FormatDetails::ascii_magic_number) {
            context.type = TContext::Type::ASCII;
            return {};
        }
    }

    if (magic_number[0] == 'P' && magic_number[1] == TContext::FormatDetails::binary_magic_number) {
        context.type = TContext::Type::RAWBITS;
        return {};
    }

    dbgln_if(PORTABLE_IMAGE_LOADER_DEBUG, "Magic number is not valid for {}{}{}", magic_number[0], magic_number[1], TContext::FormatDetails::image_type);
    return Error::from_string_literal("Unable to recognize magic bytes");
}

template<typename TContext>
static ErrorOr<void> read_whitespace(TContext& context)
{
    auto& stream = *context.stream;
    bool is_first_char = true;

    while (true) {
        auto byte_or_error = stream.template read_value<u8>();
        // Nothing went wrong if we reached eof while reading a comment.
        if (byte_or_error.is_error())
            return {};
        auto const byte = byte_or_error.value();

        if (byte == '#') {
            TRY(stream.seek(-1, SeekMode::FromCurrentPosition));
            TRY(read_comment(context));
            continue;
        }
        if (byte != ' ' && byte != '\t' && byte != '\n' && byte != '\r') {
            TRY(stream.seek(-1, SeekMode::FromCurrentPosition));
            if (is_first_char)
                return Error::from_string_literal("Can't read whitespace from stream");
            break;
        }

        if (is_first_char)
            is_first_char = false;
    }

    return {};
}

template<typename TContext>
static ErrorOr<void> read_width(TContext& context)
{
    context.width = TRY(read_number(*context.stream));
    return {};
}

template<typename TContext>
static ErrorOr<void> read_height(TContext& context)
{
    context.height = TRY(read_number(*context.stream));
    return {};
}

template<typename TContext>
static ErrorOr<void> read_max_val(TContext& context)
{
    context.format_details.max_val = TRY(read_number(*context.stream));

    if (context.format_details.max_val == 0)
        return Error::from_string_literal("The image has a maximum value of 0");

    if (context.format_details.max_val > 255) {
        dbgln_if(PORTABLE_IMAGE_LOADER_DEBUG, "We can't parse 2 byte color for {}", TContext::FormatDetails::image_type);
        return Error::from_string_literal("Can't parse 2 byte color");
    }

    return {};
}

template<typename TContext>
static ErrorOr<void> create_bitmap(TContext& context)
{
    context.bitmap = TRY(Bitmap::create(BitmapFormat::BGRx8888, { context.width, context.height }));
    return {};
}

template<typename Context>
static ErrorOr<void> read_header(Context& context)
{
    TRY(read_magic_number(context));

    TRY(read_whitespace(context));

    TRY(read_width(context));
    TRY(read_whitespace(context));
    TRY(read_height(context));

    TRY(read_whitespace(context));

    if constexpr (requires { context.format_details.max_val; }) {
        TRY(read_max_val(context));
        TRY(read_whitespace(context));
    }

    context.state = Context::State::HeaderDecoded;

    return {};
}

template<typename Context>
static ErrorOr<void> read_pam_header(Context& context);

template<typename TContext>
static ErrorOr<void> decode(TContext& context)
{
    VERIFY(context.state == TContext::State::HeaderDecoded);

    TRY(read_image_data(context));

    context.state = TContext::State::BitmapDecoded;
    return {};
}

}
