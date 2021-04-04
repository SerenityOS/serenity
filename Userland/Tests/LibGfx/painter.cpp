/*
 * Copyright (c) 2021, Oleg Sikorskiy <olegsik@gmail.com>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <AK/TestSuite.h>

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

TEST_MAIN(Painter)
