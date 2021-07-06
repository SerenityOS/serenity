/*
 * Copyright (c) 2021, Max Wipfli <max.wipfli@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/KLexicalPath.h>

namespace Kernel::KLexicalPath {

bool is_absolute(StringView const& path)
{
    return !path.is_empty() && path[0] == '/';
}

bool is_canonical(StringView const& path)
{
    // FIXME: This can probably be done more efficiently.
    if (path.is_empty())
        return false;
    if (path.ends_with('/') && path.length() != 1)
        return false;
    if (path.starts_with("./"sv) || path.contains("/./"sv) || path.ends_with("/."sv))
        return false;
    if (path.starts_with("../"sv) || path.contains("/../"sv) || path.ends_with("/.."))
        return false;
    if (path.contains("//"sv))
        return false;
    return true;
}

StringView basename(StringView const& path)
{
    auto slash_index = path.find_last('/');
    if (!slash_index.has_value()) {
        VERIFY(!path.is_empty());
        return path;
    }
    auto basename = path.substring_view(*slash_index + 1);
    VERIFY(!basename.is_empty() && basename != "."sv && basename != ".."sv);
    return basename;
}

StringView dirname(StringView const& path)
{
    VERIFY(is_canonical(path));
    auto slash_index = path.find_last('/');
    VERIFY(slash_index.has_value());
    return path.substring_view(0, *slash_index);
}

Vector<StringView> parts(StringView const& path)
{
    VERIFY(is_canonical(path));
    return path.split_view('/');
}

}
