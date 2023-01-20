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
#include <LibGfx/JPGLoader.h>
#include <LibGfx/PBMLoader.h>
#include <LibGfx/PGMLoader.h>
#include <LibGfx/PNGLoader.h>
#include <LibGfx/PPMLoader.h>
#include <LibGfx/TGALoader.h>
#include <LibTest/TestCase.h>
#include <stdio.h>
#include <string.h>

TEST_CASE(test_bmp)
{
    auto file = Core::MappedFile::map("/res/html/misc/bmpsuite_files/rgba32-1.bmp"sv).release_value();
    EXPECT_EQ(MUST(Gfx::BMPImageDecoderPlugin::sniff({ (u8 const*)file->data(), file->size() })), true);
    auto plugin_decoder_or_error = Gfx::BMPImageDecoderPlugin::create({ (u8 const*)file->data(), file->size() });
    EXPECT(!plugin_decoder_or_error.is_error());
    auto plugin_decoder = plugin_decoder_or_error.release_value();
    EXPECT_EQ(plugin_decoder->initialize(), true);

    EXPECT(plugin_decoder->frame_count());
    EXPECT(!plugin_decoder->is_animated());
    EXPECT(!plugin_decoder->loop_count());

    auto frame = plugin_decoder->frame(0).release_value_but_fixme_should_propagate_errors();
    EXPECT(frame.duration == 0);
}

TEST_CASE(test_gif)
{
    auto file = Core::MappedFile::map("/res/graphics/download-animation.gif"sv).release_value();
    EXPECT_EQ(MUST(Gfx::GIFImageDecoderPlugin::sniff({ (u8 const*)file->data(), file->size() })), true);
    auto plugin_decoder_or_error = Gfx::GIFImageDecoderPlugin::create({ (u8 const*)file->data(), file->size() });
    EXPECT(!plugin_decoder_or_error.is_error());
    auto plugin_decoder = plugin_decoder_or_error.release_value();
    EXPECT_EQ(plugin_decoder->initialize(), true);

    EXPECT(plugin_decoder->frame_count());
    EXPECT(!plugin_decoder->is_animated());
    EXPECT(!plugin_decoder->loop_count());

    auto frame = plugin_decoder->frame(1).release_value_but_fixme_should_propagate_errors();
    EXPECT(frame.duration == 400);
}

TEST_CASE(test_not_ico)
{
    auto file = Core::MappedFile::map("/res/graphics/buggie.png"sv).release_value();
    EXPECT_EQ(MUST(Gfx::ICOImageDecoderPlugin::sniff({ (u8 const*)file->data(), file->size() })), false);
    auto plugin_decoder_or_error = Gfx::ICOImageDecoderPlugin::create({ (u8 const*)file->data(), file->size() });
    EXPECT(!plugin_decoder_or_error.is_error());
    auto plugin_decoder = plugin_decoder_or_error.release_value();
    EXPECT_EQ(plugin_decoder->initialize(), false);

    EXPECT(plugin_decoder->frame_count());
    EXPECT(!plugin_decoder->is_animated());
    EXPECT(!plugin_decoder->loop_count());

    EXPECT(plugin_decoder->frame(0).is_error());
}

TEST_CASE(test_bmp_embedded_in_ico)
{
    auto file = Core::MappedFile::map("/res/icons/16x16/serenity.ico"sv).release_value();
    EXPECT_EQ(MUST(Gfx::ICOImageDecoderPlugin::sniff({ (u8 const*)file->data(), file->size() })), true);
    auto plugin_decoder_or_error = Gfx::ICOImageDecoderPlugin::create({ (u8 const*)file->data(), file->size() });
    EXPECT(!plugin_decoder_or_error.is_error());
    auto plugin_decoder = plugin_decoder_or_error.release_value();
    EXPECT_EQ(plugin_decoder->initialize(), true);

    EXPECT(plugin_decoder->frame_count());
    EXPECT(!plugin_decoder->is_animated());
    EXPECT(!plugin_decoder->loop_count());

    EXPECT(!plugin_decoder->frame(0).is_error());
}

TEST_CASE(test_jpg)
{
    auto file = Core::MappedFile::map("/res/html/misc/bmpsuite_files/rgb24.jpg"sv).release_value();
    EXPECT_EQ(MUST(Gfx::JPGImageDecoderPlugin::sniff({ (u8 const*)file->data(), file->size() })), true);
    auto plugin_decoder_or_error = Gfx::JPGImageDecoderPlugin::create({ (u8 const*)file->data(), file->size() });
    EXPECT(!plugin_decoder_or_error.is_error());
    auto plugin_decoder = plugin_decoder_or_error.release_value();
    EXPECT_EQ(plugin_decoder->initialize(), true);

    EXPECT(plugin_decoder->frame_count());
    EXPECT(!plugin_decoder->is_animated());
    EXPECT(!plugin_decoder->loop_count());

    EXPECT(!plugin_decoder->frame(0).is_error());

    auto frame = plugin_decoder->frame(0).release_value_but_fixme_should_propagate_errors();
    EXPECT(frame.duration == 0);
}

