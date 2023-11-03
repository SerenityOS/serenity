/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibArchive/Zip.h>
#include <stddef.h>
#include <stdint.h>

extern "C" int LLVMFuzzerTestOneInput(uint8_t const* data, size_t size)
{
    AK::set_debug_enabled(false);
    auto zip_file = Archive::Zip::try_create({ data, size });
    if (!zip_file.has_value())
        return 0;

    (void)zip_file->for_each_member([](auto&) {
        return IterationDecision::Continue;
    });

    return 0;
}
