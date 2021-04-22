/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>

extern "C" {

u64 __ashldi3(u64 num, unsigned int shift);

u64 __lshrdi3(u64 num, unsigned int shift);

u64 __udivdi3(u64 num, u64 den);

u64 __umoddi3(u64 num, u64 den);

uint64_t __udivmoddi4(uint64_t num, uint64_t den, uint64_t* rem_p);

int64_t __divdi3(int64_t a, int64_t b);

int64_t __moddi3(int64_t a, int64_t b);
}
