/*
 * Copyright (c) 2022, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Array.h>
#include <AK/URL.h>

namespace Web::Fetch {

// https://fetch.spec.whatwg.org/#local-scheme
// A local scheme is "about", "blob", or "data".
inline constexpr Array LOCAL_SCHEMES = {
    "about"sv, "blob"sv, "data"sv
};

// https://fetch.spec.whatwg.org/#http-scheme
// An HTTP(S) scheme is "http" or "https".
inline constexpr Array HTTP_SCHEMES = {
    "http"sv, "https"sv
};

// https://fetch.spec.whatwg.org/#fetch-scheme
// A fetch scheme is "about", "blob", "data", "file", or an HTTP(S) scheme.
inline constexpr Array FETCH_SCHEMES = {
    "about"sv, "blob"sv, "data"sv, "file"sv, "http"sv, "https"sv
};

[[nodiscard]] bool is_local_url(AK::URL const&);

}
