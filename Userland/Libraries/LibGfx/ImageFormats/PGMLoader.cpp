/*
 * Copyright (c) 2020, Hüseyin ASLITÜRK <asliturk@hotmail.com>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "PGMLoader.h"
#include "PortableImageLoaderCommon.h"

namespace Gfx {

static void set_adjusted_pixels(PGMLoadingContext& context, Vector<Gfx::Color> const& color_data)
{
    size_t index = 0;
    for (size_t y = 0; y < context.height; ++y) {
        for (size_t x = 0; x < context.width; ++x) {
            Color color = color_data.at(index);
            if (context.format_details.max_val < 255) {
                color = adjust_color(context.format_details.max_val, color);
            }
            context.bitmap->set_pixel(x, y, color);
            ++index;
        }
    }
}

bool read_image_data(PGMLoadingContext& context)
{
    auto& stream = *context.stream;
    Vector<Gfx::Color> color_data;
    auto const context_size = context.width * context.height;

    color_data.resize(context_size);

    if (context.type == PGMLoadingContext::Type::ASCII) {
        for (u64 i = 0; i < context_size; ++i) {
            auto number_or_error = read_number(stream);
            if (number_or_error.is_error())
                return false;
            auto value = number_or_error.value();

            if (read_whitespace(context).is_error())
                return false;

            color_data[i] = { (u8)value, (u8)value, (u8)value };
        }
    } else if (context.type == PGMLoadingContext::Type::RAWBITS) {
        for (u64 i = 0; i < context_size; ++i) {
            auto pixel_or_error = stream.read_value<u8>();
            if (pixel_or_error.is_error())
                return false;
            auto const pixel = pixel_or_error.value();
            color_data[i] = { pixel, pixel, pixel };
        }
    }

    if (!create_bitmap(context))
        return false;

    set_adjusted_pixels(context, color_data);

    context.state = PGMLoadingContext::State::Bitmap;
    return true;
}
}
