/*
 * Copyright (c) 2026, Sönke Holz <soenke.holz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/DistinctNumeric.h>

namespace Kernel {

AK_TYPEDEF_DISTINCT_NUMERIC_GENERAL(size_t, InterruptNumber, Arithmetic, Comparison, Increment)

}
