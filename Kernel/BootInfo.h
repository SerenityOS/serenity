/*
 * Copyright (c) 2021, Gunnar Beutner <gbeutner@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/Arch/x86/PageDirectory.h>
#include <Kernel/PhysicalAddress.h>
#include <Kernel/VirtualAddress.h>

extern "C" PhysicalAddress start_of_prekernel_image;
extern "C" PhysicalAddress end_of_prekernel_image;
extern "C" size_t physical_to_virtual_offset;
extern "C" FlatPtr kernel_base;
#if ARCH(X86_64)
extern "C" u32 gdt64ptr;
extern "C" u16 code64_sel;
extern "C" PhysicalAddress boot_pml4t;
#endif
extern "C" PhysicalAddress boot_pdpt;
extern "C" PhysicalAddress boot_pd0;
extern "C" PhysicalAddress boot_pd_kernel;
extern "C" Kernel::PageTableEntry* boot_pd_kernel_pt1023;
extern "C" const char* kernel_cmdline;
