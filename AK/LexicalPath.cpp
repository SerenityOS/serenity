/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Max Wipfli <max.wipfli@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/LexicalPath.h>
#include <AK/StringBuilder.h>
#include <AK/StringView.h>
#include <AK/Vector.h>

namespace AK {

char s_single_dot = '.';

LexicalPath::LexicalPath(String s)
    : m_string(move(s))
{
    canonicalize();
}

Vector<String> LexicalPath::parts() const
{
    Vector<String> vector;
    vector.ensure_capacity(m_parts.size());
    for (auto& part : m_parts)
        vector.unchecked_append(part);
    return vector;
}

void LexicalPath::canonicalize()
{
    // NOTE: We never allow an empty m_string, if it's empty, we just set it to '.'.
    if (m_string.is_empty()) {
        m_string = ".";
        m_dirname = m_string;
        m_basename = {};
        m_title = {};
        m_extension = {};
        m_parts.clear();
        return;
    }

    // NOTE: If there are no dots, no '//' and the path doesn't end with a slash, it is already canonical.
    if (m_string.contains("."sv) || m_string.contains("//"sv) || m_string.ends_with('/')) {
        auto parts = m_string.split_view('/');
        size_t approximate_canonical_length = 0;
        Vector<String> canonical_parts;

        for (auto& part : parts) {
            if (part == ".")
                continue;
            if (part == "..") {
                if (canonical_parts.is_empty()) {
                    if (is_absolute()) {
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
            approximate_canonical_length += part.length() + 1;
            canonical_parts.append(part);
        }

        if (canonical_parts.is_empty() && !is_absolute())
            canonical_parts.append(".");

        StringBuilder builder(approximate_canonical_length);
        if (is_absolute())
            builder.append('/');
        builder.join('/', canonical_parts);
        m_string = builder.to_string();
    }

    m_parts = m_string.split_view('/');

    auto last_slash_index = m_string.view().find_last_of('/');
    if (!last_slash_index.has_value()) {
        // The path contains a single part and is not absolute. m_dirname = "."sv
        m_dirname = { &s_single_dot, 1 };
    } else if (*last_slash_index == 0) {
        // The path contains a single part and is absolute. m_dirname = "/"sv
        m_dirname = m_string.substring_view(0, 1);
    } else {
        m_dirname = m_string.substring_view(0, *last_slash_index);
    }

    if (m_string == "/")
        m_basename = m_string;
    else {
        VERIFY(m_parts.size() > 0);
        m_basename = m_parts.last();
    }

    auto last_dot_index = m_basename.find_last_of('.');
    // NOTE: if the dot index is 0, this means we have ".foo", it's not an extension, as the title would then be "".
    if (last_dot_index.has_value() && *last_dot_index != 0) {
        m_title = m_basename.substring_view(0, *last_dot_index);
        m_extension = m_basename.substring_view(*last_dot_index + 1);
    } else {
        m_title = m_basename;
        m_extension = {};
    }
}

bool LexicalPath::has_extension(StringView const& extension) const
{
    return m_string.ends_with(extension, CaseSensitivity::CaseInsensitive);
}

String LexicalPath::canonicalized_path(String path)
{
    return LexicalPath(move(path)).string();
}

String LexicalPath::relative_path(String absolute_path, String const& prefix)
{
    if (!LexicalPath { absolute_path }.is_absolute() || !LexicalPath { prefix }.is_absolute())
        return {};

    if (!absolute_path.starts_with(prefix))
        return absolute_path;

    size_t prefix_length = LexicalPath { prefix }.string().length();
    if (prefix != "/")
        prefix_length++;
    if (prefix_length >= absolute_path.length())
        return {};

    return absolute_path.substring(prefix_length);
}

void LexicalPath::append(String const& component)
{
    StringBuilder builder;
    builder.append(m_string);
    builder.append('/');
    builder.append(component);

    m_string = builder.to_string();
    canonicalize();
}

}
