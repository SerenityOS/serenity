/*
 * Copyright (c) 2022, Tim Flynn <trflynn89@serenityos.org>
 * Copyright (c) 2023, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>

namespace TimeZone {

enum class DaylightSavingsRule : u8;
enum class Region : u8;
enum class TimeZone : u16;

struct DateTime;

}
