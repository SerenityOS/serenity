/*
 * Copyright (c) 2022-2023, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/DeprecatedString.h>
#include <AK/Function.h>
#include <AK/IterationDecision.h>

void for_each_character_containing(StringView query, Function<IterationDecision(u32 code_point, DeprecatedString const& display_name)> callback);
