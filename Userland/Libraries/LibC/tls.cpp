/*
 * Copyright (c) 2023, Daniel Bertalan <dani@danielbertalan.dev>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Types.h>
#include <LibELF/Arch/tls.h>
#include <sys/internals.h>

extern "C" {

// This function is called to compute the address of a thread-local variable
// which might not be stored in the static TLS block (local-dynamic and
// global-dynamic models). Compilers default to this when creating shared
// libraries, as they may be loaded after program startup by `dlopen()`.
//
// We currently only support a static TLS block, so we take a shortcut in the
// implementation of this interface: instead of storing the module ID in
// ti_module, we store the module's TLS block offset. This avoids the need to
// have a per-thread module ID -> TLS block address. This will have to be
// changed if we support dynamically allocated TLS blocks.
void* __tls_get_addr(__tls_index* index)
{
    return reinterpret_cast<void*>(reinterpret_cast<FlatPtr>(__builtin_thread_pointer()) + index->ti_module + index->ti_offset + ELF::TLS_DTV_OFFSET);
}
}
