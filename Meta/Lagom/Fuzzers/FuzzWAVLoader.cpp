/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibAudio/WavLoader.h>
#include <stddef.h>
#include <stdint.h>

extern "C" int LLVMFuzzerTestOneInput(const uint8_t* data, size_t size)
{
    auto wav_data = ByteBuffer::copy(data, size).release_value();
    auto wav = make<Audio::WavLoaderPlugin>(wav_data);

    for (;;) {
        auto samples = wav->get_more_samples();
        if (samples.is_error())
            return 2;
        if (samples.value()->sample_count() > 0)
            break;
    }

    return 0;
}
