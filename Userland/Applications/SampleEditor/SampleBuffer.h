/*
 * Copyright (c) 2025, Lee Hanken
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/FixedArray.h>
#include <LibAudio/Sample.h>

#pragma once

class SampleBuffer {

public:
    static constexpr size_t BUFF_SIZE = 64 * 1024;

    static FixedArray<Audio::Sample> null_samples()
    {
        auto maybe_empty_samples = FixedArray<Audio::Sample>::create(0);
        return maybe_empty_samples.release_value();
    }
};
