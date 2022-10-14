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
    auto flac_data = ByteBuffer::copy(data, size).release_value();
    auto mp3_or_error = Audio::MP3LoaderPlugin::try_create(flac_data.bytes());

    if (mp3_or_error.is_error())
        return 0;

    auto mp3 = mp3_or_error.release_value();

    for (;;) {
        auto samples = mp3->get_more_samples();
        if (samples.is_error())
            return 0;
        if (samples.value().size() > 0)
            break;
    }

    return 0;
}
