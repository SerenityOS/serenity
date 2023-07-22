/*
 * Copyright (c) 2021, the SerenityOS developers.
 * Copyright (c) 2021, Brian Gianforcaro <bgianf@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/DeprecatedString.h>
#include <LibCore/MappedFile.h>
#include <LibGfx/ImageFormats/BMPLoader.h>
#include <LibGfx/ImageFormats/GIFLoader.h>
#include <LibGfx/ImageFormats/ICOLoader.h>
#include <LibGfx/ImageFormats/ImageDecoder.h>
#include <LibGfx/ImageFormats/JPEGLoader.h>
#include <LibGfx/ImageFormats/JPEGXLLoader.h>
#include <LibGfx/ImageFormats/PBMLoader.h>
#include <LibGfx/ImageFormats/PGMLoader.h>
#include <LibGfx/ImageFormats/PNGLoader.h>
#include <LibGfx/ImageFormats/PPMLoader.h>
#include <LibGfx/ImageFormats/TGALoader.h>
#include <LibGfx/ImageFormats/TinyVGLoader.h>
#include <LibGfx/ImageFormats/WebPLoader.h>
#include <LibTest/TestCase.h>
#include <stdio.h>
#include <string.h>

#ifdef AK_OS_SERENITY
#    define TEST_INPUT(x) ("/usr/Tests/LibGfx/test-inputs/" x)
#else
#    define TEST_INPUT(x) ("test-inputs/" x)
#endif

static Gfx::ImageFrameDescriptor expect_single_frame(Gfx::ImageDecoderPlugin& plugin_decoder)
{
    EXPECT_EQ(plugin_decoder.frame_count(), 1u);
    EXPECT(!plugin_decoder.is_animated());
    EXPECT(!plugin_decoder.loop_count());

    auto frame = MUST(plugin_decoder.frame(0));
    EXPECT_EQ(frame.duration, 0);
    return frame;
}

static Gfx::ImageFrameDescriptor expect_single_frame_of_size(Gfx::ImageDecoderPlugin& plugin_decoder, Gfx::IntSize size)
{
    EXPECT_EQ(plugin_decoder.size(), size);
    auto frame = expect_single_frame(plugin_decoder);
    EXPECT_EQ(frame.image->size(), size);
    return frame;
}

TEST_CASE(test_bmp)
{
    auto file = MUST(Core::MappedFile::map(TEST_INPUT("rgba32-1.bmp"sv)));
    EXPECT(Gfx::BMPImageDecoderPlugin::sniff(file->bytes()));
    auto plugin_decoder = MUST(Gfx::BMPImageDecoderPlugin::create(file->bytes()));

    expect_single_frame(*plugin_decoder);
}

TEST_CASE(test_gif)
{
    auto file = MUST(Core::MappedFile::map(TEST_INPUT("download-animation.gif"sv)));
    EXPECT(Gfx::GIFImageDecoderPlugin::sniff(file->bytes()));
    auto plugin_decoder = MUST(Gfx::GIFImageDecoderPlugin::create(file->bytes()));

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
    EXPECT(Gfx::ICOImageDecoderPlugin::create(file->bytes()).is_error());
}

TEST_CASE(test_bmp_embedded_in_ico)
{
    auto file = MUST(Core::MappedFile::map(TEST_INPUT("serenity.ico"sv)));
    EXPECT(Gfx::ICOImageDecoderPlugin::sniff(file->bytes()));
    auto plugin_decoder = MUST(Gfx::ICOImageDecoderPlugin::create(file->bytes()));

    expect_single_frame(*plugin_decoder);
}

TEST_CASE(test_jpeg_sof0_one_scan)
{
    auto file = MUST(Core::MappedFile::map(TEST_INPUT("jpg/rgb24.jpg"sv)));
    EXPECT(Gfx::JPEGImageDecoderPlugin::sniff(file->bytes()));
    auto plugin_decoder = MUST(Gfx::JPEGImageDecoderPlugin::create(file->bytes()));

    expect_single_frame(*plugin_decoder);
}

TEST_CASE(test_jpeg_sof0_several_scans)
{
    auto file = MUST(Core::MappedFile::map(TEST_INPUT("jpg/several_scans.jpg"sv)));
    EXPECT(Gfx::JPEGImageDecoderPlugin::sniff(file->bytes()));
    auto plugin_decoder = MUST(Gfx::JPEGImageDecoderPlugin::create(file->bytes()));

    expect_single_frame_of_size(*plugin_decoder, { 592, 800 });
}

TEST_CASE(test_jpeg_rgb_components)
{
    auto file = MUST(Core::MappedFile::map(TEST_INPUT("jpg/rgb_components.jpg"sv)));
    EXPECT(Gfx::JPEGImageDecoderPlugin::sniff(file->bytes()));
    auto plugin_decoder = MUST(Gfx::JPEGImageDecoderPlugin::create(file->bytes()));

    expect_single_frame_of_size(*plugin_decoder, { 592, 800 });
}

TEST_CASE(test_jpeg_sof2_spectral_selection)
{
    auto file = MUST(Core::MappedFile::map(TEST_INPUT("jpg/spectral_selection.jpg"sv)));
    EXPECT(Gfx::JPEGImageDecoderPlugin::sniff(file->bytes()));
    auto plugin_decoder = MUST(Gfx::JPEGImageDecoderPlugin::create(file->bytes()));

    expect_single_frame_of_size(*plugin_decoder, { 592, 800 });
}

TEST_CASE(test_jpeg_sof0_several_scans_odd_number_mcu)
{
    auto file = MUST(Core::MappedFile::map(TEST_INPUT("jpg/several_scans_odd_number_mcu.jpg"sv)));
    EXPECT(Gfx::JPEGImageDecoderPlugin::sniff(file->bytes()));
    auto plugin_decoder = MUST(Gfx::JPEGImageDecoderPlugin::create(file->bytes()));

    expect_single_frame_of_size(*plugin_decoder, { 600, 600 });
}

TEST_CASE(test_jpeg_sof2_successive_aproximation)
{
    auto file = MUST(Core::MappedFile::map(TEST_INPUT("jpg/successive_approximation.jpg"sv)));
    EXPECT(Gfx::JPEGImageDecoderPlugin::sniff(file->bytes()));
    auto plugin_decoder = MUST(Gfx::JPEGImageDecoderPlugin::create(file->bytes()));

    expect_single_frame_of_size(*plugin_decoder, { 600, 800 });
}

TEST_CASE(test_jpeg_sof1_12bits)
{
    auto file = MUST(Core::MappedFile::map(TEST_INPUT("jpg/12-bit.jpg"sv)));
    EXPECT(Gfx::JPEGImageDecoderPlugin::sniff(file->bytes()));
    auto plugin_decoder = MUST(Gfx::JPEGImageDecoderPlugin::create(file->bytes()));

    expect_single_frame_of_size(*plugin_decoder, { 320, 240 });
}

TEST_CASE(test_jpeg_sof2_12bits)
{
    auto file = MUST(Core::MappedFile::map(TEST_INPUT("jpg/12-bit-progressive.jpg"sv)));
    EXPECT(Gfx::JPEGImageDecoderPlugin::sniff(file->bytes()));
    auto plugin_decoder = MUST(Gfx::JPEGImageDecoderPlugin::create(file->bytes()));

    expect_single_frame_of_size(*plugin_decoder, { 320, 240 });
}

TEST_CASE(test_jpeg_empty_icc)
{
    auto file = MUST(Core::MappedFile::map(TEST_INPUT("jpg/gradient_empty_icc.jpg"sv)));
    EXPECT(Gfx::JPEGImageDecoderPlugin::sniff(file->bytes()));
    auto plugin_decoder = MUST(Gfx::JPEGImageDecoderPlugin::create(file->bytes()));

    expect_single_frame_of_size(*plugin_decoder, { 80, 80 });
}

TEST_CASE(test_jpeg_grayscale_with_app14)
{
    auto file = MUST(Core::MappedFile::map(TEST_INPUT("jpg/grayscale_app14.jpg"sv)));
    EXPECT(Gfx::JPEGImageDecoderPlugin::sniff(file->bytes()));
    auto plugin_decoder = MUST(Gfx::JPEGImageDecoderPlugin::create(file->bytes()));

    expect_single_frame_of_size(*plugin_decoder, { 80, 80 });
}

TEST_CASE(test_pbm)
{
    auto file = MUST(Core::MappedFile::map(TEST_INPUT("pnm/buggie-raw.pbm"sv)));
    EXPECT(Gfx::PBMImageDecoderPlugin::sniff(file->bytes()));
    auto plugin_decoder = MUST(Gfx::PBMImageDecoderPlugin::create(file->bytes()));

    expect_single_frame(*plugin_decoder);
}

TEST_CASE(test_pgm)
{
    auto file = MUST(Core::MappedFile::map(TEST_INPUT("pnm/buggie-raw.pgm"sv)));
    EXPECT(Gfx::PGMImageDecoderPlugin::sniff(file->bytes()));
    auto plugin_decoder = MUST(Gfx::PGMImageDecoderPlugin::create(file->bytes()));

    expect_single_frame(*plugin_decoder);
}

TEST_CASE(test_png)
{
    auto file = MUST(Core::MappedFile::map(TEST_INPUT("buggie.png"sv)));
    EXPECT(Gfx::PNGImageDecoderPlugin::sniff(file->bytes()));
    auto plugin_decoder = MUST(Gfx::PNGImageDecoderPlugin::create(file->bytes()));

    expect_single_frame(*plugin_decoder);
}

TEST_CASE(test_ppm)
{
    auto file = MUST(Core::MappedFile::map(TEST_INPUT("pnm/buggie-raw.ppm"sv)));
    EXPECT(Gfx::PPMImageDecoderPlugin::sniff(file->bytes()));
    auto plugin_decoder = MUST(Gfx::PPMImageDecoderPlugin::create(file->bytes()));

    expect_single_frame(*plugin_decoder);
}

TEST_CASE(test_targa_bottom_left)
{
    auto file = MUST(Core::MappedFile::map(TEST_INPUT("tga/buggie-bottom-left-uncompressed.tga"sv)));
    EXPECT(MUST(Gfx::TGAImageDecoderPlugin::validate_before_create(file->bytes())));
    auto plugin_decoder = MUST(Gfx::TGAImageDecoderPlugin::create(file->bytes()));

    expect_single_frame(*plugin_decoder);
}

TEST_CASE(test_targa_top_left)
{
    auto file = MUST(Core::MappedFile::map(TEST_INPUT("tga/buggie-top-left-uncompressed.tga"sv)));
    EXPECT(MUST(Gfx::TGAImageDecoderPlugin::validate_before_create(file->bytes())));
    auto plugin_decoder = MUST(Gfx::TGAImageDecoderPlugin::create(file->bytes()));

    expect_single_frame(*plugin_decoder);
}

TEST_CASE(test_targa_bottom_left_compressed)
{
    auto file = MUST(Core::MappedFile::map(TEST_INPUT("tga/buggie-bottom-left-compressed.tga"sv)));
    EXPECT(MUST(Gfx::TGAImageDecoderPlugin::validate_before_create(file->bytes())));
    auto plugin_decoder = MUST(Gfx::TGAImageDecoderPlugin::create(file->bytes()));

    expect_single_frame(*plugin_decoder);
}

TEST_CASE(test_targa_top_left_compressed)
{
    auto file = MUST(Core::MappedFile::map(TEST_INPUT("tga/buggie-top-left-compressed.tga"sv)));
    EXPECT(MUST(Gfx::TGAImageDecoderPlugin::validate_before_create(file->bytes())));
    auto plugin_decoder = MUST(Gfx::TGAImageDecoderPlugin::create(file->bytes()));

    expect_single_frame(*plugin_decoder);
}

TEST_CASE(test_webp_simple_lossy)
{
    auto file = MUST(Core::MappedFile::map(TEST_INPUT("webp/simple-vp8.webp"sv)));
    EXPECT(Gfx::WebPImageDecoderPlugin::sniff(file->bytes()));
    auto plugin_decoder = MUST(Gfx::WebPImageDecoderPlugin::create(file->bytes()));

    auto frame = expect_single_frame_of_size(*plugin_decoder, { 240, 240 });

    // While VP8 YUV contents are defined bit-exact, the YUV->RGB conversion isn't.
    // So pixels changing by 1 or so below is fine if you change code.
    EXPECT_EQ(frame.image->get_pixel(120, 232), Gfx::Color(0xf2, 0xef, 0xf0, 255));
    EXPECT_EQ(frame.image->get_pixel(198, 202), Gfx::Color(0x7b, 0xaa, 0xd5, 255));
}

TEST_CASE(test_webp_simple_lossless)
{
    auto file = MUST(Core::MappedFile::map(TEST_INPUT("webp/simple-vp8l.webp"sv)));
    EXPECT(Gfx::WebPImageDecoderPlugin::sniff(file->bytes()));
    auto plugin_decoder = MUST(Gfx::WebPImageDecoderPlugin::create(file->bytes()));

    // Ironically, simple-vp8l.webp is a much more complex file than extended-lossless.webp tested below.
    // extended-lossless.webp tests the decoding basics.
    // This here tests the predictor, color, and subtract green transforms,
    // as well as meta prefix images, one-element canonical code handling,
    // and handling of canonical codes with more than 288 elements.
    // This image uses all 13 predictor modes of the predictor transform.
    auto frame = expect_single_frame_of_size(*plugin_decoder, { 386, 395 });
    EXPECT_EQ(frame.image->get_pixel(0, 0), Gfx::Color(0, 0, 0, 0));

    // This pixel tests all predictor modes except 5, 7, 8, 9, and 13.
    EXPECT_EQ(frame.image->get_pixel(289, 332), Gfx::Color(0xf2, 0xee, 0xd3, 255));
}

TEST_CASE(test_webp_simple_lossless_alpha_used_false)
{
    // This file is identical to simple-vp8l.webp, but the `is_alpha_used` used bit is false.
    // The file still contains alpha data. This tests that the decoder replaces the stored alpha data with 0xff if `is_alpha_used` is false.
    auto file = MUST(Core::MappedFile::map(TEST_INPUT("webp/simple-vp8l-alpha-used-false.webp"sv)));
    EXPECT(Gfx::WebPImageDecoderPlugin::sniff(file->bytes()));
    auto plugin_decoder = MUST(Gfx::WebPImageDecoderPlugin::create(file->bytes()));

    auto frame = expect_single_frame_of_size(*plugin_decoder, { 386, 395 });
    EXPECT_EQ(frame.image->get_pixel(0, 0), Gfx::Color(0, 0, 0, 0xff));
}

TEST_CASE(test_webp_extended_lossy)
{
    // This extended lossy image has an ALPH chunk for (losslessly compressed) alpha data.
    auto file = MUST(Core::MappedFile::map(TEST_INPUT("webp/extended-lossy.webp"sv)));
    EXPECT(Gfx::WebPImageDecoderPlugin::sniff(file->bytes()));
    auto plugin_decoder = MUST(Gfx::WebPImageDecoderPlugin::create(file->bytes()));

    auto frame = expect_single_frame_of_size(*plugin_decoder, { 417, 223 });

    // While VP8 YUV contents are defined bit-exact, the YUV->RGB conversion isn't.
    // So pixels changing by 1 or so below is fine if you change code.
    EXPECT_EQ(frame.image->get_pixel(89, 72), Gfx::Color(255, 1, 0, 255));
    EXPECT_EQ(frame.image->get_pixel(174, 69), Gfx::Color(0, 255, 0, 255));
    EXPECT_EQ(frame.image->get_pixel(245, 84), Gfx::Color(0, 0, 255, 255));
    EXPECT_EQ(frame.image->get_pixel(352, 125), Gfx::Color(0, 0, 0, 128));
    EXPECT_EQ(frame.image->get_pixel(355, 106), Gfx::Color(0, 0, 0, 0));

    // Check same basic pixels as in test_webp_extended_lossless too.
    // (The top-left pixel in the lossy version is fully transparent white, compared to fully transparent black in the lossless version).
    EXPECT_EQ(frame.image->get_pixel(0, 0), Gfx::Color(255, 255, 255, 0));
    EXPECT_EQ(frame.image->get_pixel(43, 75), Gfx::Color(255, 0, 2, 255));
    EXPECT_EQ(frame.image->get_pixel(141, 75), Gfx::Color(0, 255, 3, 255));
    EXPECT_EQ(frame.image->get_pixel(235, 75), Gfx::Color(0, 0, 255, 255));
    EXPECT_EQ(frame.image->get_pixel(341, 75), Gfx::Color(0, 0, 0, 128));
}

TEST_CASE(test_webp_extended_lossy_alpha_horizontal_filter)
{
    // Also lossy rgb + lossless alpha, but with a horizontal alpha filtering method.
    // The image should look like smolkling.webp, but with a horizontal alpha gradient.
    auto file = MUST(Core::MappedFile::map(TEST_INPUT("webp/smolkling-horizontal-alpha.webp"sv)));
    EXPECT(Gfx::WebPImageDecoderPlugin::sniff(file->bytes()));
    auto plugin_decoder = MUST(Gfx::WebPImageDecoderPlugin::create(file->bytes()));

    auto frame = expect_single_frame_of_size(*plugin_decoder, { 264, 264 });

    // While VP8 YUV contents are defined bit-exact, the YUV->RGB conversion isn't.
    // So pixels changing by 1 or so below is fine if you change code.
    // The important component in this test is alpha, and that shouldn't change even by 1 as it's losslessly compressed and doesn't use YUV.
    EXPECT_EQ(frame.image->get_pixel(131, 131), Gfx::Color(0x8f, 0x51, 0x2f, 0x4b));
}

TEST_CASE(test_webp_extended_lossy_alpha_vertical_filter)
{
    // Also lossy rgb + lossless alpha, but with a vertical alpha filtering method.
    // The image should look like smolkling.webp, but with a vertical alpha gradient, and with a fully transparent first column.
    auto file = MUST(Core::MappedFile::map(TEST_INPUT("webp/smolkling-vertical-alpha.webp"sv)));
    EXPECT(Gfx::WebPImageDecoderPlugin::sniff(file->bytes()));
    auto plugin_decoder = MUST(Gfx::WebPImageDecoderPlugin::create(file->bytes()));

    auto frame = expect_single_frame_of_size(*plugin_decoder, { 264, 264 });

    // While VP8 YUV contents are defined bit-exact, the YUV->RGB conversion isn't.
    // So pixels changing by 1 or so below is fine if you change code.
    // The important component in this test is alpha, and that shouldn't change even by 1 as it's losslessly compressed and doesn't use YUV.
    EXPECT_EQ(frame.image->get_pixel(131, 131), Gfx::Color(0x94, 0x50, 0x32, 0x4c));
}

TEST_CASE(test_webp_extended_lossy_alpha_gradient_filter)
{
    // Also lossy rgb + lossless alpha, but with a gradient alpha filtering method.
    // The image should look like smolkling.webp, but with a few transparent pixels in the shape of a C on it. Most of the image should not be transparent.
    auto file = MUST(Core::MappedFile::map(TEST_INPUT("webp/smolkling-gradient-alpha.webp"sv)));
    EXPECT(Gfx::WebPImageDecoderPlugin::sniff(file->bytes()));
    auto plugin_decoder = MUST(Gfx::WebPImageDecoderPlugin::create(file->bytes()));

    auto frame = expect_single_frame_of_size(*plugin_decoder, { 264, 264 });

    // While VP8 YUV contents are defined bit-exact, the YUV->RGB conversion isn't.
    // So pixels changing by 1 or so below is fine if you change code.
    // The important component in this test is alpha, and that shouldn't change even by 1 as it's losslessly compressed and doesn't use YUV.
    // In particular, the center of the image should be fully opaque, not fully transparent.
    EXPECT_EQ(frame.image->get_pixel(131, 131), Gfx::Color(0x8c, 0x47, 0x2e, 255));
}

TEST_CASE(test_webp_extended_lossy_uncompressed_alpha)
{
    auto file = MUST(Core::MappedFile::map(TEST_INPUT("webp/extended-lossy-uncompressed-alpha.webp"sv)));
    EXPECT(Gfx::WebPImageDecoderPlugin::sniff(file->bytes()));
    auto plugin_decoder = MUST(Gfx::WebPImageDecoderPlugin::create(file->bytes()));

    auto frame = expect_single_frame_of_size(*plugin_decoder, { 417, 223 });

    // While VP8 YUV contents are defined bit-exact, the YUV->RGB conversion isn't.
    // So pixels changing by 1 or so below is fine if you change code.
    EXPECT_EQ(frame.image->get_pixel(89, 72), Gfx::Color(255, 0, 4, 255));
    EXPECT_EQ(frame.image->get_pixel(174, 69), Gfx::Color(4, 255, 0, 255));
    EXPECT_EQ(frame.image->get_pixel(245, 84), Gfx::Color(0, 0, 255, 255));
    EXPECT_EQ(frame.image->get_pixel(352, 125), Gfx::Color(0, 0, 0, 128));
    EXPECT_EQ(frame.image->get_pixel(355, 106), Gfx::Color(0, 0, 0, 0));
}

TEST_CASE(test_webp_extended_lossy_negative_quantization_offset)
{
    auto file = MUST(Core::MappedFile::map(TEST_INPUT("webp/smolkling.webp"sv)));
    EXPECT(Gfx::WebPImageDecoderPlugin::sniff(file->bytes()));
    auto plugin_decoder = MUST(Gfx::WebPImageDecoderPlugin::create(file->bytes()));

    auto frame = expect_single_frame_of_size(*plugin_decoder, { 264, 264 });

    // While VP8 YUV contents are defined bit-exact, the YUV->RGB conversion isn't.
    // So pixels changing by 1 or so below is fine if you change code.
    EXPECT_EQ(frame.image->get_pixel(16, 16), Gfx::Color(0x3c, 0x24, 0x1a, 255));
}

TEST_CASE(test_webp_lossy_4)
{
    // This is https://commons.wikimedia.org/wiki/File:Fr%C3%BChling_bl%C3%BChender_Kirschenbaum.jpg,
    // under the Creative Commons Attribution-Share Alike 3.0 Unported license. The image was re-encoded
    // as webp at https://developers.google.com/speed/webp/gallery1 and the webp version is from there.
    // No other changes have been made.
    auto file = MUST(Core::MappedFile::map(TEST_INPUT("webp/4.webp"sv)));
    EXPECT(Gfx::WebPImageDecoderPlugin::sniff(file->bytes()));
    auto plugin_decoder = MUST(Gfx::WebPImageDecoderPlugin::create(file->bytes()));

    auto frame = expect_single_frame_of_size(*plugin_decoder, { 1024, 772 });

    // This image tests macroblocks that have `skip_coefficients` set to true, and it test a boolean entropy decoder edge case.
    EXPECT_EQ(frame.image->get_pixel(780, 570), Gfx::Color(0x72, 0xc8, 0xf6, 255));
}

TEST_CASE(test_webp_lossy_4_with_partitions)
{
    // Same input file as in the previous test, but re-encoded to use 8 secondary partitions.
    auto file = MUST(Core::MappedFile::map(TEST_INPUT("webp/4-with-8-partitions.webp"sv)));
    EXPECT(Gfx::WebPImageDecoderPlugin::sniff(file->bytes()));
    auto plugin_decoder = MUST(Gfx::WebPImageDecoderPlugin::create(file->bytes()));

    auto frame = expect_single_frame_of_size(*plugin_decoder, { 1024, 772 });
    EXPECT_EQ(frame.image->get_pixel(780, 570), Gfx::Color(0x73, 0xc9, 0xf9, 255));
}

TEST_CASE(test_webp_extended_lossless)
{
    auto file = MUST(Core::MappedFile::map(TEST_INPUT("webp/extended-lossless.webp"sv)));
    EXPECT(Gfx::WebPImageDecoderPlugin::sniff(file->bytes()));
    auto plugin_decoder = MUST(Gfx::WebPImageDecoderPlugin::create(file->bytes()));

    auto frame = expect_single_frame_of_size(*plugin_decoder, { 417, 223 });

    // Check some basic pixels.
    EXPECT_EQ(frame.image->get_pixel(0, 0), Gfx::Color(0, 0, 0, 0));
    EXPECT_EQ(frame.image->get_pixel(43, 75), Gfx::Color(255, 0, 0, 255));
    EXPECT_EQ(frame.image->get_pixel(141, 75), Gfx::Color(0, 255, 0, 255));
    EXPECT_EQ(frame.image->get_pixel(235, 75), Gfx::Color(0, 0, 255, 255));
    EXPECT_EQ(frame.image->get_pixel(341, 75), Gfx::Color(0, 0, 0, 128));

    // Check pixels using the color cache.
    EXPECT_EQ(frame.image->get_pixel(94, 73), Gfx::Color(255, 0, 0, 255));
    EXPECT_EQ(frame.image->get_pixel(176, 115), Gfx::Color(0, 255, 0, 255));
    EXPECT_EQ(frame.image->get_pixel(290, 89), Gfx::Color(0, 0, 255, 255));
    EXPECT_EQ(frame.image->get_pixel(359, 73), Gfx::Color(0, 0, 0, 128));
}

TEST_CASE(test_webp_simple_lossless_color_index_transform)
{
    // In addition to testing the index transform, this file also tests handling of explicity setting max_symbol.
    auto file = MUST(Core::MappedFile::map(TEST_INPUT("webp/Qpalette.webp"sv)));
    EXPECT(Gfx::WebPImageDecoderPlugin::sniff(file->bytes()));
    auto plugin_decoder = MUST(Gfx::WebPImageDecoderPlugin::create(file->bytes()));

    auto frame = expect_single_frame_of_size(*plugin_decoder, { 256, 256 });

    EXPECT_EQ(frame.image->get_pixel(100, 100), Gfx::Color(0x73, 0x37, 0x23, 0xff));
}

TEST_CASE(test_webp_simple_lossless_color_index_transform_pixel_bundling)
{
    struct TestCase {
        StringView file_name;
        Gfx::Color line_color;
        Gfx::Color background_color;
    };

    // The number after the dash is the number of colors in each file's color index bitmap.
    // catdog-alert-2 tests the 1-bit-per-pixel case,
    // catdog-alert-3 tests the 2-bit-per-pixel case,
    // catdog-alert-8 and catdog-alert-13 both test the 4-bits-per-pixel case.
    // catdog-alert-13-alpha-used-false is like catdog-alert-13, but with is_alpha_used set to false in the header
    // (which has the effect of ignoring the alpha information in the palette and instead always setting alpha to 0xff).
    TestCase test_cases[] = {
        { "webp/catdog-alert-2.webp"sv, Gfx::Color(0x35, 0x12, 0x0a, 0xff), Gfx::Color(0xf3, 0xe6, 0xd8, 0xff) },
        { "webp/catdog-alert-3.webp"sv, Gfx::Color(0x35, 0x12, 0x0a, 0xff), Gfx::Color(0, 0, 0, 0) },
        { "webp/catdog-alert-8.webp"sv, Gfx::Color(0, 0, 0, 255), Gfx::Color(0, 0, 0, 0) },
        { "webp/catdog-alert-13.webp"sv, Gfx::Color(0, 0, 0, 255), Gfx::Color(0, 0, 0, 0) },
        { "webp/catdog-alert-13-alpha-used-false.webp"sv, Gfx::Color(0, 0, 0, 255), Gfx::Color(0, 0, 0, 255) },
    };

    for (auto test_case : test_cases) {
        auto file = MUST(Core::MappedFile::map(MUST(String::formatted("{}{}", TEST_INPUT(""), test_case.file_name))));
        EXPECT(Gfx::WebPImageDecoderPlugin::sniff(file->bytes()));
        auto plugin_decoder = MUST(Gfx::WebPImageDecoderPlugin::create(file->bytes()));

        auto frame = expect_single_frame_of_size(*plugin_decoder, { 32, 32 });

        EXPECT_EQ(frame.image->get_pixel(4, 0), test_case.background_color);
        EXPECT_EQ(frame.image->get_pixel(5, 0), test_case.line_color);

        EXPECT_EQ(frame.image->get_pixel(9, 5), test_case.background_color);
        EXPECT_EQ(frame.image->get_pixel(10, 5), test_case.line_color);
        EXPECT_EQ(frame.image->get_pixel(11, 5), test_case.background_color);
    }
}

TEST_CASE(test_webp_simple_lossless_color_index_transform_pixel_bundling_odd_width)
{
    StringView file_names[] = {
        "webp/width11-height11-colors2.webp"sv,
        "webp/width11-height11-colors3.webp"sv,
        "webp/width11-height11-colors15.webp"sv,
    };

    for (auto file_name : file_names) {
        auto file = MUST(Core::MappedFile::map(MUST(String::formatted("{}{}", TEST_INPUT(""), file_name))));
        auto plugin_decoder = MUST(Gfx::WebPImageDecoderPlugin::create(file->bytes()));
        expect_single_frame_of_size(*plugin_decoder, { 11, 11 });
    }
}

TEST_CASE(test_webp_extended_lossless_animated)
{
    auto file = MUST(Core::MappedFile::map(TEST_INPUT("webp/extended-lossless-animated.webp"sv)));
    EXPECT(Gfx::WebPImageDecoderPlugin::sniff(file->bytes()));
    auto plugin_decoder = MUST(Gfx::WebPImageDecoderPlugin::create(file->bytes()));

    EXPECT_EQ(plugin_decoder->frame_count(), 8u);
    EXPECT(plugin_decoder->is_animated());
    EXPECT_EQ(plugin_decoder->loop_count(), 42u);

    EXPECT_EQ(plugin_decoder->size(), Gfx::IntSize(990, 1050));

    for (size_t frame_index = 0; frame_index < plugin_decoder->frame_count(); ++frame_index) {
        auto frame = MUST(plugin_decoder->frame(frame_index));
        EXPECT_EQ(frame.image->size(), Gfx::IntSize(990, 1050));

        // This pixel happens to be the same color in all frames.
        EXPECT_EQ(frame.image->get_pixel(500, 700), Gfx::Color::Yellow);

        // This one isn't the same in all frames.
        EXPECT_EQ(frame.image->get_pixel(500, 0), (frame_index == 2 || frame_index == 6) ? Gfx::Color::Black : Gfx::Color(255, 255, 255, 0));
    }
}

TEST_CASE(test_tvg)
{
    auto file = MUST(Core::MappedFile::map(TEST_INPUT("tvg/yak.tvg"sv)));
    EXPECT(Gfx::TinyVGImageDecoderPlugin::sniff(file->bytes()));
    auto plugin_decoder = MUST(Gfx::TinyVGImageDecoderPlugin::create(file->bytes()));

    expect_single_frame_of_size(*plugin_decoder, { 1024, 1024 });
}

TEST_CASE(test_everything_tvg)
{
    Array file_names {
        TEST_INPUT("tvg/everything.tvg"sv),
        TEST_INPUT("tvg/everything-32.tvg"sv)
    };

    for (auto file_name : file_names) {
        auto file = MUST(Core::MappedFile::map(file_name));
        EXPECT(Gfx::TinyVGImageDecoderPlugin::sniff(file->bytes()));
        auto plugin_decoder = MUST(Gfx::TinyVGImageDecoderPlugin::create(file->bytes()));

        expect_single_frame_of_size(*plugin_decoder, { 400, 768 });
    }
}

TEST_CASE(test_jxl_modular_simple_tree_upsample2_10bits)
{
    auto file = MUST(Core::MappedFile::map(TEST_INPUT("jxl/modular_simple_tree_upsample2_10bits_rct.jxl"sv)));
    EXPECT(Gfx::JPEGXLImageDecoderPlugin::sniff(file->bytes()));
    auto plugin_decoder = MUST(Gfx::JPEGXLImageDecoderPlugin::create(file->bytes()));

    expect_single_frame_of_size(*plugin_decoder, { 128, 128 });

    auto frame = MUST(plugin_decoder->frame(0));
    EXPECT_EQ(frame.image->get_pixel(42, 57), Gfx::Color::from_string("#4c0072"sv));
}
