/*
 * Copyright (c) 2025, Nico Weber <thakis@chromium.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/NonnullOwnPtr.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/ICC/TagTypes.h>
#include <LibGfx/ICC/WellKnownProfiles.h>
#include <LibGfx/ImageFormats/BilevelImage.h>

namespace Gfx {

ErrorOr<NonnullOwnPtr<BilevelImage>> BilevelImage::create(size_t width, size_t height)
{
    size_t pitch = ceil_div(width, static_cast<size_t>(8));
    auto bits = TRY(ByteBuffer::create_uninitialized(pitch * height));
    return adopt_nonnull_own_or_enomem(new (nothrow) BilevelImage(move(bits), width, height, pitch));
}

ErrorOr<NonnullOwnPtr<BilevelImage>> BilevelImage::create_from_byte_buffer(ByteBuffer bitmap, size_t width, size_t height)
{
    size_t pitch = ceil_div(width, static_cast<size_t>(8));
    return adopt_nonnull_own_or_enomem(new (nothrow) BilevelImage(move(bitmap), width, height, pitch));
}

bool BilevelImage::get_bit(size_t x, size_t y) const
{
    VERIFY(x < m_width);
    VERIFY(y < m_height);
    size_t byte_offset = x / 8;
    size_t bit_offset = x % 8;
    u8 byte = m_bits[y * m_pitch + byte_offset];
    byte = (byte >> (8 - 1 - bit_offset)) & 1;
    return byte != 0;
}

void BilevelImage::set_bit(size_t x, size_t y, bool b)
{
    VERIFY(x < m_width);
    VERIFY(y < m_height);
    size_t byte_offset = x / 8;
    size_t bit_offset = x % 8;
    u8 byte = m_bits[y * m_pitch + byte_offset];
    u8 mask = 1u << (8 - 1 - bit_offset);
    if (b)
        byte |= mask;
    else
        byte &= ~mask;
    m_bits[y * m_pitch + byte_offset] = byte;
}

void BilevelImage::fill(bool b)
{
    u8 fill_byte = b ? 0xff : 0;
    for (auto& byte : m_bits.bytes())
        byte = fill_byte;
}

ErrorOr<NonnullOwnPtr<BilevelImage>> BilevelImage::subbitmap(Gfx::IntRect const& rect) const
{
    VERIFY(rect.x() >= 0);
    VERIFY(rect.width() >= 0);
    VERIFY(static_cast<size_t>(rect.right()) <= width());

    VERIFY(rect.y() >= 0);
    VERIFY(rect.height() >= 0);
    VERIFY(static_cast<size_t>(rect.bottom()) <= height());

    auto subbitmap = TRY(create(rect.width(), rect.height()));
    for (int y = 0; y < rect.height(); ++y)
        for (int x = 0; x < rect.width(); ++x)
            subbitmap->set_bit(x, y, get_bit(rect.x() + x, rect.y() + y));
    return subbitmap;
}

ErrorOr<NonnullRefPtr<Gfx::Bitmap>> BilevelImage::to_gfx_bitmap() const
{
    auto bitmap = TRY(Gfx::Bitmap::create(Gfx::BitmapFormat::BGRx8888, { m_width, m_height }));
    for (size_t y = 0; y < m_height; ++y) {
        for (size_t x = 0; x < m_width; ++x) {
            auto color = get_bit(x, y) ? Color::Black : Color::White;
            bitmap->set_pixel(x, y, color);
        }
    }
    return bitmap;
}

ErrorOr<ByteBuffer> BilevelImage::to_byte_buffer() const
{
    return ByteBuffer::copy(m_bits);
}

BilevelImage::BilevelImage(ByteBuffer bits, size_t width, size_t height, size_t pitch)
    : m_bits(move(bits))
    , m_width(width)
    , m_height(height)
    , m_pitch(pitch)
{
}

static Array<u32, 256> compute_luminosity_histogram(ByteBuffer const& bitmap)
{
    Array<u32, 256> histogram {};
    for (u8 value : bitmap.span())
        histogram[value]++;
    return histogram;
}

static u8 compute_otsu_threshold(Array<u32, 256> const& histogram)
{
    // https://en.wikipedia.org/wiki/Otsu%27s_method#Otsu's_method
    // All multiplied through with number of pixels, since p(i) * number_of_pixels == histogram[i]
    // and it all cancels out when just looking for the max, as far as I can tell.

    u32 histogram_sum = 0;
    u32 mu_T = 0;
    for (int i = 0; i < 256; ++i) {
        histogram_sum += histogram[i];
        mu_T += i * histogram[i];
    }

    u32 sum_0 = 0;
    u32 omega_0 = 0;
    f32 max_inter_class_variance = 0;
    u8 threshold = 0;

    for (int i = 0; i < 256; ++i) {
        omega_0 += histogram[i];
        u32 omega_1 = histogram_sum - omega_0;
        if (omega_0 == 0 || omega_1 == 0)
            continue;

        sum_0 += i * histogram[i];
        u32 sum_1 = mu_T - sum_0;
        f32 mu_0 = static_cast<f32>(sum_0) / omega_0;
        f32 mu_1 = static_cast<f32>(sum_1) / omega_1;
        f32 inter_class_variance = static_cast<f32>(omega_0) * static_cast<f32>(omega_1) * (mu_0 - mu_1) * (mu_0 - mu_1);
        if (inter_class_variance > max_inter_class_variance) {
            threshold = i;
            max_inter_class_variance = inter_class_variance;
        }
    }
    return threshold;
}

ErrorOr<NonnullOwnPtr<BilevelImage>> BilevelImage::create_from_bitmap(Gfx::Bitmap const& bitmap, DitheringAlgorithm dithering_algorithm)
{
    auto gray_bitmap = TRY(ByteBuffer::create_uninitialized(bitmap.width() * bitmap.height()));
    for (int y = 0, i = 0; y < bitmap.height(); ++y) {
        for (int x = 0; x < bitmap.width(); ++x, ++i) {
            auto color = bitmap.get_pixel(x, y);
            gray_bitmap[i] = color.luminosity();
        }
    }

    auto srgb_curve = TRY(Gfx::ICC::sRGB_curve());
    for (u8& v : gray_bitmap.span())
        v = round_to<u8>(srgb_curve->evaluate(v / 255.0f) * 255.0f);

    // For now, do global thresholding with Otsu's method.
    // https://en.wikipedia.org/wiki/Otsu%27s_method
    // FIXME: Add an option to use average as threshold instead of Otsu?
    auto histogram = compute_luminosity_histogram(gray_bitmap);
    u8 threshold = compute_otsu_threshold(histogram);

    auto bilevel_image = TRY(BilevelImage::create(bitmap.width(), bitmap.height()));

    switch (dithering_algorithm) {
    case DitheringAlgorithm::None:
        for (int y = 0, i = 0; y < bitmap.height(); ++y) {
            for (int x = 0; x < bitmap.width(); ++x, ++i) {
                bilevel_image->set_bit(x, y, gray_bitmap[i] > threshold ? 0 : 1);
            }
        }
        break;
    case DitheringAlgorithm::FloydSteinberg: {
        struct Factor {
            int offset;
            u8 factor;
        };
        auto factors = Array {
            Factor { 1, 7 },
            Factor { -1 + bitmap.width(), 3 },
            Factor { 0 + bitmap.width(), 5 },
            Factor { 1 + bitmap.width(), 1 },
        };
        for (int y = 0, i = 0; y < bitmap.height(); ++y) {
            for (int x = 0; x < bitmap.width(); ++x, ++i) {
                u8 old_pixel = gray_bitmap[i];
                u8 new_pixel = old_pixel > threshold ? 255 : 0;
                bilevel_image->set_bit(x, y, new_pixel == 255 ? 0 : 1);
                int error = static_cast<int>(old_pixel) - static_cast<int>(new_pixel);
                for (auto const& factor : factors) {
                    int index = i + factor.offset;
                    if (index >= static_cast<int>(gray_bitmap.size()))
                        continue;
                    int new_value = static_cast<int>(gray_bitmap[index]) + (error * factor.factor) / 16;
                    gray_bitmap[index] = clamp(new_value, 0, 255);
                }
            }
        }
        break;
    }
    }
    return bilevel_image;
}

}
