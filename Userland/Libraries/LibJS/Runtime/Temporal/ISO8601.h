/*
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Forward.h>

namespace JS::Temporal {

// 13.33 ISO 8601 grammar, https://tc39.es/proposal-temporal/#sec-temporal-iso8601grammar

bool is_valid_time_zone_numeric_utc_offset(String const&);

}
