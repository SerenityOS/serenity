/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StringView.h>
#include <Shell/Shell.h>
#include <stddef.h>
#include <stdint.h>

extern "C" int LLVMFuzzerTestOneInput(uint8_t const* data, size_t size)
{
    auto source = StringView(static_cast<const unsigned char*>(data), size);
    Shell::Parser parser(source);
    parser.parse();
    return 0;
}
