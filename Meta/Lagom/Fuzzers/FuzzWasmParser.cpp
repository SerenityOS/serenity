/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/MemoryStream.h>
#include <AK/Stream.h>
#include <LibWasm/Types.h>
#include <stddef.h>
#include <stdint.h>

extern "C" int LLVMFuzzerTestOneInput(const uint8_t* data, size_t size)
{
    ReadonlyBytes bytes { data, size };
    InputMemoryStream stream { bytes };
    [[maybe_unused]] auto result = Wasm::Module::parse(stream);
    stream.handle_any_error();
    return 0;
}
