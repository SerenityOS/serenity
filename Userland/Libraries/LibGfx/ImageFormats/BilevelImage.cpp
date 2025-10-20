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

ErrorOr<NonnullRefPtr<BilevelImage>> BilevelImage::create(size_t width, size_t height)
{
    size_t pitch = ceil_div(width, static_cast<size_t>(8));
    auto bits = TRY(ByteBuffer::create_uninitialized(pitch * height));
    return adopt_nonnull_ref_or_enomem(new (nothrow) BilevelImage(move(bits), width, height, pitch));
}

ErrorOr<NonnullRefPtr<BilevelImage>> BilevelImage::create_from_byte_buffer(ByteBuffer bitmap, size_t width, size_t height)
{
    size_t pitch = ceil_div(width, static_cast<size_t>(8));
    return adopt_nonnull_ref_or_enomem(new (nothrow) BilevelImage(move(bitmap), width, height, pitch));
}

void BilevelImage::fill(bool b)
{
    u8 fill_byte = b ? 0xff : 0;
    for (auto& byte : m_bits.bytes())
        byte = fill_byte;
}

template<OneOf<BilevelImage, BilevelSubImage> InputType, BilevelImage::CompositionType operator_>
void composite_onto(InputType const& in, BilevelImage& out, IntPoint position)
{
    static constexpr auto combine = [](auto dst, auto src) -> decltype(dst) {
        switch (operator_) {
        case BilevelImage::CompositionType::Or:
            return dst | src;
        case BilevelImage::CompositionType::And:
            return dst & src;
        case BilevelImage::CompositionType::Xor:
            return dst ^ src;
        case BilevelImage::CompositionType::XNor:
            // Clang is not happy with using ~ on a bool, even if it's fine with our use case.
            if constexpr (SameAs<decltype(dst), bool>)
                return !(dst ^ src);
            else
                return ~(dst ^ src);
        case BilevelImage::CompositionType::Replace:
            return src;
        }
        VERIFY_NOT_REACHED();
    };

    IntRect bitmap_rect { position, { in.width(), in.height() } };
    IntRect out_rect { { 0, 0 }, { out.width(), out.height() } };
    IntRect clip_rect = bitmap_rect.intersected(out_rect);

    for (int y = clip_rect.top(); y < clip_rect.bottom(); ++y) {
        for (int x = clip_rect.left(); x < clip_rect.right(); ++x) {
            bool const can_use_byte = [&]() {
                if constexpr (IsSame<InputType, BilevelImage>)
                    return x % 8 == 0 && position.x() % 8 == 0 && clip_rect.right() - x > 8;
                else
                    return x % 8 == 0 && (position.x() + in.m_active_rect.x()) % 8 == 0 && clip_rect.right() - x > 8;
            }();

            if (can_use_byte) {
                auto const& src = [&]() {
                    if constexpr (IsSame<InputType, BilevelImage>)
                        return in.m_bits[(y - position.y()) * in.m_pitch + (x - position.x()) / 8];
                    else
                        return in.m_source->m_bits[(y - position.y() + in.m_active_rect.y()) * in.m_source->m_pitch + (x - position.x() + in.m_active_rect.x()) / 8];
                }();
                auto& dst = out.m_bits[y * out.m_pitch + x / 8];
                dst = combine(dst, src);
                x += 7;
            } else {
                bool src_bit = in.get_bit(x - position.x(), y - position.y());
                bool dst_bit = out.get_bit(x, y);
                out.set_bit(x, y, combine(dst_bit, src_bit));
            }
        }
    }
}

template<OneOf<BilevelImage, BilevelSubImage> InputType>
static void composite_onto(InputType const& in, BilevelImage& out, IntPoint position, BilevelImage::CompositionType operator_)
{
    switch (operator_) {
    case BilevelImage::CompositionType::Or:
        composite_onto<InputType, BilevelImage::CompositionType::Or>(in, out, position);
        break;
    case BilevelImage::CompositionType::And:
        composite_onto<InputType, BilevelImage::CompositionType::And>(in, out, position);
        break;
    case BilevelImage::CompositionType::Xor:
        composite_onto<InputType, BilevelImage::CompositionType::Xor>(in, out, position);
        break;
    case BilevelImage::CompositionType::XNor:
        composite_onto<InputType, BilevelImage::CompositionType::XNor>(in, out, position);
        break;
    case BilevelImage::CompositionType::Replace:
        composite_onto<InputType, BilevelImage::CompositionType::Replace>(in, out, position);
        break;
    }
}

