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

String guess_mime_type_based_on_filename(const StringView& path)
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

#define ENUMERATE_HEADER_CONTENTS                                                                                                 \
    __ENUMERATE_MIME_TYPE_HEADER(bmp, "image/bmp", 2, 'B', 'M')                                                                   \
    __ENUMERATE_MIME_TYPE_HEADER(bzip2, "application/x-bzip2", 3, 'B', 'Z', 'h')                                                  \
    __ENUMERATE_MIME_TYPE_HEADER(elf, "extra/elf", 4, 0x7F, 'E', 'L', 'F')                                                        \
    __ENUMERATE_MIME_TYPE_HEADER(gif_87, "image/gif", 6, 'G', 'I', 'F', '8', '7', 'a')                                            \
    __ENUMERATE_MIME_TYPE_HEADER(gif_89, "image/gif", 6, 'G', 'I', 'F', '8', '9', 'a')                                            \
    __ENUMERATE_MIME_TYPE_HEADER(gzip, "extra/gzip", 2, 0x1F, 0x8B)                                                               \
    __ENUMERATE_MIME_TYPE_HEADER(jpeg, "image/jpeg", 4, 0xFF, 0xD8, 0xFF, 0xDB)                                                   \
    __ENUMERATE_MIME_TYPE_HEADER(jpeg_huh, "image/jpeg", 4, 0xFF, 0xD8, 0xFF, 0xEE)                                               \
    __ENUMERATE_MIME_TYPE_HEADER(jpeg_jfif, "image/jpeg", 12, 0xFF, 0xD8, 0xFF, 0xE0, 0x00, 0x10, 'J', 'F', 'I', 'F', 0x00, 0x01) \
    __ENUMERATE_MIME_TYPE_HEADER(pbm, "image/x-portable-bitmap", 3, 0x50, 0x31, 0x0A)                                             \
    __ENUMERATE_MIME_TYPE_HEADER(pgm, "image/x-portable-graymap", 3, 0x50, 0x32, 0x0A)                                            \
    __ENUMERATE_MIME_TYPE_HEADER(png, "image/png", 8, 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A)                                \
    __ENUMERATE_MIME_TYPE_HEADER(ppm, "image/x-portable-pixmap", 3, 0x50, 0x33, 0x0A)                                             \
    __ENUMERATE_MIME_TYPE_HEADER(shell, "text/x-shellscript", 10, '#', '!', '/', 'b', 'i', 'n', '/', 's', 'h', '\n')              \
    __ENUMERATE_MIME_TYPE_HEADER(tiff, "image/tiff", 4, 'I', 'I', '*', 0x00)                                                      \
    __ENUMERATE_MIME_TYPE_HEADER(tiff_bigendian, "image/tiff", 4, 'M', 'M', 0x00, '*')

#define __ENUMERATE_MIME_TYPE_HEADER(var_name, mime_type, pattern_size, ...) \
    static const u8 var_name##_arr[pattern_size] = { __VA_ARGS__ };          \
    static constexpr ReadonlyBytes var_name = ReadonlyBytes { var_name##_arr, pattern_size };
ENUMERATE_HEADER_CONTENTS
#undef __ENUMERATE_MIME_TYPE_HEADER

Optional<String> guess_mime_type_based_on_sniffed_bytes(const ReadonlyBytes& bytes)
{
#define __ENUMERATE_MIME_TYPE_HEADER(var_name, mime_type, pattern_size, ...) \
    if (bytes.starts_with(var_name))                                         \
        return mime_type;
    ENUMERATE_HEADER_CONTENTS;
#undef __ENUMERATE_MIME_TYPE_HEADER
    return {};
}
}
