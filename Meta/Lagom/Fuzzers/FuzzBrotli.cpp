/*
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/MemoryStream.h>
#include <LibCompress/Brotli.h>
#include <stdio.h>

extern "C" int LLVMFuzzerTestOneInput(uint8_t const* data, size_t size)
{
    AK::set_debug_enabled(false);
    FixedMemoryStream bufstream { { data, size } };

    auto brotli_stream = Compress::BrotliDecompressionStream { MaybeOwned<Stream> { bufstream } };

    (void)brotli_stream.read_until_eof();
    return 0;
}