void BilevelImage::composite_onto(BilevelImage& out, IntPoint position, CompositionType operator_) const
{
    ::Gfx::composite_onto(*this, out, position, operator_);
}

void BilevelSubImage::composite_onto(BilevelImage& out, IntPoint position, BilevelImage::CompositionType operator_) const
{
    ::Gfx::composite_onto(*this, out, position, operator_);
}

bool BilevelSubImage::operator==(BilevelSubImage const& other) const
{
    if (width() != other.width() || height() != other.height())
        return false;

    for (size_t y = 0; y < height(); ++y) {
        for (size_t x = 0; x < width(); ++x) {
            if (get_bit(x, y) != other.get_bit(x, y))
                return false;
        }
    }
    return true;
}

BilevelSubImage BilevelImage::subbitmap(Gfx::IntRect const& rect) const
{
    VERIFY(rect.x() >= 0);
    VERIFY(rect.width() >= 0);
    VERIFY(static_cast<size_t>(rect.right()) <= width());

    VERIFY(rect.y() >= 0);
    VERIFY(rect.height() >= 0);
    VERIFY(static_cast<size_t>(rect.bottom()) <= height());

    return BilevelSubImage { *this, rect };
}

BilevelSubImage BilevelImage::as_subbitmap() const
{
    return subbitmap(IntRect(0, 0, m_width, m_height));
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

template<unsigned N>
static constexpr Array<u32, (1 << N) * (1 << N)> make_bayer_matrix()
{
    constexpr auto size = 1u << N;
    Array<u32, size * size> result {};
    for (size_t i = 0; i < N; ++i) {
        auto slice_size = 1u << i;
        for (size_t y = 0; y < slice_size; ++y) {
            for (size_t x = 0; x < slice_size; ++x) {
                u32 v = result[y * size + x];
                result[y * size + x] = 4 * v;
                result[y * size + x + slice_size] = 4 * v + 2;
                result[(y + slice_size) * size + x] = 4 * v + 3;
                result[(y + slice_size) * size + x + slice_size] = 4 * v + 1;
            }
        }
    }
    return result;
}
static constexpr auto bayer_matrix_2x2 = make_bayer_matrix<1>();
static constexpr auto bayer_matrix_4x4 = make_bayer_matrix<2>();
static constexpr auto bayer_matrix_8x8 = make_bayer_matrix<3>();

ErrorOr<NonnullRefPtr<BilevelImage>> BilevelImage::create_from_bitmap(Gfx::Bitmap const& bitmap, DitheringAlgorithm dithering_algorithm)
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
    case DitheringAlgorithm::Bayer2x2:
    case DitheringAlgorithm::Bayer4x4:
    case DitheringAlgorithm::Bayer8x8: {
        auto bayer_matrix = [&]() {
            switch (dithering_algorithm) {
            case DitheringAlgorithm::Bayer2x2:
                return bayer_matrix_2x2.span();
            case DitheringAlgorithm::Bayer4x4:
                return bayer_matrix_4x4.span();
            case DitheringAlgorithm::Bayer8x8:
                return bayer_matrix_8x8.span();
            default:
                VERIFY_NOT_REACHED();
            }
        }();

        auto n = static_cast<int>(sqrt(bayer_matrix.size()));
        VERIFY(is_power_of_two(n));
        auto mask = n - 1;

        for (int y = 0, i = 0; y < bitmap.height(); ++y) {
            for (int x = 0; x < bitmap.width(); ++x, ++i) {
                u8 threshold = (bayer_matrix[(y & mask) * n + (x & mask)] * 255) / ((n * n) - 1);
                bilevel_image->set_bit(x, y, gray_bitmap[i] > threshold ? 0 : 1);
            }
        }
        break;
    }
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

unsigned AK::Traits<Gfx::BilevelSubImage>::hash(Gfx::BilevelSubImage const& image)
{
    unsigned hash = 0;
    for (size_t y = 0; y < image.height(); ++y) {
        for (size_t x = 0; x < image.width(); ++x) {
            auto value_hash = Traits<bool>::hash(image.get_bit(x, y));
            hash = pair_int_hash(hash, value_hash);
        }
    }
    return hash;
}
