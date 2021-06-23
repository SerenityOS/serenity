/*
 * Copyright (c) 2021, Gunnar Beutner <gbeutner@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Arch/x86/SafeMem.h>

#define CODE_SECTION(section_name) __attribute__((section(section_name)))

namespace Kernel {

CODE_SECTION(".text.safemem")
NEVER_INLINE bool safe_memcpy(void* dest_ptr, const void* src_ptr, size_t n, void*& fault_at)
{
    (void)dest_ptr;
    (void)src_ptr;
    (void)n;
    (void)fault_at;
    TODO();
}

CODE_SECTION(".text.safemem")
NEVER_INLINE ssize_t safe_strnlen(const char* str, size_t max_n, void*& fault_at)
{
    (void)str;
    (void)max_n;
    (void)fault_at;
    TODO();
}

CODE_SECTION(".text.safemem")
NEVER_INLINE bool safe_memset(void* dest_ptr, int c, size_t n, void*& fault_at)
{
    (void)dest_ptr;
    (void)c;
    (void)n;
    (void)fault_at;
    TODO();
}

CODE_SECTION(".text.safemem.atomic")
NEVER_INLINE Optional<u32> safe_atomic_fetch_add_relaxed(volatile u32* var, u32 val)
{
    (void)var;
    (void)val;
    TODO();
}

CODE_SECTION(".text.safemem.atomic")
NEVER_INLINE Optional<u32> safe_atomic_exchange_relaxed(volatile u32* var, u32 val)
{
    (void)var;
    (void)val;
    TODO();
}

CODE_SECTION(".text.safemem.atomic")
NEVER_INLINE Optional<u32> safe_atomic_load_relaxed(volatile u32* var)
{
    (void)var;
    TODO();
}

CODE_SECTION(".text.safemem.atomic")
NEVER_INLINE bool safe_atomic_store_relaxed(volatile u32* var, u32 val)
{
    (void)var;
    (void)val;
    TODO();
}

CODE_SECTION(".text.safemem.atomic")
NEVER_INLINE Optional<bool> safe_atomic_compare_exchange_relaxed(volatile u32* var, u32& expected, u32 val)
{
    (void)var;
    (void)expected;
    (void)val;
    TODO();
}

bool handle_safe_access_fault(RegisterState& regs, u32 fault_address)
{
    (void)regs;
    (void)fault_address;
    TODO();
}

}
