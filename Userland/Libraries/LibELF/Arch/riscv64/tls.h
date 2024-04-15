/*
 * Copyright (c) 2024, Sönke Holz <sholz8530@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/BitCast.h>
#include <AK/Types.h>

namespace ELF {

struct ThreadControlBlock {
    // NOTE: "ELF Handling For Thread-Local Storage" says that when using variant I of the data structures (which RISC-V does),
    //       the TCB has to have a pointer to the dtv at offset 0.
    //       However, that document also says that the thread pointer has to point to the TCB.
    //       It's probably still a good idea to put the dtv pointer at offset 0.
    // This member is unused, as we currently only support static TLS blocks.
    void* dynamic_thread_vector;
};

static constexpr size_t TLS_VARIANT = 1;
static constexpr size_t TLS_DTV_OFFSET = 0x800;
static constexpr size_t TLS_TP_STATIC_TLS_BLOCK_OFFSET = 0;

// RISC-V ELF TLS Layout
// The padding is needed so tp is correctly aligned.
//
// [..padding..][TCB][static TLS.....]
//                    ^tp

inline size_t calculate_static_tls_region_size(size_t tls_template_size, size_t tls_alignment)
{
    return align_up_to(sizeof(ThreadControlBlock), tls_alignment) + tls_template_size;
}

inline FlatPtr calculate_tp_value_from_static_tls_region_address(FlatPtr static_tls_region_address, size_t tls_template_size, size_t tls_alignment)
{
    (void)tls_template_size;
    return static_tls_region_address + align_up_to(sizeof(ThreadControlBlock), tls_alignment);
}

inline ThreadControlBlock* get_tcb_pointer_from_thread_pointer(FlatPtr thread_pointer)
{
    return bit_cast<ThreadControlBlock*>(thread_pointer - sizeof(ThreadControlBlock));
}

inline void* get_pointer_to_first_static_tls_block_from_thread_pointer(FlatPtr thread_pointer, size_t tls_template_size, size_t tls_alignment)
{
    (void)tls_template_size;
    (void)tls_alignment;
    return bit_cast<void*>(thread_pointer);
}

inline void* get_pointer_to_static_tls_region_from_thread_pointer(FlatPtr thread_pointer, size_t tls_template_size, size_t tls_alignment)
{
    (void)tls_template_size;
    return bit_cast<void*>(thread_pointer - align_up_to(sizeof(ThreadControlBlock), tls_alignment));
}

}
