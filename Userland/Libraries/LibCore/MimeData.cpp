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
    for (auto it : m_data)
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
    if (path.ends_with("/", CaseSensitivity::CaseInsensitive))
        return "text/html";
    if (path.ends_with(".csv", CaseSensitivity::CaseInsensitive))
        return "text/csv";
    return "text/plain";
}

}
