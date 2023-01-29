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
    auto file = MUST(Core::MappedFile::map("/res/html/misc/bmpsuite_files/rgba32-1.bmp"sv));
    EXPECT(MUST(Gfx::BMPImageDecoderPlugin::sniff(file->bytes())));
    auto plugin_decoder = MUST(Gfx::BMPImageDecoderPlugin::create(file->bytes()));
    EXPECT(plugin_decoder->initialize());

    EXPECT(plugin_decoder->frame_count());
    EXPECT(!plugin_decoder->is_animated());
    EXPECT(!plugin_decoder->loop_count());

    auto frame = plugin_decoder->frame(0).release_value_but_fixme_should_propagate_errors();
    EXPECT(frame.duration == 0);
}

TEST_CASE(test_gif)
{
    auto file = MUST(Core::MappedFile::map("/res/graphics/download-animation.gif"sv));
    EXPECT(MUST(Gfx::GIFImageDecoderPlugin::sniff(file->bytes())));
    auto plugin_decoder = MUST(Gfx::GIFImageDecoderPlugin::create(file->bytes()));
    EXPECT(plugin_decoder->initialize());

    EXPECT(plugin_decoder->frame_count());
    EXPECT(plugin_decoder->is_animated());
    EXPECT(!plugin_decoder->loop_count());

    auto frame = plugin_decoder->frame(1).release_value_but_fixme_should_propagate_errors();
    EXPECT(frame.duration == 400);
}

TEST_CASE(test_not_ico)
{
    auto file = MUST(Core::MappedFile::map("/res/graphics/buggie.png"sv));
    EXPECT(!MUST(Gfx::ICOImageDecoderPlugin::sniff(file->bytes())));
    auto plugin_decoder = MUST(Gfx::ICOImageDecoderPlugin::create(file->bytes()));
    EXPECT(!plugin_decoder->initialize());

    EXPECT(plugin_decoder->frame_count());
    EXPECT(!plugin_decoder->is_animated());
    EXPECT(!plugin_decoder->loop_count());

    EXPECT(plugin_decoder->frame(0).is_error());
}

TEST_CASE(test_bmp_embedded_in_ico)
{
    auto file = MUST(Core::MappedFile::map("/res/icons/16x16/serenity.ico"sv));
    EXPECT(MUST(Gfx::ICOImageDecoderPlugin::sniff(file->bytes())));
    auto plugin_decoder = MUST(Gfx::ICOImageDecoderPlugin::create(file->bytes()));
    EXPECT(plugin_decoder->initialize());

    EXPECT(plugin_decoder->frame_count());
    EXPECT(!plugin_decoder->is_animated());
    EXPECT(!plugin_decoder->loop_count());

    EXPECT(!plugin_decoder->frame(0).is_error());
}

TEST_CASE(test_jpg)
{
    auto file = MUST(Core::MappedFile::map("/res/html/misc/bmpsuite_files/rgb24.jpg"sv));
    EXPECT(MUST(Gfx::JPGImageDecoderPlugin::sniff(file->bytes())));
    auto plugin_decoder = MUST(Gfx::JPGImageDecoderPlugin::create(file->bytes()));
    EXPECT(plugin_decoder->initialize());

    EXPECT(plugin_decoder->frame_count());
    EXPECT(!plugin_decoder->is_animated());
    EXPECT(!plugin_decoder->loop_count());

    EXPECT(!plugin_decoder->frame(0).is_error());

    auto frame = plugin_decoder->frame(0).release_value_but_fixme_should_propagate_errors();
    EXPECT(frame.duration == 0);
}

TEST_CASE(test_pbm)
{
    auto file = MUST(Core::MappedFile::map("/res/html/misc/pbmsuite_files/buggie-raw.pbm"sv));
    EXPECT(MUST(Gfx::PBMImageDecoderPlugin::sniff(file->bytes())));
    auto plugin_decoder = MUST(Gfx::PBMImageDecoderPlugin::create(file->bytes()));
    EXPECT(plugin_decoder->initialize());

    EXPECT(plugin_decoder->frame_count());
    EXPECT(!plugin_decoder->is_animated());
    EXPECT(!plugin_decoder->loop_count());

    EXPECT(!plugin_decoder->frame(0).is_error());

    auto frame = plugin_decoder->frame(0).release_value_but_fixme_should_propagate_errors();
    EXPECT(frame.duration == 0);
}

