/*
 * Copyright (c) 2021, the SerenityOS developers.
 * Copyright (c) 2021, Brian Gianforcaro <bgianf@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <YAK/String.h>
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
    auto image = Gfx::load_bmp("/res/html/misc/bmpsuite_files/rgba32-1.bmp");
    auto bmp = Gfx::BMPImageDecoderPlugin((const u8*)&image, sizeof(*image));
    EXPECT(bmp.frame_count());

    EXPECT(!bmp.sniff());
    EXPECT(!bmp.is_animated());
    EXPECT(!bmp.loop_count());

    auto frame = bmp.frame(1);
    EXPECT(frame.duration == 0);
}

TEST_CASE(test_gif)
{
    auto image = Gfx::load_gif("/res/graphics/download-animation.gif");
    auto gif = Gfx::GIFImageDecoderPlugin((const u8*)&image, sizeof(*image));
    EXPECT(gif.frame_count());

    EXPECT(!gif.sniff());
    // FIXME: is_animated() should return true
    // LibGfx::load_gif() returns a bitmap and lies about is_animated()
    EXPECT(!gif.is_animated());
    EXPECT(!gif.loop_count());

    auto frame = gif.frame(1);
    EXPECT(frame.duration == 0);
}

TEST_CASE(test_ico)
{
    // FIXME: Use an ico file
    auto image = Gfx::load_ico("/res/graphics/buggie.png");
    auto ico = Gfx::ICOImageDecoderPlugin((const u8*)&image, sizeof(*image));
    EXPECT(ico.frame_count());

    EXPECT(!ico.sniff());
    EXPECT(!ico.is_animated());
    EXPECT(!ico.loop_count());

    auto frame = ico.frame(1);
    EXPECT(frame.duration == 0);
}

TEST_CASE(test_jpg)
{
    auto image = Gfx::load_jpg("/res/html/misc/bmpsuite_files/rgb24.jpg");
    auto jpg = Gfx::JPGImageDecoderPlugin((const u8*)&image, sizeof(*image));
    EXPECT(jpg.frame_count());

    EXPECT(!jpg.sniff());
    EXPECT(!jpg.is_animated());
    EXPECT(!jpg.loop_count());

    auto frame = jpg.frame(1);
    EXPECT(frame.duration == 0);
}

TEST_CASE(test_pbm)
{
    auto image = Gfx::load_pbm("/res/html/misc/pbmsuite_files/buggie-raw.pbm");
    auto pbm = Gfx::PBMImageDecoderPlugin((const u8*)&image, sizeof(*image));
    EXPECT(pbm.frame_count());

    EXPECT(!pbm.sniff());
    EXPECT(!pbm.is_animated());
    EXPECT(!pbm.loop_count());

    auto frame = pbm.frame(1);
    EXPECT(frame.duration == 0);
}

TEST_CASE(test_pgm)
{
    auto image = Gfx::load_pbm("/res/html/misc/pbmsuite_files/buggie-raw.pbm");
    auto pgm = Gfx::PGMImageDecoderPlugin((const u8*)&image, sizeof(*image));
    EXPECT(pgm.frame_count());

    EXPECT(!pgm.sniff());
    EXPECT(!pgm.is_animated());
    EXPECT(!pgm.loop_count());

    auto frame = pgm.frame(1);
    EXPECT(frame.duration == 0);
}

TEST_CASE(test_png)
{
    auto image = Gfx::load_png("/res/graphics/buggie.png");
    auto png = Gfx::PNGImageDecoderPlugin((const u8*)&image, sizeof(*image));
    EXPECT(png.frame_count());

    EXPECT(!png.sniff());
    EXPECT(!png.is_animated());
    EXPECT(!png.loop_count());

    auto frame = png.frame(1);
    EXPECT(frame.duration == 0);
}

TEST_CASE(test_ppm)
{
    auto image = Gfx::load_ppm("/res/html/misc/ppmsuite_files/buggie-raw.ppm");
    auto ppm = Gfx::PPMImageDecoderPlugin((const u8*)&image, sizeof(*image));
    EXPECT(ppm.frame_count());

    EXPECT(!ppm.sniff());
    EXPECT(!ppm.is_animated());
    EXPECT(!ppm.loop_count());

    auto frame = ppm.frame(1);
    EXPECT(frame.duration == 0);
}
