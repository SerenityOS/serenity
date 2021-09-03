/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <YAK/StringView.h>
#include <LibRegex/Regex.h>
#include <stddef.h>
#include <stdint.h>

extern "C" int LLVMFuzzerTestOneInput(const uint8_t* data, size_t size)
{
    auto pattern = StringView(static_cast<const unsigned char*>(data), size);
    [[maybe_unused]] auto re = Regex<ECMA262>(pattern);
    return 0;
}
