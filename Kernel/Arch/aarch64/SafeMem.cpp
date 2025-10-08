/*
 * Copyright (c) 2022, Timon Kruiper <timonkruiper@gmail.com>
 * Copyright (c) 2023, Daniel Bertalan <dani@danielbertalan.dev>
 * Copyright (c) 2025, Sönke Holz <soenke.holz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Arch/RegisterState.h>
#include <Kernel/Arch/SafeMem.h>
#include <Kernel/Library/StdLib.h>

#define CODE_SECTION(section_name) __attribute__((section(section_name)))

extern "C" u8 start_of_safemem_text[];

extern "C" u8 safe_memset_ins[];
extern "C" u8 safe_memset_faulted[];

extern "C" u8 safe_strnlen_ins[];
extern "C" u8 safe_strnlen_faulted[];

extern "C" u8 safe_memcpy_ins_1[];
extern "C" u8 safe_memcpy_ins_2[];
extern "C" u8 safe_memcpy_faulted[];

extern "C" u8 end_of_safemem_text[];

extern "C" u8 start_of_safemem_atomic_text[];

extern "C" u8 safe_atomic_compare_exchange_relaxed_ins_1[];
extern "C" u8 safe_atomic_compare_exchange_relaxed_ins_2[];
extern "C" u8 safe_atomic_compare_exchange_relaxed_faulted[];

extern "C" u8 safe_atomic_load_relaxed_ins[];
extern "C" u8 safe_atomic_load_relaxed_faulted[];

extern "C" u8 safe_atomic_fetch_add_relaxed_ins_1[];
extern "C" u8 safe_atomic_fetch_add_relaxed_ins_2[];
extern "C" u8 safe_atomic_fetch_add_relaxed_faulted[];

extern "C" u8 safe_atomic_exchange_relaxed_ins_1[];
extern "C" u8 safe_atomic_exchange_relaxed_ins_2[];
extern "C" u8 safe_atomic_exchange_relaxed_faulted[];

extern "C" u8 safe_atomic_store_relaxed_ins[];
extern "C" u8 safe_atomic_store_relaxed_faulted[];

extern "C" u8 end_of_safemem_atomic_text[];
namespace Kernel {

CODE_SECTION(".text.safemem")
NEVER_INLINE FLATTEN bool safe_memset(void* dest_ptr, int c, size_t n, void*& fault_at)
{
    register FlatPtr result asm("x0") = 0;                // handle_safe_access_fault sets x0 to 0 if a fault occurred.
    register void** fault_at_in_x3 asm("x3") = &fault_at; // ensure fault_at stays in x3 so handle_safe_access_fault can set it to the faulting address.
    asm volatile(R"(
    cbz %[n], 2f
    add x4, %[dest_ptr], %[n]          // x4: pointer to the (exclusive) end of the target memory area

1:
.global safe_memset_ins
safe_memset_ins:
    strb %w[c], [%[dest_ptr]], #1
    cmp %[dest_ptr], x4
    b.ne 1b

2:
    mov %[result], #1
.global safe_memset_faulted
safe_memset_faulted:
)"
        : [dest_ptr] "+&r"(dest_ptr), [result] "+&r"(result), "+&r"(fault_at_in_x3)
        : [n] "r"(n), [c] "r"(c)
        : "memory", "x4", "cc");
    return result != 0;
}

CODE_SECTION(".text.safemem")
NEVER_INLINE FLATTEN ssize_t safe_strnlen(char const* str, unsigned long max_n, void*& fault_at)
{
    register ssize_t result asm("x0") = 0;                // handle_safe_access_fault sets x0 to -1 if a fault occurred.
    register void** fault_at_in_x2 asm("x2") = &fault_at; // ensure fault_at stays in x2 so handle_safe_access_fault can set it to the faulting address.
    asm volatile(R"(
    cbz %[max_n], 2f
    mov %[result], #0

1:
.global safe_strnlen_ins
safe_strnlen_ins:
    ldrb w3, [%[str], %[result]]     // w3: current char
    cbz w3, 2f
    add %[result], %[result], #1
    cmp %[result], %[max_n]
    b.ne 1b

2:
.global safe_strnlen_faulted
safe_strnlen_faulted:
)"
        : [result] "+&r"(result), "+&r"(fault_at_in_x2)
        : [str] "r"(str), [max_n] "r"(max_n)
        : "memory", "w3", "cc");
    return result;
}

CODE_SECTION(".text.safemem")
NEVER_INLINE FLATTEN bool safe_memcpy(void* dest_ptr, void const* src_ptr, unsigned long n, void*& fault_at)
{
    register FlatPtr result asm("x0") = 0;                // handle_safe_access_fault sets x0 to 0 if a fault occurred.
    register void** fault_at_in_x3 asm("x3") = &fault_at; // ensure fault_at stays in x3 so handle_safe_access_fault can set it to the faulting address.
    asm volatile(R"(
    cbz %[n], 2f
    mov x4, #0                 // x4: current index

1:
.global safe_memcpy_ins_1
safe_memcpy_ins_1:
    ldrb w5, [%[src_ptr], x4]  // w5: byte to copy
.global safe_memcpy_ins_2
safe_memcpy_ins_2:
    strb w5, [%[dest_ptr], x4]
    add x4, x4, #1
    cmp x4, %[n]
    b.ne 1b

2:
    mov %[result], #1
.global safe_memcpy_faulted
safe_memcpy_faulted:
)"
        : [result] "+&r"(result), "+&r"(fault_at_in_x3)
        : [dest_ptr] "r"(dest_ptr), [src_ptr] "r"(src_ptr), [n] "r"(n)
        : "memory", "x4", "w5", "cc");
    return result != 0;
}

CODE_SECTION(".text.safemem.atomic")
NEVER_INLINE FLATTEN Optional<bool> safe_atomic_compare_exchange_relaxed(u32 volatile* var, u32& expected, u32 desired)
{
    FlatPtr result;
    register FlatPtr error asm("x15") = 0; // handle_safe_access_fault sets x15 to 1 when a page fault occurs in one of the safe_atomic_* functions.
    asm volatile(R"(
    mov %[result], #0
    ldr w3, [%[expected_ptr]]                      // w3: expected value

1:
.global safe_atomic_compare_exchange_relaxed_ins_1
safe_atomic_compare_exchange_relaxed_ins_1:
    ldxr w4, [%[var_ptr]]                          // Load the value at *var into w4.
    cmp w4, w3
    b.ne 2f                                        // Doesn't match the expected value, so fail.
.global safe_atomic_compare_exchange_relaxed_ins_2
safe_atomic_compare_exchange_relaxed_ins_2:
    stxr w5, %w[desired], [%[var_ptr]]             // Try to update the value at *var.
    cbnz w5, 1b                                    // Retry if stxr failed (that is when w5 != 0).
    mov %[result], #1
    b 3f

2:
    str w4, [%[expected_ptr]]                      // Write the read value to expected on failure.
3:
.global safe_atomic_compare_exchange_relaxed_faulted
safe_atomic_compare_exchange_relaxed_faulted:
)"
        : [result] "=&r"(result), "+&r"(error)
        : [var_ptr] "r"(var), [expected_ptr] "r"(&expected), [desired] "r"(desired)
        : "memory", "w3", "w4", "w5", "cc");
    if (error != 0)
        return {};
    return static_cast<bool>(result);
}

CODE_SECTION(".text.safemem.atomic")
NEVER_INLINE FLATTEN Optional<u32> safe_atomic_load_relaxed(u32 volatile* var)
{
    u32 result;
    register FlatPtr error asm("x15") = 0; // handle_safe_access_fault sets x15 to 1 when a page fault occurs in one of the safe_atomic_* functions.
    asm volatile(R"(
.global safe_atomic_load_relaxed_ins
safe_atomic_load_relaxed_ins:
    ldr %w[result], [%[var_ptr]]
.global safe_atomic_load_relaxed_faulted
safe_atomic_load_relaxed_faulted:
)"
        : [result] "=r"(result), "+r"(error)
        : [var_ptr] "r"(var)
        : "memory");
    if (error != 0)
        return {};
    return result;
}

CODE_SECTION(".text.safemem.atomic")
NEVER_INLINE FLATTEN Optional<u32> safe_atomic_fetch_add_relaxed(u32 volatile* var, u32 val)
{
    u32 result;
    register FlatPtr error asm("x15") = 0; // handle_safe_access_fault sets x15 to 1 when a page fault occurs in one of the safe_atomic_* functions.
    asm volatile(R"(
1:
.global safe_atomic_fetch_add_relaxed_ins_1
safe_atomic_fetch_add_relaxed_ins_1:
    ldxr %w[result], [%[var_ptr]]
    add w2, %w[result], %w[val]
.global safe_atomic_fetch_add_relaxed_ins_2
safe_atomic_fetch_add_relaxed_ins_2:
    stxr w3, w2, [%[var_ptr]]
    cbnz w3, 1b
.global safe_atomic_fetch_add_relaxed_faulted
safe_atomic_fetch_add_relaxed_faulted:
)"
        : [result] "=&r"(result), "+&r"(error)
        : [val] "r"(val), [var_ptr] "r"(var)
        : "memory", "w2", "w3");
    if (error != 0)
        return {};
    return result;
}

CODE_SECTION(".text.safemem.atomic")
NEVER_INLINE FLATTEN Optional<u32> safe_atomic_exchange_relaxed(u32 volatile* var, u32 desired)
{
    u32 result;
    register FlatPtr error asm("x15") = 0; // handle_safe_access_fault sets x15 to 1 when a page fault occurs in one of the safe_atomic_* functions.
    asm volatile(R"(
1:
.global safe_atomic_exchange_relaxed_ins_1
safe_atomic_exchange_relaxed_ins_1:
    ldxr %w[result], [%[var_ptr]]
.global safe_atomic_exchange_relaxed_ins_2
safe_atomic_exchange_relaxed_ins_2:
    stxr w2, %w[desired], [%[var_ptr]]
    cbnz w2, 1b
.global safe_atomic_exchange_relaxed_faulted
safe_atomic_exchange_relaxed_faulted:
)"
        : [result] "=&r"(result), "+&r"(error)
        : [desired] "r"(desired), [var_ptr] "r"(var)
        : "memory", "w2");
    if (error != 0)
        return {};
    return result;
}

CODE_SECTION(".text.safemem.atomic")
NEVER_INLINE FLATTEN bool safe_atomic_store_relaxed(u32 volatile* var, u32 desired)
{
    register FlatPtr error asm("x15") = 0; // handle_safe_access_fault sets x15 to 1 when a page fault occurs in one of the safe_atomic_* functions.
    asm volatile(R"(
.global safe_atomic_store_relaxed_ins
safe_atomic_store_relaxed_ins:
    str %w[desired], [%[var_ptr]]
.global safe_atomic_store_relaxed_faulted
safe_atomic_store_relaxed_faulted:
)"
        : "+r"(error)
        : [desired] "r"(desired), [var_ptr] "r"(var)
        : "memory");
    return error == 0;
}

bool handle_safe_access_fault(RegisterState& regs, FlatPtr fault_address)
{
    FlatPtr pc = regs.ip();

    if (pc >= bit_cast<FlatPtr>(&start_of_safemem_text) && pc < bit_cast<FlatPtr>(&end_of_safemem_text)) {
        // If we detect that the fault happened in safe_memcpy(), safe_strnlen(),
        // or safe_memset(), then resume at the appropriate _faulted label
        // and set fault_at to the faulting address.
        if (pc == bit_cast<FlatPtr>(&safe_memset_ins)) {
            regs.set_ip(bit_cast<FlatPtr>(&safe_memset_faulted));
            regs.x[0] = 0;
            *bit_cast<FlatPtr*>(regs.x[3]) = fault_address; // x3: void*& fault_at
            return true;
        }
        if (pc == bit_cast<FlatPtr>(&safe_strnlen_ins)) {
            regs.set_ip(bit_cast<FlatPtr>(&safe_strnlen_faulted));
            regs.x[0] = -1;
            *bit_cast<FlatPtr*>(regs.x[2]) = fault_address; // x2: void*& fault_at
            return true;
        }
        if (pc == bit_cast<FlatPtr>(&safe_memcpy_ins_1) || pc == bit_cast<FlatPtr>(&safe_memcpy_ins_2)) {
            regs.set_ip(bit_cast<FlatPtr>(&safe_memcpy_faulted));
            regs.x[0] = 0;
            *bit_cast<FlatPtr*>(regs.x[3]) = fault_address; // x3: void*& fault_at
            return true;
        }
    } else if (pc >= bit_cast<FlatPtr>(&start_of_safemem_atomic_text) && pc < bit_cast<FlatPtr>(&end_of_safemem_atomic_text)) {
        // If we detect that a fault happened in one of the atomic safe_
        // functions, resume at the appropriate _faulted label and set
        // the x15 register to 1 to indicate an error.
        if (pc == bit_cast<FlatPtr>(&safe_atomic_compare_exchange_relaxed_ins_1) || pc == bit_cast<FlatPtr>(&safe_atomic_compare_exchange_relaxed_ins_2)) {
            pc = bit_cast<FlatPtr>(&safe_atomic_compare_exchange_relaxed_faulted);
        } else if (pc == bit_cast<FlatPtr>(&safe_atomic_load_relaxed_ins)) {
            pc = bit_cast<FlatPtr>(&safe_atomic_load_relaxed_faulted);
        } else if (pc == bit_cast<FlatPtr>(&safe_atomic_fetch_add_relaxed_ins_1) || pc == bit_cast<FlatPtr>(&safe_atomic_fetch_add_relaxed_ins_2)) {
            pc = bit_cast<FlatPtr>(&safe_atomic_fetch_add_relaxed_faulted);
        } else if (pc == bit_cast<FlatPtr>(&safe_atomic_exchange_relaxed_ins_1) || pc == bit_cast<FlatPtr>(&safe_atomic_exchange_relaxed_ins_2)) {
            pc = bit_cast<FlatPtr>(&safe_atomic_exchange_relaxed_faulted);
        } else if (pc == bit_cast<FlatPtr>(&safe_atomic_store_relaxed_ins)) {
            pc = bit_cast<FlatPtr>(&safe_atomic_store_relaxed_faulted);
        } else {
            return false;
        }

        regs.set_ip(pc);
        regs.x[15] = 1;
        return true;
    }

    return false;
}

}
