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
    ByteBuffer zip_data = ByteBuffer::copy(data, size);
    auto zip_file = Archive::Zip::try_create(zip_data);
    if (!zip_file.has_value())
        return 0;

    zip_file->for_each_member([](auto&) {
        return IterationDecision::Continue;
    });

    return 0;
}
