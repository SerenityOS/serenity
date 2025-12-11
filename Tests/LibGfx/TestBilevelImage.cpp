/*
 * Copyright (c) 2025, Lucas Chollet <lucas.chollet@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGfx/Bitmap.h>
#include <LibGfx/ICC/TagTypes.h>
#include <LibGfx/ICC/WellKnownProfiles.h>
#include <LibGfx/ImageFormats/BilevelImage.h>
#include <LibTest/TestCase.h>

static ErrorOr<NonnullRefPtr<Gfx::BilevelImage>> create_bilevel(size_t width = 5, size_t height = 2)
{
    ByteBuffer buffer;
    buffer.append(0xCA);
    buffer.append(0xFE);

    return Gfx::BilevelImage::create_from_byte_buffer(buffer, width, height);
}

TEST_CASE(get_bit)
{
    auto bilevel = TRY_OR_FAIL(create_bilevel());

    EXPECT(bilevel->get_bit(0, 0));
    EXPECT(bilevel->get_bit(1, 0));
    EXPECT(!bilevel->get_bit(2, 0));
    EXPECT(!bilevel->get_bit(3, 0));
    EXPECT(bilevel->get_bit(4, 0));

    EXPECT(bilevel->get_bit(0, 1));
    EXPECT(bilevel->get_bit(1, 1));
    EXPECT(bilevel->get_bit(2, 1));
    EXPECT(bilevel->get_bit(3, 1));
    EXPECT(bilevel->get_bit(4, 1));
}

TEST_CASE(get_bits_equal_get_bit)
{
    auto bilevel = TRY_OR_FAIL(create_bilevel());

    for (u8 y = 0; y < 2; ++y) {
        for (u8 x = 0; x < 5; ++x)
            EXPECT(bilevel->get_bit(x, y) == bilevel->get_bits(x, y, 1));
    }
}

TEST_CASE(get_bits)
{
    auto bilevel = TRY_OR_FAIL(create_bilevel());

    EXPECT_EQ(bilevel->get_bits(0, 0, 5), 0xCA >> 3);
    EXPECT_EQ(bilevel->get_bits(0, 1, 5), 0xFE >> 3);
}

TEST_CASE(get_bits_over_8bits)
{
    auto bilevel = TRY_OR_FAIL(create_bilevel(16, 1));

    EXPECT_EQ(bilevel->get_bits(0, 0, 8), 0xCA);
    EXPECT_EQ(bilevel->get_bits(4, 0, 8), 0xAF);
    EXPECT_EQ(bilevel->get_bits(8, 0, 8), 0xFE);
    EXPECT_EQ(bilevel->get_bits(12, 0, 4), 0xE);
}

TEST_CASE(bayer_dither)
{
    auto bitmap = TRY_OR_FAIL(Gfx::Bitmap::create(Gfx::BitmapFormat::BGRx8888, { 16, 16 }));

    auto srgb_curve = TRY_OR_FAIL(Gfx::ICC::sRGB_curve());
    auto half_gray = round_to<u8>(srgb_curve->evaluate_inverse(0.5f) * 255.0f);
    bitmap->fill(Color(half_gray, half_gray, half_gray));
    auto bilevel = TRY_OR_FAIL(Gfx::BilevelImage::create_from_bitmap(*bitmap, Gfx::DitheringAlgorithm::Bayer8x8));
    for (size_t y = 0; y < bilevel->height(); ++y)
        for (size_t x = 0; x < bilevel->width(); ++x)
            EXPECT_EQ(bilevel->get_bit(x, y), (x + y) % 2 != 0);

    bitmap->fill(Color::White);
    bilevel = TRY_OR_FAIL(Gfx::BilevelImage::create_from_bitmap(*bitmap, Gfx::DitheringAlgorithm::Bayer8x8));
    for (size_t y = 0; y < bilevel->height(); ++y)
        for (size_t x = 0; x < bilevel->width(); ++x)
            EXPECT_EQ(bilevel->get_bit(x, y), 0);

    bitmap->fill(Color::Black);
    bilevel = TRY_OR_FAIL(Gfx::BilevelImage::create_from_bitmap(*bitmap, Gfx::DitheringAlgorithm::Bayer8x8));
    for (size_t y = 0; y < bilevel->height(); ++y)
        for (size_t x = 0; x < bilevel->width(); ++x)
            EXPECT_EQ(bilevel->get_bit(x, y), 1);
}
