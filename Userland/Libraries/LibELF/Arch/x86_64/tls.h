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
    // The %fs segment register is the thread pointer register on x86-64.
    // x86-64 uses variant II of the TLS data structures described in "ELF Handling For Thread-Local Storage",
    // which requires the thread pointer to point to the TCB.
    // That document also requires that the pointer shall be accessible with "movq %fs:0, %<reg>",
    // so the first member in the TCB has to be a copy of the thread pointer.
    void* thread_pointer;

    // Variant II requires that the TCB has to contain a pointer to the dtv at an unspecified offset.
    // This member is unused, as we currently only support static TLS blocks.
    void* dynamic_thread_vector;
};

static constexpr size_t TLS_VARIANT = 2;
static constexpr size_t TLS_DTV_OFFSET = 0;
static constexpr size_t TLS_TP_STATIC_TLS_BLOCK_OFFSET = 0;

// x86-64 ELF TLS Layout
// The padding is needed so tp (fs_base) is correctly aligned.
//
// [.....static TLS][..padding..][TCB]
//                                ^tp (fs_base)

inline size_t calculate_static_tls_region_size(size_t tls_template_size, size_t tls_alignment)
{
    return align_up_to(tls_template_size, tls_alignment) + sizeof(ThreadControlBlock);
}

inline FlatPtr calculate_tp_value_from_static_tls_region_address(FlatPtr static_tls_region_address, size_t tls_template_size, size_t tls_alignment)
{
    return static_tls_region_address + align_up_to(tls_template_size, tls_alignment);
}

inline ThreadControlBlock* get_tcb_pointer_from_thread_pointer(FlatPtr thread_pointer)
{
    return bit_cast<ThreadControlBlock*>(thread_pointer);
}

inline void* get_pointer_to_first_static_tls_block_from_thread_pointer(FlatPtr thread_pointer, size_t tls_template_size, size_t tls_alignment)
{
    return bit_cast<void*>(thread_pointer - align_up_to(tls_template_size, tls_alignment));
}

inline void* get_pointer_to_static_tls_region_from_thread_pointer(FlatPtr thread_pointer, size_t tls_template_size, size_t tls_alignment)
{
    return bit_cast<void*>(thread_pointer - align_up_to(tls_template_size, tls_alignment));
}

}
