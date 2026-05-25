/*
 * Copyright (c) 2026, Lee Hanken
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/FixedArray.h>
#include <LibAudio/Sample.h>

class SampleBuffer {
public:
    static constexpr size_t BUFF_SIZE = 64 * 1024;

    static FixedArray<Audio::Sample> null_samples()
    {
        auto maybe_empty_samples = FixedArray<Audio::Sample>::create(0);
        return maybe_empty_samples.release_value();
    }
};