TEST_CASE(test_pgm)
{
    auto file = MUST(Core::MappedFile::map("/res/html/misc/pgmsuite_files/buggie-raw.pgm"sv));
    EXPECT(MUST(Gfx::PGMImageDecoderPlugin::sniff(file->bytes())));
    auto plugin_decoder = MUST(Gfx::PGMImageDecoderPlugin::create(file->bytes()));
    EXPECT(plugin_decoder->initialize());

    EXPECT(plugin_decoder->frame_count());
    EXPECT(!plugin_decoder->is_animated());
    EXPECT(!plugin_decoder->loop_count());

    EXPECT(!plugin_decoder->frame(0).is_error());

    auto frame = plugin_decoder->frame(0).release_value_but_fixme_should_propagate_errors();
    EXPECT(frame.duration == 0);
}

TEST_CASE(test_png)
{
    auto file = MUST(Core::MappedFile::map("/res/graphics/buggie.png"sv));
    EXPECT(MUST(Gfx::PNGImageDecoderPlugin::sniff(file->bytes())));
    auto plugin_decoder = MUST(Gfx::PNGImageDecoderPlugin::create(file->bytes()));
    EXPECT(plugin_decoder->initialize());

    EXPECT(plugin_decoder->frame_count());
    EXPECT(!plugin_decoder->is_animated());
    EXPECT(!plugin_decoder->loop_count());

    EXPECT(!plugin_decoder->frame(0).is_error());

    auto frame = plugin_decoder->frame(0).release_value_but_fixme_should_propagate_errors();
    EXPECT(frame.duration == 0);
}

TEST_CASE(test_ppm)
{
    auto file = MUST(Core::MappedFile::map("/res/html/misc/ppmsuite_files/buggie-raw.ppm"sv));
    EXPECT(MUST(Gfx::PPMImageDecoderPlugin::sniff(file->bytes())));
    auto plugin_decoder = MUST(Gfx::PPMImageDecoderPlugin::create(file->bytes()));
    EXPECT(plugin_decoder->initialize());

    EXPECT(plugin_decoder->frame_count());
    EXPECT(!plugin_decoder->is_animated());
    EXPECT(!plugin_decoder->loop_count());

    EXPECT(!plugin_decoder->frame(0).is_error());

    auto frame = plugin_decoder->frame(0).release_value_but_fixme_should_propagate_errors();
    EXPECT(frame.duration == 0);
}

TEST_CASE(test_targa_bottom_left)
{
    auto file = MUST(Core::MappedFile::map("/res/html/misc/targasuite_files/buggie-bottom-left-uncompressed.tga"sv));
    EXPECT(MUST(Gfx::TGAImageDecoderPlugin::validate_before_create(file->bytes())));
    auto plugin_decoder = MUST(Gfx::TGAImageDecoderPlugin::create(file->bytes()));
    EXPECT(plugin_decoder->initialize());

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
    auto file = MUST(Core::MappedFile::map("/res/html/misc/targasuite_files/buggie-top-left-uncompressed.tga"sv));
    EXPECT(MUST(Gfx::TGAImageDecoderPlugin::validate_before_create(file->bytes())));
    auto plugin_decoder = MUST(Gfx::TGAImageDecoderPlugin::create(file->bytes()));
    EXPECT(plugin_decoder->initialize());

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
    auto file = MUST(Core::MappedFile::map("/res/html/misc/targasuite_files/buggie-bottom-left-compressed.tga"sv));
    EXPECT(MUST(Gfx::TGAImageDecoderPlugin::validate_before_create(file->bytes())));
    auto plugin_decoder = MUST(Gfx::TGAImageDecoderPlugin::create(file->bytes()));
    EXPECT(plugin_decoder->initialize());

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
    auto file = MUST(Core::MappedFile::map("/res/html/misc/targasuite_files/buggie-top-left-compressed.tga"sv));
    EXPECT(MUST(Gfx::TGAImageDecoderPlugin::validate_before_create(file->bytes())));
    auto plugin_decoder = MUST(Gfx::TGAImageDecoderPlugin::create(file->bytes()));
    EXPECT(plugin_decoder->initialize());

    EXPECT_EQ(plugin_decoder->frame_count(), 1u);
    EXPECT(!plugin_decoder->is_animated());
    EXPECT(!plugin_decoder->loop_count());

    EXPECT(!plugin_decoder->frame(0).is_error());

    auto frame_or_error = plugin_decoder->frame(0);
    EXPECT(!frame_or_error.is_error());
    auto frame = frame_or_error.release_value();
    EXPECT(frame.duration == 0);
}
