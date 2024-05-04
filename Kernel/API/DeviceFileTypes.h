/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/DistinctNumeric.h>

AK_TYPEDEF_DISTINCT_ORDERED_ID(unsigned, MajorNumber);
AK_TYPEDEF_DISTINCT_ORDERED_ID(unsigned, MinorNumber);

enum class DeviceNodeType {
    Block = 1,
    Character = 2,
};
