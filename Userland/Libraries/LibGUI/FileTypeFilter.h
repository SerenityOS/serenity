/*
 * Copyright (c) 2023, Marcus Nilsson <marcus.nilsson@genarp.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/DeprecatedString.h>
#include <AK/Optional.h>
#include <AK/Vector.h>
#include <LibIPC/Decoder.h>
#include <LibIPC/Encoder.h>

namespace GUI {

struct FileTypeFilter {
    DeprecatedString name;
    Optional<Vector<DeprecatedString>> extensions;

    static FileTypeFilter all_files()
    {
        return FileTypeFilter { "All Files", {} };
    }

    static FileTypeFilter image_files()
    {
        return FileTypeFilter { "Image Files", Vector<DeprecatedString> { "png", "gif", "bmp", "dip", "pbm", "pgm", "ppm", "ico", "jpeg", "jpg", "jxl", "dds", "qoi", "webp", "tvg" } };
    }
};

}

namespace IPC {

template<>
inline ErrorOr<void> encode(Encoder& encoder, GUI::FileTypeFilter const& response)
{
    TRY(encoder.encode(response.name));
    TRY(encoder.encode(response.extensions));
    return {};
}

template<>
inline ErrorOr<GUI::FileTypeFilter> decode(Decoder& decoder)
{
    auto name = TRY(decoder.decode<DeprecatedString>());
    auto extensions = TRY(decoder.decode<Optional<Vector<AK::DeprecatedString>>>());

    return GUI::FileTypeFilter { move(name), move(extensions) };
}

}
