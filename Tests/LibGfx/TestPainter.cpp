/*
 * Copyright (c) 2024, Nico Weber <thakis@chromium.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibTest/TestCase.h>

#include <LibGfx/EdgeFlagPathRasterizer.h>
#include <LibGfx/PaintStyle.h>
#include <LibGfx/Painter.h>

TEST_CASE(draw_scaled_bitmap_with_transform)
{
    auto bitmap = MUST(Gfx::Bitmap::create(Gfx::BitmapFormat::BGRx8888, { 40, 30 }));
    bitmap->fill(Gfx::Color::White);
    Gfx::Painter painter(bitmap);

    auto source_bitmap = MUST(Gfx::Bitmap::create(Gfx::BitmapFormat::BGRx8888, { 1, 1 }));
    source_bitmap->fill(Gfx::Color::Black);

    auto dest_rect = source_bitmap->rect();
    auto source_rect = source_bitmap->rect().to_rounded<float>();

    // Identity transform: Lower left pixel is black, rest stays white.
    Gfx::AffineTransform transform;
    painter.draw_scaled_bitmap_with_transform(dest_rect, source_bitmap, source_rect, transform);
    for (int y = 0; y < bitmap->height(); ++y) {
        for (int x = 0; x < bitmap->width(); ++x) {
            if (x == 0 && y == 0)
                EXPECT_EQ(bitmap->get_pixel(x, y), Color::Black);
            else
                EXPECT_EQ(bitmap->get_pixel(x, y), Color::White);
        }
    }

    // Scale up 1x1 source bitmap 10x in x and 5x in y and paint at 10, 20. Should fill that rect:
    bitmap->fill(Gfx::Color::White);
    transform = transform.translate(10, 20).scale(10, 5);
    painter.draw_scaled_bitmap_with_transform(dest_rect, source_bitmap, source_rect, transform);
    for (int y = 0; y < bitmap->height(); ++y) {
        for (int x = 0; x < bitmap->width(); ++x) {
            if (x >= 10 && x < 10 + 10 && y >= 20 && y < 20 + 5)
                EXPECT_EQ(bitmap->get_pixel(x, y), Color::Black);
            else
                EXPECT_EQ(bitmap->get_pixel(x, y), Color::White);
        }
    }
}

TEST_CASE(draw_rect_rough_bounds)
{
    auto bitmap = MUST(Gfx::Bitmap::create(Gfx::BitmapFormat::BGRx8888, { 10, 10 }));
    Gfx::Painter painter(*bitmap);
    painter.draw_rect(Gfx::IntRect(0, 0, 1, 1), Color::Black, true);
    painter.draw_rect(Gfx::IntRect(9, 9, 1, 1), Color::Black, true);
}

TEST_CASE(draw_triangle_wave)
{
    auto bitmap = MUST(Gfx::Bitmap::create(Gfx::BitmapFormat::BGRx8888, { 10, 10 }));
    Gfx::Painter painter(*bitmap);
    for (int y = -3; y < bitmap->height() + 3; ++y)
        painter.draw_triangle_wave({ 0, y }, { bitmap->width(), y }, Gfx::Color::Red, 3, 2);
}

// FIXME: Stolen from `Userland/Demos/Tubes/Tubes.cpp`; we really need something like this in `AK/Random.h`.
static double random_double()
{
    return get_random<u32>() / static_cast<double>(NumericLimits<u32>::max());
}

TEST_CASE(aliased_fill)
{
    // Picks a random point in the bitmap, fills a triangle from each edge of the bitmap to that point,
    // and checks that the entire bitmap is filled.

    int const w = 200;
    int const h = 100;

    auto bitmap = MUST(Gfx::Bitmap::create(Gfx::BitmapFormat::BGRx8888, { w, h }));
    Gfx::Painter painter(*bitmap);
    auto paint_style = TRY_OR_FAIL(Gfx::SolidColorPaintStyle::create(Gfx::Color::Black));

    int failed_test_count = 0;
    for (int i = 0; i < 1000; ++i) {
        bitmap->fill(Gfx::Color::White);

        float x = random_double() * w;
        float y = random_double() * h;

        Array<Array<Gfx::FloatPoint, 3>, 4> triangles = {
            Array<Gfx::FloatPoint, 3> { Gfx::FloatPoint { 0, 0 }, { w, 0 }, { x, y } },
            Array<Gfx::FloatPoint, 3> { Gfx::FloatPoint { 0, h }, { w, h }, { x, y } },
            Array<Gfx::FloatPoint, 3> { Gfx::FloatPoint { 0, 0 }, { 0, h }, { x, y } },
            Array<Gfx::FloatPoint, 3> { Gfx::FloatPoint { w, 0 }, { w, h }, { x, y } },
        };

        for (auto const& triangle : triangles) {
            Gfx::Path triangle_path;
            triangle_path.move_to(triangle[0]);
            triangle_path.line_to(triangle[1]);
            triangle_path.line_to(triangle[2]);
            triangle_path.close();

            painter.fill_path<Gfx::SampleNoAA>(triangle_path, paint_style);
        }

        int filled_pixels_count = 0;
        for (int y = 0; y < bitmap->height(); ++y) {
            for (int x = 0; x < bitmap->width(); ++x) {
                if (bitmap->get_pixel(x, y) == Gfx::Color { Gfx::Color::Black })
                    filled_pixels_count++;
            }
        }

        if (filled_pixels_count != w * h)
            failed_test_count++;
    }

    EXPECT_EQ(failed_test_count, 0);
}
