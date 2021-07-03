/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/LexicalPath.h>
#include <AK/StringBuilder.h>
#include <AK/StringView.h>
#include <AK/Vector.h>

namespace AK {

LexicalPath::LexicalPath(String s)
    : m_string(move(s))
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
        m_string = m_basename = m_dirname = m_is_absolute ? "/" : ".";
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

    if (m_dirname.is_empty()) {
        m_dirname = m_is_absolute ? "/" : ".";
    }

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

String LexicalPath::canonicalized_path(String path)
{
    return LexicalPath(move(path)).string();
}

String LexicalPath::relative_path(String absolute_path, const String& prefix)
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
