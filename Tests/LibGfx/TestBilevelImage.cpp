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

static ErrorOr<void> test_bayer_dither(Gfx::DitheringAlgorithm algorithm, u32 size)
{
    auto srgb_curve = TRY(Gfx::ICC::sRGB_curve());
    auto bitmap = TRY(Gfx::Bitmap::create(Gfx::BitmapFormat::BGRA8888, { size, size }));
    auto number_of_states = size * size + 1;

    auto test_luminosity = [&](f32 input_luminosity, f32 output_luminosity) -> ErrorOr<void> {
        auto uncompressed = round_to<u8>(srgb_curve->evaluate_inverse(input_luminosity) * 255);
        bitmap->fill(Gfx::Color(uncompressed, uncompressed, uncompressed));
        auto bilevel = TRY(Gfx::BilevelImage::create_from_bitmap(bitmap, algorithm));
        double average = 0;
        for (u32 y = 0; y < bilevel->height(); ++y) {
            for (u32 x = 0; x < bilevel->width(); ++x)
                average += bilevel->get_bit(x, y) ? 0 : 1;
        }

        EXPECT_APPROXIMATE(average / (size * size), output_luminosity);
        return {};
    };

    // Test full black and full white.
    TRY(test_luminosity(0, 0));
    TRY(test_luminosity(1, 1));

    // Test all states at half the range.
    for (u32 s = 0; s < number_of_states; ++s) {
        // We test all states in the middle of there range.
        auto value = (static_cast<f32>(s) + 0.5f) / number_of_states;
        // There are only (number_of_states - 1) states of luminosity.
        TRY(test_luminosity(value, static_cast<f32>(s) / (number_of_states - 1)));
    }
    return {};
}

TEST_CASE(bayer_dither)
{

    TRY_OR_FAIL(test_bayer_dither(Gfx::DitheringAlgorithm::Bayer2x2, 2));
    TRY_OR_FAIL(test_bayer_dither(Gfx::DitheringAlgorithm::Bayer4x4, 4));
    TRY_OR_FAIL(test_bayer_dither(Gfx::DitheringAlgorithm::Bayer8x8, 8));
}
