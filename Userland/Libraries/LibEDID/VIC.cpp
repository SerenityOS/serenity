/*
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibEDID/VIC.h>

namespace EDID {

// Video ID Code details as per CTA-861-G revised 2018 Table 3
static constexpr VIC::Details s_vic_details[] = {
    { 1, 640, 480, 59940, VIC::Details::ScanType::NonInterlaced, VIC::Details::AspectRatio::AR_4_3 },
    { 2, 720, 480, 59940, VIC::Details::ScanType::NonInterlaced, VIC::Details::AspectRatio::AR_4_3 },
    { 3, 720, 480, 59940, VIC::Details::ScanType::NonInterlaced, VIC::Details::AspectRatio::AR_16_9 },
    { 4, 1280, 720, 59940, VIC::Details::ScanType::NonInterlaced, VIC::Details::AspectRatio::AR_16_9 },
    { 5, 1920, 1080, 59940, VIC::Details::ScanType::Interlaced, VIC::Details::AspectRatio::AR_16_9 },
    { 6, 1440, 480, 59940, VIC::Details::ScanType::Interlaced, VIC::Details::AspectRatio::AR_4_3 },
    { 7, 1440, 480, 59940, VIC::Details::ScanType::Interlaced, VIC::Details::AspectRatio::AR_16_9 },
    { 8, 1440, 240, 59940, VIC::Details::ScanType::NonInterlaced, VIC::Details::AspectRatio::AR_4_3 },
    { 9, 1440, 240, 59940, VIC::Details::ScanType::NonInterlaced, VIC::Details::AspectRatio::AR_16_9 },
    { 10, 2880, 480, 59940, VIC::Details::ScanType::Interlaced, VIC::Details::AspectRatio::AR_4_3 },
    { 11, 2880, 480, 59940, VIC::Details::ScanType::Interlaced, VIC::Details::AspectRatio::AR_16_9 },
    { 12, 2880, 240, 59940, VIC::Details::ScanType::NonInterlaced, VIC::Details::AspectRatio::AR_4_3 },
    { 13, 2880, 240, 59940, VIC::Details::ScanType::NonInterlaced, VIC::Details::AspectRatio::AR_16_9 },
    { 14, 1440, 480, 59940, VIC::Details::ScanType::NonInterlaced, VIC::Details::AspectRatio::AR_4_3 },
    { 15, 1440, 480, 59940, VIC::Details::ScanType::NonInterlaced, VIC::Details::AspectRatio::AR_16_9 },
    { 16, 1920, 180, 59940, VIC::Details::ScanType::NonInterlaced, VIC::Details::AspectRatio::AR_16_9 },
    { 17, 720, 576, 50000, VIC::Details::ScanType::NonInterlaced, VIC::Details::AspectRatio::AR_4_3 },
    { 18, 720, 576, 50000, VIC::Details::ScanType::NonInterlaced, VIC::Details::AspectRatio::AR_16_9 },
    { 19, 1280, 720, 50000, VIC::Details::ScanType::NonInterlaced, VIC::Details::AspectRatio::AR_16_9 },
    { 20, 1920, 1080, 50000, VIC::Details::ScanType::Interlaced, VIC::Details::AspectRatio::AR_16_9 },
    { 21, 1440, 576, 50000, VIC::Details::ScanType::Interlaced, VIC::Details::AspectRatio::AR_4_3 },
    { 22, 1440, 576, 50000, VIC::Details::ScanType::Interlaced, VIC::Details::AspectRatio::AR_16_9 },
    { 23, 1440, 288, 50000, VIC::Details::ScanType::NonInterlaced, VIC::Details::AspectRatio::AR_4_3 },
    { 24, 1440, 288, 50000, VIC::Details::ScanType::NonInterlaced, VIC::Details::AspectRatio::AR_16_9 },
    { 25, 2880, 576, 50000, VIC::Details::ScanType::Interlaced, VIC::Details::AspectRatio::AR_4_3 },
    { 26, 2880, 576, 50000, VIC::Details::ScanType::Interlaced, VIC::Details::AspectRatio::AR_16_9 },
    { 27, 2880, 288, 50000, VIC::Details::ScanType::NonInterlaced, VIC::Details::AspectRatio::AR_4_3 },
    { 28, 2880, 288, 50000, VIC::Details::ScanType::NonInterlaced, VIC::Details::AspectRatio::AR_16_9 },
    { 29, 1440, 576, 50000, VIC::Details::ScanType::NonInterlaced, VIC::Details::AspectRatio::AR_4_3 },
    { 30, 1440, 576, 50000, VIC::Details::ScanType::NonInterlaced, VIC::Details::AspectRatio::AR_16_9 },
    { 31, 1920, 1080, 50000, VIC::Details::ScanType::NonInterlaced, VIC::Details::AspectRatio::AR_16_9 },
    { 32, 1920, 1080, 23980, VIC::Details::ScanType::NonInterlaced, VIC::Details::AspectRatio::AR_16_9 },
    { 33, 1920, 1080, 25000, VIC::Details::ScanType::NonInterlaced, VIC::Details::AspectRatio::AR_16_9 },
    { 34, 1920, 1080, 29970, VIC::Details::ScanType::NonInterlaced, VIC::Details::AspectRatio::AR_16_9 },
    { 35, 2880, 480, 59940, VIC::Details::ScanType::NonInterlaced, VIC::Details::AspectRatio::AR_4_3 },
    { 36, 2880, 480, 59940, VIC::Details::ScanType::NonInterlaced, VIC::Details::AspectRatio::AR_16_9 },
    { 37, 2880, 576, 50000, VIC::Details::ScanType::NonInterlaced, VIC::Details::AspectRatio::AR_4_3 },
    { 38, 2880, 576, 50000, VIC::Details::ScanType::NonInterlaced, VIC::Details::AspectRatio::AR_16_9 },
    { 39, 1920, 1080, 50000, VIC::Details::ScanType::Interlaced, VIC::Details::AspectRatio::AR_16_9 },
    { 40, 1920, 1080, 100000, VIC::Details::ScanType::Interlaced, VIC::Details::AspectRatio::AR_16_9 },
    { 41, 1280, 720, 100000, VIC::Details::ScanType::NonInterlaced, VIC::Details::AspectRatio::AR_16_9 },
    { 42, 720, 576, 100000, VIC::Details::ScanType::NonInterlaced, VIC::Details::AspectRatio::AR_4_3 },
    { 43, 720, 576, 100000, VIC::Details::ScanType::NonInterlaced, VIC::Details::AspectRatio::AR_16_9 },
    { 44, 1440, 576, 100000, VIC::Details::ScanType::Interlaced, VIC::Details::AspectRatio::AR_4_3 },
    { 45, 1440, 576, 100000, VIC::Details::ScanType::Interlaced, VIC::Details::AspectRatio::AR_16_9 },
    { 46, 1920, 1080, 119880, VIC::Details::ScanType::Interlaced, VIC::Details::AspectRatio::AR_16_9 },
    { 47, 1280, 720, 119880, VIC::Details::ScanType::NonInterlaced, VIC::Details::AspectRatio::AR_16_9 },
    { 48, 720, 480, 119880, VIC::Details::ScanType::NonInterlaced, VIC::Details::AspectRatio::AR_4_3 },
    { 49, 720, 480, 119880, VIC::Details::ScanType::NonInterlaced, VIC::Details::AspectRatio::AR_16_9 },
    { 50, 1440, 480, 119880, VIC::Details::ScanType::Interlaced, VIC::Details::AspectRatio::AR_4_3 },
    { 51, 1440, 480, 119880, VIC::Details::ScanType::Interlaced, VIC::Details::AspectRatio::AR_16_9 },
    { 52, 720, 576, 200000, VIC::Details::ScanType::NonInterlaced, VIC::Details::AspectRatio::AR_4_3 },
    { 53, 720, 576, 200000, VIC::Details::ScanType::NonInterlaced, VIC::Details::AspectRatio::AR_16_9 },
    { 54, 1440, 576, 200000, VIC::Details::ScanType::Interlaced, VIC::Details::AspectRatio::AR_4_3 },
    { 55, 1440, 576, 200000, VIC::Details::ScanType::Interlaced, VIC::Details::AspectRatio::AR_16_9 },
    { 56, 720, 480, 239760, VIC::Details::ScanType::NonInterlaced, VIC::Details::AspectRatio::AR_4_3 },
    { 57, 720, 480, 239760, VIC::Details::ScanType::NonInterlaced, VIC::Details::AspectRatio::AR_16_9 },
    { 58, 1440, 480, 239760, VIC::Details::ScanType::Interlaced, VIC::Details::AspectRatio::AR_4_3 },
    { 59, 1440, 480, 239760, VIC::Details::ScanType::Interlaced, VIC::Details::AspectRatio::AR_16_9 },
    { 60, 1280, 720, 23980, VIC::Details::ScanType::NonInterlaced, VIC::Details::AspectRatio::AR_16_9 },
    { 61, 1280, 720, 25000, VIC::Details::ScanType::NonInterlaced, VIC::Details::AspectRatio::AR_16_9 },
    { 62, 1280, 720, 29970, VIC::Details::ScanType::NonInterlaced, VIC::Details::AspectRatio::AR_16_9 },
    { 63, 1920, 1080, 119880, VIC::Details::ScanType::NonInterlaced, VIC::Details::AspectRatio::AR_16_9 },
    { 64, 1920, 1080, 100000, VIC::Details::ScanType::NonInterlaced, VIC::Details::AspectRatio::AR_16_9 },
    { 65, 1280, 720, 23980, VIC::Details::ScanType::NonInterlaced, VIC::Details::AspectRatio::AR_64_27 },
    { 66, 1280, 720, 25000, VIC::Details::ScanType::NonInterlaced, VIC::Details::AspectRatio::AR_64_27 },
    { 67, 1280, 720, 29970, VIC::Details::ScanType::NonInterlaced, VIC::Details::AspectRatio::AR_64_27 },
    { 68, 1280, 720, 50000, VIC::Details::ScanType::NonInterlaced, VIC::Details::AspectRatio::AR_64_27 },
    { 69, 1280, 720, 59940, VIC::Details::ScanType::NonInterlaced, VIC::Details::AspectRatio::AR_64_27 },
    { 70, 1280, 720, 100000, VIC::Details::ScanType::NonInterlaced, VIC::Details::AspectRatio::AR_64_27 },
    { 71, 1280, 720, 119880, VIC::Details::ScanType::NonInterlaced, VIC::Details::AspectRatio::AR_64_27 },
    { 72, 1920, 1080, 23980, VIC::Details::ScanType::NonInterlaced, VIC::Details::AspectRatio::AR_64_27 },
    { 73, 1920, 1080, 25000, VIC::Details::ScanType::NonInterlaced, VIC::Details::AspectRatio::AR_64_27 },
    { 74, 1920, 1080, 29970, VIC::Details::ScanType::NonInterlaced, VIC::Details::AspectRatio::AR_64_27 },
    { 75, 1920, 1080, 50000, VIC::Details::ScanType::NonInterlaced, VIC::Details::AspectRatio::AR_64_27 },
    { 76, 1920, 1080, 59940, VIC::Details::ScanType::NonInterlaced, VIC::Details::AspectRatio::AR_64_27 },
    { 77, 1920, 1080, 100000, VIC::Details::ScanType::NonInterlaced, VIC::Details::AspectRatio::AR_64_27 },
    { 78, 1920, 1080, 119880, VIC::Details::ScanType::NonInterlaced, VIC::Details::AspectRatio::AR_64_27 },
    { 79, 1680, 720, 23980, VIC::Details::ScanType::NonInterlaced, VIC::Details::AspectRatio::AR_64_27 },
    { 80, 1680, 720, 25000, VIC::Details::ScanType::NonInterlaced, VIC::Details::AspectRatio::AR_64_27 },
    { 81, 1680, 720, 29970, VIC::Details::ScanType::NonInterlaced, VIC::Details::AspectRatio::AR_64_27 },
    { 82, 1680, 720, 50000, VIC::Details::ScanType::NonInterlaced, VIC::Details::AspectRatio::AR_64_27 },
    { 83, 1680, 720, 59940, VIC::Details::ScanType::NonInterlaced, VIC::Details::AspectRatio::AR_64_27 },
    { 84, 1680, 720, 100000, VIC::Details::ScanType::NonInterlaced, VIC::Details::AspectRatio::AR_64_27 },
    { 85, 1680, 720, 119880, VIC::Details::ScanType::NonInterlaced, VIC::Details::AspectRatio::AR_64_27 },
    { 86, 2560, 1080, 23980, VIC::Details::ScanType::NonInterlaced, VIC::Details::AspectRatio::AR_64_27 },
    { 87, 2560, 1080, 25000, VIC::Details::ScanType::NonInterlaced, VIC::Details::AspectRatio::AR_64_27 },
    { 88, 2560, 1080, 29970, VIC::Details::ScanType::NonInterlaced, VIC::Details::AspectRatio::AR_64_27 },
    { 89, 2560, 1080, 50000, VIC::Details::ScanType::NonInterlaced, VIC::Details::AspectRatio::AR_64_27 },
    { 90, 2560, 1080, 59940, VIC::Details::ScanType::NonInterlaced, VIC::Details::AspectRatio::AR_64_27 },
    { 91, 2560, 1080, 100000, VIC::Details::ScanType::NonInterlaced, VIC::Details::AspectRatio::AR_64_27 },
    { 92, 2560, 1080, 119880, VIC::Details::ScanType::NonInterlaced, VIC::Details::AspectRatio::AR_64_27 },
    { 93, 3840, 2160, 23980, VIC::Details::ScanType::NonInterlaced, VIC::Details::AspectRatio::AR_16_9 },
    { 94, 3840, 2160, 25000, VIC::Details::ScanType::NonInterlaced, VIC::Details::AspectRatio::AR_16_9 },
    { 95, 3840, 2160, 29970, VIC::Details::ScanType::NonInterlaced, VIC::Details::AspectRatio::AR_16_9 },
    { 96, 3840, 2160, 50000, VIC::Details::ScanType::NonInterlaced, VIC::Details::AspectRatio::AR_16_9 },
    { 97, 3840, 2160, 59940, VIC::Details::ScanType::NonInterlaced, VIC::Details::AspectRatio::AR_16_9 },
    { 98, 4096, 2160, 23980, VIC::Details::ScanType::NonInterlaced, VIC::Details::AspectRatio::AR_256_135 },
    { 99, 4096, 2160, 25000, VIC::Details::ScanType::NonInterlaced, VIC::Details::AspectRatio::AR_256_135 },
    { 100, 4096, 2160, 29970, VIC::Details::ScanType::NonInterlaced, VIC::Details::AspectRatio::AR_256_135 },
    { 101, 4096, 2160, 50000, VIC::Details::ScanType::NonInterlaced, VIC::Details::AspectRatio::AR_256_135 },
    { 102, 4096, 2160, 59940, VIC::Details::ScanType::NonInterlaced, VIC::Details::AspectRatio::AR_256_135 },
    { 103, 3840, 2160, 23980, VIC::Details::ScanType::NonInterlaced, VIC::Details::AspectRatio::AR_64_27 },
    { 104, 3840, 2160, 25000, VIC::Details::ScanType::NonInterlaced, VIC::Details::AspectRatio::AR_64_27 },
    { 105, 3840, 2160, 29970, VIC::Details::ScanType::NonInterlaced, VIC::Details::AspectRatio::AR_64_27 },
    { 106, 3840, 2160, 50000, VIC::Details::ScanType::NonInterlaced, VIC::Details::AspectRatio::AR_64_27 },
    { 107, 3840, 2160, 59940, VIC::Details::ScanType::NonInterlaced, VIC::Details::AspectRatio::AR_64_27 },
    { 108, 1280, 720, 47950, VIC::Details::ScanType::NonInterlaced, VIC::Details::AspectRatio::AR_16_9 },
    { 109, 1280, 720, 47950, VIC::Details::ScanType::NonInterlaced, VIC::Details::AspectRatio::AR_64_27 },
    { 110, 1680, 720, 47950, VIC::Details::ScanType::NonInterlaced, VIC::Details::AspectRatio::AR_16_9 },
    { 111, 1920, 1080, 47950, VIC::Details::ScanType::NonInterlaced, VIC::Details::AspectRatio::AR_16_9 },
    { 112, 1920, 1080, 47950, VIC::Details::ScanType::NonInterlaced, VIC::Details::AspectRatio::AR_64_27 },
    { 113, 2560, 1080, 47950, VIC::Details::ScanType::NonInterlaced, VIC::Details::AspectRatio::AR_64_27 },
    { 114, 3840, 2160, 47950, VIC::Details::ScanType::NonInterlaced, VIC::Details::AspectRatio::AR_16_9 },
    { 115, 4096, 2160, 47950, VIC::Details::ScanType::NonInterlaced, VIC::Details::AspectRatio::AR_256_135 },
    { 116, 3840, 2160, 47950, VIC::Details::ScanType::NonInterlaced, VIC::Details::AspectRatio::AR_64_27 },
    { 117, 3840, 2160, 100000, VIC::Details::ScanType::NonInterlaced, VIC::Details::AspectRatio::AR_16_9 },
    { 118, 3840, 2160, 119880, VIC::Details::ScanType::NonInterlaced, VIC::Details::AspectRatio::AR_16_9 },
    { 119, 3840, 2160, 100000, VIC::Details::ScanType::NonInterlaced, VIC::Details::AspectRatio::AR_64_27 },
    { 120, 3840, 2160, 119880, VIC::Details::ScanType::NonInterlaced, VIC::Details::AspectRatio::AR_64_27 },
    { 121, 5120, 2160, 23980, VIC::Details::ScanType::NonInterlaced, VIC::Details::AspectRatio::AR_64_27 },
    { 122, 5120, 2160, 25000, VIC::Details::ScanType::NonInterlaced, VIC::Details::AspectRatio::AR_64_27 },
    { 123, 5120, 2160, 29970, VIC::Details::ScanType::NonInterlaced, VIC::Details::AspectRatio::AR_64_27 },
    { 124, 5120, 2160, 47950, VIC::Details::ScanType::NonInterlaced, VIC::Details::AspectRatio::AR_64_27 },
    { 125, 5120, 2160, 50000, VIC::Details::ScanType::NonInterlaced, VIC::Details::AspectRatio::AR_64_27 },
    { 126, 5120, 2160, 59940, VIC::Details::ScanType::NonInterlaced, VIC::Details::AspectRatio::AR_64_27 },
    { 127, 5120, 2160, 100000, VIC::Details::ScanType::NonInterlaced, VIC::Details::AspectRatio::AR_64_27 },
    // 128...192 forbidden range
    { 193, 5120, 2160, 119880, VIC::Details::ScanType::NonInterlaced, VIC::Details::AspectRatio::AR_64_27 },
    { 194, 7680, 4320, 23980, VIC::Details::ScanType::NonInterlaced, VIC::Details::AspectRatio::AR_16_9 },
    { 195, 7680, 4320, 25000, VIC::Details::ScanType::NonInterlaced, VIC::Details::AspectRatio::AR_16_9 },
    { 196, 7680, 4320, 29970, VIC::Details::ScanType::NonInterlaced, VIC::Details::AspectRatio::AR_16_9 },
    { 197, 7680, 4320, 47950, VIC::Details::ScanType::NonInterlaced, VIC::Details::AspectRatio::AR_16_9 },
    { 198, 7680, 4320, 50000, VIC::Details::ScanType::NonInterlaced, VIC::Details::AspectRatio::AR_16_9 },
    { 199, 7680, 4320, 59940, VIC::Details::ScanType::NonInterlaced, VIC::Details::AspectRatio::AR_16_9 },
    { 200, 7680, 4320, 100000, VIC::Details::ScanType::NonInterlaced, VIC::Details::AspectRatio::AR_16_9 },
    { 201, 7680, 4320, 119880, VIC::Details::ScanType::NonInterlaced, VIC::Details::AspectRatio::AR_16_9 },
    { 202, 7680, 4320, 23980, VIC::Details::ScanType::NonInterlaced, VIC::Details::AspectRatio::AR_64_27 },
    { 203, 7680, 4320, 25000, VIC::Details::ScanType::NonInterlaced, VIC::Details::AspectRatio::AR_64_27 },
    { 204, 7680, 4320, 29970, VIC::Details::ScanType::NonInterlaced, VIC::Details::AspectRatio::AR_64_27 },
    { 205, 7680, 4320, 47950, VIC::Details::ScanType::NonInterlaced, VIC::Details::AspectRatio::AR_64_27 },
    { 206, 7680, 4320, 50000, VIC::Details::ScanType::NonInterlaced, VIC::Details::AspectRatio::AR_64_27 },
    { 207, 7680, 4320, 59940, VIC::Details::ScanType::NonInterlaced, VIC::Details::AspectRatio::AR_64_27 },
    { 208, 7680, 4320, 100000, VIC::Details::ScanType::NonInterlaced, VIC::Details::AspectRatio::AR_64_27 },
    { 209, 7680, 4320, 119880, VIC::Details::ScanType::NonInterlaced, VIC::Details::AspectRatio::AR_64_27 },
    { 210, 10240, 4320, 23980, VIC::Details::ScanType::NonInterlaced, VIC::Details::AspectRatio::AR_64_27 },
    { 211, 10240, 4320, 25000, VIC::Details::ScanType::NonInterlaced, VIC::Details::AspectRatio::AR_64_27 },
    { 212, 10240, 4320, 29970, VIC::Details::ScanType::NonInterlaced, VIC::Details::AspectRatio::AR_64_27 },
    { 213, 10240, 4320, 47950, VIC::Details::ScanType::NonInterlaced, VIC::Details::AspectRatio::AR_64_27 },
    { 214, 10240, 4320, 50000, VIC::Details::ScanType::NonInterlaced, VIC::Details::AspectRatio::AR_64_27 },
    { 215, 10240, 4320, 59940, VIC::Details::ScanType::NonInterlaced, VIC::Details::AspectRatio::AR_64_27 },
    { 216, 10240, 4320, 100000, VIC::Details::ScanType::NonInterlaced, VIC::Details::AspectRatio::AR_64_27 },
    { 217, 10240, 4320, 119880, VIC::Details::ScanType::NonInterlaced, VIC::Details::AspectRatio::AR_64_27 },
    { 218, 4096, 2160, 100000, VIC::Details::ScanType::NonInterlaced, VIC::Details::AspectRatio::AR_256_135 },
    { 219, 4096, 2160, 119880, VIC::Details::ScanType::NonInterlaced, VIC::Details::AspectRatio::AR_256_135 },
    // 220...255 reserved
};

static constexpr size_t s_vic_details_count = sizeof(s_vic_details) / sizeof(s_vic_details[0]);
static constexpr u8 s_reserved_vic_id_start = 220;
static_assert(s_vic_details[s_vic_details_count - 1].vic_id == s_reserved_vic_id_start - 1);

FixedPoint<16, u32> VIC::Details::refresh_rate_hz() const
{
    return FixedPoint<16, u32>(refresh_rate_millihz) / 1000;
}

auto VIC::find_details_by_vic_id(u8 vic_id) -> Details const*
{
    if (vic_id == 0 || (vic_id >= 128 && vic_id <= 192) || vic_id >= s_reserved_vic_id_start)
        return nullptr;

    u8 table_index = vic_id - 1;
    if (table_index < 128) {
        // Before the forbidden block (128...192)
        VERIFY(s_vic_details[table_index].vic_id == vic_id);
        return &s_vic_details[table_index];
    }

    // After the forbidden block range (128...192)
    table_index = table_index - 192 + 128 - 1;
    VERIFY(s_vic_details[table_index].vic_id == vic_id);
    return &s_vic_details[table_index];
}

}
