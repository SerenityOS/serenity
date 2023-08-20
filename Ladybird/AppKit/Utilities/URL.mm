/*
 * Copyright (c) 2023, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/DeprecatedString.h>
#include <AK/String.h>
#include <AK/Vector.h>
#include <Ladybird/Utilities.h>
#include <LibFileSystem/FileSystem.h>

#import <Utilities/URL.h>

namespace Ladybird {

URL sanitize_url(StringView url_string)
{
    if (url_string.starts_with('/') || FileSystem::exists(url_string))
        return MUST(String::formatted("file://{}", MUST(FileSystem::real_path(url_string))));

    URL url { url_string };
    if (!url.is_valid())
        url = MUST(String::formatted("https://{}", url_string));

    return url;
}

URL sanitize_url(NSString* url_string)
{
    auto const* utf8 = [url_string UTF8String];
    return sanitize_url({ utf8, strlen(utf8) });
}

URL rebase_url_on_serenity_resource_root(StringView url_string)
{
    URL url { url_string };
    Vector<DeprecatedString> paths;

    for (auto segment : s_serenity_resource_root.split('/'))
        paths.append(move(segment));

    for (size_t i = 0; i < url.path_segment_count(); ++i)
        paths.append(url.path_segment_at_index(i));

    url.set_paths(move(paths));

    return url;
}

}
