/*
 * Copyright (c) 2020, Itamar S. <itamar8910@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Platform.h>

#if defined(__cplusplus) && defined(__cpp_concepts)
#    include <AK/Types.h>
#else
#    include <sys/types.h>
#endif

#include <Kernel/Arch/mcontext.h>

#ifdef __cplusplus
struct [[gnu::packed]] PtraceRegisters : public __mcontext {
#    if defined(__cplusplus) && defined(__cpp_concepts)
    FlatPtr ip() const
    {
#        if ARCH(X86_64)
        return rip;
#        elif ARCH(AARCH64)
        return pc;
#        else
#            error Unknown architecture
#        endif
    }

    void set_ip(FlatPtr ip)
    {
#        if ARCH(X86_64)
        rip = ip;
#        elif ARCH(AARCH64)
        pc = ip;
#        else
#            error Unknown architecture
#        endif
    }

    FlatPtr bp() const
    {
#        if ARCH(X86_64)
        return rbp;
#        elif ARCH(AARCH64)
        return r29;
#        else
#            error Unknown architecture
#        endif
    }

    void set_bp(FlatPtr bp)
    {
#        if ARCH(X86_64)
        rbp = bp;
#        elif ARCH(AARCH64)
        r29 = bp;
#        else
#            error Unknown architecture
#        endif
    }
#    endif
};

#else
typedef struct __mcontext PthreadRegisters;
#endif
