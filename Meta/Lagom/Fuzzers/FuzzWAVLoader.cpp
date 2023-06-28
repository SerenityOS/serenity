/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/MemoryStream.h>
#include <LibAudio/WavLoader.h>
#include <stddef.h>
#include <stdint.h>

extern "C" int LLVMFuzzerTestOneInput(uint8_t const* data, size_t size)
{
    auto const wav_bytes = ByteBuffer::copy(data, size).release_value();
    auto wav_data = try_make<FixedMemoryStream>(wav_bytes).release_value();
    auto wav_or_error = Audio::WavLoaderPlugin::create(move(wav_data));

    if (wav_or_error.is_error())
        return 0;

    auto wav = wav_or_error.release_value();

    for (;;) {
        auto samples = wav->load_chunks(4 * KiB);
        if (samples.is_error())
            return 0;
        if (samples.value().size() == 0)
            break;
    }

    return 0;
}
