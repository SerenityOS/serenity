/*
 * Copyright (c) 2023, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "AudioFuzzerCommon.h"
#include <LibAudio/FlacLoader.h>

extern "C" int LLVMFuzzerTestOneInput(uint8_t const* data, size_t size)
{
    AK::set_debug_enabled(false);
    return fuzz_audio_loader<Audio::FlacLoaderPlugin>(data, size);
}
