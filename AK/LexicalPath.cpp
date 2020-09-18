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

#include <AK/LexicalPath.h>
#include <AK/StringBuilder.h>
#include <AK/StringView.h>
#include <AK/Vector.h>

namespace AK {

LexicalPath::LexicalPath(const StringView& s)
    : m_string(s)
{
    canonicalize();
    m_is_valid = true;
}

void LexicalPath::canonicalize()
{
    if (m_string.is_empty()) {
        m_parts.clear();
        return;
    }

    m_is_absolute = m_string[0] == '/';
    auto parts = m_string.split_view('/');

    size_t approximate_canonical_length = 0;
    Vector<String> canonical_parts;

    for (size_t i = 0; i < parts.size(); ++i) {
        auto& part = parts[i];
        if (part == ".")
            continue;
        if (part == "..") {
            if (canonical_parts.is_empty()) {
                if (m_is_absolute) {
                    // At the root, .. does nothing.
                    continue;
                }
            } else {
                if (canonical_parts.last() != "..") {
                    // A .. and a previous non-.. part cancel each other.
                    canonical_parts.take_last();
                    continue;
                }
            }
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
        if (m_is_absolute || i != 0)
            dirname_builder.append('/');
        dirname_builder.append(canonical_part);
    }
    m_dirname = dirname_builder.to_string();

    m_basename = canonical_parts.last();

    Optional<size_t> last_dot = StringView(m_basename).find_last_of('.');
    if (last_dot.has_value()) {
        m_title = m_basename.substring(0, last_dot.value());
        m_extension = m_basename.substring(last_dot.value() + 1, m_basename.length() - last_dot.value() - 1);
    } else {
        m_title = m_basename;
    }

    StringBuilder builder(approximate_canonical_length);
    for (size_t i = 0; i < canonical_parts.size(); ++i) {
        auto& canonical_part = canonical_parts[i];
        if (m_is_absolute || i != 0)
            builder.append('/');
        builder.append(canonical_part);
    }
    m_parts = move(canonical_parts);
    m_string = builder.to_string();
}

bool LexicalPath::has_extension(const StringView& extension) const
{
    return m_string.ends_with(extension, CaseSensitivity::CaseInsensitive);
}

String LexicalPath::canonicalized_path(const StringView& path)
{
    return LexicalPath(path).string();
}

}
