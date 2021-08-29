/*
 * Copyright (c) 2021, Mahmoud Mandour <ma.mandourr@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/StringView.h>

namespace Core::Version {

constexpr StringView SERENITY_VERSION = "Version 1.0"sv;

String read_long_version_string();

}
