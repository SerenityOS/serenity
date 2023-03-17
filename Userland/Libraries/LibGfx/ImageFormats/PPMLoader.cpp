/*
 * Copyright (c) 2020, Hüseyin ASLITÜRK <asliturk@hotmail.com>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "PPMLoader.h"
#include "PortableImageLoaderCommon.h"
#include <AK/Endian.h>
#include <AK/LexicalPath.h>
#include <AK/ScopeGuard.h>
#include <AK/StringBuilder.h>
#include <LibGfx/Streamer.h>
#include <string.h>

namespace Gfx {

bool read_image_data(PPMLoadingContext& context, Streamer& streamer)
{
    Vector<Gfx::Color> color_data;
    auto const context_size = context.width * context.height;
    color_data.resize(context_size);

    if (context.type == PPMLoadingContext::Type::ASCII) {
        for (u64 i = 0; i < context_size; ++i) {
            auto const red_or_error = read_number(streamer);
            if (red_or_error.is_error())
                return false;

            if (read_whitespace(context, streamer).is_error())
                return false;

            auto const green_or_error = read_number(streamer);
            if (green_or_error.is_error())
                return false;

            if (read_whitespace(context, streamer).is_error())
                return false;

            auto const blue_or_error = read_number(streamer);
            if (blue_or_error.is_error())
                return false;

            if (read_whitespace(context, streamer).is_error())
                return false;

            Color color { (u8)red_or_error.value(), (u8)green_or_error.value(), (u8)blue_or_error.value() };
            if (context.format_details.max_val < 255)
                color = adjust_color(context.format_details.max_val, color);
            color_data[i] = color;
        }
    } else if (context.type == PPMLoadingContext::Type::RAWBITS) {
        for (u64 i = 0; i < context_size; ++i) {
            u8 pixel[3];
            if (!streamer.read_bytes(pixel, 3))
                return false;
            color_data[i] = { pixel[0], pixel[1], pixel[2] };
        }
    }

    if (!create_bitmap(context)) {
        return false;
    }

    set_pixels(context, color_data);

    context.state = PPMLoadingContext::State::Bitmap;
    return true;
}
}
