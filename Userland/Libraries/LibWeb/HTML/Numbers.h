/*
 * Copyright (c) 2023, Jonatan Klemets <jonatan.r.klemets@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Forward.h>
#include <AK/String.h>

namespace Web::HTML {

Optional<i32> parse_integer(StringView string);

Optional<u32> parse_non_negative_integer(StringView string);

}
