/*
 * Copyright (c) 2021, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/String.h>

namespace HTTP {

// https://fetch.spec.whatwg.org/#concept-header
struct Header {
    // FIXME: These should be byte sequences.
    String name;
    String value;
};

bool is_forbidden_header_name(String const& header_name);
String normalize_header_value(String const& header_value);

}
