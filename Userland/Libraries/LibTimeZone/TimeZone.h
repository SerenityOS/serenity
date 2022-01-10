/*
 * Copyright (c) 2022, Tim Flynn <trflynn89@pm.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Optional.h>
#include <AK/StringView.h>
#include <AK/Time.h>
#include <AK/Types.h>
#include <LibTimeZone/Forward.h>

namespace TimeZone {

Optional<TimeZone> time_zone_from_string(StringView time_zone);
StringView time_zone_to_string(TimeZone time_zone);
Optional<StringView> canonicalize_time_zone(StringView time_zone);

Optional<i64> get_time_zone_offset(TimeZone time_zone, AK::Time time);
Optional<i64> get_time_zone_offset(StringView time_zone, AK::Time time);

}
