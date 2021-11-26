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

    if (!wav->sniff())
        return 1;

    while (wav->get_more_samples())
        ;

    return 0;
}
