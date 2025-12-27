/*
 * Copyright (c) 2025, Lee Hanken
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/String.h>
#include <AK/Types.h>

typedef struct SampleFormat {
    ByteString format_name = ByteString { "RIFF WAVE (.wav)" };
    int sample_rate = 44100.0;
    u16 num_channels = 1;
    u16 bits_per_sample = 16;

    bool operator==(SampleFormat const& other) const
    {
        return sample_rate == other.sample_rate && num_channels == other.num_channels && bits_per_sample == other.bits_per_sample;
    }

    bool operator!=(SampleFormat const& other) const
    {
        return !(*this == other);
    }

} SampleFormat;
