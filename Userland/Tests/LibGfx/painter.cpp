/*
 * Copyright (c) 2021, Oleg Sikorskiy <olegsik@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibTest/TestCase.h>

#include <LibGfx/Bitmap.h>
#include <LibGfx/Painter.h>
#include <stdio.h>

BENCHMARK_CASE(diagonal_lines)
{
    const int run_count = 50;
    const int bitmap_size = 2000;

    auto bitmap = Gfx::Bitmap::create(Gfx::BitmapFormat::BGRx8888, { bitmap_size, bitmap_size });
    Gfx::Painter painter(*bitmap);

    for (int run = 0; run < run_count; run++) {
        for (int i = 0; i < bitmap_size; i++) {
            painter.draw_line({ 0, 0 }, { i, bitmap_size - 1 }, Color::Blue);
            painter.draw_line({ 0, 0 }, { bitmap_size - 1, i }, Color::Blue);
        }
    }
}

BENCHMARK_CASE(fill)
{
    const int run_count = 1000;
    const int bitmap_size = 2000;

    auto bitmap = Gfx::Bitmap::create(Gfx::BitmapFormat::BGRx8888, { bitmap_size, bitmap_size });
    Gfx::Painter painter(*bitmap);

    for (int run = 0; run < run_count; run++) {
        painter.fill_rect(bitmap->rect(), Color::Blue);
    }
}

BENCHMARK_CASE(fill_with_gradient)
{
    const int run_count = 50;
    const int bitmap_size = 2000;

    auto bitmap = Gfx::Bitmap::create(Gfx::BitmapFormat::BGRx8888, { bitmap_size, bitmap_size });
    Gfx::Painter painter(*bitmap);

    for (int run = 0; run < run_count; run++) {
        painter.fill_rect_with_gradient(bitmap->rect(), Color::Blue, Color::Red);
    }
}
