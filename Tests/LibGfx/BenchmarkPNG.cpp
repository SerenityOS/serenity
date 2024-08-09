/*
 * Copyright (c) 2024, Nico Weber <thakis@chromium.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/FixedArray.h>
#include <LibCore/File.h>
#include <LibGfx/ImageFormats/JPEGLoader.h>
#include <LibGfx/ImageFormats/PNGShared.h>
#include <LibTest/TestCase.h>

#ifdef AK_OS_SERENITY
#    define TEST_INPUT(x) ("/usr/Tests/LibGfx/test-inputs/" x)
#else
#    define TEST_INPUT(x) ("test-inputs/" x)
#endif

auto bitmap = Gfx::JPEGImageDecoderPlugin::create(Core::File::open(TEST_INPUT("jpg/big_image.jpg"sv), Core::File::OpenMode::Read).release_value()->read_until_eof().release_value()).release_value()->frame(0).release_value().image;

BENCHMARK_CASE(paeth)
{
    Vector<AK::SIMD::u8x4> output;
    output.ensure_capacity(bitmap->width() * bitmap->height());

    auto dummy_scanline = MUST(FixedArray<Gfx::ARGB32>::create(bitmap->width()));
    auto const* scanline_minus_1 = dummy_scanline.data();

    for (int y = 0; y < bitmap->height(); ++y) {
        auto* scanline = bitmap->scanline(y);

        auto pixel_x_minus_1 = (AK::SIMD::u8x4)dummy_scanline[0];
        auto pixel_xy_minus_1 = (AK::SIMD::u8x4)dummy_scanline[0];

        for (int x = 0; x < bitmap->width(); ++x) {
            auto pixel = (AK::SIMD::u8x4)(scanline[x]);
            auto pixel_y_minus_1 = (AK::SIMD::u8x4)(scanline_minus_1[x]);

            auto out = pixel - Gfx::PNG::paeth_predictor(pixel_x_minus_1, pixel_y_minus_1, pixel_xy_minus_1);

            output.append(out);

            pixel_x_minus_1 = pixel;
            pixel_xy_minus_1 = pixel_y_minus_1;
        }

        scanline_minus_1 = scanline;
    }
}
