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

class VIC final {
public:
    struct Details {
        enum class ScanType : u8 {
            NonInterlaced,
            Interlaced
        };
        enum class AspectRatio : u8 {
            AR_4_3,
            AR_16_9,
            AR_64_27,
            AR_256_135,
        };

        u8 vic_id;
        u16 horizontal_pixels;
        u16 vertical_lines;
        u32 refresh_rate_millihz;
        ScanType scan_type;
        AspectRatio aspect_ratio;

        FixedPoint<16, u32> refresh_rate_hz() const;
    };

    static Details const* find_details_by_vic_id(u8);
};

}
