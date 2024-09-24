/*
 * Copyright (c) 2024, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Platform.h>
#include <Kernel/Boot/Multiboot.h>
#include <Kernel/Prekernel/Prekernel.h>
#include <Kernel/Prekernel/Random.h>
#if ARCH(X86_64)
#    include <Kernel/Arch/x86_64/ASM_wrapper.h>
#    include <Kernel/Arch/x86_64/CPUID.h>
#endif

u64 generate_secure_seed()
{
    u32 seed = 0xFEEBDAED;

#if ARCH(X86_64)
    Kernel::CPUID processor_info(0x1);
    if (processor_info.edx() & (1 << 4)) // TSC
        seed ^= Kernel::read_tsc();

    if (processor_info.ecx() & (1 << 30)) // RDRAND
        seed ^= Kernel::read_rdrand();

    Kernel::CPUID extended_features(0x7);
    if (extended_features.ebx() & (1 << 18)) // RDSEED
        seed ^= Kernel::read_rdseed();
#else
#    warning No native randomness source available for this architecture
#endif

    seed ^= multiboot_info_ptr->mods_addr;
    seed ^= multiboot_info_ptr->framebuffer_addr;

    return seed;
}
