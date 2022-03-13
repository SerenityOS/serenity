/*
 * Copyright (c) 2020, Hüseyin ASLITÜRK <asliturk@hotmail.com>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "PBMLoader.h"
#include "PortableImageLoaderCommon.h"
#include "Streamer.h"
#include <AK/Endian.h>
#include <string.h>

namespace Gfx {

bool read_image_data(PBMLoadingContext& context, Streamer& streamer)
{
    u8 byte;
    Vector<Gfx::Color> color_data;

    if (context.type == PBMLoadingContext::Type::ASCII) {
        while (streamer.read(byte)) {
            if (byte == '0') {
                color_data.append(Color::White);
            } else if (byte == '1') {
                color_data.append(Color::Black);
            }
        }
    } else if (context.type == PBMLoadingContext::Type::RAWBITS) {
        size_t color_index = 0;

        while (streamer.read(byte)) {
            for (int i = 0; i < 8; i++) {
                int val = byte & 0x80;

                if (val == 0) {
                    color_data.append(Color::White);
                } else {
                    color_data.append(Color::Black);
                }

                byte = byte << 1;
                color_index++;

                if (color_index % context.width == 0) {
                    break;
                }
            }
        }
    }

    size_t context_size = (u32)context.width * (u32)context.height;
    if (context_size != color_data.size()) {
        dbgln("Not enough color data in image.");
        return false;
    }

    if (!create_bitmap(context)) {
        return false;
    }

    set_pixels(context, color_data);

    context.state = PBMLoadingContext::State::Bitmap;
    return true;
}
}
