/*
 * Copyright (c) 2026, Bastiaan van der Plaat <bastiaan.v.d.plaat@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Array.h>
#include <AK/StringView.h>

namespace UsersSettings {

static constexpr u32 MIN_NORMAL_UID = 100;
static constexpr Array ACCOUNT_TYPE_NAMES = { "Standard"sv, "Administrator"sv };
static constexpr StringView WHEEL_GROUP_NAME = "wheel"sv;
static constexpr Array DEFAULT_USER_GROUPS = { "users"sv, "window"sv, "audio"sv, "lookup"sv, "phys"sv };

}
