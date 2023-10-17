/*
 * Copyright (c) 2023, Tim Flynn <trflynn89@serenityos.org>
 * Copyright (c) 2023, Cameron Youell <cameronyouell@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/String.h>
#include <LibCore/System.h>
#include <LibFileSystem/FileSystem.h>
#include <LibWebView/URL.h>

#if defined(ENABLE_PUBLIC_SUFFIX)
#    include <LibWebView/PublicSuffixData.h>
#endif

namespace WebView {

static Optional<URL> query_public_suffix_list(StringView url_string)
{
    auto out = MUST(String::from_utf8(url_string));
    if (!out.contains("://"sv))
        out = MUST(String::formatted("https://{}"sv, out));

    auto url = URL::create_with_url_or_path(out.to_deprecated_string());
    if (!url.is_valid())
        return {};

#if defined(ENABLE_PUBLIC_SUFFIX)
    if (url.host().has<URL::IPv4Address>() || url.host().has<URL::IPv6Address>())
        return url;

    if (url.scheme() != "http"sv && url.scheme() != "https"sv)
        return url;

    if (url.host().has<String>()) {
        auto const& host = url.host().get<String>();

        if (auto public_suffix = MUST(PublicSuffixData::the()->get_public_suffix(host)); public_suffix.has_value())
            return url;

        if (host.ends_with_bytes(".local"sv) || host.ends_with_bytes("localhost"sv))
            return url;
    }

    return {};
#else
    return url;
#endif
}

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

    auto result = query_public_suffix_list(url);
    if (!result.has_value())
        return format_search_engine();

    return result.release_value();
}

static URLParts break_file_url_into_parts(URL const& url, StringView url_string)
{
    auto scheme = url_string.substring_view(0, url.scheme().bytes_as_string_view().length() + "://"sv.length());
    auto path = url_string.substring_view(scheme.length());

    return URLParts { scheme, path, {} };
}

static URLParts break_web_url_into_parts(URL const& url, StringView url_string)
{
    auto host = MUST(url.serialized_host());

    auto public_suffix = MUST(PublicSuffixData::the()->get_public_suffix(host));
    if (!public_suffix.has_value())
        return {};

    auto public_suffix_start = url_string.find(*public_suffix);
    auto public_suffix_end = public_suffix_start.value() + public_suffix->bytes_as_string_view().length();

    auto scheme_and_subdomain = url_string.substring_view(0, *public_suffix_start);
    scheme_and_subdomain = scheme_and_subdomain.trim("."sv, TrimMode::Right);

    if (auto index = scheme_and_subdomain.find_last('.'); index.has_value())
        scheme_and_subdomain = scheme_and_subdomain.substring_view(0, *index + 1);
    else
        scheme_and_subdomain = scheme_and_subdomain.substring_view(0, url.scheme().bytes_as_string_view().length() + "://"sv.length());

    auto effective_tld_plus_one = url_string.substring_view(scheme_and_subdomain.length(), public_suffix_end - scheme_and_subdomain.length());
    auto remainder = url_string.substring_view(public_suffix_end);

    return URLParts { scheme_and_subdomain, effective_tld_plus_one, remainder };
}

Optional<URLParts> break_url_into_parts(StringView url_string)
{
    auto url = URL::create_with_url_or_path(url_string);
    if (!url.is_valid())
        return {};

    if (url.scheme() == "file"sv)
        return break_file_url_into_parts(url, url_string);
    if (url.scheme().is_one_of("http"sv, "https"sv, "gemini"sv))
        return break_web_url_into_parts(url, url_string);

    return {};
}

}
