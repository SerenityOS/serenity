/*
 * Copyright (c) 2021, the SerenityOS developers.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <AK/String.h>
#include <LibGfx/BMPLoader.h>
#include <LibGfx/GIFLoader.h>
#include <LibGfx/ICOLoader.h>
#include <LibGfx/ImageDecoder.h>
#include <LibGfx/JPGLoader.h>
#include <LibGfx/PBMLoader.h>
#include <LibGfx/PGMLoader.h>
#include <LibGfx/PNGLoader.h>
#include <LibGfx/PPMLoader.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

static void test_bmp()
{
    auto image = Gfx::load_bmp("/res/html/misc/bmpsuite_files/rgba32-1.bmp");
    auto bmp = Gfx::BMPImageDecoderPlugin((const u8*)&image, sizeof(*image));
    assert(bmp.frame_count());

    assert(!bmp.sniff());
    assert(!bmp.is_animated());
    assert(!bmp.loop_count());

    auto frame = bmp.frame(1);
    assert(frame.duration == 0);
}

static void test_gif()
{
    auto image = Gfx::load_gif("/res/graphics/download-animation.gif");
    auto gif = Gfx::GIFImageDecoderPlugin((const u8*)&image, sizeof(*image));
    assert(gif.frame_count());

    assert(!gif.sniff());
    // FIXME: is_animated() should return true
    // LibGfx::load_gif() returns a bitmap and lies about is_animated()
    assert(!gif.is_animated());
    assert(!gif.loop_count());

    auto frame = gif.frame(1);
    assert(frame.duration == 0);
}

static void test_ico()
{
    // FIXME: Use an ico file
    auto image = Gfx::load_ico("/res/graphics/buggie.png");
    auto ico = Gfx::ICOImageDecoderPlugin((const u8*)&image, sizeof(*image));
    assert(ico.frame_count());

    assert(!ico.sniff());
    assert(!ico.is_animated());
    assert(!ico.loop_count());

    auto frame = ico.frame(1);
    assert(frame.duration == 0);
}

static void test_jpg()
{
    auto image = Gfx::load_jpg("/res/html/misc/bmpsuite_files/rgb24.jpg");
    auto jpg = Gfx::JPGImageDecoderPlugin((const u8*)&image, sizeof(*image));
    assert(jpg.frame_count());

    assert(!jpg.sniff());
    assert(!jpg.is_animated());
    assert(!jpg.loop_count());

    auto frame = jpg.frame(1);
    assert(frame.duration == 0);
}

static void test_pbm()
{
    auto image = Gfx::load_pbm("/res/html/misc/pbmsuite_files/buggie-raw.pbm");
    auto pbm = Gfx::PBMImageDecoderPlugin((const u8*)&image, sizeof(*image));
    assert(pbm.frame_count());

    assert(!pbm.sniff());
    assert(!pbm.is_animated());
    assert(!pbm.loop_count());

    auto frame = pbm.frame(1);
    assert(frame.duration == 0);
}

static void test_pgm()
{
    auto image = Gfx::load_pbm("/res/html/misc/pbmsuite_files/buggie-raw.pbm");
    auto pgm = Gfx::PGMImageDecoderPlugin((const u8*)&image, sizeof(*image));
    assert(pgm.frame_count());

    assert(!pgm.sniff());
    assert(!pgm.is_animated());
    assert(!pgm.loop_count());

    auto frame = pgm.frame(1);
    assert(frame.duration == 0);
}

static void test_png()
{
    auto image = Gfx::load_png("/res/graphics/buggie.png");
    auto png = Gfx::PNGImageDecoderPlugin((const u8*)&image, sizeof(*image));
    assert(png.frame_count());

    assert(!png.sniff());
    assert(!png.is_animated());
    assert(!png.loop_count());

    auto frame = png.frame(1);
    assert(frame.duration == 0);
}

static void test_ppm()
{
    auto image = Gfx::load_ppm("/res/html/misc/ppmsuite_files/buggie-raw.ppm");
    auto ppm = Gfx::PPMImageDecoderPlugin((const u8*)&image, sizeof(*image));
    assert(ppm.frame_count());

    assert(!ppm.sniff());
    assert(!ppm.is_animated());
    assert(!ppm.loop_count());

    auto frame = ppm.frame(1);
    assert(frame.duration == 0);
}

int main(int, char**)
{
#define RUNTEST(x)                      \
    {                                   \
        printf("Running " #x " ...\n"); \
        x();                            \
        printf("Success!\n");           \
    }
    RUNTEST(test_bmp);
    RUNTEST(test_gif);
    RUNTEST(test_ico);
    RUNTEST(test_jpg);
    RUNTEST(test_pbm);
    RUNTEST(test_pgm);
    RUNTEST(test_png);
    RUNTEST(test_ppm);
    printf("PASS\n");

    return 0;
}
