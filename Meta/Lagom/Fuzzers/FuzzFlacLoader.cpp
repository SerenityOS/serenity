/*
 * Copyright (c) 2021, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/MemoryStream.h>
#include <LibAudio/FlacLoader.h>
#include <stddef.h>
#include <stdint.h>

extern "C" int LLVMFuzzerTestOneInput(uint8_t const* data, size_t size)
{
    auto const flac_bytes = ByteBuffer::copy(data, size).release_value();
    auto flac_data = try_make<FixedMemoryStream>(flac_bytes).release_value();
    auto flac_or_error = Audio::FlacLoaderPlugin::create(move(flac_data));

    if (flac_or_error.is_error())
        return 0;

    auto flac = flac_or_error.release_value();

    for (;;) {
        auto samples = flac->load_chunks(10 * KiB);
        if (samples.is_error())
            return 0;
        if (samples.value().size() == 0)
            break;
    }

    return 0;
}
