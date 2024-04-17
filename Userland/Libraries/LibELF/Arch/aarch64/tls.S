/*
 * Copyright (c) 2023, Daniel Bertalan <dani@danielbertalan.dev>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

// This file implements the runtime components of the AArch64 TLSDESC ABI,
// which is used when accessing thread-local variables which might not be
// stored in the static TLS block (global-dynamic and local-dynamic access
// models). Compilers default to this when creating shared libraries, as they
// may be loaded after program startup by `dlopen()`.
//
// Each referenced thread-local symbol is associated with a descriptor:
//
// struct TlsDescriptor {
//     size_t (*resolver)(TlsDescriptor*);
//     union {
//         size_t tpoff; // for static TLS
//         struct {
//             size_t module_id;
//             size_t module_offset;
//         } *dynamic; // for dynamic TLS, not yet implemented
//     };
// };
//
// The resolver takes a pointer to the descriptor as an argument and returns
// the symbol's offset to the thread pointer (tpidr_el0). The second field of
// the descriptor is an implementation-defined value which the resolver uses to
// identify the symbol.
//
// Thus, the address of a thread-local variable is retrieved as follows:
//
//   &var = thread_pointer + descriptor.resolver(&descriptor);
//
// The two essential types of resolver functions are:
//
// - `__tlsdesc_static`: If the variable is located in the static TLS block,
//   its thread pointer offset is a load-time constant, which can be stored in
//   the descriptor. This function simply returns that.
//
// - `tlsdesc_dynamic`: Looks up a variable by its module ID and module offset.
//   This is used if the TLS block is allocated separately, so might have a
//   different thread pointer offset for each thread. This works similarly to
//   the traditional TLS ABI's __tls_get_addr function. Not yet implemented in
//   SerenityOS.
//
// The TLSDESC format strives to make the code sequence for thread-local
// variable access as short as possible, hence the resolver functions follow a
// special calling convention: they must not clobber any registers. To ensure
// that even the usually volatile registers are saved off, we need to implement
// the resolvers in assembly.

// size_t __tlsdesc_static(TlsDescriptor* desc)
// {
//     return desc->tpoff;
// }
.p2align 4
.globl __tlsdesc_static
.hidden __tlsdesc_static
.type __tlsdesc_static,@function
__tlsdesc_static:
    ldr x0, [x0, #8]
    // The first static TLS block is 16 bytes after the thread pointer on AArch64.
    add x0, x0, 16
    ret
