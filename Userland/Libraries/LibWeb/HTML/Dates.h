/*
 * Copyright (c) 2023, stelar7 <dudedbz@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Forward.h>
#include <AK/String.h>

namespace Web::HTML {

u32 week_number_of_the_last_day(u64 year);
bool is_valid_week_string(StringView value);
bool is_valid_month_string(StringView value);
bool is_valid_date_string(StringView value);
bool is_valid_local_date_and_time_string(StringView value);
String normalize_local_date_and_time_string(String const& value);
bool is_valid_time_string(StringView value);

}
