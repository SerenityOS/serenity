/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>

#include <AK/Platform.h>
VALIDATE_IS_X86()

namespace Kernel {

AK_ALWAYS_INLINE void cli()
{
    asm volatile("cli" ::
                     : "memory");
}
AK_ALWAYS_INLINE void sti()
{
    asm volatile("sti" ::
                     : "memory");
}
AK_ALWAYS_INLINE FlatPtr cpu_flags()
{
    FlatPtr flags;
    asm volatile(
        "pushf\n"
        "pop %0\n"
        : "=rm"(flags)::"memory");
    return flags;
}

#if ARCH(I386)
AK_ALWAYS_INLINE void set_fs(u16 segment)
{
    asm volatile(
        "mov %%ax, %%fs" ::"a"(segment)
        : "memory");
}

AK_ALWAYS_INLINE void set_gs(u16 segment)
{
    asm volatile(
        "mov %%ax, %%gs" ::"a"(segment)
        : "memory");
}

AK_ALWAYS_INLINE u16 get_fs()
{
    u16 fs;
    asm("mov %%fs, %%eax"
        : "=a"(fs));
    return fs;
}

AK_ALWAYS_INLINE u16 get_gs()
{
    u16 gs;
    asm("mov %%gs, %%eax"
        : "=a"(gs));
    return gs;
}
#endif

template<typename T>
AK_ALWAYS_INLINE T read_gs_value(FlatPtr offset)
{
    T val;
    asm volatile(
        "mov %%gs:%a[off], %[val]"
        : [val] "=r"(val)
        : [off] "ir"(offset));
    return val;
}

template<typename T>
AK_ALWAYS_INLINE void write_gs_value(FlatPtr offset, T val)
{
    asm volatile(
        "mov %[val], %%gs:%a[off]" ::[off] "ir"(offset), [val] "r"(val)
        : "memory");
}

AK_ALWAYS_INLINE FlatPtr read_gs_ptr(FlatPtr offset)
{
    FlatPtr val;
    asm volatile(
        "mov %%gs:%a[off], %[val]"
        : [val] "=r"(val)
        : [off] "ir"(offset));
    return val;
}

AK_ALWAYS_INLINE void write_gs_ptr(u32 offset, FlatPtr val)
{
    asm volatile(
        "mov %[val], %%gs:%a[off]" ::[off] "ir"(offset), [val] "r"(val)
        : "memory");
}

AK_ALWAYS_INLINE bool are_interrupts_enabled()
{
    return (cpu_flags() & 0x200) != 0;
}

FlatPtr read_cr0();
FlatPtr read_cr2();
FlatPtr read_cr3();
FlatPtr read_cr4();
u64 read_xcr0();

void write_cr0(FlatPtr);
void write_cr3(FlatPtr);
void write_cr4(FlatPtr);
void write_xcr0(u64);

void flush_idt();

AK_ALWAYS_INLINE void load_task_register(u16 selector)
{
    asm("ltr %0" ::"r"(selector));
}

FlatPtr read_dr0();
void write_dr0(FlatPtr);
FlatPtr read_dr1();
void write_dr1(FlatPtr);
FlatPtr read_dr2();
void write_dr2(FlatPtr);
FlatPtr read_dr3();
void write_dr3(FlatPtr);
FlatPtr read_dr6();
void write_dr6(FlatPtr);
FlatPtr read_dr7();
void write_dr7(FlatPtr);

AK_ALWAYS_INLINE static bool is_kernel_mode()
{
    u16 cs;
    asm volatile(
        "mov %%cs, %[cs] \n"
        : [cs] "=g"(cs));
    return (cs & 3) == 0;
}

AK_ALWAYS_INLINE void read_tsc(u32& lsw, u32& msw)
{
    asm volatile("rdtsc"
                 : "=d"(msw), "=a"(lsw));
}

AK_ALWAYS_INLINE u64 read_tsc()
{
    u32 lsw;
    u32 msw;
    read_tsc(lsw, msw);
    return ((u64)msw << 32) | lsw;
}

void stac();
void clac();

[[noreturn]] AK_ALWAYS_INLINE void halt_this()
{
    for (;;) {
        asm volatile("cli; hlt");
    }
}

}
