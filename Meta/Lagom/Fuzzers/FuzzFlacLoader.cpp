/*
 * Copyright (c) 2021, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibAudio/FlacLoader.h>
#include <stddef.h>
#include <stdint.h>

extern "C" int LLVMFuzzerTestOneInput(uint8_t const* data, size_t size)
{
    auto flac_data = ByteBuffer::copy(data, size).release_value();
    auto flac_or_error = Audio::FlacLoaderPlugin::try_create(flac_data.bytes());

    if (flac_or_error.is_error())
        return 0;

    auto flac = flac_or_error.release_value();

    for (;;) {
        auto samples = flac->get_more_samples();
        if (samples.is_error())
            return 2;
        if (samples.value().size() > 0)
            break;
    }

    return 0;
}
