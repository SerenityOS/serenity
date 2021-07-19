/*
 * Copyright (c) 2021, Gunnar Beutner <gbeutner@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/VirtualAddress.h>

extern "C" u8 const* start_of_prekernel_image;
extern "C" u8 const* end_of_prekernel_image;
extern "C" __attribute__((section(".boot_bss"))) FlatPtr kernel_base;
#if ARCH(X86_64)
extern "C" u32 gdt64ptr;
extern "C" u16 code64_sel;
extern "C" FlatPtr boot_pml4t;
#endif
extern "C" FlatPtr boot_pdpt;
extern "C" FlatPtr boot_pd0;
extern "C" FlatPtr boot_pd_kernel;
extern "C" FlatPtr boot_pd_kernel_pt1023;
extern "C" const char* kernel_cmdline;
