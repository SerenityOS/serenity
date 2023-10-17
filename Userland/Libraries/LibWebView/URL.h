/*
 * Copyright (c) 2023, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Optional.h>
#include <AK/StringView.h>
#include <AK/URL.h>

namespace WebView {

enum class AppendTLD {
    No,
    Yes,
};
Optional<URL> sanitize_url(StringView, Optional<StringView> search_engine = {}, AppendTLD = AppendTLD::No);

struct URLParts {
    StringView scheme_and_subdomain;
    StringView effective_tld_plus_one;
    StringView remainder;
};
Optional<URLParts> break_url_into_parts(StringView url);

}
