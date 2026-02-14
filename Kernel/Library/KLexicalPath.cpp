/*
 * Copyright (c) 2021, Max Wipfli <max.wipfli@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Vector.h>
#include <Kernel/Library/KLexicalPath.h>
#include <Kernel/Library/MiniStdLib.h>

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

void canonicalize_absolute_path(KString& absolute_path)
{
    VERIFY(is_absolute(absolute_path.view()));
    char* path = const_cast<char*>(absolute_path.characters());
    char* src = path;
    char* dst = path;

    while (*src) {
        /* Collapse multiple slashes */
        if (*src == '/') {
            if (dst == path || *(dst - 1) != '/')
                *dst++ = '/';

            src++;
            while (*src == '/')
                src++;
            continue;
        }

        /* Identify segment */
        char* seg = src;
        while (*src && *src != '/')
            src++;

        int len = src - seg;

        /* Handle "." */
        if (len == 1 && seg[0] == '.') {
            continue;
        }

        /* Handle ".." */
        if (len == 2 && seg[0] == '.' && seg[1] == '.') {
            if (dst > path + 1) {
                dst--;
                while (dst > path && *(dst - 1) != '/')
                    dst--;
            } else {
                dst = path + 1;
            }
            continue;
        }

        /* Add slash if needed */
        if (dst > path && *(dst - 1) != '/')
            *dst++ = '/';

        /* Copy segment */
        memcpy(dst, seg, len);
        dst += len;
    }

    /* Remove trailing slash unless root */
    if (dst > path + 1 && *(dst - 1) == '/')
        dst--;

    /* Ensure at least "/" */
    if (dst == path)
        *dst++ = '/';

    *dst = '\0';

    absolute_path.decrease_size((dst - path));
}

ErrorOr<NonnullOwnPtr<KString>> try_join_non_canonical_second(StringView first, StringView second)
{
    VERIFY(is_absolute(first));
    VERIFY(is_canonical(first));
    VERIFY(!is_absolute(second));
    char* buffer;
    auto string = TRY(KString::try_create_uninitialized(first.length() + 1 + second.length(), buffer));
    __builtin_memcpy(buffer, first.characters_without_null_termination(), first.length());
    buffer[first.length()] = '/';
    __builtin_memcpy(buffer + first.length() + 1, second.characters_without_null_termination(), second.length());
    buffer[string->length()] = 0;
    return string;
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
