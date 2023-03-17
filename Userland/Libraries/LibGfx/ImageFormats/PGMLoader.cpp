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

ErrorOr<void> read_image_data(PGMLoadingContext& context)
{
    auto& stream = *context.stream;
    Vector<Gfx::Color> color_data;
    auto const context_size = context.width * context.height;

    color_data.resize(context_size);

    if (context.type == PGMLoadingContext::Type::ASCII) {
        for (u64 i = 0; i < context_size; ++i) {
            auto value = TRY(read_number(stream));

            TRY(read_whitespace(context));

            color_data[i] = { static_cast<u8>(value), static_cast<u8>(value), static_cast<u8>(value) };
        }
    } else if (context.type == PGMLoadingContext::Type::RAWBITS) {
        for (u64 i = 0; i < context_size; ++i) {
            auto const pixel = TRY(stream.read_value<u8>());
            color_data[i] = { pixel, pixel, pixel };
        }
    }

    TRY(create_bitmap(context));

    set_adjusted_pixels(context, color_data);

    context.state = PGMLoadingContext::State::Bitmap;
    return {};
}
}
