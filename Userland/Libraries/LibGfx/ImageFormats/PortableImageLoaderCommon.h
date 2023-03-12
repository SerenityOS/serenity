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
#include <LibGfx/Streamer.h>

namespace Gfx {

static constexpr Color adjust_color(u16 max_val, Color color)
{
    color.set_red((color.red() * 255) / max_val);
    color.set_green((color.green() * 255) / max_val);
    color.set_blue((color.blue() * 255) / max_val);

    return color;
}

static inline ErrorOr<u16> read_number(Streamer& streamer)
{
    u8 byte {};
    StringBuilder sb {};

    while (streamer.read(byte)) {
        if (byte == ' ' || byte == '\t' || byte == '\n' || byte == '\r') {
            streamer.step_back();
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
static ErrorOr<void> read_comment([[maybe_unused]] TContext& context, Streamer& streamer)
{
    bool is_first_char = true;
    u8 byte {};

    while (streamer.read(byte)) {
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
static bool read_magic_number(TContext& context, Streamer& streamer)
{
    if (context.state >= TContext::State::MagicNumber) {
        return true;
    }

    if (!context.data || context.data_size < 2) {
        context.state = TContext::State::Error;
        dbgln_if(PORTABLE_IMAGE_LOADER_DEBUG, "There is no enough data for {}", TContext::FormatDetails::image_type);
        return false;
    }

    u8 magic_number[2] {};
    if (!streamer.read_bytes(magic_number, 2)) {
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
static ErrorOr<void> read_whitespace(TContext& context, Streamer& streamer)
{
    bool is_first_char = true;
    u8 byte {};

    while (streamer.read(byte)) {
        if (byte == '#') {
            streamer.step_back();
            TRY(read_comment(context, streamer));
            continue;
        }
        if (byte != ' ' && byte != '\t' && byte != '\n' && byte != '\r') {
            streamer.step_back();
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
static ErrorOr<void> read_width(TContext& context, Streamer& streamer)
{
    context.width = TRY(read_number(streamer));
    context.state = TContext::State::Width;
    return {};
}

template<typename TContext>
static ErrorOr<void> read_height(TContext& context, Streamer& streamer)
{
    context.height = TRY(read_number(streamer));
    context.state = TContext::State::Height;
    return {};
}

template<typename TContext>
static ErrorOr<void> read_max_val(TContext& context, Streamer& streamer)
{
    context.format_details.max_val = TRY(read_number(streamer));

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

    Streamer streamer(context.data, context.data_size);

    if (!read_magic_number(context, streamer))
        return false;

    if (read_whitespace(context, streamer).is_error())
        return false;

    if (read_width(context, streamer).is_error())
        return false;

    if (read_whitespace(context, streamer).is_error())
        return false;

    if (read_height(context, streamer).is_error())
        return false;

    if (context.width > maximum_width_for_decoded_images || context.height > maximum_height_for_decoded_images) {
        dbgln("This portable network image is too large for comfort: {}x{}", context.width, context.height);
        return false;
    }

    if (read_whitespace(context, streamer).is_error())
        return false;

    if constexpr (requires { context.format_details.max_val; }) {
        if (read_max_val(context, streamer).is_error())
            return false;

        if (read_whitespace(context, streamer).is_error())
            return false;
    }

    if (!read_image_data(context, streamer))
        return false;

    error_guard.disarm();
    context.state = TContext::State::Decoded;
    return true;
}

}
