/*
 * Copyright (c) 2024, Sönke Holz <sholz8530@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>

#include <Kernel/Sections.h>

namespace Kernel {

// Kernel virtual memory layout:
// Kernel stack | BootInfo | Quickmap page table | EFI memory map | Kernel cmdline | Kernel
// ^ KERNEL_MAPPING_BASE
// NOTE: If the kernel cmdline overflows into the kernel memory range, we catch that in the map_pages function (a page is not allowed to be remapped)

static constexpr size_t KERNEL_STACK_SIZE = 64 * KiB;
static_assert(KERNEL_STACK_SIZE % PAGE_SIZE == 0);

static constexpr FlatPtr KERNEL_STACK_VADDR = KERNEL_MAPPING_BASE;
static constexpr FlatPtr BOOT_INFO_VADDR = KERNEL_MAPPING_BASE + KERNEL_STACK_SIZE;

static constexpr FlatPtr QUICKMAP_PAGE_TABLE_VADDR = round_up_to_power_of_two(BOOT_INFO_VADDR + sizeof(BootInfo), PAGE_SIZE);

// This assumes PAGE_SIZE == PAGE_TABLE_SIZE
static constexpr FlatPtr EFI_MEMORY_MAP_VADDR = QUICKMAP_PAGE_TABLE_VADDR + PAGE_SIZE;

static constexpr size_t EFI_MEMORY_MAP_MAX_SIZE = 10uz * PAGE_SIZE;

static constexpr FlatPtr KERNEL_CMDLINE_VADDR = EFI_MEMORY_MAP_VADDR + EFI_MEMORY_MAP_MAX_SIZE;

}
