/*
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/String.h>
#include <LibJS/Runtime/Temporal/ISO8601.h>

namespace JS::Temporal {

// 13.33 ISO 8601 grammar, https://tc39.es/proposal-temporal/#sec-temporal-iso8601grammar

// TimeZoneNumericUTCOffset, https://tc39.es/proposal-temporal/#prod-TimeZoneNumericUTCOffset
bool is_valid_time_zone_numeric_utc_offset(String const&)
{
    // TODO: Implement me :^)
    return false;
}

}
