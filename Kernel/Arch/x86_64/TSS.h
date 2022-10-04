/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Leon Albrecht <leon2002.la@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>

#include <AK/Platform.h>
VALIDATE_IS_X86()

namespace Kernel {

struct [[gnu::packed]] TSS32 {
    u16 backlink, __blh;
    u32 esp0;
    u16 ss0, __ss0h;
    u32 esp1;
    u16 ss1, __ss1h;
    u32 esp2;
    u16 ss2, __ss2h;
    u32 cr3, eip, eflags;
    u32 eax, ecx, edx, ebx, esp, ebp, esi, edi;
    u16 es, __esh;
    u16 cs, __csh;
    u16 ss, __ssh;
    u16 ds, __dsh;
    u16 fs, __fsh;
    u16 gs, __gsh;
    u16 ldt, __ldth;
    u16 trace, iomapbase;
};

struct [[gnu::packed]] TSS64 {
    u32 __1; // Link?
    u32 rsp0l;
    u32 rsp0h;
    u32 rsp1l;
    u32 rsp1h;
    u32 rsp2l;
    u32 rsp2h;
    u64 __2; // probably CR3 and EIP?
    u32 ist1l;
    u32 ist1h;
    u32 ist2l;
    u32 ist2h;
    u32 ist3l;
    u32 ist3h;
    u32 ist4l;
    u32 ist4h;
    u32 ist5l;
    u32 ist5h;
    u32 ist6l;
    u32 ist6h;
    u32 ist7l;
    u32 ist7h;
    u64 __3; // GS and LDTR?
    u16 __4;
    u16 iomapbase;
};

using TSS = TSS64;

}
