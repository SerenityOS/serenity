/*
 * Copyright (c) 2021, Leon Albrecht <leon2002.la@gmail.com>
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
        return rip;
    }

    void set_ip(FlatPtr ip)
    {
        rip = ip;
    }

    FlatPtr bp() const
    {
        return rbp;
    }

    void set_bp(FlatPtr bp)
    {
        rbp = bp;
    }
#    endif
};

#else
typedef struct __mcontext PthreadRegisters;
#endif
