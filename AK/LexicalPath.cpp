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

ErrorOr<LexicalPath> LexicalPath::from_string(StringView string_view)
{
    return from_string(TRY(String::from_utf8(string_view)));
}

ErrorOr<LexicalPath> LexicalPath::from_string(String path_string)
{
    LexicalPath path;
    path.m_string = TRY(canonicalized_path(path_string.bytes_as_string_view()));
    if (path.m_string.is_empty()) {
        path.m_string = TRY(String::from_utf8("."sv));
        path.m_dirname = path.m_string;
        path.m_basename = {};
        path.m_title = {};
        path.m_extension = {};
        path.m_parts.clear();
        return path;
    }

    path.m_parts = TRY(path.m_string.split_bytes('/'));

    auto last_slash_index = path.m_string.bytes_as_string_view().find_last('/');
    if (!last_slash_index.has_value()) {
        // The path contains a single part and is not absolute. m_dirname = "."sv
        path.m_dirname = TRY(String::from_utf8("."sv));
    } else if (*last_slash_index == 0) {
        // The path contains a single part and is absolute. m_dirname = "/"sv
        path.m_dirname = TRY(path.m_string.substring_from_byte_offset(0, 1));
    } else {
        path.m_dirname = TRY(path.m_string.substring_from_byte_offset(0, *last_slash_index));
    }

    if (path.m_string == "/")
        path.m_basename = path.m_string;
    else {
        VERIFY(path.m_parts.size() > 0);
        path.m_basename = path.m_parts.last();
    }

    auto last_dot_index = path.m_basename.find_last('.');
    // NOTE: if the dot index is 0, this means we have ".foo", it's not an extension, as the title would then be "".
    if (last_dot_index.has_value() && *last_dot_index != 0) {
        path.m_title = TRY(path.m_basename.substring_from_byte_offset(0, *last_dot_index));
        path.m_extension = TRY(path.m_basename.substring_from_byte_offset(*last_dot_index + 1));
    } else {
        path.m_title = path.m_basename;
        path.m_extension = {};
    }
    return path;
}

ErrorOr<Vector<String>> LexicalPath::parts() const
{
    Vector<String> vector;
    TRY(vector.try_ensure_capacity(m_parts.size()));
    for (auto& part : m_parts)
        vector.unchecked_append(part);
    return vector;
}

bool LexicalPath::has_extension(StringView extension) const
{
    return m_string.ends_with_bytes(extension, CaseSensitivity::CaseInsensitive);
}

bool LexicalPath::is_child_of(LexicalPath const& possible_parent) const
{
    // Any relative path is a child of an absolute path.
    if (!this->is_absolute() && possible_parent.is_absolute())
        return true;
    // An absolute path can't meaningfully be a child of a relative path.
    if (this->is_absolute() && !possible_parent.is_absolute())
        return false;

    // Two relative paths and two absolute paths can be meaningfully compared.
    if (possible_parent.parts_view().size() > this->parts_view().size())
        return false;
    auto common_parts_with_parent = this->parts_view().span().trim(possible_parent.parts_view().size());
    return common_parts_with_parent == possible_parent.parts_view().span();
}

ErrorOr<String> LexicalPath::canonicalized_path(StringView path)
{
    VERIFY(!path.is_null());

    // NOTE: We never allow an empty m_string, if it's empty, we just set it to '.'.
    if (path.is_empty())
        return String::from_utf8("."sv);

    // NOTE: If there are no dots, no '//' and the path doesn't end with a slash, it is already canonical.
    if (!path.contains("."sv) && !path.contains("//"sv) && !path.ends_with('/'))
        return String::from_utf8(path);

    auto is_absolute = path.starts_with('/');
    auto parts = path.split_view('/');
    size_t approximate_canonical_length = 0;
    Vector<String> canonical_parts;

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
        approximate_canonical_length += part.bytes().size() + 1;
        TRY(canonical_parts.try_append(TRY(String::from_utf8(part))));
    }

    if (canonical_parts.is_empty() && !is_absolute)
        TRY(canonical_parts.try_append(TRY(String::from_utf8("."sv))));

    StringBuilder builder(approximate_canonical_length);
    if (is_absolute)
        TRY(builder.try_append('/'));
    builder.join('/', canonical_parts);
    return builder.to_string();
}

ErrorOr<String> LexicalPath::absolute_path(StringView dir_path, StringView target)
{
    if (TRY(LexicalPath::from_string(target)).is_absolute()) {
        return LexicalPath::canonicalized_path(target);
    }
    return LexicalPath::canonicalized_path(TRY(join(dir_path, target)).string());
}

ErrorOr<String> LexicalPath::relative_path(StringView a_path, StringView a_prefix)
{
    if (!a_path.starts_with('/') || !a_prefix.starts_with('/')) {
        // FIXME: This should probably VERIFY or return an Optional<String>.
        return String {};
    }

    if (a_path == a_prefix)
        return String::from_utf8("."sv);

    // NOTE: Strip optional trailing slashes, except if the full path is only "/".
    auto path = TRY(canonicalized_path(TRY(String::from_utf8(a_path))));
    auto prefix = TRY(canonicalized_path(TRY(String::from_utf8(a_prefix))));

    if (path == prefix)
        return String::from_utf8("."sv);

    // NOTE: Handle this special case first.
    if (prefix == "/"sv)
        return path.substring_from_byte_offset(1);

    // NOTE: This means the prefix is a direct child of the path.
    auto prefix_with_slash = TRY(String::formatted("{}/", prefix));
    if (path.starts_with_bytes(prefix_with_slash)) {
        return path.substring_from_byte_offset(prefix_with_slash.bytes().size());
    }

    auto path_parts = TRY(path.split_bytes('/'));
    auto prefix_parts = TRY(prefix.split_bytes('/'));
    size_t index_of_first_part_that_differs = 0;
    for (; index_of_first_part_that_differs < path_parts.size() && index_of_first_part_that_differs < prefix_parts.size(); index_of_first_part_that_differs++) {
        if (path_parts[index_of_first_part_that_differs] != prefix_parts[index_of_first_part_that_differs])
            break;
    }

    StringBuilder builder;
    for (size_t part_index = index_of_first_part_that_differs; part_index < prefix_parts.size(); part_index++) {
        builder.append("../"sv);
    }
    for (size_t part_index = index_of_first_part_that_differs; part_index < path_parts.size(); part_index++) {
        builder.append(path_parts[part_index]);
        if (part_index != path_parts.size() - 1) // We don't need a slash after the file name or the name of the last directory
            builder.append('/');
    }

    return builder.to_string();
}

ErrorOr<LexicalPath> LexicalPath::append(StringView value) const
{
    return LexicalPath::join(m_string, value);
}

ErrorOr<LexicalPath> LexicalPath::prepend(StringView value) const
{
    return LexicalPath::join(value, m_string);
}

ErrorOr<LexicalPath> LexicalPath::parent() const
{
    return append(".."sv);
}

}
