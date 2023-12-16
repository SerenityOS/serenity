/*
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibEDID/DMT.h>

#ifndef KERNEL
#    include <AK/ByteString.h>
#endif

namespace EDID {

// Monitor timings as per Display Monitor Timing Standard (DMT) 1.0 rev 13
static constexpr DMT::MonitorTiming s_monitor_timings[] = {
    { 0x1, {}, 8, 640, 350, 37861, 85080, 31500, 32, 32, 192, 95, 64, 3 },
    { 0x2, { 0x31, 0x19 }, 8, 640, 400, 37861, 85080, 31500, 32, 1, 192, 45, 64, 3 },
    { 0x3, {}, 9, 720, 400, 37927, 85039, 35500, 36, 1, 216, 46, 72, 3 },
    { 0x4, { 0x31, 0x40 }, 8, 640, 480, 31469, 59940, 25175, 8, 2, 144, 29, 96, 2 },
    { 0x5, { 0x31, 0x4c }, 8, 640, 480, 37861, 72809, 31500, 16, 1, 176, 24, 40, 3 },
    { 0x6, { 0x31, 0x4f }, 8, 640, 480, 37500, 75000, 31500, 16, 1, 200, 20, 64, 3 },
    { 0x7, { 0x31, 0x59 }, 8, 640, 480, 43269, 85008, 36000, 56, 1, 192, 29, 56, 3 },
    { 0x8, {}, 8, 800, 600, 35156, 56250, 36000, 24, 1, 224, 25, 72, 2 },
    { 0x9, { 0x45, 0x40 }, 8, 800, 600, 37879, 60317, 40000, 40, 1, 256, 28, 128, 4 },
    { 0xa, { 0x45, 0x4c }, 8, 800, 600, 48077, 72188, 50000, 56, 37, 240, 66, 120, 6 },
    { 0xb, { 0x45, 0x4f }, 8, 800, 600, 46875, 75000, 49500, 16, 1, 256, 25, 80, 3 },
    { 0xc, { 0x45, 0x59 }, 8, 800, 600, 53674, 85061, 56250, 32, 1, 248, 31, 64, 3 },
    { 0xd, {}, 8, 800, 600, 76302, 119972, 73250, 48, 3, 160, 36, 32, 4, DMT::MonitorTiming::CVTCompliance::CompliantReducedBlanking },
    { 0xe, {}, 8, 848, 480, 31020, 60000, 33750, 16, 6, 240, 37, 112, 8 },
    { 0xf, {}, 8, 1024, 768, 35522, 86957, 44900, 8, 0, 240, 24, 176, 4, DMT::MonitorTiming::CVTCompliance::NotCompliant, {}, DMT::MonitorTiming::ScanType::Interlaced },
    { 0x10, { 0x61, 0x40 }, 8, 1024, 768, 48363, 60004, 65000, 24, 3, 320, 38, 136, 6 },
    { 0x11, { 0x61, 0x4a }, 8, 1024, 768, 56476, 70069, 75000, 24, 3, 304, 38, 136, 6 },
    { 0x12, { 0x61, 0x4f }, 8, 1024, 768, 60023, 75029, 78750, 16, 1, 288, 32, 96, 3 },
    { 0x13, { 0x61, 0x59 }, 8, 1024, 768, 68677, 84997, 94500, 48, 1, 352, 40, 96, 3 },
    { 0x14, {}, 8, 1024, 768, 97551, 119989, 115500, 48, 3, 160, 45, 32, 4, DMT::MonitorTiming::CVTCompliance::CompliantReducedBlanking },
    { 0x15, { 0x71, 0x4f }, 8, 1152, 864, 67500, 75000, 108000, 64, 1, 448, 36, 128, 3 },
    { 0x55, { 0x81, 0xc0 }, 1, 1280, 720, 45000, 60000, 74250, 110, 5, 370, 30, 40, 5 },
    { 0x16, {}, 8, 1280, 768, 47396, 59995, 68250, 48, 3, 160, 22, 32, 7, DMT::MonitorTiming::CVTCompliance::CompliantReducedBlanking, { 0x7f, 0x1c, 0x22 } },
    { 0x17, {}, 8, 1280, 768, 47776, 59870, 79500, 64, 3, 384, 30, 128, 7, DMT::MonitorTiming::CVTCompliance::Compliant, { 0x7f, 0x1c, 0x28 } },
    { 0x18, {}, 8, 1280, 768, 60289, 74893, 102250, 80, 3, 416, 37, 128, 7, DMT::MonitorTiming::CVTCompliance::Compliant, { 0x7f, 0x1c, 0x44 } },
    { 0x19, {}, 8, 1280, 768, 68633, 84837, 117500, 80, 3, 432, 41, 136, 7, DMT::MonitorTiming::CVTCompliance::Compliant, { 0x7f, 0x1c, 0x62 } },
    { 0x1a, {}, 8, 1280, 768, 97396, 119798, 140250, 48, 3, 160, 45, 32, 7, DMT::MonitorTiming::CVTCompliance::CompliantReducedBlanking },
    { 0x1b, {}, 8, 1280, 800, 49306, 59910, 71000, 48, 3, 160, 23, 32, 6, DMT::MonitorTiming::CVTCompliance::CompliantReducedBlanking, { 0x8f, 0x18, 0x21 } },
    { 0x1c, { 0x81, 0x0 }, 8, 1280, 800, 49702, 59810, 83500, 72, 3, 400, 31, 128, 6, DMT::MonitorTiming::CVTCompliance::Compliant, { 0x8f, 0x18, 0x28 } },
    { 0x1d, { 0x81, 0xf }, 8, 1280, 800, 62795, 74934, 106500, 80, 3, 416, 38, 128, 6, DMT::MonitorTiming::CVTCompliance::Compliant, { 0x8f, 0x18, 0x44 } },
    { 0x1e, { 0x81, 0x19 }, 8, 1280, 800, 71554, 84880, 122500, 80, 3, 432, 43, 136, 6, DMT::MonitorTiming::CVTCompliance::Compliant, { 0x8f, 0x18, 0x62 } },
    { 0x1f, {}, 8, 1280, 800, 101563, 119909, 146250, 48, 3, 160, 47, 32, 6, DMT::MonitorTiming::CVTCompliance::CompliantReducedBlanking },
    { 0x20, { 0x81, 0x40 }, 8, 1280, 960, 60000, 60000, 108000, 96, 1, 520, 40, 112, 3 },
    { 0x21, { 0x81, 0x59 }, 8, 1280, 960, 85938, 85002, 148500, 64, 1, 448, 3, 160, 3 },
    { 0x22, {}, 8, 1280, 960, 121875, 119838, 175500, 48, 3, 160, 57, 32, 4, DMT::MonitorTiming::CVTCompliance::CompliantReducedBlanking },
    { 0x23, { 0x81, 0x80 }, 8, 1280, 1024, 63981, 60020, 108000, 48, 1, 408, 42, 112, 3 },
    { 0x24, { 0x81, 0x8f }, 8, 1280, 1024, 79976, 75025, 135000, 16, 1, 408, 42, 144, 3 },
    { 0x25, { 0x81, 0x99 }, 8, 1280, 1024, 91146, 85024, 157500, 64, 1, 448, 48, 160, 3 },
    { 0x26, {}, 8, 1280, 1024, 130035, 119958, 187250, 48, 3, 160, 60, 32, 7, DMT::MonitorTiming::CVTCompliance::CompliantReducedBlanking },
    { 0x27, {}, 8, 1360, 768, 47712, 60015, 85500, 64, 3, 432, 27, 112, 6 },
    { 0x28, {}, 8, 1360, 768, 97533, 119967, 148250, 48, 5, 160, 45, 32, 5, DMT::MonitorTiming::CVTCompliance::CompliantReducedBlanking },
    { 0x51, {}, 1, 1366, 768, 57712, 59790, 85500, 70, 3, 426, 30, 143, 3 },
    { 0x56, {}, 1, 1366, 768, 48000, 60000, 72000, 14, 1, 134, 32, 56, 3 },
    { 0x29, {}, 8, 1400, 1050, 64744, 59948, 101000, 48, 3, 160, 30, 32, 4, DMT::MonitorTiming::CVTCompliance::CompliantReducedBlanking, { 0x0c, 0x20, 0x21 } },
    { 0x2a, { 0x90, 0x40 }, 8, 1400, 1050, 65317, 59978, 121750, 88, 3, 464, 39, 144, 4, DMT::MonitorTiming::CVTCompliance::Compliant, { 0x0c, 0x20, 0x28 } },
    { 0x2b, { 0x90, 0x4f }, 8, 1400, 1050, 82278, 74867, 156000, 104, 3, 496, 49, 144, 4, DMT::MonitorTiming::CVTCompliance::Compliant, { 0x0c, 0x20, 0x44 } },
    { 0x2c, { 0x90, 0x59 }, 8, 1400, 1050, 93881, 84960, 179500, 104, 3, 512, 55, 152, 4, DMT::MonitorTiming::CVTCompliance::Compliant, { 0x0c, 0x20, 0x62 } },
    { 0x2d, {}, 8, 1400, 1050, 133333, 119904, 208000, 48, 3, 160, 62, 32, 4, DMT::MonitorTiming::CVTCompliance::CompliantReducedBlanking },
    { 0x2e, {}, 8, 1440, 900, 55469, 59901, 88750, 48, 3, 160, 26, 32, 6, DMT::MonitorTiming::CVTCompliance::CompliantReducedBlanking, { 0xc1, 0x18, 0x21 } },
    { 0x2f, { 0x95, 0x0 }, 8, 1440, 900, 55935, 59887, 106500, 80, 3, 464, 34, 152, 6, DMT::MonitorTiming::CVTCompliance::Compliant, { 0xc1, 0x18, 0x28 } },
    { 0x30, { 0x95, 0xf }, 8, 1440, 900, 70635, 74984, 136750, 96, 3, 496, 42, 152, 6, DMT::MonitorTiming::CVTCompliance::Compliant, { 0xc1, 0x18, 0x44 } },
    { 0x31, { 0x95, 0x19 }, 8, 1440, 900, 80430, 84842, 157000, 104, 3, 512, 48, 152, 6, DMT::MonitorTiming::CVTCompliance::Compliant, { 0xc1, 0x18, 0x68 } },
    { 0x32, {}, 8, 1440, 900, 114219, 119852, 182750, 48, 3, 160, 53, 32, 6, DMT::MonitorTiming::CVTCompliance::CompliantReducedBlanking },
    { 0x53, { 0xa9, 0xc0 }, 8, 1600, 900, 60000, 60000, 108000, 24, 1, 200, 100, 80, 3 },
    { 0x33, { 0xa9, 0x40 }, 8, 1600, 1200, 75000, 60000, 162000, 64, 1, 560, 50, 192, 3 },
    { 0x34, { 0xa9, 0x45 }, 8, 1600, 1200, 81250, 65000, 175500, 64, 1, 560, 50, 192, 3 },
    { 0x35, { 0xa9, 0x4a }, 8, 1600, 1200, 87500, 70000, 189000, 64, 1, 560, 50, 192, 3 },
    { 0x36, { 0xa9, 0x4f }, 8, 1600, 1200, 93750, 75000, 202500, 64, 1, 560, 50, 192, 3 },
    { 0x37, { 0xa9, 0x59 }, 8, 1600, 1200, 106250, 85000, 229500, 64, 1, 560, 50, 192, 3 },
    { 0x38, {}, 8, 1600, 1200, 152415, 119917, 268250, 48, 3, 160, 71, 32, 4, DMT::MonitorTiming::CVTCompliance::CompliantReducedBlanking },
    { 0x39, {}, 8, 1680, 1050, 64674, 59883, 119000, 48, 3, 160, 30, 32, 6, DMT::MonitorTiming::CVTCompliance::CompliantReducedBlanking, { 0x0c, 0x28, 0x21 } },
    { 0x3a, { 0xb3, 0x0 }, 8, 1680, 1050, 65290, 59954, 146250, 104, 3, 560, 39, 176, 6, DMT::MonitorTiming::CVTCompliance::Compliant, { 0x0c, 0x28, 0x28 } },
    { 0x3b, { 0xb3, 0xf }, 8, 1680, 1050, 82306, 74892, 187000, 120, 3, 592, 49, 176, 6, DMT::MonitorTiming::CVTCompliance::Compliant, { 0x0c, 0x28, 0x44 } },
    { 0x3c, { 0xb3, 0x19 }, 8, 1680, 1050, 93859, 84941, 214750, 128, 3, 608, 55, 176, 6, DMT::MonitorTiming::CVTCompliance::Compliant, { 0x0c, 0x28, 0x68 } },
    { 0x3d, {}, 8, 1680, 1050, 133424, 119986, 245500, 48, 3, 160, 62, 32, 6, DMT::MonitorTiming::CVTCompliance::CompliantReducedBlanking },
    { 0x3e, { 0xc1, 0x40 }, 8, 1792, 1344, 83640, 60000, 204750, 128, 1, 656, 50, 200, 3 },
    { 0x3f, { 0xc1, 0x4f }, 8, 1792, 1344, 106270, 74997, 261000, 96, 1, 664, 73, 216, 3 },
    { 0x40, {}, 8, 1792, 1344, 170722, 119974, 333250, 48, 3, 160, 79, 32, 4, DMT::MonitorTiming::CVTCompliance::CompliantReducedBlanking },
    { 0x41, { 0xc9, 0x40 }, 8, 1856, 1392, 86333, 59995, 218250, 96, 1, 672, 47, 224, 3 },
    { 0x42, { 0xc9, 0x4f }, 8, 1856, 1392, 112500, 75000, 288000, 128, 1, 704, 108, 224, 3 },
    { 0x43, {}, 8, 1856, 1392, 176835, 119970, 356500, 48, 3, 160, 82, 32, 4, DMT::MonitorTiming::CVTCompliance::CompliantReducedBlanking },
    { 0x52, { 0xd1, 0xc0 }, 4, 1920, 1080, 67500, 60000, 148500, 88, 4, 280, 45, 44, 5 },
    { 0x44, {}, 8, 1920, 1200, 74038, 59950, 154000, 48, 3, 160, 35, 32, 6, DMT::MonitorTiming::CVTCompliance::CompliantReducedBlanking, { 0x57, 0x28, 0x21 } },
    { 0x45, { 0xd1, 0x0 }, 8, 1920, 1200, 74556, 59885, 193250, 136, 3, 672, 45, 200, 6, DMT::MonitorTiming::CVTCompliance::Compliant, { 0x57, 0x28, 0x28 } },
    { 0x46, { 0xd1, 0xf }, 8, 1920, 1200, 94038, 74930, 245250, 136, 3, 688, 55, 208, 6, DMT::MonitorTiming::CVTCompliance::Compliant, { 0x57, 0x28, 0x44 } },
    { 0x47, { 0xd1, 0x19 }, 8, 1920, 1200, 107184, 84932, 281250, 144, 3, 704, 62, 208, 6, DMT::MonitorTiming::CVTCompliance::Compliant, { 0x57, 0x28, 0x62 } },
    { 0x48, {}, 8, 1920, 1200, 152404, 119909, 317000, 48, 3, 160, 71, 32, 6, DMT::MonitorTiming::CVTCompliance::CompliantReducedBlanking },
    { 0x49, { 0xd1, 0x40 }, 8, 1920, 1440, 90000, 60000, 234000, 128, 1, 680, 60, 208, 3 },
    { 0x4a, { 0xd1, 0x4f }, 8, 1920, 1440, 112500, 75000, 297000, 144, 1, 720, 60, 224, 3 },
    { 0x4b, {}, 8, 1920, 1440, 182933, 119956, 380500, 48, 3, 160, 85, 32, 4, DMT::MonitorTiming::CVTCompliance::CompliantReducedBlanking },
    { 0x54, { 0xe1, 0xc0 }, 1, 2048, 1152, 72000, 60000, 162000, 26, 1, 202, 48, 80, 3 },
    { 0x4c, {}, 8, 2560, 1600, 98713, 59972, 268500, 48, 3, 160, 46, 32, 6, DMT::MonitorTiming::CVTCompliance::Compliant, { 0x1f, 0x38, 0x21 } },
    { 0x4d, {}, 8, 2650, 1600, 99458, 59987, 348500, 192, 3, 944, 58, 280, 6, DMT::MonitorTiming::CVTCompliance::Compliant, { 0x1f, 0x38, 0x28 } },
    { 0x4e, {}, 8, 2560, 1600, 125354, 74972, 443250, 208, 3, 976, 72, 280, 6, DMT::MonitorTiming::CVTCompliance::Compliant, { 0x1f, 0x38, 0x44 } },
    { 0x4f, {}, 8, 2560, 1600, 142887, 84951, 505250, 208, 3, 976, 82, 280, 6, DMT::MonitorTiming::CVTCompliance::Compliant, { 0x1f, 0x38, 0x62 } },
    { 0x50, {}, 8, 2560, 1600, 203217, 119963, 552750, 48, 3, 160, 94, 32, 6, DMT::MonitorTiming::CVTCompliance::CompliantReducedBlanking },
    { 0x57, {}, 1, 4096, 2160, 133320, 60000, 556744, 8, 48, 80, 62, 32, 8, DMT::MonitorTiming::CVTCompliance::CompliantReducedBlankingV2 },
    { 0x58, {}, 1, 4096, 2160, 133187, 59940, 556188, 8, 48, 80, 62, 32, 8, DMT::MonitorTiming::CVTCompliance::CompliantReducedBlankingV2 },
};

FixedPoint<16, u32> DMT::MonitorTiming::horizontal_frequency_khz() const
{
    return FixedPoint<16, u32>(horizontal_frequency_hz) / 1000;
}

FixedPoint<16, u32> DMT::MonitorTiming::vertical_frequency_hz() const
{
    return FixedPoint<16, u32>(vertical_frequency_millihz) / 1000;
}

u32 DMT::MonitorTiming::refresh_rate_hz() const
{
    return vertical_frequency_hz().ltrunc();
}

#ifndef KERNEL
ByteString DMT::MonitorTiming::name() const
{
    if (scan_type == ScanType::Interlaced)
        return ByteString::formatted("{} x {} @ {}Hz (Interlaced)", horizontal_pixels, vertical_lines, refresh_rate_hz());
    return ByteString::formatted("{} x {} @ {}Hz", horizontal_pixels, vertical_lines, refresh_rate_hz());
}
#endif

auto DMT::find_timing_by_dmt_id(u8 dmt_id) -> MonitorTiming const*
{
    if (dmt_id == 0)
        return nullptr;

    for (auto& monitor_timing : s_monitor_timings) {
        if (monitor_timing.dmt_id == dmt_id)
            return &monitor_timing;
    }

    return nullptr;
}

auto DMT::find_timing_by_std_id(u8 std_id_byte1, u8 std_id_byte2) -> MonitorTiming const*
{
    for (auto& monitor_timing : s_monitor_timings) {
        if (!monitor_timing.has_std())
            continue;
        if (monitor_timing.std_bytes[0] == std_id_byte1 && monitor_timing.std_bytes[1] == std_id_byte2)
            return &monitor_timing;
    }

    return nullptr;
}

auto DMT::find_timing_by_cvt(CVT cvt) -> MonitorTiming const*
{
    for (auto& monitor_timing : s_monitor_timings) {
        if (!monitor_timing.has_cvt())
            continue;
        if (monitor_timing.cvt_bytes[0] == cvt.bytes[0] && monitor_timing.cvt_bytes[1] == cvt.bytes[1] && monitor_timing.cvt_bytes[2] == cvt.bytes[2])
            return &monitor_timing;
    }

    return nullptr;
}

}
