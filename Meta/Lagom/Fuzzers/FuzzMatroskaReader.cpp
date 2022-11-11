/*
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibVideo/Containers/Matroska/Reader.h>
#include <stddef.h>

extern "C" int LLVMFuzzerTestOneInput(u8 const* data, size_t size)
{
    auto matroska_reader_result = Video::Matroska::Reader::from_data({ data, size });
    if (matroska_reader_result.is_error())
        return -1;
    if (auto result = matroska_reader_result.value().segment_information(); result.is_error())
        return -1;
    if (auto result = matroska_reader_result.value().track_count(); result.is_error())
        return -1;
    return 0;
}
