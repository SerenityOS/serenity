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
#        else
        return rip;
#        endif
    }

    void set_ip(FlatPtr ip)
    {
#        if ARCH(I386)
        eip = ip;
#        else
        rip = ip;
#        endif
    }

    FlatPtr bp() const
    {
#        if ARCH(I386)
        return ebp;
#        else
        return rbp;
#        endif
    }

    void set_bp(FlatPtr bp)
    {
#        if ARCH(I386)
        ebp = bp;
#        else
        rbp = bp;
#        endif
    }
#    endif
};

#else
typedef struct __mcontext PthreadRegisters;
#endif
