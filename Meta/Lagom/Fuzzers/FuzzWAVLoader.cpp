/*
 * Copyright (c) 2023, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "AudioFuzzerCommon.h"
#include <LibAudio/WavLoader.h>

extern "C" int LLVMFuzzerTestOneInput(uint8_t const* data, size_t size)
{
    return fuzz_audio_loader<Audio::WavLoaderPlugin>(data, size);
}
