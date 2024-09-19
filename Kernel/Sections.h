/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Platform.h>
#ifdef __cplusplus
#    include <AK/Types.h>
#    include <Kernel/Boot/BootInfo.h>
#endif

#define READONLY_AFTER_INIT __attribute__((section(".ro_after_init")))
#define UNMAP_AFTER_INIT NEVER_INLINE __attribute__((section(".unmap_after_init")))

#define KERNEL_MAPPING_BASE 0x2000000000

#define KERNEL_PD_END (g_boot_info.kernel_mapping_base + KERNEL_PD_SIZE)
#define KERNEL_PT1024_OFFSET 0x3FE00000
#define KERNEL_PT1024_BASE (g_boot_info.kernel_mapping_base + KERNEL_PT1024_OFFSET)

#define KERNEL_MAX_CPU_COUNT 64
#define KERNEL_QUICKMAP_PT_PER_CPU_BASE (KERNEL_PT1024_BASE + (1 * KERNEL_MAX_CPU_COUNT * PAGE_SIZE))
#define KERNEL_QUICKMAP_PD_PER_CPU_BASE (KERNEL_PT1024_BASE + (2 * KERNEL_MAX_CPU_COUNT * PAGE_SIZE))
#define KERNEL_QUICKMAP_PER_CPU_BASE (KERNEL_PT1024_BASE + (3 * KERNEL_MAX_CPU_COUNT * PAGE_SIZE))

#define USER_RANGE_BASE 0x10000
#define USER_RANGE_CEILING (g_boot_info.kernel_mapping_base - 0x2000000)
