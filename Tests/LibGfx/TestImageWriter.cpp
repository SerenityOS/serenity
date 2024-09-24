/*
 * Copyright (c) 2024, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/MemoryStream.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/ICC/BinaryWriter.h>
#include <LibGfx/ICC/Profile.h>
#include <LibGfx/ICC/WellKnownProfiles.h>
#include <LibGfx/ImageFormats/AnimationWriter.h>
#include <LibGfx/ImageFormats/BMPLoader.h>
#include <LibGfx/ImageFormats/BMPWriter.h>
#include <LibGfx/ImageFormats/GIFLoader.h>
#include <LibGfx/ImageFormats/GIFWriter.h>
#include <LibGfx/ImageFormats/JPEGLoader.h>
#include <LibGfx/ImageFormats/JPEGWriter.h>
#include <LibGfx/ImageFormats/PNGLoader.h>
#include <LibGfx/ImageFormats/PNGWriter.h>
#include <LibGfx/ImageFormats/QOILoader.h>
#include <LibGfx/ImageFormats/QOIWriter.h>
#include <LibGfx/ImageFormats/WebPLoader.h>
#include <LibGfx/ImageFormats/WebPSharedLossless.h>
#include <LibGfx/ImageFormats/WebPWriter.h>
#include <LibTest/TestCase.h>

static ErrorOr<NonnullRefPtr<Gfx::Bitmap>> expect_single_frame(Gfx::ImageDecoderPlugin& plugin_decoder)
{
    EXPECT_EQ(plugin_decoder.frame_count(), 1u);
    EXPECT(!plugin_decoder.is_animated());
    EXPECT(!plugin_decoder.loop_count());

    auto frame_descriptor = TRY(plugin_decoder.frame(0));
    EXPECT_EQ(frame_descriptor.duration, 0);
    return *frame_descriptor.image;
}

static ErrorOr<NonnullRefPtr<Gfx::Bitmap>> expect_single_frame_of_size(Gfx::ImageDecoderPlugin& plugin_decoder, Gfx::IntSize size)
{
    EXPECT_EQ(plugin_decoder.size(), size);
    auto frame = TRY(expect_single_frame(plugin_decoder));
    EXPECT_EQ(frame->size(), size);
    return frame;
}

template<class Writer, class... ExtraArgs>
static ErrorOr<ByteBuffer> encode_bitmap(Gfx::Bitmap const& bitmap, ExtraArgs... extra_args)
{
    if constexpr (requires(AllocatingMemoryStream stream) { Writer::encode(stream, bitmap, extra_args...); }) {
        AllocatingMemoryStream stream;
        TRY(Writer::encode(stream, bitmap, extra_args...));
        return stream.read_until_eof();
    } else {
        return Writer::encode(bitmap, extra_args...);
    }
}

template<class Writer, class Loader>
static ErrorOr<NonnullRefPtr<Gfx::Bitmap>> get_roundtrip_bitmap(Gfx::Bitmap const& bitmap)
{
    auto encoded_data = TRY(encode_bitmap<Writer>(bitmap));
    return expect_single_frame_of_size(*TRY(Loader::create(encoded_data)), bitmap.size());
}

static void expect_bitmaps_equal(Gfx::Bitmap const& a, Gfx::Bitmap const& b)
{
    VERIFY(a.size() == b.size());
    for (int y = 0; y < a.height(); ++y)
        for (int x = 0; x < a.width(); ++x)
            EXPECT_EQ(a.get_pixel(x, y), b.get_pixel(x, y));
}

template<class Writer, class Loader>
static ErrorOr<void> test_roundtrip(Gfx::Bitmap const& bitmap)
{
    auto decoded = TRY((get_roundtrip_bitmap<Writer, Loader>(bitmap)));
    expect_bitmaps_equal(*decoded, bitmap);
    return {};
}

static ErrorOr<AK::NonnullRefPtr<Gfx::Bitmap>> create_test_rgb_bitmap()
{
    auto bitmap = TRY(Gfx::Bitmap::create(Gfx::BitmapFormat::BGRA8888, { 47, 33 }));

    for (int y = 0; y < bitmap->height(); ++y)
        for (int x = 0; x < bitmap->width(); ++x)
            bitmap->set_pixel(x, y, Gfx::Color((x * 255) / bitmap->width(), (y * 255) / bitmap->height(), x + y));

    return bitmap;
}

static ErrorOr<AK::NonnullRefPtr<Gfx::Bitmap>> create_test_rgba_bitmap()
{
    auto bitmap = TRY(create_test_rgb_bitmap());

    for (int y = 0; y < bitmap->height(); ++y) {
        for (int x = 0; x < bitmap->width(); ++x) {
            Color pixel = bitmap->get_pixel(x, y);
            pixel.set_alpha(255 - x);
            bitmap->set_pixel(x, y, pixel);
        }
    }

    return bitmap;
}

TEST_CASE(test_bmp)
{
    TRY_OR_FAIL((test_roundtrip<Gfx::BMPWriter, Gfx::BMPImageDecoderPlugin>(TRY_OR_FAIL(create_test_rgb_bitmap()))));
    TRY_OR_FAIL((test_roundtrip<Gfx::BMPWriter, Gfx::BMPImageDecoderPlugin>(TRY_OR_FAIL(create_test_rgba_bitmap()))));
}

TEST_CASE(test_gif)
{
    // Let's limit the size of the image so every color can fit in a color table of 256 elements.
    auto bitmap = TRY_OR_FAIL(TRY_OR_FAIL(create_test_rgb_bitmap())->cropped({ 0, 0, 16, 16 }));

    auto encoded_bitmap = TRY_OR_FAIL((encode_bitmap<Gfx::GIFWriter>(bitmap)));
    auto decoder = TRY_OR_FAIL(Gfx::GIFImageDecoderPlugin::create(encoded_bitmap));

    EXPECT_EQ(decoder->size(), bitmap->size());
    EXPECT_EQ(decoder->frame_count(), 1u);
    EXPECT(!decoder->is_animated());

    expect_bitmaps_equal(*TRY_OR_FAIL(decoder->frame(0)).image, bitmap);
}

TEST_CASE(test_gif_animated)
{
    auto bitmap_1 = TRY_OR_FAIL(TRY_OR_FAIL(create_test_rgb_bitmap())->cropped({ 0, 0, 16, 16 }));
    auto bitmap_2 = TRY_OR_FAIL(TRY_OR_FAIL(create_test_rgb_bitmap())->cropped({ 16, 16, 16, 16 }));
    auto bitmap_3 = TRY_OR_FAIL(bitmap_2->clone());

    bitmap_3->scanline(3)[3] = Color(Color::NamedColor::Red).value();

    auto stream_buffer = TRY_OR_FAIL(ByteBuffer::create_uninitialized(3072));
    FixedMemoryStream stream { Bytes { stream_buffer } };
    auto animation_writer = TRY_OR_FAIL(Gfx::GIFWriter::start_encoding_animation(stream, bitmap_1->size(), 0));
    TRY_OR_FAIL(animation_writer->add_frame(*bitmap_1, 100));
    TRY_OR_FAIL(animation_writer->add_frame(*bitmap_2, 200));
    TRY_OR_FAIL(animation_writer->add_frame_relative_to_last_frame(*bitmap_3, 200, *bitmap_2));

    auto encoded_animation = ReadonlyBytes { stream_buffer.data(), stream.offset() };

    auto decoder = TRY_OR_FAIL(Gfx::GIFImageDecoderPlugin::create(encoded_animation));

    EXPECT_EQ(decoder->size(), bitmap_1->size());
    EXPECT_EQ(decoder->frame_count(), 3u);
    EXPECT_EQ(decoder->loop_count(), 0u);
    EXPECT(decoder->is_animated());

    auto const frame_1 = TRY_OR_FAIL(decoder->frame(0));
    EXPECT_EQ(frame_1.duration, 100);
    expect_bitmaps_equal(*frame_1.image, bitmap_1);

    auto const frame_2 = TRY_OR_FAIL(decoder->frame(1));
    EXPECT_EQ(frame_2.duration, 200);
    expect_bitmaps_equal(*frame_2.image, bitmap_2);

    auto const frame_3 = TRY_OR_FAIL(decoder->frame(2));
    EXPECT_EQ(frame_3.duration, 200);
    expect_bitmaps_equal(*frame_3.image, bitmap_3);
}

TEST_CASE(test_jpeg)
{
    // JPEG is lossy, so the roundtripped bitmap won't match the original bitmap. But it should still have the same size.
    (void)TRY_OR_FAIL((get_roundtrip_bitmap<Gfx::JPEGWriter, Gfx::JPEGImageDecoderPlugin>(TRY_OR_FAIL(create_test_rgb_bitmap()))));
}

TEST_CASE(test_png)
{
    TRY_OR_FAIL((test_roundtrip<Gfx::PNGWriter, Gfx::PNGImageDecoderPlugin>(TRY_OR_FAIL(create_test_rgb_bitmap()))));
    TRY_OR_FAIL((test_roundtrip<Gfx::PNGWriter, Gfx::PNGImageDecoderPlugin>(TRY_OR_FAIL(create_test_rgba_bitmap()))));
}

TEST_CASE(test_png_paeth_simd)
{
    for (int a = 0; a < 256; ++a) {
        for (int b = 0; b < 256; ++b) {
            for (int c = 0; c < 256; ++c) {
                u8 expected = Gfx::PNG::paeth_predictor(a, b, c);

                AK::SIMD::u8x4 va { (u8)a, (u8)a, (u8)a, (u8)a };
                AK::SIMD::u8x4 vb { (u8)b, (u8)b, (u8)b, (u8)b };
                AK::SIMD::u8x4 vc { (u8)c, (u8)c, (u8)c, (u8)c };
                AK::SIMD::u8x4 actual = Gfx::PNG::paeth_predictor(va, vb, vc);

                EXPECT_EQ(actual[0], expected);
                EXPECT_EQ(actual[1], expected);
                EXPECT_EQ(actual[2], expected);
                EXPECT_EQ(actual[3], expected);
            }
        }
    }
}

TEST_CASE(test_png_animation)
{
    auto rgb_bitmap = TRY_OR_FAIL(create_test_rgb_bitmap());
    auto rgba_bitmap = TRY_OR_FAIL(create_test_rgba_bitmap());

    // 20 kiB is enough for two 47x33 frames.
    auto stream_buffer = TRY_OR_FAIL(ByteBuffer::create_uninitialized(20 * 1024));
    FixedMemoryStream stream { Bytes { stream_buffer } };

    auto animation_writer = TRY_OR_FAIL(Gfx::PNGWriter::start_encoding_animation(stream, rgb_bitmap->size()));

    TRY_OR_FAIL(animation_writer->add_frame(*rgb_bitmap, 100));
    TRY_OR_FAIL(animation_writer->add_frame(*rgba_bitmap, 200));

    auto encoded_animation = ReadonlyBytes { stream_buffer.data(), stream.offset() };

    auto decoded_animation_plugin = TRY_OR_FAIL(Gfx::PNGImageDecoderPlugin::create(encoded_animation));
    EXPECT(decoded_animation_plugin->is_animated());
    EXPECT_EQ(decoded_animation_plugin->frame_count(), 2u);
    EXPECT_EQ(decoded_animation_plugin->loop_count(), 0u);
    EXPECT_EQ(decoded_animation_plugin->size(), rgb_bitmap->size());

    auto frame0 = TRY_OR_FAIL(decoded_animation_plugin->frame(0));
    EXPECT_EQ(frame0.duration, 100);
    expect_bitmaps_equal(*frame0.image, *rgb_bitmap);

    auto frame1 = TRY_OR_FAIL(decoded_animation_plugin->frame(1));
    EXPECT_EQ(frame1.duration, 200);
    expect_bitmaps_equal(*frame1.image, *rgba_bitmap);
}

TEST_CASE(test_png_incremental_animation)
{
    auto rgb_bitmap_1 = TRY_OR_FAIL(create_test_rgb_bitmap());

    auto rgb_bitmap_2 = TRY_OR_FAIL(create_test_rgb_bitmap());

    rgb_bitmap_2->scanline(3)[3] = Gfx::Color(Color::Red).value();

    // 20 kiB is enough for two 47x33 frames.
    auto stream_buffer = TRY_OR_FAIL(ByteBuffer::create_uninitialized(20 * 1024));
    FixedMemoryStream stream { Bytes { stream_buffer } };

    auto animation_writer = TRY_OR_FAIL(Gfx::PNGWriter::start_encoding_animation(stream, rgb_bitmap_1->size()));

    TRY_OR_FAIL(animation_writer->add_frame(*rgb_bitmap_1, 100));
    TRY_OR_FAIL(animation_writer->add_frame_relative_to_last_frame(*rgb_bitmap_2, 200, *rgb_bitmap_1));

    auto encoded_animation = ReadonlyBytes { stream_buffer.data(), stream.offset() };

    auto decoded_animation_plugin = TRY_OR_FAIL(Gfx::PNGImageDecoderPlugin::create(encoded_animation));
    EXPECT(decoded_animation_plugin->is_animated());
    EXPECT_EQ(decoded_animation_plugin->frame_count(), 2u);
    EXPECT_EQ(decoded_animation_plugin->loop_count(), 0u);
    EXPECT_EQ(decoded_animation_plugin->size(), rgb_bitmap_1->size());

    auto frame0 = TRY_OR_FAIL(decoded_animation_plugin->frame(0));
    EXPECT_EQ(frame0.duration, 100);
    expect_bitmaps_equal(*frame0.image, *rgb_bitmap_1);

    auto frame1 = TRY_OR_FAIL(decoded_animation_plugin->frame(1));
    EXPECT_EQ(frame1.duration, 200);
    expect_bitmaps_equal(*frame1.image, *rgb_bitmap_2);
}

TEST_CASE(test_qoi)
{
    TRY_OR_FAIL((test_roundtrip<Gfx::QOIWriter, Gfx::QOIImageDecoderPlugin>(TRY_OR_FAIL(create_test_rgb_bitmap()))));
    TRY_OR_FAIL((test_roundtrip<Gfx::QOIWriter, Gfx::QOIImageDecoderPlugin>(TRY_OR_FAIL(create_test_rgba_bitmap()))));
}

TEST_CASE(test_webp)
{
    TRY_OR_FAIL((test_roundtrip<Gfx::WebPWriter, Gfx::WebPImageDecoderPlugin>(TRY_OR_FAIL(create_test_rgb_bitmap()))));
    TRY_OR_FAIL((test_roundtrip<Gfx::WebPWriter, Gfx::WebPImageDecoderPlugin>(TRY_OR_FAIL(create_test_rgba_bitmap()))));
}

TEST_CASE(test_webp_color_indexing_transform)
{
    Array<Color, 256> colors;
    for (size_t i = 0; i < colors.size(); ++i) {
        colors[i].set_red(i);
        colors[i].set_green(255 - i);
        colors[i].set_blue(128);
        colors[i].set_alpha(255 - i / 16);
    }
    for (int bits_per_pixel : { 1, 2, 4, 8 }) {
        int number_of_colors = 1 << bits_per_pixel;

        auto bitmap = TRY_OR_FAIL(Gfx::Bitmap::create(Gfx::BitmapFormat::BGRA8888, { 47, 33 }));
        for (int y = 0; y < bitmap->height(); ++y)
            for (int x = 0; x < bitmap->width(); ++x)
                bitmap->set_pixel(x, y, colors[(x * bitmap->width() + y) % number_of_colors]);

        auto encoded_data = TRY_OR_FAIL(encode_bitmap<Gfx::WebPWriter>(bitmap));
        auto decoded_bitmap = TRY_OR_FAIL(expect_single_frame_of_size(*TRY_OR_FAIL(Gfx::WebPImageDecoderPlugin::create(encoded_data)), bitmap->size()));
        expect_bitmaps_equal(*decoded_bitmap, *bitmap);

        Gfx::WebPEncoderOptions options;
        options.vp8l_options.allowed_transforms = 0;
        auto encoded_data_without_color_indexing = TRY_OR_FAIL(encode_bitmap<Gfx::WebPWriter>(bitmap, options));
        EXPECT(encoded_data.size() < encoded_data_without_color_indexing.size());
        auto decoded_bitmap_without_color_indexing = TRY_OR_FAIL(expect_single_frame_of_size(*TRY_OR_FAIL(Gfx::WebPImageDecoderPlugin::create(encoded_data)), bitmap->size()));
        expect_bitmaps_equal(*decoded_bitmap_without_color_indexing, *decoded_bitmap);
    }
}

TEST_CASE(test_webp_color_indexing_transform_single_channel)
{
    Array<Color, 256> colors;
    for (size_t i = 0; i < colors.size(); ++i) {
        colors[i].set_red(0);
        colors[i].set_green(255 - i);
        colors[i].set_blue(128);
        colors[i].set_alpha(255);
    }
    for (int bits_per_pixel : { 1, 2, 4, 8 }) {
        int number_of_colors = 1 << bits_per_pixel;

        auto bitmap = TRY_OR_FAIL(Gfx::Bitmap::create(Gfx::BitmapFormat::BGRA8888, { 47, 33 }));
        for (int y = 0; y < bitmap->height(); ++y)
            for (int x = 0; x < bitmap->width(); ++x)
                bitmap->set_pixel(x, y, colors[(x * bitmap->width() + y) % number_of_colors]);

        auto encoded_data = TRY_OR_FAIL(encode_bitmap<Gfx::WebPWriter>(bitmap));
        auto decoded_bitmap = TRY_OR_FAIL(expect_single_frame_of_size(*TRY_OR_FAIL(Gfx::WebPImageDecoderPlugin::create(encoded_data)), bitmap->size()));
        expect_bitmaps_equal(*decoded_bitmap, *bitmap);

        Gfx::WebPEncoderOptions options;
        options.vp8l_options.allowed_transforms = options.vp8l_options.allowed_transforms & ~((1u << Gfx::COLOR_INDEXING_TRANSFORM) | (1u << Gfx::PREDICTOR_TRANSFORM));
        auto encoded_data_without_color_indexing = TRY_OR_FAIL(encode_bitmap<Gfx::WebPWriter>(bitmap, options));
        if (bits_per_pixel == 8)
            EXPECT(encoded_data.size() <= encoded_data_without_color_indexing.size());
        else
            EXPECT(encoded_data.size() < encoded_data_without_color_indexing.size());
        auto decoded_bitmap_without_color_indexing = TRY_OR_FAIL(expect_single_frame_of_size(*TRY_OR_FAIL(Gfx::WebPImageDecoderPlugin::create(encoded_data)), bitmap->size()));
        expect_bitmaps_equal(*decoded_bitmap_without_color_indexing, *decoded_bitmap);
    }
}

TEST_CASE(test_webp_grayscale)
{
    Array<Color, 256> colors;
    for (size_t i = 0; i < colors.size(); ++i) {
        colors[i].set_red(i);
        colors[i].set_green(255 - i);
        colors[i].set_blue(128);
        colors[i].set_alpha(255 - i / 16);
    }
    Array<Color, 256> grays;
    for (size_t i = 0; i < grays.size(); ++i) {
        // `255 - i` because:
        // * webpwriter sorts palette colors by luminance
        // * luminance is mostly green and `colors` uses `255 - i` for green
        // * and palette order should match for compressed size comparisons to be meaningful
        grays[i].set_red(255 - i);
        grays[i].set_green(255 - i);
        grays[i].set_blue(255 - i);
        grays[i].set_alpha(255);
    }
    Array<Color, 256> grays_with_alpha = grays;
    for (size_t i = 0; i < grays_with_alpha.size(); ++i)
        grays_with_alpha[i].set_alpha(255 - i / 16);

    auto make_bitmap = [](Array<Color, 256> const& palette) -> ErrorOr<NonnullRefPtr<Gfx::Bitmap>> {
        auto bitmap = TRY(Gfx::Bitmap::create(Gfx::BitmapFormat::BGRA8888, { 47, 33 }));
        for (int y = 0; y < bitmap->height(); ++y)
            for (int x = 0; x < bitmap->width(); ++x)
                bitmap->set_pixel(x, y, palette[(x * bitmap->width() + y) % palette.size()]);
        return bitmap;
    };
    auto colors_bitmap = TRY_OR_FAIL(make_bitmap(colors));
    auto grays_bitmap = TRY_OR_FAIL(make_bitmap(grays));
    auto grays_with_alpha_bitmap = TRY_OR_FAIL(make_bitmap(grays_with_alpha));

    auto encoded_grays = TRY_OR_FAIL(encode_bitmap<Gfx::WebPWriter>(grays_bitmap));
    auto decoded_grays = TRY_OR_FAIL(expect_single_frame_of_size(*TRY_OR_FAIL(Gfx::WebPImageDecoderPlugin::create(encoded_grays)), grays_bitmap->size()));
    expect_bitmaps_equal(*decoded_grays, *grays_bitmap);

    auto encoded_colors = TRY_OR_FAIL(encode_bitmap<Gfx::WebPWriter>(colors_bitmap));
    EXPECT(encoded_grays.size() < encoded_colors.size());

    auto encoded_grays_with_alpha = TRY_OR_FAIL(encode_bitmap<Gfx::WebPWriter>(grays_with_alpha_bitmap));
    EXPECT(encoded_grays_with_alpha.size() <= encoded_colors.size());
    EXPECT(encoded_grays.size() < encoded_grays_with_alpha.size());
}

TEST_CASE(test_webp_color_cache)
{
    auto bitmap = TRY_OR_FAIL(create_test_rgba_bitmap());
    for (int color_cache_bits = 0; color_cache_bits <= 11; ++color_cache_bits) {
        Gfx::WebPEncoderOptions options;
        if (color_cache_bits == 0)
            options.vp8l_options.color_cache_bits = {};
        else
            options.vp8l_options.color_cache_bits = color_cache_bits;

        auto encoded_data = TRY_OR_FAIL(encode_bitmap<Gfx::WebPWriter>(bitmap));
        auto decoded_bitmap = TRY_OR_FAIL(expect_single_frame_of_size(*TRY_OR_FAIL(Gfx::WebPImageDecoderPlugin::create(encoded_data)), bitmap->size()));
        expect_bitmaps_equal(*decoded_bitmap, *bitmap);
    }
}

TEST_CASE(test_webp_icc)
{
    auto sRGB_icc_profile = MUST(Gfx::ICC::sRGB());
    auto sRGB_icc_data = MUST(Gfx::ICC::encode(sRGB_icc_profile));

    auto rgba_bitmap = TRY_OR_FAIL(create_test_rgba_bitmap());
    Gfx::WebPEncoderOptions options;
    options.icc_data = sRGB_icc_data;
    auto encoded_rgba_bitmap = TRY_OR_FAIL((encode_bitmap<Gfx::WebPWriter>(rgba_bitmap, options)));

    auto decoded_rgba_plugin = TRY_OR_FAIL(Gfx::WebPImageDecoderPlugin::create(encoded_rgba_bitmap));
    expect_bitmaps_equal(*TRY_OR_FAIL(expect_single_frame_of_size(*decoded_rgba_plugin, rgba_bitmap->size())), rgba_bitmap);
    auto decoded_rgba_profile = TRY_OR_FAIL(Gfx::ICC::Profile::try_load_from_externally_owned_memory(TRY_OR_FAIL(decoded_rgba_plugin->icc_data()).value()));
    auto reencoded_icc_data = TRY_OR_FAIL(Gfx::ICC::encode(decoded_rgba_profile));
    EXPECT_EQ(sRGB_icc_data, reencoded_icc_data);
}

TEST_CASE(test_webp_animation)
{
    auto rgb_bitmap = TRY_OR_FAIL(create_test_rgb_bitmap());
    auto rgba_bitmap = TRY_OR_FAIL(create_test_rgba_bitmap());

    // 20 kiB is enough for two 47x33 frames.
    auto stream_buffer = TRY_OR_FAIL(ByteBuffer::create_uninitialized(20 * 1024));
    FixedMemoryStream stream { Bytes { stream_buffer } };

    auto animation_writer = TRY_OR_FAIL(Gfx::WebPWriter::start_encoding_animation(stream, rgb_bitmap->size()));

    TRY_OR_FAIL(animation_writer->add_frame(*rgb_bitmap, 100));
    TRY_OR_FAIL(animation_writer->add_frame(*rgba_bitmap, 200));

    auto encoded_animation = ReadonlyBytes { stream_buffer.data(), stream.offset() };

    auto decoded_animation_plugin = TRY_OR_FAIL(Gfx::WebPImageDecoderPlugin::create(encoded_animation));
    EXPECT(decoded_animation_plugin->is_animated());
    EXPECT_EQ(decoded_animation_plugin->frame_count(), 2u);
    EXPECT_EQ(decoded_animation_plugin->loop_count(), 0u);
    EXPECT_EQ(decoded_animation_plugin->size(), rgb_bitmap->size());

    auto frame0 = TRY_OR_FAIL(decoded_animation_plugin->frame(0));
    EXPECT_EQ(frame0.duration, 100);
    expect_bitmaps_equal(*frame0.image, *rgb_bitmap);

    auto frame1 = TRY_OR_FAIL(decoded_animation_plugin->frame(1));
    EXPECT_EQ(frame1.duration, 200);
    expect_bitmaps_equal(*frame1.image, *rgba_bitmap);
}

TEST_CASE(test_webp_incremental_animation)
{
    auto rgb_bitmap_1 = TRY_OR_FAIL(create_test_rgb_bitmap());

    auto rgb_bitmap_2 = TRY_OR_FAIL(create_test_rgb_bitmap());

    // WebP frames can't be at odd coordinates. Make a pixel at an odd coordinate different to make sure we handle this.
    rgb_bitmap_2->scanline(3)[3] = Gfx::Color(Color::Red).value();

    // 20 kiB is enough for two 47x33 frames.
    auto stream_buffer = TRY_OR_FAIL(ByteBuffer::create_uninitialized(20 * 1024));
    FixedMemoryStream stream { Bytes { stream_buffer } };

    auto animation_writer = TRY_OR_FAIL(Gfx::WebPWriter::start_encoding_animation(stream, rgb_bitmap_1->size()));

    TRY_OR_FAIL(animation_writer->add_frame(*rgb_bitmap_1, 100));
    TRY_OR_FAIL(animation_writer->add_frame_relative_to_last_frame(*rgb_bitmap_2, 200, *rgb_bitmap_1));

    auto encoded_animation = ReadonlyBytes { stream_buffer.data(), stream.offset() };

    auto decoded_animation_plugin = TRY_OR_FAIL(Gfx::WebPImageDecoderPlugin::create(encoded_animation));
    EXPECT(decoded_animation_plugin->is_animated());
    EXPECT_EQ(decoded_animation_plugin->frame_count(), 2u);
    EXPECT_EQ(decoded_animation_plugin->loop_count(), 0u);
    EXPECT_EQ(decoded_animation_plugin->size(), rgb_bitmap_1->size());

    auto frame0 = TRY_OR_FAIL(decoded_animation_plugin->frame(0));
    EXPECT_EQ(frame0.duration, 100);
    expect_bitmaps_equal(*frame0.image, *rgb_bitmap_1);

    auto frame1 = TRY_OR_FAIL(decoded_animation_plugin->frame(1));
    EXPECT_EQ(frame1.duration, 200);
    expect_bitmaps_equal(*frame1.image, *rgb_bitmap_2);
}

TEST_CASE(test_webp_incremental_animation_two_identical_frames)
{
    auto rgb_bitmap = TRY_OR_FAIL(create_test_rgba_bitmap());
    rgb_bitmap = TRY_OR_FAIL(rgb_bitmap->cropped({ 0, 0, 40, 20 })); // Even-sized.

    // 20 kiB is enough for two 47x33 frames.
    auto stream_buffer = TRY_OR_FAIL(ByteBuffer::create_uninitialized(20 * 1024));
    FixedMemoryStream stream { Bytes { stream_buffer } };

    auto animation_writer = TRY_OR_FAIL(Gfx::WebPWriter::start_encoding_animation(stream, rgb_bitmap->size()));

    TRY_OR_FAIL(animation_writer->add_frame(*rgb_bitmap, 100));
    TRY_OR_FAIL(animation_writer->add_frame_relative_to_last_frame(*rgb_bitmap, 200, *rgb_bitmap));

    auto encoded_animation = ReadonlyBytes { stream_buffer.data(), stream.offset() };

    auto decoded_animation_plugin = TRY_OR_FAIL(Gfx::WebPImageDecoderPlugin::create(encoded_animation));
    EXPECT(decoded_animation_plugin->is_animated());
    EXPECT_EQ(decoded_animation_plugin->frame_count(), 2u);
    EXPECT_EQ(decoded_animation_plugin->loop_count(), 0u);
    EXPECT_EQ(decoded_animation_plugin->size(), rgb_bitmap->size());

    auto frame0 = TRY_OR_FAIL(decoded_animation_plugin->frame(0));
    EXPECT_EQ(frame0.duration, 100);
    expect_bitmaps_equal(*frame0.image, *rgb_bitmap);

    auto frame1 = TRY_OR_FAIL(decoded_animation_plugin->frame(1));
    EXPECT_EQ(frame1.duration, 200);
    expect_bitmaps_equal(*frame1.image, *rgb_bitmap);
}
