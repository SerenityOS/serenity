/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/OwnPtr.h>
#include <AK/StringView.h>
#include <LibMarkdown/Document.h>
#include <stddef.h>
#include <stdint.h>

extern "C" int LLVMFuzzerTestOneInput(uint8_t const* data, size_t size)
{
    auto markdown = StringView(static_cast<const unsigned char*>(data), size);
    Markdown::Document::parse(markdown);
    return 0;
}
