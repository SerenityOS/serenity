/*
 * Copyright (c) 2021, Max Wipfli <max.wipfli@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Vector.h>
#include <Kernel/Library/KLexicalPath.h>

namespace Kernel::KLexicalPath {

static StringView const s_single_dot = "."sv;

bool is_absolute(StringView path)
{
    return !path.is_empty() && path[0] == '/';
}

bool is_canonical(StringView path)
{
    // FIXME: This can probably be done more efficiently.
    if (path.is_empty())
        return false;
    if (path.ends_with('/') && path.length() != 1)
        return false;
    if (path.starts_with("./"sv) || path.contains("/./"sv) || path.ends_with("/."sv))
        return false;
    if (path.starts_with("../"sv) || path.contains("/../"sv) || path.ends_with("/.."sv))
        return false;
    if (path.contains("//"sv))
        return false;
    return true;
}

StringView basename(StringView a_path)
{
    if (a_path == "/"sv)
        return a_path;
    if (a_path.is_empty())
        return s_single_dot;
    auto path = a_path.trim("/"sv, TrimMode::Right);
    // NOTE: If it's empty now, it means the path was just a series of slashes.
    if (path.is_empty())
        return a_path.substring_view(0, 1);
    auto slash_index = path.find_last('/');
    if (!slash_index.has_value())
        return path;
    auto basename = path.substring_view(*slash_index + 1);
    return basename;
}

StringView dirname(StringView path)
{
    VERIFY(is_canonical(path));
    auto slash_index = path.find_last('/');
    VERIFY(slash_index.has_value());
    return path.substring_view(0, *slash_index);
}

Vector<StringView> parts(StringView path)
{
    VERIFY(is_canonical(path));
    return path.split_view('/');
}

ErrorOr<NonnullOwnPtr<KString>> try_join(StringView first, StringView second)
{
    VERIFY(is_canonical(first));
    VERIFY(is_canonical(second));
    VERIFY(!is_absolute(second));

    if (first == "/"sv) {
        char* buffer;
        auto string = TRY(KString::try_create_uninitialized(1 + second.length(), buffer));
        buffer[0] = '/';
        __builtin_memcpy(buffer + 1, second.characters_without_null_termination(), second.length());
        buffer[string->length()] = 0;
        return string;
    }
    char* buffer;
    auto string = TRY(KString::try_create_uninitialized(first.length() + 1 + second.length(), buffer));
    __builtin_memcpy(buffer, first.characters_without_null_termination(), first.length());
    buffer[first.length()] = '/';
    __builtin_memcpy(buffer + first.length() + 1, second.characters_without_null_termination(), second.length());
    buffer[string->length()] = 0;
    return string;
}

}
