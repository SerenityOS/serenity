#pragma once

#include <AK/Types.h>

struct [[gnu::packed]] TSS32 {
    word backlink, __blh;
    dword esp0;
    word ss0, __ss0h;
    dword esp1;
    word ss1, __ss1h;
    dword esp2;
    word ss2, __ss2h;
    dword cr3, eip, eflags;
    dword eax,ecx,edx,ebx,esp,ebp,esi,edi;
    word es, __esh;
    word cs, __csh;
    word ss, __ssh;
    word ds, __dsh;
    word fs, __fsh;
    word gs, __gsh;
    word ldt, __ldth;
    word trace, iomapbase;
};
