/*
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibVideo/MatroskaReader.h>
#include <stddef.h>

extern "C" int LLVMFuzzerTestOneInput(u8 const* data, size_t size)
{
    auto matroska_document = Video::MatroskaReader::parse_matroska_from_data(data, size);
    if (!matroska_document)
        return -1;
    return 0;
}
