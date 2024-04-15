/*
 * Copyright (c) 2024, Sönke Holz <sholz8530@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/BitCast.h>
#include <AK/StdLibExtraDetails.h>
#include <AK/Types.h>

namespace ELF {

struct ThreadControlBlock {
    // Variant I of the ELF TLS data structures requires that the TCB has to contain a pointer to the dtv at offset 0.
    // This member is unused, as we currently only support static TLS blocks.
    void* dynamic_thread_vector;

    FlatPtr padding;
};

// The TCB needs to have a size of 2 * sizeof(FlatPtr) on AArch64.
static_assert(AssertSize<ThreadControlBlock, 2 * sizeof(FlatPtr)>());

static constexpr size_t TLS_VARIANT = 1;
static constexpr size_t TLS_DTV_OFFSET = 0;
static constexpr size_t TLS_TP_STATIC_TLS_BLOCK_OFFSET = sizeof(ThreadControlBlock);

// AArch64 ELF TLS Layout
//
// [TCB][static TLS.....]
//  ^tp (tpidr_el0)

inline size_t calculate_static_tls_region_size(size_t tls_template_size, size_t tls_alignment)
{
    (void)tls_alignment;
    return sizeof(ThreadControlBlock) + tls_template_size;
}

inline FlatPtr calculate_tp_value_from_static_tls_region_address(FlatPtr static_tls_region_address, size_t tls_template_size, size_t tls_alignment)
{
    (void)tls_template_size;
    (void)tls_alignment;
    return static_tls_region_address;
}

inline ThreadControlBlock* get_tcb_pointer_from_thread_pointer(FlatPtr thread_pointer)
{
    return bit_cast<ThreadControlBlock*>(thread_pointer);
}

inline void* get_pointer_to_first_static_tls_block_from_thread_pointer(FlatPtr thread_pointer, size_t tls_template_size, size_t tls_alignment)
{
    (void)tls_template_size;
    (void)tls_alignment;
    return bit_cast<void*>(thread_pointer + sizeof(ThreadControlBlock));
}

inline void* get_pointer_to_static_tls_region_from_thread_pointer(FlatPtr thread_pointer, size_t tls_template_size, size_t tls_alignment)
{
    (void)tls_template_size;
    (void)tls_alignment;
    return bit_cast<void*>(thread_pointer);
}

}
