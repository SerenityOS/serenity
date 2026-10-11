/*
 * Copyright (c) 2024, Nico Weber <thakis@chromium.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibTest/TestCase.h>

#include <LibGfx/Bitmap.h>
#include <LibGfx/CharacterBitmap.h>
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

// A painter on a bitmap with scale s should produce exactly the same image as a
// painter on an unscaled bitmap, with every logical pixel expanded to an s x s block.
static void expect_scaled_painter_matches_pixel_doubled_unscaled(Gfx::Bitmap const& scaled, Gfx::Bitmap const& unscaled)
{
    EXPECT_EQ(scaled.scale(), 2);
    EXPECT_EQ(unscaled.scale(), 1);
    EXPECT_EQ(scaled.size(), unscaled.size());

    for (int y = 0; y < unscaled.height(); ++y) {
        for (int x = 0; x < unscaled.width(); ++x) {
            auto expected = unscaled.get_pixel(x, y);
            for (int dy = 0; dy < scaled.scale(); ++dy)
                for (int dx = 0; dx < scaled.scale(); ++dx)
                    EXPECT_EQ(scaled.get_pixel(x * scaled.scale() + dx, y * scaled.scale() + dy), expected);
        }
    }
}

TEST_CASE(painter_scale_matches_pixel_doubled_unscaled)
{
    int const width = 16;
    int const height = 12;

    auto unscaled = MUST(Gfx::Bitmap::create(Gfx::BitmapFormat::BGRx8888, { width, height }));
    auto scaled = MUST(Gfx::Bitmap::create(Gfx::BitmapFormat::BGRx8888, { width, height }, 2));
    unscaled->fill(Gfx::Color::White);
    scaled->fill(Gfx::Color::White);

    Gfx::Painter unscaled_painter(*unscaled);
    Gfx::Painter scaled_painter(*scaled);

    static Gfx::CharacterBitmap const glyph { AK::StringView { "##.#....", 8 }, 4, 2 };

    auto paint = [&](Gfx::Painter& painter) {
        painter.fill_rect({ 0, 0, 8, 8 }, Gfx::Color::Green);
        painter.fill_rect({ 9, 1, 4, 4 }, Gfx::Color { 0, 0, 0, 128 });
        painter.fill_rect_with_checkerboard({ 2, 2, 10, 8 }, { 2, 3 }, Gfx::Color::Black, Gfx::Color::White);
        painter.fill_rect_with_dither_pattern({ 4, 3, 8, 6 }, Gfx::Color::Black, Gfx::Color::White);
        painter.draw_rect({ 1, 1, 12, 9 }, Gfx::Color::Red);
        painter.draw_focus_rect({ 3, 2, 9, 7 }, Gfx::Color::Blue);
        painter.draw_line({ 0, 5 }, { width - 1, 5 }, Gfx::Color::Magenta, 2);
        painter.draw_line({ 5, 0 }, { 5, height - 1 }, Gfx::Color::Magenta, 2);
        painter.draw_bitmap({ 6, 6 }, glyph, Gfx::Color::Black);
    };

    paint(unscaled_painter);
    paint(scaled_painter);

    expect_scaled_painter_matches_pixel_doubled_unscaled(*scaled, *unscaled);
}

TEST_CASE(painter_scale_get_region_bitmap)
{
    auto bitmap = MUST(Gfx::Bitmap::create(Gfx::BitmapFormat::BGRx8888, { 8, 8 }, 2));
    bitmap->fill(Gfx::Color::White);
    Gfx::Painter painter(*bitmap);
    painter.fill_rect({ 2, 2, 4, 4 }, Gfx::Color::Black);

    auto region = MUST(painter.get_region_bitmap({ 2, 2, 4, 4 }, Gfx::BitmapFormat::BGRx8888));
    EXPECT_EQ(region->scale(), 2);
    auto expected_size = Gfx::IntSize { 4, 4 };
    EXPECT_EQ(region->size(), expected_size);
    for (int y = 0; y < region->physical_height(); ++y)
        for (int x = 0; x < region->physical_width(); ++x)
            EXPECT_EQ(region->get_pixel(x, y), Gfx::Color::Black);
}
