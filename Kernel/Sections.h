/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Platform.h>

#define READONLY_AFTER_INIT __attribute__((section(".ro_after_init")))
#define UNMAP_AFTER_INIT NEVER_INLINE __attribute__((section(".unmap_after_init")))

#define KERNEL_BASE 0xC0000000
#define KERNEL_PD_OFFSET 0x2000000
#define KERNEL_PD_END 0xF1000000
#define KERNEL_PT1024_BASE 0xFFE00000
#define KERNEL_QUICKMAP_PT (KERNEL_PT1024_BASE + 0x6000)
#define KERNEL_QUICKMAP_PD (KERNEL_PT1024_BASE + 0x7000)
#define KERNEL_QUICKMAP_PER_CPU_BASE (KERNEL_PT1024_BASE + 0x8000)
#define KERNEL_PHYSICAL_PAGES_BASE (KERNEL_BASE + KERNEL_PD_OFFSET)

#define USER_RANGE_CEILING 0xBE000000
