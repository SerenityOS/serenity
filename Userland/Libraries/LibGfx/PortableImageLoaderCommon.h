/*
 * Copyright (c) 2020, Hüseyin Aslıtürk <asliturk@hotmail.com>
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Array.h>
#include <AK/Debug.h>
#include <AK/Endian.h>
#include <AK/ScopeGuard.h>
#include <AK/String.h>
#include <AK/StringBuilder.h>
#include <AK/Types.h>
#include <AK/Vector.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/Color.h>
#include <LibGfx/ImageDecoder.h>
#include <LibGfx/Streamer.h>

namespace Gfx {

static constexpr Color adjust_color(u16 max_val, Color color)
{
    color.set_red((color.red() * 255) / max_val);
    color.set_green((color.green() * 255) / max_val);
    color.set_blue((color.blue() * 255) / max_val);

    return color;
}

template<typename TValue>
static bool read_number(Streamer& streamer, TValue* value)
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

    const auto opt_value = sb.to_string().to_uint();
    if (!opt_value.has_value()) {
        *value = 0;
        return false;
    }

    *value = static_cast<u16>(opt_value.value());
    return true;
}

template<typename TContext>
static bool read_comment([[maybe_unused]] TContext& context, Streamer& streamer)
{
    bool exist = false;
    u8 byte {};

    while (streamer.read(byte)) {
        if (byte == '#') {
            exist = true;
        } else if (byte == '\t' || byte == '\n') {
            return exist;
        }
    }

    return exist;
}

template<typename TContext>
static bool read_magic_number(TContext& context, Streamer& streamer)
{
    if (context.state >= TContext::State::MagicNumber) {
        return true;
    }

    if (!context.data || context.data_size < 2) {
        context.state = TContext::State::Error;
        dbgln_if(PORTABLE_IMAGE_LOADER_DEBUG, "There is no enough data for {}", TContext::image_type);
        return false;
    }

    u8 magic_number[2] {};
    if (!streamer.read_bytes(magic_number, 2)) {
        context.state = TContext::State::Error;
        dbgln_if(PORTABLE_IMAGE_LOADER_DEBUG, "We can't read magic number for {}", TContext::image_type);
        return false;
    }

    if (magic_number[0] == 'P' && magic_number[1] == TContext::ascii_magic_number) {
        context.type = TContext::Type::ASCII;
        context.state = TContext::State::MagicNumber;
        return true;
    }

    if (magic_number[0] == 'P' && magic_number[1] == TContext::binary_magic_number) {
        context.type = TContext::Type::RAWBITS;
        context.state = TContext::State::MagicNumber;
        return true;
    }

    context.state = TContext::State::Error;
    dbgln_if(PORTABLE_IMAGE_LOADER_DEBUG, "Magic number is not valid for {}{}{}", magic_number[0], magic_number[1], TContext::image_type);
    return false;
}

template<typename TContext>
static bool read_whitespace(TContext& context, Streamer& streamer)
{
    bool exist = false;
    u8 byte {};

    while (streamer.read(byte)) {
        if (byte == ' ' || byte == '\t' || byte == '\n' || byte == '\r') {
            exist = true;
        } else if (byte == '#') {
            streamer.step_back();
            read_comment(context, streamer);
        } else {
            streamer.step_back();
            return exist;
        }
    }

    return exist;
}

template<typename TContext>
static bool read_width(TContext& context, Streamer& streamer)
{
    if (const bool result = read_number(streamer, &context.width);
        !result || context.width == 0) {
        return false;
    }

    context.state = TContext::Width;
    return true;
}

template<typename TContext>
static bool read_height(TContext& context, Streamer& streamer)
{
    if (const bool result = read_number(streamer, &context.height);
        !result || context.height == 0) {
        return false;
    }

    context.state = TContext::Height;
    return true;
}

template<typename TContext>
static bool read_max_val(TContext& context, Streamer& streamer)
{
    if (const bool result = read_number(streamer, &context.max_val);
        !result || context.max_val == 0) {
        return false;
    }

    if (context.max_val > 255) {
        dbgln_if(PORTABLE_IMAGE_LOADER_DEBUG, "We can't parse 2 byte color for {}", TContext::image_type);
        context.state = TContext::Error;
        return false;
    }

    context.state = TContext::Maxval;
    return true;
}

template<typename TContext>
static bool create_bitmap(TContext& context)
{
    auto bitmap_or_error = Bitmap::try_create(BitmapFormat::BGRx8888, { context.width, context.height });
    if (bitmap_or_error.is_error()) {
        context.state = TContext::State::Error;
        return false;
    }
    context.bitmap = bitmap_or_error.release_value_but_fixme_should_propagate_errors();
    return true;
}

template<typename TContext>
static void set_pixels(TContext& context, const Vector<Gfx::Color>& color_data)
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

    if (!read_whitespace(context, streamer))
        return false;

    if (!read_width(context, streamer))
        return false;

    if (!read_whitespace(context, streamer))
        return false;

    if (!read_height(context, streamer))
        return false;

    if (context.width > maximum_width_for_decoded_images || context.height > maximum_height_for_decoded_images) {
        dbgln("This portable network image is too large for comfort: {}x{}", context.width, context.height);
        return false;
    }

    if (!read_whitespace(context, streamer))
        return false;

    if constexpr (requires { context.max_val; }) {
        if (!read_max_val(context, streamer))
            return false;

        if (!read_whitespace(context, streamer))
            return false;
    }

    if (!read_image_data(context, streamer))
        return false;

    error_guard.disarm();
    context.state = TContext::State::Decoded;
    return true;
}

template<typename TContext>
static RefPtr<Gfx::Bitmap> load_impl(const u8* data, size_t data_size)
{
    TContext context {};
    context.data = data;
    context.data_size = data_size;

    if (!decode(context)) {
        return nullptr;
    }
    return context.bitmap;
}

template<typename TContext>
static RefPtr<Gfx::Bitmap> load_from_memory(u8 const* data, size_t length, String const& mmap_name)
{
    auto bitmap = load_impl<TContext>(data, length);
    if (bitmap)
        bitmap->set_mmap_name(String::formatted("Gfx::Bitmap [{}] - Decoded {}: {}", bitmap->size(), TContext::image_type, mmap_name));
    return bitmap;
}

}
