/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibPDF/Document.h>
#include <stdint.h>

extern "C" int LLVMFuzzerTestOneInput(const uint8_t* data, size_t size)
{
    ReadonlyBytes bytes { data, size };
    auto doc = PDF::Document::create(bytes);

    if (doc) {
        auto pages = doc->get_page_count();
        for (size_t i = 0; i < pages; ++i) {
            (void)doc->get_page(i);
        }
    }

    return 0;
}
