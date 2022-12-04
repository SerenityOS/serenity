/*
 * Copyright (c) 2020, Stephan Unverwerth <s.unverwerth@serenityos.org>
 * Copyright (c) 2021-2022, David Tuin <davidot@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/DeprecatedString.h>
#include <AK/Optional.h>
#include <LibJS/SourceRange.h>

namespace JS {

struct ParserError {
    DeprecatedString message;
    Optional<Position> position;

    DeprecatedString to_string() const;
    DeprecatedString source_location_hint(StringView source, char const spacer = ' ', char const indicator = '^') const;
};

}
