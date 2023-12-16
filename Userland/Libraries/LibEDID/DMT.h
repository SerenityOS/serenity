/*
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/FixedPoint.h>
#include <AK/Optional.h>
#include <AK/Types.h>

namespace EDID {

class DMT final {
public:
    struct CVT {
        u8 bytes[3];
    };
    struct MonitorTiming {
        enum class ScanType : u8 {
            NonInterlaced,
            Interlaced
        };
        enum class CVTCompliance : u8 {
            NotCompliant,
            Compliant,
            CompliantReducedBlanking,
            CompliantReducedBlankingV2,
        };

        u8 dmt_id;
        u8 std_bytes[2];
        u8 char_width_pixels;
        u16 horizontal_pixels;
        u16 vertical_lines;
        u32 horizontal_frequency_hz;
        u32 vertical_frequency_millihz;
        u32 pixel_clock_khz;
        u8 horizontal_front_porch_pixels;
        u8 vertical_front_porch_lines;
        u16 horizontal_blank_pixels;
        u16 vertical_blank_lines;
        u16 horizontal_sync_time_pixels;
        u8 vertical_sync_time_lines;
        CVTCompliance cvt_compliance { CVTCompliance::NotCompliant };
        u8 cvt_bytes[3] {};
        ScanType scan_type { ScanType::NonInterlaced };

        ALWAYS_INLINE bool has_std() const { return std_bytes[0] != 0; }
        ALWAYS_INLINE bool has_cvt() const { return cvt_bytes[0] != 0; }

        FixedPoint<16, u32> horizontal_frequency_khz() const;
        FixedPoint<16, u32> vertical_frequency_hz() const;
        u32 refresh_rate_hz() const;
#ifndef KERNEL
        ByteString name() const;
#endif
    };

    static MonitorTiming const* find_timing_by_dmt_id(u8);
    static MonitorTiming const* find_timing_by_std_id(u8, u8);
    static MonitorTiming const* find_timing_by_cvt(CVT);
};

}
