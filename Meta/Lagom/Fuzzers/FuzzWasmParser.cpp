/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/MemoryStream.h>
#include <LibWasm/Types.h>
#include <stddef.h>
#include <stdint.h>

extern "C" int LLVMFuzzerTestOneInput(uint8_t const* data, size_t size)
{
    ReadonlyBytes bytes { data, size };
    auto stream_or_error = FixedMemoryStream::construct(bytes);
    if (stream_or_error.is_error())
        return 0;
    auto stream = stream_or_error.release_value();
    [[maybe_unused]] auto result = Wasm::Module::parse(*stream);
    return 0;
}
