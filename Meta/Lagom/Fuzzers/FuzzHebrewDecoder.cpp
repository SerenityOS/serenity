/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibTextCodec/Decoder.h>
#include <stddef.h>
#include <stdint.h>

extern "C" int LLVMFuzzerTestOneInput(uint8_t const* data, size_t size)
{
    auto decoder = TextCodec::decoder_for("windows-1255"sv);
    VERIFY(decoder.has_value());
    (void)decoder->to_utf8({ data, size });
    return 0;
}
