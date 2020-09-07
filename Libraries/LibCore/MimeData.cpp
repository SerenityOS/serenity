/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
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

String guess_mime_type_based_on_filename(const URL& url)
{
    String lowercase_url = url.path().to_lowercase();
    if (lowercase_url.ends_with(".pbm"))
        return "image/x‑portable‑bitmap";
    if (url.path().ends_with(".pgm"))
        return "image/x‑portable‑graymap";
    if (url.path().ends_with(".png"))
        return "image/png";
    if (lowercase_url.ends_with(".ppm"))
        return "image/x‑portable‑pixmap";
    if (lowercase_url.ends_with(".gif"))
        return "image/gif";
    if (lowercase_url.ends_with(".bmp"))
        return "image/bmp";
    if (lowercase_url.ends_with(".jpg") || lowercase_url.ends_with(".jpeg"))
        return "image/jpeg";
    if (lowercase_url.ends_with(".svg"))
        return "image/svg+xml";
    if (lowercase_url.ends_with(".md"))
        return "text/markdown";
    if (lowercase_url.ends_with(".html") || lowercase_url.ends_with(".htm"))
        return "text/html";
    if (lowercase_url.ends_with("/"))
        return "text/html";
    return "text/plain";
}

}
