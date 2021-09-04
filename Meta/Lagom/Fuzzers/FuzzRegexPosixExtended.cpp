/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StringView.h>
#include <LibRegex/Regex.h>
#include <stddef.h>
#include <stdint.h>

extern "C" int LLVMFuzzerTestOneInput(uint8_t const* data, size_t size)
{
    auto pattern = StringView(static_cast<const unsigned char*>(data), size);
    [[maybe_unused]] auto re = Regex<PosixExtended>(pattern);
    return 0;
}
