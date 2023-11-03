/*
 * Copyright (c) 2022, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/MemoryStream.h>
#include <AK/NonnullOwnPtr.h>
#include <LibArchive/TarStream.h>
#include <stdio.h>

extern "C" int LLVMFuzzerTestOneInput(uint8_t const* data, size_t size)
{
    AK::set_debug_enabled(false);

    auto input_stream_or_error = try_make<FixedMemoryStream>(ReadonlyBytes { data, size });

    if (input_stream_or_error.is_error())
        return 0;

    auto tar_stream_or_error = Archive::TarInputStream::construct(input_stream_or_error.release_value());

    if (tar_stream_or_error.is_error())
        return 0;

    auto tar_stream = tar_stream_or_error.release_value();

    while (!tar_stream->finished()) {
        auto const& header = tar_stream->header();

        if (!header.content_is_like_extended_header()) {
            if (tar_stream->advance().is_error())
                return 0;
            else
                continue;
        }

        switch (header.type_flag()) {
        case Archive::TarFileType::GlobalExtendedHeader:
        case Archive::TarFileType::ExtendedHeader: {
            auto result = tar_stream->for_each_extended_header([&](StringView, StringView) {});
            if (result.is_error())
                return 0;
            break;
        }
        default:
            return 0;
        }

        if (tar_stream->advance().is_error())
            return 0;
    }

    return 0;
}
