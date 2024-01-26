/*
 * Copyright (c) 2024, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "PAMLoader.h"
#include "PortableImageLoaderCommon.h"

namespace Gfx {

ErrorOr<void> read_image_data(PAMLoadingContext& context)
{
    VERIFY(context.type == PAMLoadingContext::Type::RAWBITS);

    // FIXME: Technically it's more to spec to check that a known tupl type has a minimum depth and then skip additional channels.
    bool is_gray = context.format_details.depth == 1 && context.format_details.tupl_type == "GRAYSCALE"sv;
    bool is_gray_alpha = context.format_details.depth == 2 && context.format_details.tupl_type == "GRAYSCALE_ALPHA"sv;
    bool is_rgb = context.format_details.depth == 3 && context.format_details.tupl_type == "RGB"sv;
    bool is_rgba = context.format_details.depth == 4 && context.format_details.tupl_type == "RGB_ALPHA"sv;

    bool is_cmyk = context.format_details.depth == 4 && context.format_details.tupl_type == "CMYK"sv;

    if (!is_gray && !is_gray_alpha && !is_rgb && !is_rgba && !is_cmyk)
        return Error::from_string_view("Unsupported PAM depth"sv);

    auto& stream = *context.stream;

    if (is_cmyk) {
        context.format_details.cmyk_bitmap = TRY(CMYKBitmap::create_with_size({ context.width, context.height }));
        CMYK* data = context.format_details.cmyk_bitmap.value()->begin();
        for (u64 i = 0; i < context.width * context.height; ++i) {
            Array<u8, 4> pixel;
            TRY(stream.read_until_filled(pixel));
            data[i] = { pixel[0], pixel[1], pixel[2], pixel[3] };
        }
    } else {
        TRY(create_bitmap(context));
        for (u64 i = 0; i < context.width * context.height; ++i) {
            if (is_gray) {
                Array<u8, 1> pixel;
                TRY(stream.read_until_filled(pixel));
                context.bitmap->set_pixel(i % context.width, i / context.width, { pixel[0], pixel[0], pixel[0] });
            } else if (is_gray_alpha) {
                Array<u8, 2> pixel;
                TRY(stream.read_until_filled(pixel));
                context.bitmap->set_pixel(i % context.width, i / context.width, { pixel[0], pixel[0], pixel[0], pixel[1] });
            } else if (is_rgb) {
                Array<u8, 3> pixel;
                TRY(stream.read_until_filled(pixel));
                context.bitmap->set_pixel(i % context.width, i / context.width, { pixel[0], pixel[1], pixel[2] });
            } else if (is_rgba) {
                Array<u8, 4> pixel;
                TRY(stream.read_until_filled(pixel));
                context.bitmap->set_pixel(i % context.width, i / context.width, { pixel[0], pixel[1], pixel[2], pixel[3] });
            }
        }
    }

    return {};
}
}
