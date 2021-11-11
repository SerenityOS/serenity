/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StringBuilder.h>
#include <LibCore/MimeData.h>

namespace Core {

Vector<String> MimeData::formats() const
{
    Vector<String> mime_types;
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

void MimeData::set_urls(const Vector<URL>& urls)
{
    StringBuilder builder;
    for (auto& url : urls) {
        builder.append(url.to_string());
        builder.append('\n');
    }
    set_data("text/uri-list", builder.to_byte_buffer());
}

String MimeData::text() const
{
    return String::copy(m_data.get("text/plain").value_or({}));
}

void MimeData::set_text(const String& text)
{
    set_data("text/plain", text.to_byte_buffer());
}

String guess_mime_type_based_on_filename(StringView path)
{
    if (path.ends_with(".pbm", CaseSensitivity::CaseInsensitive))
        return "image/x‑portable‑bitmap";
    if (path.ends_with(".pgm", CaseSensitivity::CaseInsensitive))
        return "image/x‑portable‑graymap";
    if (path.ends_with(".png", CaseSensitivity::CaseInsensitive))
        return "image/png";
    if (path.ends_with(".ppm", CaseSensitivity::CaseInsensitive))
        return "image/x‑portable‑pixmap";
    if (path.ends_with(".gif", CaseSensitivity::CaseInsensitive))
        return "image/gif";
    if (path.ends_with(".bmp", CaseSensitivity::CaseInsensitive))
        return "image/bmp";
    if (path.ends_with(".jpg", CaseSensitivity::CaseInsensitive) || path.ends_with(".jpeg", CaseSensitivity::CaseInsensitive))
        return "image/jpeg";
    if (path.ends_with(".svg", CaseSensitivity::CaseInsensitive))
        return "image/svg+xml";
    if (path.ends_with(".md", CaseSensitivity::CaseInsensitive))
        return "text/markdown";
    if (path.ends_with(".html", CaseSensitivity::CaseInsensitive) || path.ends_with(".htm", CaseSensitivity::CaseInsensitive))
        return "text/html";
    if (path.ends_with(".js", CaseSensitivity::CaseInsensitive))
        return "application/javascript";
    if (path.ends_with(".json", CaseSensitivity::CaseInsensitive))
        return "application/json";
    if (path.ends_with(".md", CaseSensitivity::CaseInsensitive))
        return "text/markdown";
    if (path.ends_with("/", CaseSensitivity::CaseInsensitive))
        return "text/html";
    if (path.ends_with(".csv", CaseSensitivity::CaseInsensitive))
        return "text/csv";
    return "text/plain";
}

#define ENUMERATE_HEADER_CONTENTS                                                                                                                \
    __ENUMERATE_MIME_TYPE_HEADER(blend, "extra/blender", 0, 7, 'B', 'L', 'E', 'N', 'D', 'E', 'R')                                                \
    __ENUMERATE_MIME_TYPE_HEADER(bmp, "image/bmp", 0, 2, 'B', 'M')                                                                               \
    __ENUMERATE_MIME_TYPE_HEADER(bzip2, "application/x-bzip2", 0, 3, 'B', 'Z', 'h')                                                              \
    __ENUMERATE_MIME_TYPE_HEADER(compressed_iso, "extra/isz", 0, 4, 'I', 's', 'Z', '!')                                                          \
    __ENUMERATE_MIME_TYPE_HEADER(elf, "extra/elf", 0, 4, 0x7F, 'E', 'L', 'F')                                                                    \
    __ENUMERATE_MIME_TYPE_HEADER(ext, "extra/ext", 0x438, 2, 0x53, 0xEF)                                                                         \
    __ENUMERATE_MIME_TYPE_HEADER(flac, "extra/flac", 0, 4, 0x66, 0x4C, 0x61, 0x43)                                                               \
    __ENUMERATE_MIME_TYPE_HEADER(gif_87, "image/gif", 0, 6, 'G', 'I', 'F', '8', '7', 'a')                                                        \
    __ENUMERATE_MIME_TYPE_HEADER(gif_89, "image/gif", 0, 6, 'G', 'I', 'F', '8', '9', 'a')                                                        \
    __ENUMERATE_MIME_TYPE_HEADER(gzip, "application/gzip", 0, 2, 0x1F, 0x8B)                                                                     \
    __ENUMERATE_MIME_TYPE_HEADER(iso9660_0, "extra/iso-9660", 0x8001, 5, 0x43, 0x44, 0x30, 0x30, 0x31)                                           \
    __ENUMERATE_MIME_TYPE_HEADER(iso9660_1, "extra/iso-9660", 0x8801, 5, 0x43, 0x44, 0x30, 0x30, 0x31)                                           \
    __ENUMERATE_MIME_TYPE_HEADER(iso9660_2, "extra/iso-9660", 0x9001, 5, 0x43, 0x44, 0x30, 0x30, 0x31)                                           \
    __ENUMERATE_MIME_TYPE_HEADER(jpeg, "image/jpeg", 0, 4, 0xFF, 0xD8, 0xFF, 0xDB)                                                               \
    __ENUMERATE_MIME_TYPE_HEADER(jpeg_huh, "image/jpeg", 0, 4, 0xFF, 0xD8, 0xFF, 0xEE)                                                           \
    __ENUMERATE_MIME_TYPE_HEADER(jpeg_jfif, "image/jpeg", 0, 12, 0xFF, 0xD8, 0xFF, 0xE0, 0x00, 0x10, 'J', 'F', 'I', 'F', 0x00, 0x01)             \
    __ENUMERATE_MIME_TYPE_HEADER(lua_bytecode, "extra/lua-bytecode", 0, 4, 0x1B, 'L', 'u', 'a')                                                  \
    __ENUMERATE_MIME_TYPE_HEADER(midi, "audio/midi", 0, 4, 0x4D, 0x54, 0x68, 0x64)                                                               \
    __ENUMERATE_MIME_TYPE_HEADER(mkv, "extra/matroska", 0, 4, 0x1A, 0x45, 0xDF, 0xA3)                                                            \
    __ENUMERATE_MIME_TYPE_HEADER(nesrom, "extra/nes-rom", 0, 4, 'N', 'E', 'S', 0x1A)                                                             \
    __ENUMERATE_MIME_TYPE_HEADER(pbm, "image/x-portable-bitmap", 0, 3, 0x50, 0x31, 0x0A)                                                         \
    __ENUMERATE_MIME_TYPE_HEADER(pdf, "application/pdf", 0, 5, 0x25, 'P', 'D', 'F', 0x2D)                                                        \
    __ENUMERATE_MIME_TYPE_HEADER(pgm, "image/x-portable-graymap", 0, 3, 0x50, 0x32, 0x0A)                                                        \
    __ENUMERATE_MIME_TYPE_HEADER(png, "image/png", 0, 8, 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A)                                            \
    __ENUMERATE_MIME_TYPE_HEADER(ppm, "image/x-portable-pixmap", 0, 3, 0x50, 0x33, 0x0A)                                                         \
    __ENUMERATE_MIME_TYPE_HEADER(qcow, "extra/qcow", 0, 3, 'Q', 'F', 'I')                                                                        \
    __ENUMERATE_MIME_TYPE_HEADER(rtf, "application/rtf", 0, 6, 0x7B, 0x5C, 0x72, 0x74, 0x66, 0x31)                                               \
    __ENUMERATE_MIME_TYPE_HEADER(sevenzip, "application/x-7z-compressed", 0, 6, 0x37, 0x7A, 0xBC, 0xAF, 0x27, 0x1C)                              \
    __ENUMERATE_MIME_TYPE_HEADER(shell, "text/x-shellscript", 0, 10, '#', '!', '/', 'b', 'i', 'n', '/', 's', 'h', '\n')                          \
    __ENUMERATE_MIME_TYPE_HEADER(sqlite, "extra/sqlite", 0, 16, 'S', 'Q', 'L', 'i', 't', 'e', ' ', 'f', 'o', 'r', 'm', 'a', 't', ' ', '3', 0x00) \
    __ENUMERATE_MIME_TYPE_HEADER(tar, "application/tar", 0x101, 5, 0x75, 0x73, 0x74, 0x61, 0x72)                                                 \
    __ENUMERATE_MIME_TYPE_HEADER(tiff, "image/tiff", 0, 4, 'I', 'I', '*', 0x00)                                                                  \
    __ENUMERATE_MIME_TYPE_HEADER(tiff_bigendian, "image/tiff", 0, 4, 'M', 'M', 0x00, '*')                                                        \
    __ENUMERATE_MIME_TYPE_HEADER(wasm, "application/wasm", 0, 4, 0x00, 'a', 's', 'm')                                                            \
    __ENUMERATE_MIME_TYPE_HEADER(win_31x_archive, "extra/win-31x-compressed", 0, 4, 'K', 'W', 'A', 'J')                                          \
    __ENUMERATE_MIME_TYPE_HEADER(win_95_archive, "extra/win-95-compressed", 0, 4, 'S', 'Z', 'D', 'D')                                            \
    __ENUMERATE_MIME_TYPE_HEADER(zlib_0, "extra/raw-zlib", 0, 2, 0x78, 0x01)                                                                     \
    __ENUMERATE_MIME_TYPE_HEADER(zlib_1, "extra/raw-zlib", 0, 2, 0x78, 0x5E)                                                                     \
    __ENUMERATE_MIME_TYPE_HEADER(zlib_2, "extra/raw-zlib", 0, 2, 0x78, 0x9C)                                                                     \
    __ENUMERATE_MIME_TYPE_HEADER(zlib_3, "extra/raw-zlib", 0, 2, 0x78, 0xDA)                                                                     \
    __ENUMERATE_MIME_TYPE_HEADER(zlib_4, "extra/raw-zlib", 0, 2, 0x78, 0x20)                                                                     \
    __ENUMERATE_MIME_TYPE_HEADER(zlib_5, "extra/raw-zlib", 0, 2, 0x78, 0x7D)                                                                     \
    __ENUMERATE_MIME_TYPE_HEADER(zlib_6, "extra/raw-zlib", 0, 2, 0x78, 0xBB)                                                                     \
    __ENUMERATE_MIME_TYPE_HEADER(zlib_7, "extra/raw-zlib", 0, 2, 0x78, 0xF9)

#define __ENUMERATE_MIME_TYPE_HEADER(var_name, mime_type, pattern_offset, pattern_size, ...) \
    static const u8 var_name##_arr[pattern_size] = { __VA_ARGS__ };                          \
    static constexpr ReadonlyBytes var_name = ReadonlyBytes { var_name##_arr, pattern_size };
ENUMERATE_HEADER_CONTENTS
#undef __ENUMERATE_MIME_TYPE_HEADER

Optional<String> guess_mime_type_based_on_sniffed_bytes(ReadonlyBytes bytes)
{
#define __ENUMERATE_MIME_TYPE_HEADER(var_name, mime_type, pattern_offset, pattern_size, ...)                       \
    if (static_cast<ssize_t>(bytes.size()) >= pattern_offset && bytes.slice(pattern_offset).starts_with(var_name)) \
        return mime_type;
    ENUMERATE_HEADER_CONTENTS;
#undef __ENUMERATE_MIME_TYPE_HEADER
    return {};
}
}
