/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StringView.h>
#include <LibGemini/Document.h>
#include <stddef.h>
#include <stdint.h>

extern "C" int LLVMFuzzerTestOneInput(uint8_t const* data, size_t size)
{
    AK::set_debug_enabled(false);
    auto gemini = StringView(static_cast<unsigned char const*>(data), size);
    (void)Gemini::Document::parse(gemini, {});
    return 0;
}
