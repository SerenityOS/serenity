/*
 * Copyright (c) 2020, Hüseyin Aslıtürk <asliturk@hotmail.com>
 * Copyright (c) 2020-2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Debug.h>
#include <AK/DeprecatedString.h>
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

static inline ErrorOr<u16> read_number(SeekableStream& stream)
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

    auto const maybe_value = TRY(sb.to_string()).to_number<u16>();
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
static bool read_magic_number(TContext& context)
{
    if (context.state >= TContext::State::MagicNumber) {
        return true;
    }

    if (context.stream->size().release_value_but_fixme_should_propagate_errors() < 2) {
        context.state = TContext::State::Error;
        dbgln_if(PORTABLE_IMAGE_LOADER_DEBUG, "There is no enough data for {}", TContext::FormatDetails::image_type);
        return false;
    }

    Array<u8, 2> magic_number {};
    if (context.stream->read_until_filled(Bytes { magic_number }).is_error()) {
        context.state = TContext::State::Error;
        dbgln_if(PORTABLE_IMAGE_LOADER_DEBUG, "We can't read magic number for {}", TContext::FormatDetails::image_type);
        return false;
    }

    if (magic_number[0] == 'P' && magic_number[1] == TContext::FormatDetails::ascii_magic_number) {
        context.type = TContext::Type::ASCII;
        context.state = TContext::State::MagicNumber;
        return true;
    }

    if (magic_number[0] == 'P' && magic_number[1] == TContext::FormatDetails::binary_magic_number) {
        context.type = TContext::Type::RAWBITS;
        context.state = TContext::State::MagicNumber;
        return true;
    }

    context.state = TContext::State::Error;
    dbgln_if(PORTABLE_IMAGE_LOADER_DEBUG, "Magic number is not valid for {}{}{}", magic_number[0], magic_number[1], TContext::FormatDetails::image_type);
    return false;
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
            stream.seek(-1, SeekMode::FromCurrentPosition).release_value_but_fixme_should_propagate_errors();
            TRY(read_comment(context));
            continue;
        }
        if (byte != ' ' && byte != '\t' && byte != '\n' && byte != '\r') {
            stream.seek(-1, SeekMode::FromCurrentPosition).release_value_but_fixme_should_propagate_errors();
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
    context.state = TContext::State::Width;
    return {};
}

template<typename TContext>
static ErrorOr<void> read_height(TContext& context)
{
    context.height = TRY(read_number(*context.stream));
    context.state = TContext::State::Height;
    return {};
}

template<typename TContext>
static ErrorOr<void> read_max_val(TContext& context)
{
    context.format_details.max_val = TRY(read_number(*context.stream));

    if (context.format_details.max_val > 255) {
        dbgln_if(PORTABLE_IMAGE_LOADER_DEBUG, "We can't parse 2 byte color for {}", TContext::FormatDetails::image_type);
        context.state = TContext::State::Error;
        return Error::from_string_literal("Can't parse 2 byte color");
    }

    context.state = TContext::State::Maxval;
    return {};
}

template<typename TContext>
static bool create_bitmap(TContext& context)
{
    auto bitmap_or_error = Bitmap::create(BitmapFormat::BGRx8888, { context.width, context.height });
    if (bitmap_or_error.is_error()) {
        context.state = TContext::State::Error;
        return false;
    }
    context.bitmap = bitmap_or_error.release_value_but_fixme_should_propagate_errors();
    return true;
}

template<typename TContext>
static void set_pixels(TContext& context, Vector<Gfx::Color> const& color_data)
{
    size_t index = 0;
    for (size_t y = 0; y < context.height; ++y) {
        for (size_t x = 0; x < context.width; ++x) {
            context.bitmap->set_pixel(x, y, color_data.at(index));
            index++;
        }
    }
}

template<typename TContext>
static bool decode(TContext& context)
{
    if (context.state >= TContext::State::Decoded)
        return true;

    auto error_guard = ArmedScopeGuard([&] {
        context.state = TContext::State::Error;
    });

    if (!read_magic_number(context))
        return false;

    if (read_whitespace(context).is_error())
        return false;

    if (read_width(context).is_error())
        return false;

    if (read_whitespace(context).is_error())
        return false;

    if (read_height(context).is_error())
        return false;

    if (context.width > maximum_width_for_decoded_images || context.height > maximum_height_for_decoded_images) {
        dbgln("This portable network image is too large for comfort: {}x{}", context.width, context.height);
        return false;
    }

    if (read_whitespace(context).is_error())
        return false;

    if constexpr (requires { context.format_details.max_val; }) {
        if (read_max_val(context).is_error())
            return false;

        if (read_whitespace(context).is_error())
            return false;
    }

    if (!read_image_data(context))
        return false;

    error_guard.disarm();
    context.state = TContext::State::Decoded;
    return true;
}

}
