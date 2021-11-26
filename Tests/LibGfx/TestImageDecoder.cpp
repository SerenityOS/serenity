/*
 * Copyright (c) 2021, the SerenityOS developers.
 * Copyright (c) 2021, Brian Gianforcaro <bgianf@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/String.h>
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
#include <LibTest/TestCase.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

TEST_CASE(test_bmp)
{
    auto file = Core::MappedFile::map("/res/html/misc/bmpsuite_files/rgba32-1.bmp").release_value();
    auto bmp = Gfx::BMPImageDecoderPlugin((u8 const*)file->data(), file->size());
    EXPECT(bmp.frame_count());

    EXPECT(bmp.sniff());
    EXPECT(!bmp.is_animated());
    EXPECT(!bmp.loop_count());

    auto frame = bmp.frame(0).release_value_but_fixme_should_propagate_errors();
    EXPECT(frame.duration == 0);
}

TEST_CASE(test_gif)
{
    auto file = Core::MappedFile::map("/res/graphics/download-animation.gif").release_value();
    auto gif = Gfx::GIFImageDecoderPlugin((u8 const*)file->data(), file->size());
    EXPECT(gif.frame_count());

    EXPECT(gif.sniff());
    EXPECT(gif.is_animated());
    EXPECT(!gif.loop_count());

    auto frame = gif.frame(1).release_value_but_fixme_should_propagate_errors();
    EXPECT(frame.duration == 400);
}

TEST_CASE(test_ico)
{
    // FIXME: Use an ico file
    auto file = Core::MappedFile::map("/res/graphics/buggie.png").release_value();
    auto ico = Gfx::ICOImageDecoderPlugin((u8 const*)file->data(), file->size());
    EXPECT(ico.frame_count());

    EXPECT(!ico.sniff());
    EXPECT(!ico.is_animated());
    EXPECT(!ico.loop_count());

    EXPECT(ico.frame(0).is_error());
}

TEST_CASE(test_jpg)
{
    auto file = Core::MappedFile::map("/res/html/misc/bmpsuite_files/rgb24.jpg").release_value();
    auto jpg = Gfx::JPGImageDecoderPlugin((u8 const*)file->data(), file->size());
    EXPECT(jpg.frame_count());

    EXPECT(jpg.sniff());
    EXPECT(!jpg.is_animated());
    EXPECT(!jpg.loop_count());

    auto frame = jpg.frame(0).release_value_but_fixme_should_propagate_errors();
    EXPECT(frame.duration == 0);
}

TEST_CASE(test_pbm)
{
    auto file = Core::MappedFile::map("/res/html/misc/pbmsuite_files/buggie-raw.pbm").release_value();
    auto pbm = Gfx::PBMImageDecoderPlugin((u8 const*)file->data(), file->size());
    EXPECT(pbm.frame_count());

    EXPECT(pbm.sniff());
    EXPECT(!pbm.is_animated());
    EXPECT(!pbm.loop_count());

    auto frame = pbm.frame(0).release_value_but_fixme_should_propagate_errors();
    EXPECT(frame.duration == 0);
}

TEST_CASE(test_pgm)
{
    auto file = Core::MappedFile::map("/res/html/misc/pgmsuite_files/buggie-raw.pgm").release_value();
    auto pgm = Gfx::PGMImageDecoderPlugin((u8 const*)file->data(), file->size());
    EXPECT(pgm.frame_count());

    EXPECT(pgm.sniff());
    EXPECT(!pgm.is_animated());
    EXPECT(!pgm.loop_count());

    auto frame = pgm.frame(0).release_value_but_fixme_should_propagate_errors();
    EXPECT(frame.duration == 0);
}

TEST_CASE(test_png)
{
    auto file = Core::MappedFile::map("/res/graphics/buggie.png").release_value();
    auto png = Gfx::PNGImageDecoderPlugin((u8 const*)file->data(), file->size());
    EXPECT(png.frame_count());

    EXPECT(png.sniff());
    EXPECT(!png.is_animated());
    EXPECT(!png.loop_count());

    auto frame = png.frame(0).release_value_but_fixme_should_propagate_errors();
    EXPECT(frame.duration == 0);
}

TEST_CASE(test_ppm)
{
    auto file = Core::MappedFile::map("/res/html/misc/ppmsuite_files/buggie-raw.ppm").release_value();
    auto ppm = Gfx::PPMImageDecoderPlugin((u8 const*)file->data(), file->size());
    EXPECT(ppm.frame_count());

    EXPECT(ppm.sniff());
    EXPECT(!ppm.is_animated());
    EXPECT(!ppm.loop_count());

    auto frame = ppm.frame(0).release_value_but_fixme_should_propagate_errors();
    EXPECT(frame.duration == 0);
}
