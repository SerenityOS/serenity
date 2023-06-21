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
    auto& stream = *context.stream;
    Vector<Gfx::Color> color_data;

    auto const context_size = context.width * context.height;
    color_data.resize(context_size);

    if (context.type == PBMLoadingContext::Type::ASCII) {
        for (u64 i = 0; i < context_size; ++i) {
            auto const byte = TRY(stream.read_value<u8>());
            if (byte == '0')
                color_data[i] = Color::White;
            else if (byte == '1')
                color_data[i] = Color::Black;
            else
                i--;
        }
    } else if (context.type == PBMLoadingContext::Type::RAWBITS) {
        for (u64 color_index = 0; color_index < context_size;) {
            auto byte = TRY(stream.read_value<u8>());
            for (int i = 0; i < 8; i++) {
                auto const val = byte & 0x80;

                if (val == 0)
                    color_data[color_index] = Color::White;
                else
                    color_data[color_index] = Color::Black;

                byte = byte << 1;
                color_index++;

                if (color_index % context.width == 0) {
                    break;
                }
            }
        }
    }

    TRY(create_bitmap(context));

    set_pixels(context, color_data);

    context.state = PBMLoadingContext::State::Bitmap;
    return {};
}
}
