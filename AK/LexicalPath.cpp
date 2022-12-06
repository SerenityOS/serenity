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

LexicalPath::LexicalPath(DeprecatedString path)
    : m_string(canonicalized_path(move(path)))
{
    if (m_string.is_empty()) {
        m_string = ".";
        m_dirname = m_string;
        m_basename = {};
        m_title = {};
        m_extension = {};
        m_parts.clear();
        return;
    }

    m_parts = m_string.split_view('/');

    auto last_slash_index = m_string.view().find_last('/');
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

    auto last_dot_index = m_basename.find_last('.');
    // NOTE: if the dot index is 0, this means we have ".foo", it's not an extension, as the title would then be "".
    if (last_dot_index.has_value() && *last_dot_index != 0) {
        m_title = m_basename.substring_view(0, *last_dot_index);
        m_extension = m_basename.substring_view(*last_dot_index + 1);
    } else {
        m_title = m_basename;
        m_extension = {};
    }
}

Vector<DeprecatedString> LexicalPath::parts() const
{
    Vector<DeprecatedString> vector;
    vector.ensure_capacity(m_parts.size());
    for (auto& part : m_parts)
        vector.unchecked_append(part);
    return vector;
}

bool LexicalPath::has_extension(StringView extension) const
{
    return m_string.ends_with(extension, CaseSensitivity::CaseInsensitive);
}

DeprecatedString LexicalPath::canonicalized_path(DeprecatedString path)
{
    if (path.is_null())
        return {};

    // NOTE: We never allow an empty m_string, if it's empty, we just set it to '.'.
    if (path.is_empty())
        return ".";

    // NOTE: If there are no dots, no '//' and the path doesn't end with a slash, it is already canonical.
    if (!path.contains("."sv) && !path.contains("//"sv) && !path.ends_with('/'))
        return path;

    auto is_absolute = path[0] == '/';
    auto parts = path.split_view('/');
    size_t approximate_canonical_length = 0;
    Vector<DeprecatedString> canonical_parts;

    for (auto& part : parts) {
        if (part == ".")
            continue;
        if (part == "..") {
            if (canonical_parts.is_empty()) {
                if (is_absolute) {
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

    if (canonical_parts.is_empty() && !is_absolute)
        canonical_parts.append(".");

    StringBuilder builder(approximate_canonical_length);
    if (is_absolute)
        builder.append('/');
    builder.join('/', canonical_parts);
    return builder.to_deprecated_string();
}

DeprecatedString LexicalPath::absolute_path(DeprecatedString dir_path, DeprecatedString target)
{
    if (LexicalPath(target).is_absolute()) {
        return LexicalPath::canonicalized_path(target);
    }
    return LexicalPath::canonicalized_path(join(dir_path, target).string());
}

DeprecatedString LexicalPath::relative_path(StringView a_path, StringView a_prefix)
{
    if (!a_path.starts_with('/') || !a_prefix.starts_with('/')) {
        // FIXME: This should probably VERIFY or return an Optional<DeprecatedString>.
        return ""sv;
    }

    if (a_path == a_prefix)
        return ".";

    // NOTE: Strip optional trailing slashes, except if the full path is only "/".
    auto path = canonicalized_path(a_path);
    auto prefix = canonicalized_path(a_prefix);

    if (path == prefix)
        return ".";

    // NOTE: Handle this special case first.
    if (prefix == "/"sv)
        return path.substring_view(1);

    // NOTE: This means the prefix is a direct child of the path.
    if (path.starts_with(prefix) && path[prefix.length()] == '/') {
        return path.substring_view(prefix.length() + 1);
    }

    // FIXME: It's still possible to generate a relative path in this case, it just needs some "..".
    return path;
}

LexicalPath LexicalPath::append(StringView value) const
{
    return LexicalPath::join(m_string, value);
}

LexicalPath LexicalPath::prepend(StringView value) const
{
    return LexicalPath::join(value, m_string);
}

LexicalPath LexicalPath::parent() const
{
    return append(".."sv);
}

}
