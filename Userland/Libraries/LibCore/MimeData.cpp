/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/LexicalPath.h>
#include <AK/StringBuilder.h>
#include <LibCore/File.h>
#include <LibCore/MimeData.h>

namespace Core {

Vector<DeprecatedString> MimeData::formats() const
{
    Vector<DeprecatedString> mime_types;
    mime_types.ensure_capacity(m_data.size());
    for (auto& it : m_data)
        mime_types.unchecked_append(it.key);
    return mime_types;
}

Vector<URL> MimeData::urls() const
{
    auto it = m_data.find("text/uri-list");
    if (it == m_data.end())
        return {};
    Vector<URL> urls;
    for (auto& line : StringView(it->value).split_view('\n')) {
        urls.append(URL(line));
    }
    return urls;
}

ErrorOr<void> MimeData::set_urls(Vector<URL> const& urls)
{
    StringBuilder builder;
    for (auto& url : urls) {
        TRY(builder.try_append(url.to_deprecated_string()));
        TRY(builder.try_append('\n'));
    }
    set_data("text/uri-list", TRY(builder.to_byte_buffer()));

    return {};
}

DeprecatedString MimeData::text() const
{
    return DeprecatedString::copy(m_data.get("text/plain").value_or({}));
}

void MimeData::set_text(DeprecatedString const& text)
{
    set_data("text/plain", text.to_byte_buffer());
}

static AK::Array<StringView, 8> s_plaintext_suffixes = {
    ".c"sv,
    ".cpp"sv,
    ".gml"sv,
    ".h"sv,
    ".hpp"sv,
    ".ini"sv,
    ".ipc"sv,
    ".txt"sv
};

static AK::Array<StringView, 3> s_plaintext_basenames = {
    ".history"sv,
    ".shellrc"sv
    "CMakeLists.txt"sv,
};

// FIXME: Share this, TextEditor and HackStudio language detection somehow.
static bool should_contain_plain_text_based_on_filename(StringView path)
{
    for (auto suffix : s_plaintext_suffixes) {
        if (path.ends_with(suffix))
            return true;
    }
    return s_plaintext_basenames.contains_slow(LexicalPath::basename(path));
}

StringView guess_mime_type_based_on_filename(StringView path)
{
    if (path.ends_with(".js"sv, CaseSensitivity::CaseInsensitive))
        return "application/javascript"sv;
    if (path.ends_with(".json"sv, CaseSensitivity::CaseInsensitive))
        return "application/json"sv;
    if (path.ends_with(".icc"sv, CaseSensitivity::CaseInsensitive) || path.ends_with(".icm"sv, CaseSensitivity::CaseInsensitive))
        return "application/vnd.iccprofile"sv;
    if (path.ends_with(".sheets"sv, CaseSensitivity::CaseInsensitive))
        return "application/x-sheets+json"sv;
    if (path.ends_with(".zip"sv, CaseSensitivity::CaseInsensitive))
        return "application/zip"sv;
    if (path.ends_with(".flac"sv, CaseSensitivity::CaseInsensitive))
        return "audio/flac"sv;
    if (path.ends_with(".mid"sv, CaseSensitivity::CaseInsensitive) || path.ends_with(".midi"sv, CaseSensitivity::CaseInsensitive))
        return "audio/midi"sv;
    if (path.ends_with(".mp3"sv, CaseSensitivity::CaseInsensitive))
        return "audio/mpeg"sv;
    if (path.ends_with(".qoa"sv, CaseSensitivity::CaseInsensitive))
        return "audio/qoa"sv;
    if (path.ends_with(".wav"sv, CaseSensitivity::CaseInsensitive))
        return "audio/wav"sv;
    if (path.ends_with(".bmp"sv, CaseSensitivity::CaseInsensitive))
        return "image/bmp"sv;
    if (path.ends_with(".gif"sv, CaseSensitivity::CaseInsensitive))
        return "image/gif"sv;
    if (path.ends_with(".jpg"sv, CaseSensitivity::CaseInsensitive) || path.ends_with(".jpeg"sv, CaseSensitivity::CaseInsensitive))
        return "image/jpeg"sv;
    if (path.ends_with(".png"sv, CaseSensitivity::CaseInsensitive))
        return "image/png"sv;
    if (path.ends_with(".svg"sv, CaseSensitivity::CaseInsensitive))
        return "image/svg+xml"sv;
    if (path.ends_with(".tvg"sv, CaseSensitivity::CaseInsensitive))
        return "image/tinyvg"sv;
    if (path.ends_with(".webp"sv, CaseSensitivity::CaseInsensitive))
        return "image/webp"sv;
    if (path.ends_with(".pbm"sv, CaseSensitivity::CaseInsensitive))
        return "image/x‑portable‑bitmap"sv;
    if (path.ends_with(".pgm"sv, CaseSensitivity::CaseInsensitive))
        return "image/x‑portable‑graymap"sv;
    if (path.ends_with(".ppm"sv, CaseSensitivity::CaseInsensitive))
        return "image/x‑portable‑pixmap"sv;
    if (path.ends_with(".qoi"sv, CaseSensitivity::CaseInsensitive))
        return "image/x-qoi"sv;
    if (path.ends_with(".tga"sv, CaseSensitivity::CaseInsensitive))
        return "image/x-targa"sv;
    if (path.ends_with(".css"sv, CaseSensitivity::CaseInsensitive))
        return "text/css"sv;
    if (path.ends_with(".csv"sv, CaseSensitivity::CaseInsensitive))
        return "text/csv"sv;
    if (path.ends_with(".html"sv, CaseSensitivity::CaseInsensitive) || path.ends_with(".htm"sv, CaseSensitivity::CaseInsensitive))
        return "text/html"sv;
    if (path.ends_with("/"sv, CaseSensitivity::CaseInsensitive)) // FIXME: This seems dubious
        return "text/html"sv;
    if (path.ends_with(".md"sv, CaseSensitivity::CaseInsensitive))
        return "text/markdown"sv;
    if (should_contain_plain_text_based_on_filename(path))
        return "text/plain"sv;
    if (path.ends_with(".webm"sv, CaseSensitivity::CaseInsensitive))
        return "video/webm"sv;

    return "application/octet-stream"sv;
}

#define ENUMERATE_HEADER_CONTENTS                                                                                                                  \
    __ENUMERATE_MIME_TYPE_HEADER("application/gzip"sv, gzip, 0, 2, 0x1F, 0x8B)                                                                     \
    __ENUMERATE_MIME_TYPE_HEADER("application/pdf"sv, pdf, 0, 5, 0x25, 'P', 'D', 'F', 0x2D)                                                        \
    __ENUMERATE_MIME_TYPE_HEADER("application/rtf"sv, rtf, 0, 6, 0x7B, 0x5C, 0x72, 0x74, 0x66, 0x31)                                               \
    __ENUMERATE_MIME_TYPE_HEADER("application/tar"sv, tar, 0x101, 5, 0x75, 0x73, 0x74, 0x61, 0x72)                                                 \
    __ENUMERATE_MIME_TYPE_HEADER("application/vnd.iccprofile"sv, icc, 36, 4, 'a', 'c', 's', 'p')                                                   \
    __ENUMERATE_MIME_TYPE_HEADER("application/wasm"sv, wasm, 0, 4, 0x00, 'a', 's', 'm')                                                            \
    __ENUMERATE_MIME_TYPE_HEADER("application/x-7z-compressed"sv, sevenzip, 0, 6, 0x37, 0x7A, 0xBC, 0xAF, 0x27, 0x1C)                              \
    __ENUMERATE_MIME_TYPE_HEADER("application/x-bzip2"sv, bzip2, 0, 3, 'B', 'Z', 'h')                                                              \
    __ENUMERATE_MIME_TYPE_HEADER("application/zip"sv, zip, 0, 2, 0x50, 0x4B)                                                                       \
    __ENUMERATE_MIME_TYPE_HEADER("audio/flac"sv, flac, 0, 4, 'f', 'L', 'a', 'C')                                                                   \
    __ENUMERATE_MIME_TYPE_HEADER("audio/midi"sv, midi, 0, 4, 0x4D, 0x54, 0x68, 0x64)                                                               \
    __ENUMERATE_MIME_TYPE_HEADER("audio/mpeg"sv, mp3, 0, 2, 0xFF, 0xFB)                                                                            \
    __ENUMERATE_MIME_TYPE_HEADER("audio/qoa"sv, qoa, 0, 4, 'q', 'o', 'a', 'f')                                                                     \
    __ENUMERATE_MIME_TYPE_HEADER("audio/wav"sv, wav, 8, 4, 'W', 'A', 'V', 'E')                                                                     \
    __ENUMERATE_MIME_TYPE_HEADER("extra/blender"sv, blend, 0, 7, 'B', 'L', 'E', 'N', 'D', 'E', 'R')                                                \
    __ENUMERATE_MIME_TYPE_HEADER("extra/elf"sv, elf, 0, 4, 0x7F, 'E', 'L', 'F')                                                                    \
    __ENUMERATE_MIME_TYPE_HEADER("extra/ext"sv, ext, 0x438, 2, 0x53, 0xEF)                                                                         \
    __ENUMERATE_MIME_TYPE_HEADER("extra/iso-9660"sv, iso9660_0, 0x8001, 5, 0x43, 0x44, 0x30, 0x30, 0x31)                                           \
    __ENUMERATE_MIME_TYPE_HEADER("extra/iso-9660"sv, iso9660_1, 0x8801, 5, 0x43, 0x44, 0x30, 0x30, 0x31)                                           \
    __ENUMERATE_MIME_TYPE_HEADER("extra/iso-9660"sv, iso9660_2, 0x9001, 5, 0x43, 0x44, 0x30, 0x30, 0x31)                                           \
    __ENUMERATE_MIME_TYPE_HEADER("extra/isz"sv, compressed_iso, 0, 4, 'I', 's', 'Z', '!')                                                          \
    __ENUMERATE_MIME_TYPE_HEADER("extra/lua-bytecode"sv, lua_bytecode, 0, 4, 0x1B, 'L', 'u', 'a')                                                  \
    __ENUMERATE_MIME_TYPE_HEADER("extra/matroska"sv, mkv, 0, 4, 0x1A, 0x45, 0xDF, 0xA3)                                                            \
    __ENUMERATE_MIME_TYPE_HEADER("extra/nes-rom"sv, nesrom, 0, 4, 'N', 'E', 'S', 0x1A)                                                             \
    __ENUMERATE_MIME_TYPE_HEADER("extra/qcow"sv, qcow, 0, 3, 'Q', 'F', 'I')                                                                        \
    __ENUMERATE_MIME_TYPE_HEADER("extra/raw-zlib"sv, zlib_0, 0, 2, 0x78, 0x01)                                                                     \
    __ENUMERATE_MIME_TYPE_HEADER("extra/raw-zlib"sv, zlib_1, 0, 2, 0x78, 0x5E)                                                                     \
    __ENUMERATE_MIME_TYPE_HEADER("extra/raw-zlib"sv, zlib_2, 0, 2, 0x78, 0x9C)                                                                     \
    __ENUMERATE_MIME_TYPE_HEADER("extra/raw-zlib"sv, zlib_3, 0, 2, 0x78, 0xDA)                                                                     \
    __ENUMERATE_MIME_TYPE_HEADER("extra/raw-zlib"sv, zlib_4, 0, 2, 0x78, 0x20)                                                                     \
    __ENUMERATE_MIME_TYPE_HEADER("extra/raw-zlib"sv, zlib_5, 0, 2, 0x78, 0x7D)                                                                     \
    __ENUMERATE_MIME_TYPE_HEADER("extra/raw-zlib"sv, zlib_6, 0, 2, 0x78, 0xBB)                                                                     \
    __ENUMERATE_MIME_TYPE_HEADER("extra/raw-zlib"sv, zlib_7, 0, 2, 0x78, 0xF9)                                                                     \
    __ENUMERATE_MIME_TYPE_HEADER("extra/sqlite"sv, sqlite, 0, 16, 'S', 'Q', 'L', 'i', 't', 'e', ' ', 'f', 'o', 'r', 'm', 'a', 't', ' ', '3', 0x00) \
    __ENUMERATE_MIME_TYPE_HEADER("extra/win-31x-compressed"sv, win_31x_archive, 0, 4, 'K', 'W', 'A', 'J')                                          \
    __ENUMERATE_MIME_TYPE_HEADER("extra/win-95-compressed"sv, win_95_archive, 0, 4, 'S', 'Z', 'D', 'D')                                            \
    __ENUMERATE_MIME_TYPE_HEADER("image/bmp"sv, bmp, 0, 2, 'B', 'M')                                                                               \
    __ENUMERATE_MIME_TYPE_HEADER("image/gif"sv, gif_87, 0, 6, 'G', 'I', 'F', '8', '7', 'a')                                                        \
    __ENUMERATE_MIME_TYPE_HEADER("image/gif"sv, gif_89, 0, 6, 'G', 'I', 'F', '8', '9', 'a')                                                        \
    __ENUMERATE_MIME_TYPE_HEADER("image/jpeg"sv, jpeg, 0, 4, 0xFF, 0xD8, 0xFF, 0xDB)                                                               \
    __ENUMERATE_MIME_TYPE_HEADER("image/jpeg"sv, jpeg_huh, 0, 4, 0xFF, 0xD8, 0xFF, 0xEE)                                                           \
    __ENUMERATE_MIME_TYPE_HEADER("image/jpeg"sv, jpeg_jfif, 0, 12, 0xFF, 0xD8, 0xFF, 0xE0, 0x00, 0x10, 'J', 'F', 'I', 'F', 0x00, 0x01)             \
    __ENUMERATE_MIME_TYPE_HEADER("image/png"sv, png, 0, 8, 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A)                                            \
    __ENUMERATE_MIME_TYPE_HEADER("image/tiff"sv, tiff, 0, 4, 'I', 'I', '*', 0x00)                                                                  \
    __ENUMERATE_MIME_TYPE_HEADER("image/tiff"sv, tiff_bigendian, 0, 4, 'M', 'M', 0x00, '*')                                                        \
    __ENUMERATE_MIME_TYPE_HEADER("image/tinyvg"sv, tinyvg, 0, 2, 0x72, 0x56)                                                                       \
    __ENUMERATE_MIME_TYPE_HEADER("image/webp"sv, webp, 8, 4, 'W', 'E', 'B', 'P')                                                                   \
    __ENUMERATE_MIME_TYPE_HEADER("image/x-portable-bitmap"sv, pbm, 0, 3, 0x50, 0x31, 0x0A)                                                         \
    __ENUMERATE_MIME_TYPE_HEADER("image/x-portable-graymap"sv, pgm, 0, 3, 0x50, 0x32, 0x0A)                                                        \
    __ENUMERATE_MIME_TYPE_HEADER("image/x-portable-pixmap"sv, ppm, 0, 3, 0x50, 0x33, 0x0A)                                                         \
    __ENUMERATE_MIME_TYPE_HEADER("image/x-qoi"sv, qoi, 0, 4, 'q', 'o', 'i', 'f')                                                                   \
    __ENUMERATE_MIME_TYPE_HEADER("text/x-shellscript"sv, shell, 0, 10, '#', '!', '/', 'b', 'i', 'n', '/', 's', 'h', '\n')

#define __ENUMERATE_MIME_TYPE_HEADER(mime_type, var_name, pattern_offset, pattern_size, ...) \
    static const u8 var_name##_arr[pattern_size] = { __VA_ARGS__ };                          \
    static constexpr ReadonlyBytes var_name = ReadonlyBytes { var_name##_arr, pattern_size };
ENUMERATE_HEADER_CONTENTS
#undef __ENUMERATE_MIME_TYPE_HEADER

Optional<StringView> guess_mime_type_based_on_sniffed_bytes(ReadonlyBytes bytes)
{
#define __ENUMERATE_MIME_TYPE_HEADER(mime_type, var_name, pattern_offset, pattern_size, ...)                       \
    if (static_cast<ssize_t>(bytes.size()) >= pattern_offset && bytes.slice(pattern_offset).starts_with(var_name)) \
        return mime_type;
    ENUMERATE_HEADER_CONTENTS;
#undef __ENUMERATE_MIME_TYPE_HEADER
    return {};
}

Optional<StringView> guess_mime_type_based_on_sniffed_bytes(Core::File& file)
{
    // Read accounts for longest possible offset + signature we currently match against (extra/iso-9660)
    auto maybe_buffer = ByteBuffer::create_uninitialized(0x9006);
    if (maybe_buffer.is_error())
        return {};

    auto maybe_bytes = file.read_some(maybe_buffer.value());
    if (maybe_bytes.is_error())
        return {};

    return Core::guess_mime_type_based_on_sniffed_bytes(maybe_bytes.value());
}

}
