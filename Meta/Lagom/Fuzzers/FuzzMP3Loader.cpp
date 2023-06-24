/*
 * Copyright (c) 2022, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibAudio/MP3Loader.h>
#include <stddef.h>
#include <stdint.h>

extern "C" int LLVMFuzzerTestOneInput(uint8_t const* data, size_t size)
{
    auto const mp3_bytes = ByteBuffer::copy(data, size).release_value();
    auto mp3_data = try_make<FixedMemoryStream>(mp3_bytes).release_value();
    auto mp3_or_error = Audio::MP3LoaderPlugin::create(move(mp3_data));

    if (mp3_or_error.is_error())
        return 0;

    auto mp3 = mp3_or_error.release_value();

    for (;;) {
        auto samples = mp3->load_chunks(1 * KiB);
        if (samples.is_error())
            return 0;
        if (samples.value().size() == 0)
            break;
    }

    return 0;
}
