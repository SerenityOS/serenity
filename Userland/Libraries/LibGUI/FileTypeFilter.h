/*
 * Copyright (c) 2023, Marcus Nilsson <marcus.nilsson@genarp.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/ByteString.h>
#include <AK/Optional.h>
#include <AK/Vector.h>
#include <LibIPC/Decoder.h>
#include <LibIPC/Encoder.h>

namespace GUI {

struct FileTypeFilter {
    ByteString name;
    Optional<Vector<ByteString>> extensions;

    static FileTypeFilter all_files()
    {
        return FileTypeFilter { "All Files", {} };
    }

    static FileTypeFilter audio_files()
    {
        return FileTypeFilter { "Audio Files", Vector<ByteString> { "flac", "m3u", "m3u8", "m4a", "m4b", "m4r", "mid", "midi", "mka", "mp3", "mpga", "oga", "ogg", "opus", "spx", "vlc", "wav", "wax", "wma", "wmx", "wvx" } };
    }

    static FileTypeFilter image_files()
    {
        return FileTypeFilter { "Image Files", Vector<ByteString> { "png", "gif", "bmp", "dip", "pam", "pbm", "pgm", "ppm", "ico", "iff", "jb2", "jbig2", "jp2", "jpeg", "jpf", "jpg", "jpx", "jxl", "dds", "qoi", "tif", "tiff", "webp", "tvg" } };
    }

    static FileTypeFilter video_files()
    {
        return FileTypeFilter { "Video Files", Vector<ByteString> { "avf", "avi", "flv", "m4u", "m4v", "mk3d", "mkv", "mov", "movie", "mp4", "mpeg", "mpg", "ogg", "ogv", "vob", "webm", "wmv" } };
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
    auto name = TRY(decoder.decode<ByteString>());
    auto extensions = TRY(decoder.decode<Optional<Vector<AK::ByteString>>>());

    return GUI::FileTypeFilter { move(name), move(extensions) };
}

}
