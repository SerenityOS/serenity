/*
 * Copyright (c) 2025, Lucas Chollet <lucas.chollet@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Array.h>
#include <AK/FixedArray.h>
#include <AK/Types.h>
#include <LibGfx/ImageFormats/JPEGXL/Channel.h>

namespace Gfx::JPEGXL {
/// H.5 - Self-correcting predictor

// H.5.1 - General
struct WPHeader {
    u8 wp_p1 { 16 };
    u8 wp_p2 { 10 };
    u8 wp_p3a { 7 };
    u8 wp_p3b { 7 };
    u8 wp_p3c { 7 };
    u8 wp_p3d { 0 };
    u8 wp_p3e { 0 };
    Array<u8, 4> wp_w { 13, 12, 12, 12 };
};

ErrorOr<WPHeader> read_self_correcting_predictor(LittleEndianInputBitStream& stream);

struct Neighborhood {
    i32 N {};
    i32 NW {};
    i32 NE {};
    i32 W {};
    i32 NN {};
    i32 WW {};
    i32 NEE {};
};

class SelfCorrectingData {
public:
    struct Predictions {
        i32 prediction {};
        Array<i32, 4> subpred {};

        i32 max_error {};
        i32 true_err {};
        Array<i32, 4> err {};
    };

    static ErrorOr<SelfCorrectingData> create(WPHeader const& wp_params, u32 width)
    {
        SelfCorrectingData self_correcting_data { wp_params };
        self_correcting_data.m_width = width;

        self_correcting_data.m_previous = TRY(FixedArray<Predictions>::create(width));
        self_correcting_data.m_current_row = TRY(FixedArray<Predictions>::create(width));
        self_correcting_data.m_next_row = TRY(FixedArray<Predictions>::create(width));

        return self_correcting_data;
    }

    void register_next_row()
    {
        auto tmp = move(m_previous);
        m_previous = move(m_current_row);
        m_current_row = move(m_next_row);
        // We reuse m_previous to avoid an allocation, no values are kept
        // everything will be overridden.
        m_next_row = move(tmp);
        m_current_row_index++;
    }

    // H.5.2 - Prediction
    Predictions compute_predictions(Neighborhood const& neighborhood, u32 x);

    // H.5.1 - General
    void compute_errors(u32 x, i32 true_value);

private:
    SelfCorrectingData(WPHeader const& wp)
        : wp_params(wp)
    {
    }

    enum class Direction {
        North,
        NorthWest,
        NorthEast,
        West,
        NorthNorth,
        WestWest
    };

    Predictions predictions_for(u32 x, Direction direction) const;

    WPHeader const& wp_params {};

    u32 m_width {};
    u32 m_current_row_index {};

    FixedArray<Predictions> m_previous {};
    FixedArray<Predictions> m_current_row {};

    FixedArray<Predictions> m_next_row {};
};

Neighborhood retrieve_neighborhood(Channel const& channel, u32 x, u32 y);

// Table H.3 — Modular predictors
i32 prediction(Neighborhood const& neighborhood, i32 self_correcting, u32 predictor);

}
