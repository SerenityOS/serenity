/*
 * Copyright (c) 2022, Tim Flynn <trflynn89@pm.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibTimeZone/TimeZone.h>

namespace TimeZone {

Optional<TimeZone> __attribute__((weak)) time_zone_from_string(StringView) { return {}; }

}