TEST_CASE(test_pbm)
{
    auto file = Core::MappedFile::map("/res/html/misc/pbmsuite_files/buggie-raw.pbm"sv).release_value();
    EXPECT_EQ(MUST(Gfx::PBMImageDecoderPlugin::sniff({ (u8 const*)file->data(), file->size() })), true);
    auto plugin_decoder_or_error = Gfx::PBMImageDecoderPlugin::create({ (u8 const*)file->data(), file->size() });
    EXPECT(!plugin_decoder_or_error.is_error());
    auto plugin_decoder = plugin_decoder_or_error.release_value();
    EXPECT_EQ(plugin_decoder->initialize(), true);

    EXPECT(plugin_decoder->frame_count());
    EXPECT(!plugin_decoder->is_animated());
    EXPECT(!plugin_decoder->loop_count());

    EXPECT(!plugin_decoder->frame(0).is_error());

    auto frame = plugin_decoder->frame(0).release_value_but_fixme_should_propagate_errors();
    EXPECT(frame.duration == 0);
}

TEST_CASE(test_pgm)
{
    auto file = Core::MappedFile::map("/res/html/misc/pgmsuite_files/buggie-raw.pgm"sv).release_value();
    EXPECT_EQ(MUST(Gfx::PGMImageDecoderPlugin::sniff({ (u8 const*)file->data(), file->size() })), true);
    auto plugin_decoder_or_error = Gfx::PGMImageDecoderPlugin::create({ (u8 const*)file->data(), file->size() });
    EXPECT(!plugin_decoder_or_error.is_error());
    auto plugin_decoder = plugin_decoder_or_error.release_value();
    EXPECT_EQ(plugin_decoder->initialize(), true);

    EXPECT(plugin_decoder->frame_count());
    EXPECT(!plugin_decoder->is_animated());
    EXPECT(!plugin_decoder->loop_count());

    EXPECT(!plugin_decoder->frame(0).is_error());

    auto frame = plugin_decoder->frame(0).release_value_but_fixme_should_propagate_errors();
    EXPECT(frame.duration == 0);
}

TEST_CASE(test_png)
{
    auto file = Core::MappedFile::map("/res/graphics/buggie.png"sv).release_value();
    EXPECT_EQ(MUST(Gfx::PNGImageDecoderPlugin::sniff({ (u8 const*)file->data(), file->size() })), true);
    auto plugin_decoder_or_error = Gfx::PNGImageDecoderPlugin::create({ (u8 const*)file->data(), file->size() });
    EXPECT(!plugin_decoder_or_error.is_error());
    auto plugin_decoder = plugin_decoder_or_error.release_value();
    EXPECT_EQ(plugin_decoder->initialize(), true);

    EXPECT(plugin_decoder->frame_count());
    EXPECT(!plugin_decoder->is_animated());
    EXPECT(!plugin_decoder->loop_count());

    EXPECT(!plugin_decoder->frame(0).is_error());

    auto frame = plugin_decoder->frame(0).release_value_but_fixme_should_propagate_errors();
    EXPECT(frame.duration == 0);
}

TEST_CASE(test_ppm)
{
    auto file = Core::MappedFile::map("/res/html/misc/ppmsuite_files/buggie-raw.ppm"sv).release_value();
    EXPECT_EQ(MUST(Gfx::PPMImageDecoderPlugin::sniff({ (u8 const*)file->data(), file->size() })), true);
    auto plugin_decoder_or_error = Gfx::PPMImageDecoderPlugin::create({ (u8 const*)file->data(), file->size() });
    EXPECT(!plugin_decoder_or_error.is_error());
    auto plugin_decoder = plugin_decoder_or_error.release_value();
    EXPECT_EQ(plugin_decoder->initialize(), true);

    EXPECT(plugin_decoder->frame_count());
    EXPECT(!plugin_decoder->is_animated());
    EXPECT(!plugin_decoder->loop_count());

    EXPECT(!plugin_decoder->frame(0).is_error());

    auto frame = plugin_decoder->frame(0).release_value_but_fixme_should_propagate_errors();
    EXPECT(frame.duration == 0);
}

