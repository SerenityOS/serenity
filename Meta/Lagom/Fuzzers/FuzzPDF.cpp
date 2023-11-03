/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibPDF/Document.h>
#include <stdint.h>

extern "C" int LLVMFuzzerTestOneInput(uint8_t const* data, size_t size)
{
    AK::set_debug_enabled(false);

    ReadonlyBytes bytes { data, size };

    if (auto maybe_document = PDF::Document::create(bytes); !maybe_document.is_error()) {
        auto document = maybe_document.release_value();
        (void)document->initialize();
        auto pages = document->get_page_count();
        for (size_t i = 0; i < pages; ++i) {
            (void)document->get_page(i);
        }
    }

    return 0;
}
