/*
 * Copyright (c) 2024, Lucas Chollet <lucas.chollet@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGfx/Bitmap.h>
#include <LibGfx/MedianCut.h>
#include <LibTest/TestCase.h>

TEST_CASE(single_element)
{
    auto const bitmap = TRY_OR_FAIL(Gfx::Bitmap::create(Gfx::BitmapFormat::BGRA8888, { 1, 1 }));
    bitmap->set_pixel(0, 0, Gfx::Color::NamedColor::White);

    auto const result = TRY_OR_FAIL(Gfx::median_cut(bitmap, 1));

    EXPECT_EQ(result.palette().size(), 1ul);
    EXPECT_EQ(result.closest_color(Gfx::Color::NamedColor::White), Gfx::Color::NamedColor::White);
}

namespace {
constexpr auto colors = to_array<Gfx::Color>({ { 253, 0, 0 }, { 255, 0, 0 }, { 0, 253, 0 }, { 0, 255, 0 } });

ErrorOr<NonnullRefPtr<Gfx::Bitmap>> create_test_bitmap()
{
    auto bitmap = TRY(Gfx::Bitmap::create(Gfx::BitmapFormat::BGRA8888, { colors.size(), 1 }));
    for (u8 i = 0; i < colors.size(); ++i)
        bitmap->set_pixel(i, 0, colors[i]);
    return bitmap;
}
}

TEST_CASE(four_in_four_out)
{
    auto const bitmap = TRY_OR_FAIL(create_test_bitmap());

    auto const result = TRY_OR_FAIL(Gfx::median_cut(bitmap, 4));

    EXPECT_EQ(result.palette().size(), 4ul);
    for (auto const color : colors)
        EXPECT_EQ(result.closest_color(color), color);
}

TEST_CASE(four_in_two_out)
{
    auto const bitmap = TRY_OR_FAIL(create_test_bitmap());

    auto const result = TRY_OR_FAIL(Gfx::median_cut(bitmap, 2));

    EXPECT_EQ(result.palette().size(), 2ul);
    EXPECT_EQ(result.closest_color(Gfx::Color(253, 0, 0)), Gfx::Color(254, 0, 0));
    EXPECT_EQ(result.closest_color(Gfx::Color(255, 0, 0)), Gfx::Color(254, 0, 0));
    EXPECT_EQ(result.closest_color(Gfx::Color(0, 253, 0)), Gfx::Color(0, 254, 0));
    EXPECT_EQ(result.closest_color(Gfx::Color(0, 255, 0)), Gfx::Color(0, 254, 0));
}
