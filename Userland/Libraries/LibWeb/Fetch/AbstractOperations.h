/*
 * Copyright (c) 2022, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Forward.h>

namespace Web::Fetch {

enum class HttpQuotedStringExtractValue {
    No,
    Yes,
};

String collect_an_http_quoted_string(GenericLexer& lexer, HttpQuotedStringExtractValue extract_value);

}
