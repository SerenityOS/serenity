/*
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibEDID/DMT.h>
#include <LibEDID/EDID.h>
#include <LibTest/TestCase.h>

static u8 const edid1_bin[] = {
    0x00, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0x00, 0x49, 0x14, 0x34, 0x12,
    0x00, 0x00, 0x00, 0x00, 0x2a, 0x18, 0x01, 0x04, 0xa5, 0x1a, 0x13, 0x78,
    0x06, 0xee, 0x91, 0xa3, 0x54, 0x4c, 0x99, 0x26, 0x0f, 0x50, 0x54, 0x21,
    0x08, 0x00, 0xe1, 0xc0, 0xd1, 0xc0, 0xd1, 0x00, 0xa9, 0x40, 0xb3, 0x00,
    0x95, 0x00, 0x81, 0x80, 0x81, 0x40, 0x25, 0x20, 0x00, 0x66, 0x41, 0x00,
    0x1a, 0x30, 0x00, 0x1e, 0x33, 0x40, 0x04, 0xc3, 0x10, 0x00, 0x00, 0x18,
    0x00, 0x00, 0x00, 0xfd, 0x00, 0x32, 0x7d, 0x1e, 0xa0, 0x78, 0x01, 0x0a,
    0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x00, 0x00, 0x00, 0xfc, 0x00, 0x51,
    0x45, 0x4d, 0x55, 0x20, 0x4d, 0x6f, 0x6e, 0x69, 0x74, 0x6f, 0x72, 0x0a,
    0x00, 0x00, 0x00, 0xf7, 0x00, 0x0a, 0x00, 0x40, 0x82, 0x00, 0x28, 0x20,
    0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01, 0xc4, 0x02, 0x03, 0x0a, 0x00,
    0x45, 0x7d, 0x65, 0x60, 0x59, 0x1f, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
    0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
    0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
    0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
    0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
    0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
    0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
    0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
    0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
    0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
    0x00, 0x00, 0x00, 0xf2
};

TEST_CASE(edid1)
{
    auto edid = TRY_OR_FAIL(EDID::Parser::from_bytes({ edid1_bin, sizeof(edid1_bin) }));
    EXPECT(edid.legacy_manufacturer_id() == "RHT");
    EXPECT(!edid.aspect_ratio().has_value());
    auto screen_size = edid.screen_size();
    EXPECT(screen_size.has_value());
    EXPECT(screen_size.value().horizontal_cm() == 26);
    EXPECT(screen_size.value().vertical_cm() == 19);
    auto gamma = edid.gamma();
    EXPECT(gamma.has_value());
    EXPECT(gamma.value() >= 2.19f && gamma.value() <= 2.21f);
    EXPECT(edid.display_product_name() == "QEMU Monitor");

    {
        static constexpr struct {
            unsigned width;
            unsigned height;
            unsigned refresh_rate;
            EDID::Parser::EstablishedTiming::Source source;
            u8 dmt_id { 0 };
        } expected_established_timings[] = {
            { 640, 480, 60, EDID::Parser::EstablishedTiming::Source::IBM, 0x4 },
            { 800, 600, 60, EDID::Parser::EstablishedTiming::Source::VESA, 0x9 },
            { 1024, 768, 60, EDID::Parser::EstablishedTiming::Source::VESA, 0x10 },
            { 1280, 768, 60, EDID::Parser::EstablishedTiming::Source::VESA, 0x17 },
            { 1360, 768, 60, EDID::Parser::EstablishedTiming::Source::VESA, 0x27 },
            { 1400, 1050, 60, EDID::Parser::EstablishedTiming::Source::VESA, 0x2a },
            { 1792, 1344, 60, EDID::Parser::EstablishedTiming::Source::VESA, 0x3e },
            { 1856, 1392, 60, EDID::Parser::EstablishedTiming::Source::VESA, 0x41 },
            { 1920, 1440, 60, EDID::Parser::EstablishedTiming::Source::VESA, 0x49 }
        };
        static constexpr size_t expected_established_timings_count = sizeof(expected_established_timings) / sizeof(expected_established_timings[0]);
        size_t established_timings_found = 0;
        auto result = TRY_OR_FAIL(edid.for_each_established_timing([&](auto& established_timings) {
            EXPECT(established_timings_found < expected_established_timings_count);
            auto& expected_timings = expected_established_timings[established_timings_found];
            EXPECT(established_timings.width() == expected_timings.width);
            EXPECT(established_timings.height() == expected_timings.height);
            EXPECT(established_timings.refresh_rate() == expected_timings.refresh_rate);
            EXPECT(established_timings.source() == expected_timings.source);
            EXPECT(established_timings.dmt_id() == expected_timings.dmt_id);
            established_timings_found++;
            return IterationDecision::Continue;
        }));
        EXPECT(result == IterationDecision::Continue);
        EXPECT(established_timings_found == expected_established_timings_count);
    }

    {
        static constexpr struct {
            unsigned width;
            unsigned height;
            unsigned refresh_rate;
            u8 dmt_id { 0 };
        } expected_standard_established_timings[] = {
            { 2048, 1152, 60, 0x54 },
            { 1920, 1080, 60, 0x52 },
            { 1920, 1200, 60, 0x45 },
            { 1600, 1200, 60, 0x33 },
            { 1680, 1050, 60, 0x3a },
            { 1440, 900, 60, 0x2f },
            { 1280, 1024, 60, 0x23 },
            { 1280, 960, 60, 0x20 }
        };
        static constexpr size_t expected_standard_timings_count = sizeof(expected_standard_established_timings) / sizeof(expected_standard_established_timings[0]);
        size_t standard_timings_found = 0;
        auto result = TRY_OR_FAIL(edid.for_each_standard_timing([&](auto& standard_timings) {
            EXPECT(standard_timings_found < expected_standard_timings_count);
            auto& expected_timings = expected_standard_established_timings[standard_timings_found];
            EXPECT(standard_timings.dmt_id() == expected_timings.dmt_id);
            EXPECT(standard_timings.width() == expected_timings.width);
            EXPECT(standard_timings.height() == expected_timings.height);
            EXPECT(standard_timings.refresh_rate() == expected_timings.refresh_rate);
            standard_timings_found++;
            return IterationDecision::Continue;
        }));
        EXPECT(result == IterationDecision::Continue);
        EXPECT(standard_timings_found == expected_standard_timings_count);
    }

    {
        static constexpr struct {
            unsigned block_id;
            unsigned width;
            unsigned height;
            unsigned refresh_rate;
        } expected_detailed_timings[] = {
            { 0, 1024, 768, 75 }
        };
        static constexpr size_t expected_detailed_timings_count = sizeof(expected_detailed_timings) / sizeof(expected_detailed_timings[0]);
        size_t detailed_timings_found = 0;
        auto result = TRY_OR_FAIL(edid.for_each_detailed_timing([&](auto& detailed_timing, unsigned block_id) {
            EXPECT(detailed_timings_found < expected_detailed_timings_count);
            auto& expected_timings = expected_detailed_timings[detailed_timings_found];
            EXPECT(block_id == expected_timings.block_id);
            EXPECT(detailed_timing.horizontal_addressable_pixels() == expected_timings.width);
            EXPECT(detailed_timing.vertical_addressable_lines() == expected_timings.height);
            EXPECT(detailed_timing.refresh_rate().lrint() == expected_timings.refresh_rate);
            detailed_timings_found++;
            return IterationDecision::Continue;
        }));
        EXPECT(result == IterationDecision::Continue);
        EXPECT(detailed_timings_found == expected_detailed_timings_count);
    }

    {
        static constexpr u8 expected_vic_ids[] = { 125, 101, 96, 89, 31 };
        static constexpr size_t expected_vic_ids_count = sizeof(expected_vic_ids) / sizeof(expected_vic_ids[0]);
        size_t vic_ids_found = 0;
        auto result = TRY_OR_FAIL(edid.for_each_short_video_descriptor([&](unsigned block_id, bool is_native, EDID::VIC::Details const& vic) {
            EXPECT(vic_ids_found < expected_vic_ids_count);
            EXPECT(block_id == 1);
            EXPECT(!is_native); // none are marked as native
            EXPECT(vic.vic_id == expected_vic_ids[vic_ids_found]);
            vic_ids_found++;
            return IterationDecision::Continue;
        }));
        EXPECT(result == IterationDecision::Continue);
        EXPECT(vic_ids_found == expected_vic_ids_count);
    }

    {
        // This edid has one CEA861 extension block only
        size_t extension_blocks_found = 0;
        auto result = TRY_OR_FAIL(edid.for_each_extension_block([&](unsigned block_id, u8 tag, u8 revision, ReadonlyBytes) {
            EXPECT(block_id == 1);
            EXPECT(tag == 0x2);
            EXPECT(revision == 3);
            extension_blocks_found++;
            return IterationDecision::Continue;
        }));
        EXPECT(result == IterationDecision::Continue);
        EXPECT(extension_blocks_found == 1);
    }
}

static u8 const edid2_bin[] = {
    0x00, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0x00, 0x04, 0x72, 0x1d, 0x08,
    0xd2, 0x02, 0x96, 0x49, 0x20, 0x1e, 0x01, 0x04, 0xb5, 0x3c, 0x22, 0x78,
    0x3b, 0xff, 0x15, 0xa6, 0x53, 0x4a, 0x98, 0x26, 0x0f, 0x50, 0x54, 0xbf,
    0xef, 0x80, 0xd1, 0xc0, 0xb3, 0x00, 0x95, 0x00, 0x81, 0x80, 0x81, 0x40,
    0x81, 0xc0, 0x01, 0x01, 0x01, 0x01, 0x86, 0x6f, 0x00, 0x3c, 0xa0, 0xa0,
    0x0f, 0x50, 0x08, 0x20, 0x35, 0x00, 0x55, 0x50, 0x21, 0x00, 0x00, 0x1e,
    0x56, 0x5e, 0x00, 0xa0, 0xa0, 0xa0, 0x29, 0x50, 0x30, 0x20, 0x35, 0x00,
    0x55, 0x50, 0x21, 0x00, 0x00, 0x1e, 0x00, 0x00, 0x00, 0xfd, 0x00, 0x30,
    0x4b, 0x78, 0x78, 0x1e, 0x01, 0x0a, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20,
    0x00, 0x00, 0x00, 0xfc, 0x00, 0x43, 0x42, 0x32, 0x37, 0x32, 0x55, 0x0a,
    0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x01, 0xc5, 0x02, 0x03, 0x33, 0x71,
    0x4c, 0x12, 0x13, 0x04, 0x1f, 0x90, 0x14, 0x05, 0x01, 0x11, 0x02, 0x03,
    0x4a, 0x23, 0x09, 0x07, 0x07, 0x83, 0x01, 0x00, 0x00, 0xe2, 0x00, 0xc0,
    0x67, 0x03, 0x0c, 0x00, 0x10, 0x00, 0x38, 0x3c, 0xe3, 0x05, 0xe3, 0x01,
    0xe3, 0x0f, 0x00, 0x00, 0xe6, 0x06, 0x07, 0x01, 0x60, 0x60, 0x45, 0x01,
    0x1d, 0x00, 0x72, 0x51, 0xd0, 0x1e, 0x20, 0x6e, 0x28, 0x55, 0x00, 0x55,
    0x50, 0x21, 0x00, 0x00, 0x1e, 0x01, 0x1d, 0x00, 0xbc, 0x52, 0xd0, 0x1e,
    0x20, 0xb8, 0x28, 0x55, 0x40, 0x55, 0x50, 0x21, 0x00, 0x00, 0x1e, 0x56,
    0x5e, 0x00, 0xa0, 0xa0, 0xa0, 0x29, 0x50, 0x30, 0x20, 0x35, 0x00, 0x55,
    0x50, 0x21, 0x00, 0x00, 0x1e, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
    0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
    0x00, 0x00, 0x00, 0xe1
};

TEST_CASE(edid2)
{
    auto edid = TRY_OR_FAIL(EDID::Parser::from_bytes({ edid2_bin, sizeof(edid2_bin) }));
    EXPECT(edid.legacy_manufacturer_id() == "ACR");
    EXPECT(edid.serial_number() == 1234567890);
    auto digital_interface = edid.digital_display();
    EXPECT(digital_interface.has_value());
    EXPECT(digital_interface.value().color_bit_depth() == EDID::Parser::DigitalDisplay::ColorBitDepth::BPP_10);
    EXPECT(digital_interface.value().supported_interface() == EDID::Parser::DigitalDisplay::SupportedInterface::DisplayPort);
    EXPECT(!digital_interface.value().features().supports_standby());
    EXPECT(!digital_interface.value().features().supports_suspend());
    EXPECT(digital_interface.value().features().supports_off());
    EXPECT(digital_interface.value().features().preferred_timing_mode_includes_pixel_format_and_refresh_rate());
    EXPECT(!digital_interface.value().features().srgb_is_default_color_space());
    EXPECT(digital_interface.value().features().frequency() == EDID::Parser::DigitalDisplayFeatures::Frequency::Continuous);
    EXPECT(digital_interface.value().features().supported_color_encodings() == EDID::Parser::DigitalDisplayFeatures::SupportedColorEncodings::RGB444_YCrCb444_YCrCb422);
    EXPECT(!edid.aspect_ratio().has_value());
    auto screen_size = edid.screen_size();
    EXPECT(screen_size.has_value());
    EXPECT(screen_size.value().horizontal_cm() == 60);
    EXPECT(screen_size.value().vertical_cm() == 34);
    auto gamma = edid.gamma();
    EXPECT(gamma.has_value());
    EXPECT(gamma.value() >= 2.19f && gamma.value() <= 2.21f);
    EXPECT(edid.display_product_name() == "CB272U");

    {
        static constexpr struct {
            unsigned width;
            unsigned height;
            unsigned refresh_rate;
            EDID::Parser::EstablishedTiming::Source source;
            u8 dmt_id { 0 };
        } expected_established_timings[] = {
            { 720, 400, 70, EDID::Parser::EstablishedTiming::Source::IBM },
            { 640, 480, 60, EDID::Parser::EstablishedTiming::Source::IBM, 0x4 },
            { 640, 480, 67, EDID::Parser::EstablishedTiming::Source::Apple },
            { 640, 480, 73, EDID::Parser::EstablishedTiming::Source::VESA, 0x5 },
            { 640, 480, 75, EDID::Parser::EstablishedTiming::Source::VESA, 0x6 },
            { 800, 600, 56, EDID::Parser::EstablishedTiming::Source::VESA, 0x8 },
            { 800, 600, 60, EDID::Parser::EstablishedTiming::Source::VESA, 0x9 },
            { 800, 600, 72, EDID::Parser::EstablishedTiming::Source::VESA, 0xa },
            { 800, 600, 75, EDID::Parser::EstablishedTiming::Source::VESA, 0xb },
            { 832, 624, 75, EDID::Parser::EstablishedTiming::Source::Apple },
            { 1024, 768, 60, EDID::Parser::EstablishedTiming::Source::VESA, 0x10 },
            { 1024, 768, 70, EDID::Parser::EstablishedTiming::Source::VESA, 0x11 },
            { 1024, 768, 75, EDID::Parser::EstablishedTiming::Source::VESA, 0x12 },
            { 1280, 1024, 75, EDID::Parser::EstablishedTiming::Source::VESA, 0x24 },
            { 1152, 870, 75, EDID::Parser::EstablishedTiming::Source::Apple }
        };
        static constexpr size_t expected_established_timings_count = sizeof(expected_established_timings) / sizeof(expected_established_timings[0]);
        size_t established_timings_found = 0;
        auto result = TRY_OR_FAIL(edid.for_each_established_timing([&](auto& established_timings) {
            EXPECT(established_timings_found < expected_established_timings_count);
            auto& expected_timings = expected_established_timings[established_timings_found];
            EXPECT(established_timings.width() == expected_timings.width);
            EXPECT(established_timings.height() == expected_timings.height);
            EXPECT(established_timings.refresh_rate() == expected_timings.refresh_rate);
            EXPECT(established_timings.source() == expected_timings.source);
            EXPECT(established_timings.dmt_id() == expected_timings.dmt_id);
            established_timings_found++;
            return IterationDecision::Continue;
        }));
        EXPECT(result == IterationDecision::Continue);
        EXPECT(established_timings_found == expected_established_timings_count);
    }

    {
        static constexpr struct {
            unsigned width;
            unsigned height;
            unsigned refresh_rate;
            u8 dmt_id { 0 };
        } expected_standard_established_timings[] = {
            { 1920, 1080, 60, 0x52 },
            { 1680, 1050, 60, 0x3a },
            { 1440, 900, 60, 0x2f },
            { 1280, 1024, 60, 0x23 },
            { 1280, 960, 60, 0x20 },
            { 1280, 720, 60, 0x55 },
        };
        static constexpr size_t expected_standard_timings_count = sizeof(expected_standard_established_timings) / sizeof(expected_standard_established_timings[0]);
        size_t standard_timings_found = 0;
        auto result = TRY_OR_FAIL(edid.for_each_standard_timing([&](auto& standard_timings) {
            EXPECT(standard_timings_found < expected_standard_timings_count);
            auto& expected_timings = expected_standard_established_timings[standard_timings_found];
            EXPECT(standard_timings.dmt_id() == expected_timings.dmt_id);
            EXPECT(standard_timings.width() == expected_timings.width);
            EXPECT(standard_timings.height() == expected_timings.height);
            EXPECT(standard_timings.refresh_rate() == expected_timings.refresh_rate);
            standard_timings_found++;
            return IterationDecision::Continue;
        }));
        EXPECT(result == IterationDecision::Continue);
        EXPECT(standard_timings_found == expected_standard_timings_count);
    }

    {
        static constexpr struct {
            unsigned block_id;
            unsigned width;
            unsigned height;
            unsigned refresh_rate;
        } expected_detailed_timings[] = {
            { 0, 2560, 1440, 75 },
            { 0, 2560, 1440, 60 },
            { 1, 1280, 720, 60 },
            { 1, 1280, 720, 50 },
            { 1, 2560, 1440, 60 }
        };
        static constexpr size_t expected_detailed_timings_count = sizeof(expected_detailed_timings) / sizeof(expected_detailed_timings[0]);
        size_t detailed_timings_found = 0;
        auto result = TRY_OR_FAIL(edid.for_each_detailed_timing([&](auto& detailed_timing, unsigned block_id) {
            EXPECT(detailed_timings_found < expected_detailed_timings_count);
            auto& expected_timings = expected_detailed_timings[detailed_timings_found];
            EXPECT(block_id == expected_timings.block_id);
            EXPECT(detailed_timing.horizontal_addressable_pixels() == expected_timings.width);
            EXPECT(detailed_timing.vertical_addressable_lines() == expected_timings.height);
            EXPECT(detailed_timing.refresh_rate().lrint() == expected_timings.refresh_rate);
            detailed_timings_found++;
            return IterationDecision::Continue;
        }));
        EXPECT(result == IterationDecision::Continue);
        EXPECT(detailed_timings_found == expected_detailed_timings_count);
    }

    {
        static constexpr u8 expected_vic_ids[] = { 18, 19, 4, 31, 16, 20, 5, 1, 17, 2, 3, 74 };
        static constexpr size_t expected_vic_ids_count = sizeof(expected_vic_ids) / sizeof(expected_vic_ids[0]);
        size_t vic_ids_found = 0;
        auto result = TRY_OR_FAIL(edid.for_each_short_video_descriptor([&](unsigned block_id, bool is_native, EDID::VIC::Details const& vic) {
            EXPECT(vic_ids_found < expected_vic_ids_count);
            EXPECT(block_id == 1);
            EXPECT(is_native == (vic_ids_found == 4)); // the 5th value is marked native
            EXPECT(vic.vic_id == expected_vic_ids[vic_ids_found]);
            vic_ids_found++;
            return IterationDecision::Continue;
        }));
        EXPECT(result == IterationDecision::Continue);
        EXPECT(vic_ids_found == expected_vic_ids_count);
    }

    {
        // This edid has one CEA861 extension block only
        size_t extension_blocks_found = 0;
        auto result = TRY_OR_FAIL(edid.for_each_extension_block([&](unsigned block_id, u8 tag, u8 revision, ReadonlyBytes) {
            EXPECT(block_id == 1);
            EXPECT(tag == 0x2);
            EXPECT(revision == 3);
            extension_blocks_found++;
            return IterationDecision::Continue;
        }));
        EXPECT(result == IterationDecision::Continue);
        EXPECT(extension_blocks_found == 1);
    }
}

// This EDID has extension maps
static u8 const edid_extension_maps[] = {
    0x00, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0x00, 0x4d, 0x29, 0x48, 0x44,
    0x01, 0x00, 0x00, 0x00, 0x0a, 0x0d, 0x01, 0x03, 0x80, 0x50, 0x2d, 0x78,
    0x0a, 0x0d, 0xc9, 0xa0, 0x57, 0x47, 0x98, 0x27, 0x12, 0x48, 0x4c, 0x20,
    0x00, 0x00, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01,
    0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x1d, 0x80, 0x18, 0x71, 0x1c,
    0x16, 0x20, 0x58, 0x2c, 0x25, 0x00, 0x20, 0xc2, 0x31, 0x00, 0x00, 0x9e,
    0x8c, 0x0a, 0xd0, 0x8a, 0x20, 0xe0, 0x2d, 0x10, 0x10, 0x3e, 0x96, 0x00,
    0x13, 0x8e, 0x21, 0x00, 0x00, 0x18, 0x00, 0x00, 0x00, 0xfc, 0x00, 0x48,
    0x44, 0x4d, 0x49, 0x20, 0x54, 0x56, 0x0a, 0x20, 0x20, 0x20, 0x20, 0x20,
    0x00, 0x00, 0x00, 0xfd, 0x00, 0x3b, 0x3d, 0x0f, 0x2e, 0x08, 0x02, 0x00,
    0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x03, 0xf1, 0xf0, 0x02, 0x02, 0x00,
    0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
    0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
    0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
    0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
    0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
    0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
    0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
    0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
    0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
    0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
    0x00, 0x00, 0x00, 0x0c, 0x02, 0x03, 0x1e, 0xf1, 0x4a, 0x85, 0x04, 0x10,
    0x02, 0x01, 0x06, 0x14, 0x12, 0x16, 0x13, 0x23, 0x09, 0x07, 0x07, 0x83,
    0x01, 0x00, 0x00, 0x66, 0x03, 0x0c, 0x00, 0x10, 0x00, 0x80, 0x01, 0x1d,
    0x00, 0x72, 0x51, 0xd0, 0x1e, 0x20, 0x6e, 0x28, 0x55, 0x00, 0xc4, 0x8e,
    0x21, 0x00, 0x00, 0x1e, 0xd6, 0x09, 0x80, 0xa0, 0x20, 0xe0, 0x2d, 0x10,
    0x10, 0x60, 0x22, 0x00, 0x12, 0x8e, 0x21, 0x08, 0x08, 0x18, 0x8c, 0x0a,
    0xd0, 0x90, 0x20, 0x40, 0x31, 0x20, 0x0c, 0x40, 0x55, 0x00, 0xc4, 0x8e,
    0x21, 0x00, 0x00, 0x18, 0x01, 0x1d, 0x80, 0xd0, 0x72, 0x1c, 0x16, 0x20,
    0x10, 0x2c, 0x25, 0x80, 0xc4, 0x8e, 0x21, 0x00, 0x00, 0x9e, 0x8c, 0x0a,
    0xa0, 0x14, 0x51, 0xf0, 0x16, 0x00, 0x26, 0x7c, 0x43, 0x00, 0x13, 0x8e,
    0x21, 0x00, 0x00, 0x98, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0xf5,
    0x02, 0x03, 0x04, 0xf1, 0xf3, 0x39, 0x80, 0x18, 0x71, 0x38, 0x2d, 0x40,
    0x58, 0x2c, 0x45, 0x00, 0xc4, 0x8e, 0x21, 0x00, 0x00, 0x1e, 0x8c, 0x0a,
    0xa0, 0x20, 0x51, 0x20, 0x18, 0x10, 0x18, 0x7e, 0x23, 0x00, 0xc4, 0x8e,
    0x21, 0x00, 0x00, 0x98, 0x01, 0x1d, 0x00, 0xbc, 0x52, 0xd0, 0x1e, 0x20,
    0xb8, 0x28, 0x55, 0x40, 0xc4, 0x8e, 0x21, 0x00, 0x00, 0x1e, 0x00, 0x00,
    0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
    0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
    0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
    0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
    0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
    0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0xdf
};

TEST_CASE(edid_extension_maps)
{
    auto edid = TRY_OR_FAIL(EDID::Parser::from_bytes({ edid_extension_maps, sizeof(edid_extension_maps) }));
    EXPECT(edid.legacy_manufacturer_id() == "SII");

    {
        static constexpr struct {
            unsigned block_id;
            unsigned width;
            unsigned height;
            unsigned refresh_rate;
        } expected_detailed_timings[] = {
            { 0, 1920, 1080, 60 },
            { 0, 720, 480, 60 },
            { 2, 1280, 720, 60 },
            { 2, 640, 480, 60 },
            { 2, 720, 576, 50 },
            { 2, 1920, 1080, 50 },
            { 2, 1440, 480, 60 },
            { 3, 1920, 1080, 60 },
            { 3, 1440, 576, 50 },
            { 3, 1280, 720, 50 }
        };
        static constexpr size_t expected_detailed_timings_count = sizeof(expected_detailed_timings) / sizeof(expected_detailed_timings[0]);
        size_t detailed_timings_found = 0;
        auto result = TRY_OR_FAIL(edid.for_each_detailed_timing([&](auto& detailed_timing, unsigned block_id) {
            EXPECT(detailed_timings_found < expected_detailed_timings_count);
            auto& expected_timings = expected_detailed_timings[detailed_timings_found];
            EXPECT(block_id == expected_timings.block_id);
            EXPECT(detailed_timing.horizontal_addressable_pixels() == expected_timings.width);
            EXPECT(detailed_timing.vertical_addressable_lines() == expected_timings.height);
            EXPECT(detailed_timing.refresh_rate().lrint() == expected_timings.refresh_rate);
            detailed_timings_found++;
            return IterationDecision::Continue;
        }));
        EXPECT(result == IterationDecision::Continue);
        EXPECT(detailed_timings_found == expected_detailed_timings_count);
    }
}

static u8 const edid_1_0[] = {
    0x00, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0x00, 0x34, 0x38, 0xc2, 0x0b,
    0x7b, 0x00, 0x00, 0x00, 0x0f, 0x0a, 0x01, 0x00, 0x28, 0x20, 0x18, 0x32,
    0xe8, 0x7e, 0x4e, 0x9e, 0x57, 0x45, 0x98, 0x24, 0x10, 0x47, 0x4f, 0xa4,
    0x42, 0x01, 0x31, 0x59, 0x45, 0x59, 0x61, 0x59, 0x71, 0x4f, 0x81, 0x80,
    0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0xf9, 0x15, 0x20, 0xf8, 0x30, 0x58,
    0x1f, 0x20, 0x20, 0x40, 0x13, 0x00, 0x40, 0xf0, 0x10, 0x00, 0x00, 0x1e,
    0xa4, 0x1a, 0x20, 0x10, 0x31, 0x58, 0x24, 0x20, 0x2f, 0x55, 0x33, 0x00,
    0x40, 0xf0, 0x10, 0x00, 0x00, 0x1e, 0x30, 0x2a, 0x00, 0x98, 0x51, 0x00,
    0x2a, 0x40, 0x30, 0x70, 0x13, 0x00, 0x40, 0xf0, 0x10, 0x00, 0x00, 0x1e,
    0xea, 0x24, 0x00, 0x60, 0x41, 0x00, 0x28, 0x30, 0x30, 0x60, 0x13, 0x00,
    0x40, 0xf0, 0x10, 0x00, 0x00, 0x1e, 0x00, 0x72
};

TEST_CASE(edid_1_0)
{
    auto edid = TRY_OR_FAIL(EDID::Parser::from_bytes({ edid_1_0, sizeof(edid_1_0) }));
    EXPECT(edid.legacy_manufacturer_id() == "MAX");
    EXPECT(edid.serial_number() == 123);

    {
        static constexpr struct {
            unsigned block_id;
            unsigned width;
            unsigned height;
            unsigned refresh_rate;
        } expected_detailed_timings[] = {
            { 0, 800, 600, 85 },
            { 0, 800, 600, 100 },
            { 0, 1280, 1024, 60 },
            { 0, 1024, 768, 85 }
        };
        static constexpr size_t expected_detailed_timings_count = sizeof(expected_detailed_timings) / sizeof(expected_detailed_timings[0]);
        size_t detailed_timings_found = 0;
        auto result = TRY_OR_FAIL(edid.for_each_detailed_timing([&](auto& detailed_timing, unsigned block_id) {
            EXPECT(detailed_timings_found < expected_detailed_timings_count);
            auto& expected_timings = expected_detailed_timings[detailed_timings_found];
            EXPECT(block_id == expected_timings.block_id);
            EXPECT(detailed_timing.horizontal_addressable_pixels() == expected_timings.width);
            EXPECT(detailed_timing.vertical_addressable_lines() == expected_timings.height);
            EXPECT(detailed_timing.refresh_rate().lrint() == expected_timings.refresh_rate);
            detailed_timings_found++;
            return IterationDecision::Continue;
        }));
        EXPECT(result == IterationDecision::Continue);
        EXPECT(detailed_timings_found == expected_detailed_timings_count);
    }
}

TEST_CASE(dmt_find_std_id)
{
    auto* dmt = EDID::DMT::find_timing_by_std_id(0xd1, 0xf);
    EXPECT(dmt);
    EXPECT(dmt->dmt_id == 0x46);
    EXPECT(dmt->horizontal_pixels == 1920 && dmt->vertical_lines == 1200);
}

TEST_CASE(dmt_frequency)
{
    auto* dmt = EDID::DMT::find_timing_by_dmt_id(0x4);
    EXPECT(dmt);

    // FIXME: Use the FixedPoint(double) ctor like `expected_vertical_frequency(59.940)` instead of
    //        dividing by 1000 in the next line once FixedPoint::operator/ rounds.
    //        1. DMT.cpp is built as part of the kernel (despite being in Userland/)
    //        2. The Kernel can't use floating point
    //        3. So it has to use FixedPoint(59940) / 1000
    //        4. The FixedPoint(double) ctor rounds, but FixedPoint::operator/ currently doesn't,
    //           so FixedPoint(59.940) has a different lowest bit than
    //           FixedPoint(59940) / 1000. So the test can't use the FixedPoint(double) ctor at the moment.
    static FixedPoint<16, u32> const expected_vertical_frequency(59940);
    EXPECT(dmt->vertical_frequency_hz() == expected_vertical_frequency / 1000);
    static FixedPoint<16, u32> const expected_horizontal_frequency(31469);
    EXPECT(dmt->horizontal_frequency_khz() == expected_horizontal_frequency / 1000);
}

TEST_CASE(vic)
{
    EXPECT(!EDID::VIC::find_details_by_vic_id(0));   // invalid
    EXPECT(!EDID::VIC::find_details_by_vic_id(160)); // forbidden range
    EXPECT(!EDID::VIC::find_details_by_vic_id(250)); // reserved
    auto* vic_def_32 = EDID::VIC::find_details_by_vic_id(32);
    EXPECT(vic_def_32);
    EXPECT(vic_def_32->vic_id == 32);
    auto* vic_def_200 = EDID::VIC::find_details_by_vic_id(200);
    EXPECT(vic_def_200);
    EXPECT(vic_def_200->vic_id == 200);

    for (unsigned vic_id = 0; vic_id <= 0xff; vic_id++) {
        auto* vic_def = EDID::VIC::find_details_by_vic_id((u8)vic_id);
        if (vic_def) {
            EXPECT((vic_id >= 1 && vic_id <= 127) || (vic_id >= 193 && vic_id <= 219));
            EXPECT(vic_def->vic_id == vic_id);
        } else {
            EXPECT(vic_id == 0 || (vic_id >= 128 && vic_id <= 192) || (vic_id >= 220));
        }
    }
}
