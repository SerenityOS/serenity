/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <YAK/String.h>
#include <LibTextCodec/Decoder.h>
#include <stddef.h>
#include <stdint.h>

extern "C" int LLVMFuzzerTestOneInput(const uint8_t* data, size_t size)
{
    auto* decoder = TextCodec::decoder_for("utf-16be");
    VERIFY(decoder);
    decoder->to_utf8({ data, size });
    return 0;
}
