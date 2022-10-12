/*
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibVideo/VP9/Decoder.h>
#include <stddef.h>

extern "C" int LLVMFuzzerTestOneInput(u8 const* data, size_t size)
{
    Video::VP9::Decoder vp9_decoder;
    if (auto decode_error = vp9_decoder.decode({ data, size }); decode_error.is_error())
        return -1;
    return 0;
}
