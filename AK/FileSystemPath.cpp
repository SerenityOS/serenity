/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
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

#include "FileSystemPath.h"
#include "StringBuilder.h"
#include "Vector.h"

namespace AK {

FileSystemPath::FileSystemPath(const StringView& s)
    : m_string(s)
{
    canonicalize();
    m_is_valid = true;
}

void FileSystemPath::canonicalize()
{
    if (m_string.is_empty()) {
        m_parts.clear();
        return;
    }

    bool is_absolute_path = m_string[0] == '/';
    auto parts = m_string.split_view('/');

    if (!is_absolute_path)
        parts.prepend(".");

    size_t approximate_canonical_length = 0;
    Vector<String> canonical_parts;

    for (size_t i = 0; i < parts.size(); ++i) {
        auto& part = parts[i];
        if (is_absolute_path || i != 0) {
            if (part == ".")
                continue;
        }
        if (part == "..") {
            if (!canonical_parts.is_empty())
                canonical_parts.take_last();
            continue;
        }
        if (!part.is_empty()) {
            approximate_canonical_length += part.length() + 1;
            canonical_parts.append(part);
        }
    }
    if (canonical_parts.is_empty()) {
        m_string = m_basename = m_dirname = "/";
        return;
    }

    StringBuilder dirname_builder(approximate_canonical_length);
    for (size_t i = 0; i < canonical_parts.size() - 1; ++i) {
        auto& canonical_part = canonical_parts[i];
        if (is_absolute_path || i != 0)
            dirname_builder.append('/');
        dirname_builder.append(canonical_part);
    }
    m_dirname = dirname_builder.to_string();

    m_basename = canonical_parts.last();
    auto name_parts = m_basename.split('.');
    m_title = name_parts.is_empty() ? String() : name_parts[0];
    if (name_parts.size() > 1)
        m_extension = name_parts[1];

    StringBuilder builder(approximate_canonical_length);
    for (size_t i = 0; i < canonical_parts.size(); ++i) {
        auto& canonical_part = canonical_parts[i];
        if (is_absolute_path || i != 0)
            builder.append('/');
        builder.append(canonical_part);
    }
    m_parts = move(canonical_parts);
    m_string = builder.to_string();
}

bool FileSystemPath::has_extension(StringView extension) const
{
    // FIXME: This is inefficient, expand StringView with enough functionality that we don't need to copy strings here.
    String extension_string = extension;
    return m_string.to_lowercase().ends_with(extension_string.to_lowercase());
}

String canonicalized_path(const StringView& path)
{
    return FileSystemPath(path).string();
}

}
