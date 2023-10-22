/*
 * Copyright (c) 2023, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/JsonParser.h>

extern "C" int LLVMFuzzerTestOneInput(uint8_t const* data, size_t size)
{
    JsonParser parser({ data, size });
    (void)parser.parse();
    return 0;
}
