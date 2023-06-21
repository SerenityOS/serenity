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
