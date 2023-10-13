/*
 * Copyright (c) 2023, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/System.h>
#include <LibFileSystem/FileSystem.h>
#include <LibPublicSuffix/URL.h>
#include <LibWebView/URL.h>

namespace WebView {

Optional<URL> sanitize_url(StringView url, Optional<StringView> search_engine, AppendTLD append_tld)
{
    if (FileSystem::exists(url)) {
        auto path = FileSystem::real_path(url);
        if (path.is_error())
            return {};

        return URL::create_with_file_scheme(path.value().to_deprecated_string());
    }

    auto format_search_engine = [&]() -> Optional<URL> {
        if (!search_engine.has_value())
            return {};

        return MUST(String::formatted(*search_engine, URL::percent_decode(url)));
    };

    String url_buffer;

    if (append_tld == AppendTLD::Yes) {
        // FIXME: Expand the list of top level domains.
        if (!url.ends_with(".com"sv) && !url.ends_with(".net"sv) && !url.ends_with(".org"sv)) {
            url_buffer = MUST(String::formatted("{}.com", url));
            url = url_buffer;
        }
    }

    auto result = PublicSuffix::absolute_url(url);
    if (result.is_error())
        return format_search_engine();

    return result.release_value();
}

}
