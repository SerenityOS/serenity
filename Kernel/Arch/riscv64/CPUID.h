/*
 * Copyright (c) 2023, Sönke Holz <sholz8530@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/ArbitrarySizedEnum.h>
#include <AK/Types.h>
#include <AK/UFixedBigInt.h>

#include <AK/Platform.h>
VALIDATE_IS_RISCV64()

AK_MAKE_ARBITRARY_SIZED_ENUM(CPUFeature, u256,
    __End = CPUFeature(1u) << 255u) // SENTINEL VALUE
