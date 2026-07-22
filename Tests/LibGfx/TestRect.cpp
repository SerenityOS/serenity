/*
 * Copyright (c) 2023, Jelle Raaijmakers <jelle@gmta.nl>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGfx/Rect.h>
#include <LibTest/TestCase.h>

TEST_CASE(int_rect_right_and_bottom)
{
    Gfx::IntRect rect = { 2, 3, 4, 5 };
    EXPECT_EQ(rect.right(), 6);
    EXPECT_EQ(rect.bottom(), 8);
}

TEST_CASE(float_rect_right_and_bottom)
{
    Gfx::FloatRect rect = { 1.f, 2.f, 3.5f, 4.5f };
    EXPECT_APPROXIMATE(rect.right(), 4.5f);
    EXPECT_APPROXIMATE(rect.bottom(), 6.5f);
}

TEST_CASE(rect_contains_vertically)
{
    Gfx::FloatRect rect = { 0.f, 0.f, 100.f, 100.f };
    EXPECT(rect.contains_vertically(99.f));
    EXPECT(!rect.contains_vertically(100.f));
}

TEST_CASE(rect_shatter)
{
    Gfx::IntRect glass_plate = { 0, 0, 100, 100 };
    Gfx::IntRect hammer = { 30, 40, 40, 10 };

    auto shards = glass_plate.shatter(hammer);
    EXPECT(!shards.is_empty());

    int total_shard_area = 0;
    for (auto shard : shards) {
        EXPECT(glass_plate.contains(shard));
        EXPECT(!hammer.intersects(shard));
        total_shard_area += shard.size().area();
    }

    EXPECT_EQ(glass_plate.size().area() - hammer.size().area(), total_shard_area);
}

TEST_CASE(rect_closest_to)
{
    Gfx::IntRect const screen_rect = { 0, 0, 960, 540 };
    Gfx::Point<int> p = { 460, 592 }; // point is below the rect
    Gfx::Point<int> closest = screen_rect.closest_to(p);
    EXPECT_EQ(screen_rect.side(closest), Gfx::IntRect::Side::Bottom);
    p = { 960, 0 }; // point exactly on top right corner
    closest = screen_rect.closest_to(p);
    EXPECT_EQ(screen_rect.side(closest), Gfx::IntRect::Side::Top);
}

TEST_CASE(rect_unite_horizontally)
{
    Gfx::IntRect rect { 10, 10, 100, 100 };
    Gfx::IntRect huge_rect { 0, 0, 1000, 1000 };

    rect.unite_horizontally(huge_rect);

    EXPECT_EQ(rect.left(), 0);
    EXPECT_EQ(rect.right(), 1000);
    EXPECT_EQ(rect.top(), 10);
    EXPECT_EQ(rect.bottom(), 110);
}

TEST_CASE(rect_unite_vertically)
{
    Gfx::IntRect rect { 10, 10, 100, 100 };
    Gfx::IntRect huge_rect { 0, 0, 1000, 1000 };

    rect.unite_vertically(huge_rect);

    EXPECT_EQ(rect.top(), 0);
    EXPECT_EQ(rect.bottom(), 1000);
    EXPECT_EQ(rect.left(), 10);
    EXPECT_EQ(rect.right(), 110);
}

TEST_CASE(rect_scaled_to_fit_within)
{
    Gfx::IntRect wide_source { 0, 0, 40, 8 };
    Gfx::IntRect tall_source { 0, 0, 8, 40 };
    Gfx::IntRect wide_destination { 0, 0, 100, 50 };
    Gfx::IntRect tall_destination { 0, 0, 50, 100 };

    EXPECT_EQ(wide_source.scaled_to_fit_within(wide_destination), Gfx::IntRect(0, 15, 100, 20));
    EXPECT_EQ(wide_source.scaled_to_fit_within(tall_destination), Gfx::IntRect(0, 45, 50, 10));
    EXPECT_EQ(tall_source.scaled_to_fit_within(wide_destination), Gfx::IntRect(45, 0, 10, 50));
    EXPECT_EQ(tall_source.scaled_to_fit_within(tall_destination), Gfx::IntRect(15, 0, 20, 100));

    EXPECT_EQ(Gfx::IntRect(0, 0, 160, 90).scaled_to_fit_within({ 0, 0, 320, 90 }), Gfx::IntRect(80, 0, 160, 90));
    EXPECT_EQ(Gfx::IntRect(0, 0, 90, 160).scaled_to_fit_within({ 0, 0, 90, 320 }), Gfx::IntRect(0, 80, 90, 160));

    Gfx::IntRect square { 0, 0, 100, 100 };
    EXPECT_EQ(Gfx::IntRect(0, 0, 200, 100).scaled_to_fit_within(square), Gfx::IntRect(0, 25, 100, 50));
    EXPECT_EQ(Gfx::IntRect(0, 0, 100, 200).scaled_to_fit_within(square), Gfx::IntRect(25, 0, 50, 100));
    EXPECT_EQ(Gfx::IntRect(0, 0, 50, 50).scaled_to_fit_within(square), square);
    EXPECT_EQ(Gfx::IntRect(0, 0, 400, 400).scaled_to_fit_within(square), square);

    EXPECT_EQ(Gfx::IntRect(7, 9, 200, 100).scaled_to_fit_within({ 10, 20, 100, 100 }), Gfx::IntRect(10, 45, 100, 50));

    // Rounding test.
    EXPECT_EQ(Gfx::IntRect(0, 0, 7, 7).scaled_to_fit_within({ 0, 0, 61, 61 }), Gfx::IntRect(0, 0, 61, 61));

    EXPECT_EQ(Gfx::IntRect(0, 0, 0, 100).scaled_to_fit_within(square), Gfx::IntRect());
    EXPECT_EQ(Gfx::IntRect(0, 0, 100, 0).scaled_to_fit_within(square), Gfx::IntRect());
}

TEST_CASE(rect_scaled_to_cover)
{
    Gfx::IntRect wide_source { 0, 0, 40, 8 };
    Gfx::IntRect tall_source { 0, 0, 8, 40 };
    Gfx::IntRect wide_destination { 0, 0, 100, 50 };
    Gfx::IntRect tall_destination { 0, 0, 50, 100 };

    EXPECT_EQ(wide_source.scaled_to_cover(wide_destination), Gfx::IntRect(-75, 0, 250, 50));
    EXPECT_EQ(wide_source.scaled_to_cover(tall_destination), Gfx::IntRect(-225, 0, 500, 100));
    EXPECT_EQ(tall_source.scaled_to_cover(wide_destination), Gfx::IntRect(0, -225, 100, 500));
    EXPECT_EQ(tall_source.scaled_to_cover(tall_destination), Gfx::IntRect(0, -75, 50, 250));

    EXPECT_EQ(Gfx::IntRect(0, 0, 160, 90).scaled_to_cover({ 0, 0, 320, 90 }), Gfx::IntRect(0, -45, 320, 180));
    EXPECT_EQ(Gfx::IntRect(0, 0, 90, 160).scaled_to_cover({ 0, 0, 90, 320 }), Gfx::IntRect(-45, 0, 180, 320));

    Gfx::IntRect square { 0, 0, 100, 100 };
    EXPECT_EQ(Gfx::IntRect(0, 0, 200, 100).scaled_to_cover(square), Gfx::IntRect(-50, 0, 200, 100));
    EXPECT_EQ(Gfx::IntRect(0, 0, 100, 200).scaled_to_cover(square), Gfx::IntRect(0, -50, 100, 200));
    EXPECT_EQ(Gfx::IntRect(0, 0, 50, 50).scaled_to_cover(square), square);
    EXPECT_EQ(Gfx::IntRect(7, 9, 200, 100).scaled_to_cover({ 10, 20, 100, 100 }), Gfx::IntRect(-40, 20, 200, 100));

    // Rounding test.
    EXPECT_EQ(Gfx::IntRect(0, 0, 7, 10).scaled_to_cover({ 0, 0, 61, 61 }), Gfx::IntRect(0, -13, 61, 87));

    EXPECT_EQ(Gfx::IntRect(0, 0, 0, 100).scaled_to_cover(square), Gfx::IntRect());
}

TEST_CASE(float_rect_scaled_to_fit_within)
{
    auto fractional = Gfx::FloatRect(0.f, 0.f, 3.f, 1.f).scaled_to_fit_within({ 0.f, 0.f, 7.f, 7.f });
    EXPECT_EQ(fractional.width(), 7.f); // Exact in matched direction.
    EXPECT_APPROXIMATE(fractional.height(), 7.f / 3.f);
    EXPECT_EQ(fractional.x(), 0.f);
    EXPECT_APPROXIMATE(fractional.y(), (7.f - 7.f / 3.f) / 2.f);

    auto tall_in_wide = Gfx::FloatRect(0.f, 0.f, 1.f, 3.f).scaled_to_fit_within({ 0.f, 0.f, 7.f, 5.f });
    EXPECT_APPROXIMATE(tall_in_wide.width(), 5.f / 3.f);
    EXPECT_EQ(tall_in_wide.height(), 5.f); // Exact in matched direction.
    EXPECT_APPROXIMATE(tall_in_wide.x(), (7.f - 5.f / 3.f) / 2.f);
    EXPECT_EQ(tall_in_wide.y(), 0.f);

    auto wide_in_tall = Gfx::FloatRect(0.f, 0.f, 3.f, 1.f).scaled_to_fit_within({ 0.f, 0.f, 5.f, 7.f });
    EXPECT_EQ(wide_in_tall.width(), 5.f); // Exact in matched direction.
    EXPECT_APPROXIMATE(wide_in_tall.height(), 5.f / 3.f);
    EXPECT_EQ(wide_in_tall.x(), 0.f);
    EXPECT_APPROXIMATE(wide_in_tall.y(), (7.f - 5.f / 3.f) / 2.f);

    // Rounding tests.
    EXPECT_EQ(Gfx::FloatRect(0, 0, 7, 7).scaled_to_fit_within({ 0, 0, 61, 61 }), Gfx::FloatRect(0, 0, 61, 61));

    EXPECT_EQ(Gfx::FloatRect(0, 0, 640, 480).scaled_to_fit_within({ 0, 0, 1280, 720 }), Gfx::FloatRect(160, 0, 960, 720));
}

TEST_CASE(float_rect_scaled_to_cover)
{
    auto covered = Gfx::FloatRect(0.f, 0.f, 3.f, 1.f).scaled_to_cover({ 0.f, 0.f, 10.f, 10.f });
    EXPECT_APPROXIMATE(covered.width(), 30.f);
    EXPECT_EQ(covered.height(), 10.f); // Exact in matched direction.
    EXPECT_APPROXIMATE(covered.x(), -10.f);
    EXPECT_EQ(covered.y(), 0.f);

    // Rounding test.
    EXPECT_EQ(Gfx::FloatRect(0, 0, 7, 7).scaled_to_cover({ 0, 0, 61, 61 }), Gfx::FloatRect(0, 0, 61, 61));
}

TEST_CASE(double_rect_scaling_has_exact_longer_side)
{
    auto inner = Gfx::DoubleRect(0., 0., 7., 7.);
    auto outer = Gfx::DoubleRect { 0., 0., 61., 61. };

    auto within = inner.scaled_to_fit_within(outer);
    EXPECT_EQ(within.width(), 61.);
    EXPECT_EQ(within.height(), 61.);
    EXPECT_EQ(within.x(), 0.);
    EXPECT_EQ(within.y(), 0.);

    auto tall_outer = inner.scaled_to_fit_within({ 0., 0., 61., 200. });
    EXPECT_EQ(tall_outer.width(), 61.);
    EXPECT_APPROXIMATE(tall_outer.height(), 61.);
    EXPECT_EQ(tall_outer.x(), 0.);
    EXPECT_APPROXIMATE(tall_outer.y(), (200. - 61.) / 2.);

    auto cover = inner.scaled_to_cover(outer);
    EXPECT_EQ(cover.width(), 61.);
    EXPECT_EQ(cover.height(), 61.);
    EXPECT_EQ(cover.x(), 0.);
    EXPECT_EQ(cover.y(), 0.);
}
