/*
 * Copyright (c) 2022, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/MemoryStream.h>
#include <LibArchive/TarStream.h>
#include <stdio.h>

extern "C" int LLVMFuzzerTestOneInput(uint8_t const* data, size_t size)
{
    InputMemoryStream input_stream(ReadonlyBytes { data, size });
    Archive::TarInputStream tar_stream(input_stream);

    if (!tar_stream.valid())
        return 0;

    for (; !tar_stream.finished(); tar_stream.advance()) {
        auto const& header = tar_stream.header();

        if (!header.content_is_like_extended_header())
            continue;

        switch (header.type_flag()) {
        case Archive::TarFileType::GlobalExtendedHeader:
        case Archive::TarFileType::ExtendedHeader: {
            auto result = tar_stream.for_each_extended_header([&](StringView, StringView) {});
            if (result.is_error())
                return 0;
            break;
        }
        default:
            return 0;
        }
    }

    return 0;
}
