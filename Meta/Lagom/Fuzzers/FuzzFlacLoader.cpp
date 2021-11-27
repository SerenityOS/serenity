/*
 * Copyright (c) 2021, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibAudio/FlacLoader.h>
#include <stddef.h>
#include <stdint.h>

extern "C" int LLVMFuzzerTestOneInput(const uint8_t* data, size_t size)
{
    auto flac_data = ByteBuffer::copy(data, size).release_value();
    auto flac = make<Audio::FlacLoaderPlugin>(flac_data);

    if (flac->initialize().is_error())
        return 1;

    for (;;) {
        auto samples = flac->get_more_samples();
        if (samples.is_error())
            return 2;
        if (samples.value()->sample_count() > 0)
            break;
    }

    return 0;
}
