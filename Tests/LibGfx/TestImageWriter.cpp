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

TEST_CASE(test_webp_icc)
{
    auto sRGB_icc_profile = MUST(Gfx::ICC::sRGB());
    auto sRGB_icc_data = MUST(Gfx::ICC::encode(sRGB_icc_profile));

    auto rgba_bitmap = TRY_OR_FAIL(create_test_rgba_bitmap());
    auto encoded_rgba_bitmap = TRY_OR_FAIL((encode_bitmap<Gfx::WebPWriter>(rgba_bitmap, Gfx::WebPEncoderOptions { .icc_data = sRGB_icc_data })));

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
    rgb_bitmap_2->scanline(3)[3] = Color::Red;

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
