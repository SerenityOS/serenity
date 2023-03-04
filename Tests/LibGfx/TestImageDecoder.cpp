/*
 * Copyright (c) 2021, the SerenityOS developers.
 * Copyright (c) 2021, Brian Gianforcaro <bgianf@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/DeprecatedString.h>
#include <LibCore/MappedFile.h>
#include <LibGfx/BMPLoader.h>
#include <LibGfx/GIFLoader.h>
#include <LibGfx/ICOLoader.h>
#include <LibGfx/ImageDecoder.h>
#include <LibGfx/JPEGLoader.h>
#include <LibGfx/PBMLoader.h>
#include <LibGfx/PGMLoader.h>
#include <LibGfx/PNGLoader.h>
#include <LibGfx/PPMLoader.h>
#include <LibGfx/TGALoader.h>
#include <LibGfx/WebPLoader.h>
#include <LibTest/TestCase.h>
#include <stdio.h>
#include <string.h>

#ifdef AK_OS_SERENITY
#    define TEST_INPUT(x) ("/usr/Tests/LibGfx/test-inputs/" x)
#else
#    define TEST_INPUT(x) ("test-inputs/" x)
#endif

TEST_CASE(test_bmp)
{
    auto file = MUST(Core::MappedFile::map(TEST_INPUT("rgba32-1.bmp"sv)));
    EXPECT(Gfx::BMPImageDecoderPlugin::sniff(file->bytes()));
    auto plugin_decoder = MUST(Gfx::BMPImageDecoderPlugin::create(file->bytes()));
    EXPECT(plugin_decoder->initialize());

    EXPECT(plugin_decoder->frame_count());
    EXPECT(!plugin_decoder->is_animated());
    EXPECT(!plugin_decoder->loop_count());

    auto frame = MUST(plugin_decoder->frame(0));
    EXPECT(frame.duration == 0);
}

TEST_CASE(test_gif)
{
    auto file = MUST(Core::MappedFile::map(TEST_INPUT("download-animation.gif"sv)));
    EXPECT(Gfx::GIFImageDecoderPlugin::sniff(file->bytes()));
    auto plugin_decoder = MUST(Gfx::GIFImageDecoderPlugin::create(file->bytes()));
    EXPECT(plugin_decoder->initialize());

    EXPECT(plugin_decoder->frame_count());
    EXPECT(plugin_decoder->is_animated());
    EXPECT(!plugin_decoder->loop_count());

    auto frame = MUST(plugin_decoder->frame(1));
    EXPECT(frame.duration == 400);
}

TEST_CASE(test_not_ico)
{
    auto file = MUST(Core::MappedFile::map(TEST_INPUT("buggie.png"sv)));
    EXPECT(!Gfx::ICOImageDecoderPlugin::sniff(file->bytes()));
    auto plugin_decoder = MUST(Gfx::ICOImageDecoderPlugin::create(file->bytes()));
    EXPECT(!plugin_decoder->initialize());

    EXPECT(plugin_decoder->frame_count());
    EXPECT(!plugin_decoder->is_animated());
    EXPECT(!plugin_decoder->loop_count());

    EXPECT(plugin_decoder->frame(0).is_error());
}

TEST_CASE(test_bmp_embedded_in_ico)
{
    auto file = MUST(Core::MappedFile::map(TEST_INPUT("serenity.ico"sv)));
    EXPECT(Gfx::ICOImageDecoderPlugin::sniff(file->bytes()));
    auto plugin_decoder = MUST(Gfx::ICOImageDecoderPlugin::create(file->bytes()));
    EXPECT(plugin_decoder->initialize());

    EXPECT(plugin_decoder->frame_count());
    EXPECT(!plugin_decoder->is_animated());
    EXPECT(!plugin_decoder->loop_count());

    EXPECT(!plugin_decoder->frame(0).is_error());
}

TEST_CASE(test_jpeg_sof0_one_scan)
{
    auto file = MUST(Core::MappedFile::map(TEST_INPUT("rgb24.jpg"sv)));
    EXPECT(Gfx::JPEGImageDecoderPlugin::sniff(file->bytes()));
    auto plugin_decoder = MUST(Gfx::JPEGImageDecoderPlugin::create(file->bytes()));
    EXPECT(plugin_decoder->initialize());

    EXPECT(plugin_decoder->frame_count());
    EXPECT(!plugin_decoder->is_animated());
    EXPECT(!plugin_decoder->loop_count());

    auto frame = MUST(plugin_decoder->frame(0));
    EXPECT(frame.duration == 0);
}

TEST_CASE(test_jpeg_sof0_several_scans)
{
    auto file = MUST(Core::MappedFile::map(TEST_INPUT("several_scans.jpg"sv)));
    EXPECT(Gfx::JPEGImageDecoderPlugin::sniff(file->bytes()));
    auto plugin_decoder = MUST(Gfx::JPEGImageDecoderPlugin::create(file->bytes()));
    EXPECT(plugin_decoder->initialize());

    auto frame = MUST(plugin_decoder->frame(0));
    EXPECT_EQ(frame.image->size(), Gfx::IntSize(592, 800));
}

TEST_CASE(test_jpeg_sof2_spectral_selection)
{
    auto file = MUST(Core::MappedFile::map(TEST_INPUT("spectral_selection.jpg"sv)));
    EXPECT(Gfx::JPEGImageDecoderPlugin::sniff(file->bytes()));
    auto plugin_decoder = MUST(Gfx::JPEGImageDecoderPlugin::create(file->bytes()));
    EXPECT(plugin_decoder->initialize());

    auto frame = MUST(plugin_decoder->frame(0));
    EXPECT_EQ(frame.image->size(), Gfx::IntSize(592, 800));
}

TEST_CASE(test_pbm)
{
    auto file = MUST(Core::MappedFile::map(TEST_INPUT("buggie-raw.pbm"sv)));
    EXPECT(Gfx::PBMImageDecoderPlugin::sniff(file->bytes()));
    auto plugin_decoder = MUST(Gfx::PBMImageDecoderPlugin::create(file->bytes()));
    EXPECT(plugin_decoder->initialize());

    EXPECT(plugin_decoder->frame_count());
    EXPECT(!plugin_decoder->is_animated());
    EXPECT(!plugin_decoder->loop_count());

    auto frame = MUST(plugin_decoder->frame(0));
    EXPECT(frame.duration == 0);
}

TEST_CASE(test_pgm)
{
    auto file = MUST(Core::MappedFile::map(TEST_INPUT("buggie-raw.pgm"sv)));
    EXPECT(Gfx::PGMImageDecoderPlugin::sniff(file->bytes()));
    auto plugin_decoder = MUST(Gfx::PGMImageDecoderPlugin::create(file->bytes()));
    EXPECT(plugin_decoder->initialize());

    EXPECT(plugin_decoder->frame_count());
    EXPECT(!plugin_decoder->is_animated());
    EXPECT(!plugin_decoder->loop_count());

    auto frame = MUST(plugin_decoder->frame(0));
    EXPECT(frame.duration == 0);
}

TEST_CASE(test_png)
{
    auto file = MUST(Core::MappedFile::map(TEST_INPUT("buggie.png"sv)));
    EXPECT(Gfx::PNGImageDecoderPlugin::sniff(file->bytes()));
    auto plugin_decoder = MUST(Gfx::PNGImageDecoderPlugin::create(file->bytes()));
    EXPECT(plugin_decoder->initialize());

    EXPECT(plugin_decoder->frame_count());
    EXPECT(!plugin_decoder->is_animated());
    EXPECT(!plugin_decoder->loop_count());

    auto frame = MUST(plugin_decoder->frame(0));
    EXPECT(frame.duration == 0);
}

TEST_CASE(test_ppm)
{
    auto file = MUST(Core::MappedFile::map(TEST_INPUT("buggie-raw.ppm"sv)));
    EXPECT(Gfx::PPMImageDecoderPlugin::sniff(file->bytes()));
    auto plugin_decoder = MUST(Gfx::PPMImageDecoderPlugin::create(file->bytes()));
    EXPECT(plugin_decoder->initialize());

    EXPECT(plugin_decoder->frame_count());
    EXPECT(!plugin_decoder->is_animated());
    EXPECT(!plugin_decoder->loop_count());

    auto frame = MUST(plugin_decoder->frame(0));
    EXPECT(frame.duration == 0);
}

TEST_CASE(test_targa_bottom_left)
{
    auto file = MUST(Core::MappedFile::map(TEST_INPUT("buggie-bottom-left-uncompressed.tga"sv)));
    EXPECT(MUST(Gfx::TGAImageDecoderPlugin::validate_before_create(file->bytes())));
    auto plugin_decoder = MUST(Gfx::TGAImageDecoderPlugin::create(file->bytes()));
    EXPECT(plugin_decoder->initialize());

    EXPECT_EQ(plugin_decoder->frame_count(), 1u);
    EXPECT(!plugin_decoder->is_animated());
    EXPECT(!plugin_decoder->loop_count());

    auto frame = MUST(plugin_decoder->frame(0));
    EXPECT(frame.duration == 0);
}

TEST_CASE(test_targa_top_left)
{
    auto file = MUST(Core::MappedFile::map(TEST_INPUT("buggie-top-left-uncompressed.tga"sv)));
    EXPECT(MUST(Gfx::TGAImageDecoderPlugin::validate_before_create(file->bytes())));
    auto plugin_decoder = MUST(Gfx::TGAImageDecoderPlugin::create(file->bytes()));
    EXPECT(plugin_decoder->initialize());

    EXPECT_EQ(plugin_decoder->frame_count(), 1u);
    EXPECT(!plugin_decoder->is_animated());
    EXPECT(!plugin_decoder->loop_count());

    auto frame = MUST(plugin_decoder->frame(0));
    EXPECT(frame.duration == 0);
}

TEST_CASE(test_targa_bottom_left_compressed)
{
    auto file = MUST(Core::MappedFile::map(TEST_INPUT("buggie-bottom-left-compressed.tga"sv)));
    EXPECT(MUST(Gfx::TGAImageDecoderPlugin::validate_before_create(file->bytes())));
    auto plugin_decoder = MUST(Gfx::TGAImageDecoderPlugin::create(file->bytes()));
    EXPECT(plugin_decoder->initialize());

    EXPECT_EQ(plugin_decoder->frame_count(), 1u);
    EXPECT(!plugin_decoder->is_animated());
    EXPECT(!plugin_decoder->loop_count());

    auto frame = MUST(plugin_decoder->frame(0));
    EXPECT(frame.duration == 0);
}

TEST_CASE(test_targa_top_left_compressed)
{
    auto file = MUST(Core::MappedFile::map(TEST_INPUT("buggie-top-left-compressed.tga"sv)));
    EXPECT(MUST(Gfx::TGAImageDecoderPlugin::validate_before_create(file->bytes())));
    auto plugin_decoder = MUST(Gfx::TGAImageDecoderPlugin::create(file->bytes()));
    EXPECT(plugin_decoder->initialize());

    EXPECT_EQ(plugin_decoder->frame_count(), 1u);
    EXPECT(!plugin_decoder->is_animated());
    EXPECT(!plugin_decoder->loop_count());

    auto frame = MUST(plugin_decoder->frame(0));
    EXPECT(frame.duration == 0);
}

TEST_CASE(test_webp_simple_lossy)
{
    auto file = MUST(Core::MappedFile::map(TEST_INPUT("simple-vp8.webp"sv)));
    EXPECT(Gfx::WebPImageDecoderPlugin::sniff(file->bytes()));
    auto plugin_decoder = MUST(Gfx::WebPImageDecoderPlugin::create(file->bytes()));
    EXPECT(plugin_decoder->initialize());

    EXPECT_EQ(plugin_decoder->frame_count(), 1u);
    EXPECT(!plugin_decoder->is_animated());
    EXPECT(!plugin_decoder->loop_count());

    EXPECT_EQ(plugin_decoder->size(), Gfx::IntSize(240, 240));
}

TEST_CASE(test_webp_simple_lossless)
{
    auto file = MUST(Core::MappedFile::map(TEST_INPUT("simple-vp8l.webp"sv)));
    EXPECT(Gfx::WebPImageDecoderPlugin::sniff(file->bytes()));
    auto plugin_decoder = MUST(Gfx::WebPImageDecoderPlugin::create(file->bytes()));
    EXPECT(plugin_decoder->initialize());

    EXPECT_EQ(plugin_decoder->frame_count(), 1u);
    EXPECT(!plugin_decoder->is_animated());
    EXPECT(!plugin_decoder->loop_count());

    EXPECT_EQ(plugin_decoder->size(), Gfx::IntSize(386, 395));
}

TEST_CASE(test_webp_extended_lossy)
{
    auto file = MUST(Core::MappedFile::map(TEST_INPUT("extended-lossy.webp"sv)));
    EXPECT(Gfx::WebPImageDecoderPlugin::sniff(file->bytes()));
    auto plugin_decoder = MUST(Gfx::WebPImageDecoderPlugin::create(file->bytes()));
    EXPECT(plugin_decoder->initialize());

    EXPECT_EQ(plugin_decoder->frame_count(), 1u);
    EXPECT(!plugin_decoder->is_animated());
    EXPECT(!plugin_decoder->loop_count());

    EXPECT_EQ(plugin_decoder->size(), Gfx::IntSize(417, 223));
}

TEST_CASE(test_webp_extended_lossless)
{
    auto file = MUST(Core::MappedFile::map(TEST_INPUT("extended-lossless.webp"sv)));
    EXPECT(Gfx::WebPImageDecoderPlugin::sniff(file->bytes()));
    auto plugin_decoder = MUST(Gfx::WebPImageDecoderPlugin::create(file->bytes()));
    EXPECT(plugin_decoder->initialize());

    EXPECT_EQ(plugin_decoder->frame_count(), 1u);
    EXPECT(!plugin_decoder->is_animated());
    EXPECT(!plugin_decoder->loop_count());

    EXPECT_EQ(plugin_decoder->size(), Gfx::IntSize(417, 223));
}

TEST_CASE(test_webp_extended_lossless_animated)
{
    auto file = MUST(Core::MappedFile::map(TEST_INPUT("extended-lossless-animated.webp"sv)));
    EXPECT(Gfx::WebPImageDecoderPlugin::sniff(file->bytes()));
    auto plugin_decoder = MUST(Gfx::WebPImageDecoderPlugin::create(file->bytes()));
    EXPECT(plugin_decoder->initialize());

    EXPECT_EQ(plugin_decoder->frame_count(), 8u);
    EXPECT(plugin_decoder->is_animated());
    EXPECT_EQ(plugin_decoder->loop_count(), 42u);

    EXPECT_EQ(plugin_decoder->size(), Gfx::IntSize(990, 1050));
}
