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
    color_data.ensure_capacity(context.width * context.height);

    if (context.type == PPMLoadingContext::Type::ASCII) {
        while (true) {
            auto const red_or_error = read_number(streamer);
            if (red_or_error.is_error())
                break;

            if (read_whitespace(context, streamer).is_error())
                break;

            auto const green_or_error = read_number(streamer);
            if (green_or_error.is_error())
                break;

            if (read_whitespace(context, streamer).is_error())
                break;

            auto const blue_or_error = read_number(streamer);
            if (blue_or_error.is_error())
                break;

            if (read_whitespace(context, streamer).is_error())
                break;

            Color color { (u8)red_or_error.value(), (u8)green_or_error.value(), (u8)blue_or_error.value() };
            if (context.format_details.max_val < 255)
                color = adjust_color(context.format_details.max_val, color);
            color_data.append(color);
        }
    } else if (context.type == PPMLoadingContext::Type::RAWBITS) {
        u8 pixel[3];
        while (streamer.read_bytes(pixel, 3)) {
            color_data.append({ pixel[0], pixel[1], pixel[2] });
        }
    }

    if (context.width * context.height != color_data.size())
        return false;

    if (!create_bitmap(context)) {
        return false;
    }

    set_pixels(context, color_data);

    context.state = PPMLoadingContext::State::Bitmap;
    return true;
}
}
