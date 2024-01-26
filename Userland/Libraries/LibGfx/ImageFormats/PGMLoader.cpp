/*
 * Copyright (c) 2020, Hüseyin ASLITÜRK <asliturk@hotmail.com>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "PGMLoader.h"
#include "PortableImageLoaderCommon.h"

namespace Gfx {

ErrorOr<void> read_image_data(PGMLoadingContext& context)
{
    TRY(create_bitmap(context));

    auto& stream = *context.stream;
    auto const context_size = context.width * context.height;

    if (context.type == PGMLoadingContext::Type::ASCII) {
        for (u64 i = 0; i < context_size; ++i) {
            auto value = TRY(read_number(stream));

            TRY(read_whitespace(context));

            Color color { static_cast<u8>(value), static_cast<u8>(value), static_cast<u8>(value) };
            if (context.format_details.max_val < 255)
                color = adjust_color(context.format_details.max_val, color);

            context.bitmap->set_pixel(i % context.width, i / context.width, color);
        }
    } else if (context.type == PGMLoadingContext::Type::RAWBITS) {
        for (u64 i = 0; i < context_size; ++i) {
            auto const pixel = TRY(stream.read_value<u8>());
            context.bitmap->set_pixel(i % context.width, i / context.width, { pixel, pixel, pixel });
        }
    }

    return {};
}
}
