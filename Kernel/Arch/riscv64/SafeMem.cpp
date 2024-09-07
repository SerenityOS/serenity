/*
 * Copyright (c) 2022, Timon Kruiper <timonkruiper@gmail.com>
 * Copyright (c) 2023, Daniel Bertalan <dani@danielbertalan.dev>
 * Copyright (c) 2024, Sönke Holz <sholz8530@gmail.com>
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

extern "C" u8 safe_atomic_fetch_add_relaxed_ins[];
extern "C" u8 safe_atomic_fetch_add_relaxed_faulted[];

extern "C" u8 safe_atomic_exchange_relaxed_ins[];
extern "C" u8 safe_atomic_exchange_relaxed_faulted[];

extern "C" u8 safe_atomic_store_relaxed_ins[];
extern "C" u8 safe_atomic_store_relaxed_faulted[];

extern "C" u8 end_of_safemem_atomic_text[];

namespace Kernel {

CODE_SECTION(".text.safemem")
NEVER_INLINE NAKED bool safe_memset(void*, int, size_t, void*&)
{
    // a0: void* dest_ptr
    // a1: int c
    // a2: size_t n
    // a3: void*& fault_at

    asm(R"(
    beqz a2, 2f
    add t0, a0, a2      # t0: pointer to the (exclusive) end of the target memory area

1:
.global safe_memset_ins
safe_memset_ins:
    sb a1, (a0)
    addi a0, a0, 1
    bne a0, t0, 1b

2:
    li a0, 1
.global safe_memset_faulted
safe_memset_faulted:
    ret
)");
}

CODE_SECTION(".text.safemem")
NEVER_INLINE NAKED ssize_t safe_strnlen(char const*, unsigned long, void*&)
{
    // a0: char const* str
    // a1: unsigned long max_n
    // a2: void*& fault_at

    asm(R"(
    mv t0, a0            # t0: original string pointer
    li a0, 0             # a0: result
    beqz a1, 2f

1:
    add t1, t0, a0       # t1: pointer to current char
.global safe_strnlen_ins
safe_strnlen_ins:
    lbu t2, (t1)         # t2: current char
    beqz t2, 2f
    addi a0, a0, 1
    bne a0, a1, 1b

2:
.global safe_strnlen_faulted
safe_strnlen_faulted:
    ret
)");
}

CODE_SECTION(".text.safemem")
NEVER_INLINE NAKED bool safe_memcpy(void*, void const*, unsigned long, void*&)
{
    // a0: void* dest_ptr
    // a1: void const* src_ptr
    // a2: unsigned long n
    // a3: void*& fault_at

    asm(R"(
    beqz a2, 2f
    add t0, a0, a2         # t0: pointer to the (exclusive) end of the target memory area

1:
.global safe_memcpy_ins_1
safe_memcpy_ins_1:
    lbu t1, (a1)           # t1: byte to copy
.global safe_memcpy_ins_2
safe_memcpy_ins_2:
    sb t1, (a0)
    addi a0, a0, 1
    addi a1, a1, 1
    bne a0, t0, 1b

2:
    li a0, 1
.global safe_memcpy_faulted
safe_memcpy_faulted:
    ret
)");
}

CODE_SECTION(".text.safemem.atomic")
NEVER_INLINE FLATTEN Optional<bool> safe_atomic_compare_exchange_relaxed(u32 volatile* var, u32& expected, u32 desired)
{
    // Based on Example 2, https://github.com/riscv/riscv-isa-manual/releases/download/20240411/unpriv-isa-asciidoc.pdf
    FlatPtr result;
    register FlatPtr error asm("t6") = 0; // handle_safe_access_fault sets t6 to 1 when a page fault occurs in one of the safe_atomic_* functions.
    asm volatile(R"(
    li %[result], 0
    lw t0, (%[expected_ptr])                       # t0: expected value

1:
.global safe_atomic_compare_exchange_relaxed_ins_1
safe_atomic_compare_exchange_relaxed_ins_1:
    lr.w t1, (%[var_ptr])                          # Load the value at *var into t1.
    bne t1, t0, 2f                                 # Doesn't match the expected value, so fail.
.global safe_atomic_compare_exchange_relaxed_ins_2
safe_atomic_compare_exchange_relaxed_ins_2:
    sc.w t2, %[desired], (%[var_ptr])              # Try to update the value at *var.
    bnez t2, 1b                                    # Retry if sc.w failed (that is when t2 != 0).
    li %[result], 1
    j 3f

2:
    sw t1, (%[expected_ptr])                       # Write the read value to expected on failure.
3:
.global safe_atomic_compare_exchange_relaxed_faulted
safe_atomic_compare_exchange_relaxed_faulted:
)"
                 : [result] "=&r"(result), "+r"(error)
                 : [var_ptr] "r"(var), [expected_ptr] "r"(&expected), [desired] "r"(desired)
                 : "memory", "t0", "t1", "t2");
    if (error != 0)
        return {};
    return static_cast<bool>(result);
}

CODE_SECTION(".text.safemem.atomic")
NEVER_INLINE FLATTEN Optional<u32> safe_atomic_load_relaxed(u32 volatile* var)
{
    u32 result;
    register FlatPtr error asm("t6") = 0; // handle_safe_access_fault sets t6 to 1 when a page fault occurs in one of the safe_atomic_* functions.
    asm volatile(R"(
.global safe_atomic_load_relaxed_ins
safe_atomic_load_relaxed_ins:
    lw %[result], (%[var_ptr])
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
    register FlatPtr error asm("t6") = 0; // handle_safe_access_fault sets t6 to 1 when a page fault occurs in one of the safe_atomic_* functions.
    asm volatile(R"(
.global safe_atomic_fetch_add_relaxed_ins
safe_atomic_fetch_add_relaxed_ins:
    amoadd.w %[result], %[val], (%[var_ptr])
.global safe_atomic_fetch_add_relaxed_faulted
safe_atomic_fetch_add_relaxed_faulted:
)"
                 : [result] "=r"(result), "+r"(error)
                 : [val] "r"(val), [var_ptr] "r"(var)
                 : "memory");
    if (error != 0)
        return {};
    return result;
}

CODE_SECTION(".text.safemem.atomic")
NEVER_INLINE FLATTEN Optional<u32> safe_atomic_exchange_relaxed(u32 volatile* var, u32 desired)
{
    u32 result;
    register FlatPtr error asm("t6") = 0; // handle_safe_access_fault sets t6 to 1 when a page fault occurs in one of the safe_atomic_* functions.
    asm volatile(R"(
.global safe_atomic_exchange_relaxed_ins
safe_atomic_exchange_relaxed_ins:
    amoswap.w %[result], %[desired], (%[var_ptr])
.global safe_atomic_exchange_relaxed_faulted
safe_atomic_exchange_relaxed_faulted:
)"
                 : [result] "=r"(result), "+r"(error)
                 : [desired] "r"(desired), [var_ptr] "r"(var)
                 : "memory");
    if (error != 0)
        return {};
    return result;
}

CODE_SECTION(".text.safemem.atomic")
NEVER_INLINE FLATTEN bool safe_atomic_store_relaxed(u32 volatile* var, u32 desired)
{
    register FlatPtr error asm("t6") = 0; // handle_safe_access_fault sets t6 to 1 when a page fault occurs in one of the safe_atomic_* functions.
    asm volatile(R"(
.global safe_atomic_store_relaxed_ins
safe_atomic_store_relaxed_ins:
    sw %[desired], (%[var_ptr])
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
        // If we detect that the fault happened in safe_memcpy() safe_strnlen(),
        // or safe_memset() then resume at the appropriate _faulted label
        // and set fault_at to the faulting address
        if (pc == bit_cast<FlatPtr>(&safe_memset_ins)) {
            regs.set_ip(bit_cast<FlatPtr>(&safe_memset_faulted));
            regs.x[9] = 0;
            *bit_cast<FlatPtr*>(regs.x[12]) = fault_address; // a3: void*& fault_at
            return true;
        }
        if (pc == bit_cast<FlatPtr>(&safe_strnlen_ins)) {
            regs.set_ip(bit_cast<FlatPtr>(&safe_strnlen_faulted));
            regs.x[9] = -1;
            *bit_cast<FlatPtr*>(regs.x[11]) = fault_address; // a2: void*& fault_at
            return true;
        }
        if (pc == bit_cast<FlatPtr>(&safe_memcpy_ins_1) || pc == bit_cast<FlatPtr>(&safe_memcpy_ins_2)) {
            regs.set_ip(bit_cast<FlatPtr>(&safe_memcpy_faulted));
            regs.x[9] = 0;
            *bit_cast<FlatPtr*>(regs.x[12]) = fault_address; // a3: void*& fault_at
            return true;
        }
    } else if (pc >= bit_cast<FlatPtr>(&start_of_safemem_atomic_text) && pc < bit_cast<FlatPtr>(&end_of_safemem_atomic_text)) {
        // If we detect that a fault happened in one of the atomic safe_
        // functions, resume at the appropriate _faulted label and set
        // the t6 register to 1 to indicate an error
        if (pc == bit_cast<FlatPtr>(&safe_atomic_compare_exchange_relaxed_ins_1) || pc == bit_cast<FlatPtr>(&safe_atomic_compare_exchange_relaxed_ins_2)) {
            pc = bit_cast<FlatPtr>(&safe_atomic_compare_exchange_relaxed_faulted);
        } else if (pc == bit_cast<FlatPtr>(&safe_atomic_load_relaxed_ins)) {
            pc = bit_cast<FlatPtr>(&safe_atomic_load_relaxed_faulted);
        } else if (pc == bit_cast<FlatPtr>(&safe_atomic_fetch_add_relaxed_ins)) {
            pc = bit_cast<FlatPtr>(&safe_atomic_fetch_add_relaxed_faulted);
        } else if (pc == bit_cast<FlatPtr>(&safe_atomic_exchange_relaxed_ins)) {
            pc = bit_cast<FlatPtr>(&safe_atomic_exchange_relaxed_faulted);
        } else if (pc == bit_cast<FlatPtr>(&safe_atomic_store_relaxed_ins)) {
            pc = bit_cast<FlatPtr>(&safe_atomic_store_relaxed_faulted);
        } else {
            return false;
        }

        regs.set_ip(pc);
        regs.x[30] = 1;
        return true;
    }

    return false;
}

}
