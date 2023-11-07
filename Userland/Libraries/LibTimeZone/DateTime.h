/*
 * Copyright (c) 2023, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Optional.h>
#include <AK/StringView.h>
#include <AK/Time.h>

// This file contains definitions of Core::DateTime methods which require TZDB data.
namespace Core {

Optional<StringView> parse_time_zone_name(GenericLexer&);
void apply_time_zone_offset(StringView time_zone, UnixDateTime& time);

}
