/*
 * Copyright (c) 2021, Patrick Meyer <git@the-space.agency>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>

typedef u64 volatile kcov_pc_t;
#define KCOV_ENTRY_SIZE sizeof(kcov_pc_t)
