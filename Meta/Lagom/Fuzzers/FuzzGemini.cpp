/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <YAK/StringView.h>
#include <YAK/URL.h>
#include <LibGemini/Document.h>
#include <stddef.h>
#include <stdint.h>

extern "C" int LLVMFuzzerTestOneInput(const uint8_t* data, size_t size)
{
    auto gemini = StringView(static_cast<const unsigned char*>(data), size);
    Gemini::Document::parse(gemini, {});
    return 0;
}
