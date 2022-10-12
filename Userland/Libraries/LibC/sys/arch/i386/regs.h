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
#        if ARCH(I386)
        return eip;
#        elif ARCH(X86_64)
        return rip;
#        else
        TODO_AARCH64();
#        endif
    }

    void set_ip(FlatPtr ip)
    {
#        if ARCH(I386)
        eip = ip;
#        elif ARCH(X86_64)
        rip = ip;
#        else
        (void)ip;
        TODO_AARCH64();
#        endif
    }

    FlatPtr bp() const
    {
#        if ARCH(I386)
        return ebp;
#        elif ARCH(X86_64)
        return rbp;
#        else
        TODO_AARCH64();
#        endif
    }

    void set_bp(FlatPtr bp)
    {
#        if ARCH(I386)
        ebp = bp;
#        elif ARCH(X86_64)
        rbp = bp;
#        else
        (void)bp;
        TODO_AARCH64();
#        endif
    }
#    endif
};

#else
typedef struct __mcontext PthreadRegisters;
#endif
