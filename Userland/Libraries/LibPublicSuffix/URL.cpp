/*
 * Copyright (c) 2023, Cameron Youell <cameronyouell@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/String.h>
#include <AK/URL.h>
#include <LibPublicSuffix/URL.h>
#if defined(ENABLE_PUBLIC_SUFFIX_DOWNLOAD)
#    include <LibPublicSuffix/PublicSuffixData.h>
#endif

namespace PublicSuffix {
ErrorOr<String> absolute_url(StringView url)
{
    String out = TRY(String::from_utf8(url));
#if !defined(ENABLE_PUBLIC_SUFFIX_DOWNLOAD)
    return out;
#else
    if (!out.contains("://"sv))
        out = TRY(String::formatted("https://{}"sv, out));

    auto final_url = URL::create_with_url_or_path(out.to_deprecated_string());
    if (!final_url.is_valid())
        return Error::from_string_view("Invalid URL"sv);

    if (final_url.host().has<URL::IPv4Address>() || final_url.host().has<URL::IPv6Address>())
        return out;

    if (final_url.scheme() != "http"sv && final_url.scheme() != "https"sv)
        return out;

    if (final_url.host().has<String>()) {
        auto string_host = final_url.host().get<String>();
        auto maybe_public_suffix = TRY(PublicSuffixData::the()->get_public_suffix(string_host));
        if (maybe_public_suffix.has_value())
            return out;

        if (string_host.ends_with_bytes(".local"sv) || string_host.ends_with_bytes("localhost"sv))
            return out;
    }

    return Error::from_string_view("Invalid URL"sv);
#endif
}
}