TEST_CASE(test_targa_bottom_left)
{
    auto file = Core::MappedFile::map("/res/html/misc/targasuite_files/buggie-bottom-left-uncompressed.tga"sv).release_value();
    EXPECT_EQ(MUST(Gfx::TGAImageDecoderPlugin::validate_before_create({ (u8 const*)file->data(), file->size() })), true);
    auto plugin_decoder_or_error = Gfx::TGAImageDecoderPlugin::create({ (u8 const*)file->data(), file->size() });
    EXPECT(!plugin_decoder_or_error.is_error());
    auto plugin_decoder = plugin_decoder_or_error.release_value();
    EXPECT_EQ(plugin_decoder->initialize(), true);

    EXPECT_EQ(plugin_decoder->frame_count(), 1u);
    EXPECT(!plugin_decoder->is_animated());
    EXPECT(!plugin_decoder->loop_count());

    EXPECT(!plugin_decoder->frame(0).is_error());

    auto frame_or_error = plugin_decoder->frame(0);
    EXPECT(!frame_or_error.is_error());
    auto frame = frame_or_error.release_value();
    EXPECT(frame.duration == 0);
}

TEST_CASE(test_targa_top_left)
{
    auto file = Core::MappedFile::map("/res/html/misc/targasuite_files/buggie-top-left-uncompressed.tga"sv).release_value();
    EXPECT_EQ(MUST(Gfx::TGAImageDecoderPlugin::validate_before_create({ (u8 const*)file->data(), file->size() })), true);
    auto plugin_decoder_or_error = Gfx::TGAImageDecoderPlugin::create({ (u8 const*)file->data(), file->size() });
    EXPECT(!plugin_decoder_or_error.is_error());
    auto plugin_decoder = plugin_decoder_or_error.release_value();
    EXPECT_EQ(plugin_decoder->initialize(), true);

    EXPECT_EQ(plugin_decoder->frame_count(), 1u);
    EXPECT(!plugin_decoder->is_animated());
    EXPECT(!plugin_decoder->loop_count());

    EXPECT(!plugin_decoder->frame(0).is_error());

    auto frame_or_error = plugin_decoder->frame(0);
    EXPECT(!frame_or_error.is_error());
    auto frame = frame_or_error.release_value();
    EXPECT(frame.duration == 0);
}

TEST_CASE(test_targa_bottom_left_compressed)
{
    auto file = Core::MappedFile::map("/res/html/misc/targasuite_files/buggie-bottom-left-compressed.tga"sv).release_value();
    EXPECT_EQ(MUST(Gfx::TGAImageDecoderPlugin::validate_before_create({ (u8 const*)file->data(), file->size() })), true);
    auto plugin_decoder_or_error = Gfx::TGAImageDecoderPlugin::create({ (u8 const*)file->data(), file->size() });
    EXPECT(!plugin_decoder_or_error.is_error());
    auto plugin_decoder = plugin_decoder_or_error.release_value();
    EXPECT_EQ(plugin_decoder->initialize(), true);

    EXPECT_EQ(plugin_decoder->frame_count(), 1u);
    EXPECT(!plugin_decoder->is_animated());
    EXPECT(!plugin_decoder->loop_count());

    EXPECT(!plugin_decoder->frame(0).is_error());

    auto frame_or_error = plugin_decoder->frame(0);
    EXPECT(!frame_or_error.is_error());
    auto frame = frame_or_error.release_value();
    EXPECT(frame.duration == 0);
}

TEST_CASE(test_targa_top_left_compressed)
{
    auto file = Core::MappedFile::map("/res/html/misc/targasuite_files/buggie-top-left-compressed.tga"sv).release_value();
    EXPECT_EQ(MUST(Gfx::TGAImageDecoderPlugin::validate_before_create({ (u8 const*)file->data(), file->size() })), true);
    auto plugin_decoder_or_error = Gfx::TGAImageDecoderPlugin::create({ (u8 const*)file->data(), file->size() });
    EXPECT(!plugin_decoder_or_error.is_error());
    auto plugin_decoder = plugin_decoder_or_error.release_value();
    EXPECT_EQ(plugin_decoder->initialize(), true);

    EXPECT_EQ(plugin_decoder->frame_count(), 1u);
    EXPECT(!plugin_decoder->is_animated());
    EXPECT(!plugin_decoder->loop_count());

    EXPECT(!plugin_decoder->frame(0).is_error());

    auto frame_or_error = plugin_decoder->frame(0);
    EXPECT(!frame_or_error.is_error());
    auto frame = frame_or_error.release_value();
    EXPECT(frame.duration == 0);
}
