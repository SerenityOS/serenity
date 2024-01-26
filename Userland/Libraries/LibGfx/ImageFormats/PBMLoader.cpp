/*
 * Copyright (c) 2020, Hüseyin ASLITÜRK <asliturk@hotmail.com>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "PBMLoader.h"
#include "PortableImageLoaderCommon.h"

namespace Gfx {

ErrorOr<void> read_image_data(PBMLoadingContext& context)
{
    TRY(create_bitmap(context));

    auto& stream = *context.stream;

    auto const context_size = context.width * context.height;

    if (context.type == PBMLoadingContext::Type::ASCII) {
        for (u64 i = 0; i < context_size; ++i) {
            auto const byte = TRY(stream.read_value<u8>());
            if (byte == '0')
                context.bitmap->set_pixel(i % context.width, i / context.width, Color::White);
            else if (byte == '1')
                context.bitmap->set_pixel(i % context.width, i / context.width, Color::Black);
            else
                i--;
        }
    } else if (context.type == PBMLoadingContext::Type::RAWBITS) {
        for (u64 color_index = 0; color_index < context_size;) {
            auto byte = TRY(stream.read_value<u8>());
            for (int i = 0; i < 8; i++) {
                auto const val = byte & 0x80;

                if (val == 0)
                    context.bitmap->set_pixel(color_index % context.width, color_index / context.width, Color::White);
                else
                    context.bitmap->set_pixel(color_index % context.width, color_index / context.width, Color::Black);

                byte = byte << 1;
                color_index++;

                if (color_index % context.width == 0) {
                    break;
                }
            }
        }
    }

    return {};
}
}
