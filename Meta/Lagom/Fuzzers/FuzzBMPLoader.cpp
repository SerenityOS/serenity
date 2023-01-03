/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGfx/BMPLoader.h>
#include <stdio.h>

extern "C" int LLVMFuzzerTestOneInput(uint8_t const* data, size_t size)
{
    Gfx::BMPImageDecoderPlugin decoder(data, size);
    (void)decoder.frame(0);
    return 0;
}
