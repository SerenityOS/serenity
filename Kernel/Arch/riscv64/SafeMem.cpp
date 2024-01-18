/*
 * Copyright (c) 2022, Timon Kruiper <timonkruiper@gmail.com>
 * Copyright (c) 2023, Daniel Bertalan <dani@danielbertalan.dev>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Arch/RegisterState.h>
#include <Kernel/Arch/SafeMem.h>
#include <Kernel/Library/StdLib.h>

#define CODE_SECTION(section_name) __attribute__((section(section_name)))

extern "C" u8 start_of_safemem_text[];
extern "C" u8 end_of_safemem_text[];

extern "C" u8 safe_memcpy_start[];
extern "C" u8 safe_memcpy_end[];
extern "C" u8 safe_memset_start[];
extern "C" u8 safe_memset_end[];
extern "C" u8 safe_strnlen_start[];
extern "C" u8 safe_strnlen_end[];

extern "C" u8 start_of_safemem_atomic_text[];
extern "C" u8 end_of_safemem_atomic_text[];

extern "C" u8 safe_atomic_fetch_add_relaxed_ins[];
extern "C" u8 safe_atomic_fetch_add_relaxed_faulted[];
extern "C" u8 safe_atomic_exchange_relaxed_ins[];
extern "C" u8 safe_atomic_exchange_relaxed_faulted[];
extern "C" u8 safe_atomic_load_relaxed_ins[];
extern "C" u8 safe_atomic_load_relaxed_faulted[];
extern "C" u8 safe_atomic_store_relaxed_ins[];
extern "C" u8 safe_atomic_store_relaxed_faulted[];
extern "C" u8 safe_atomic_compare_exchange_relaxed_ins[];
extern "C" u8 safe_atomic_compare_exchange_relaxed_faulted[];

namespace Kernel {

// The RISC-V Instruction Set Manual, Volume II: Privileged Architecture, Chapter 4.4.1
// https://github.com/riscv/riscv-isa-manual/releases/download/Priv-v1.12/riscv-privileged-20211203.pdf
ALWAYS_INLINE bool validate_virtual_address(size_t address)
{
    // FIXME: Don't hardcode this when we support other virtual memory systems other than Sv39
    auto most_significant_bits = 39 - 1;
    auto insignificant_bits = address >> most_significant_bits;
    return insignificant_bits == 0 || insignificant_bits == (0xffffffffffffffffull >> most_significant_bits);
}

CODE_SECTION(".text.safemem")
NEVER_INLINE FLATTEN bool safe_memcpy(void* dest_ptr, void const* src_ptr, unsigned long n, void*& fault_at)
{
    fault_at = nullptr;

    FlatPtr dest = bit_cast<FlatPtr>(dest_ptr);
    if (!validate_virtual_address(dest)) {
        fault_at = dest_ptr;
        return false;
    }

    FlatPtr src = bit_cast<FlatPtr>(src_ptr);
    if (!validate_virtual_address(src)) {
        fault_at = const_cast<void*>(src_ptr);
        return false;
    }

    register void* asm_fault_at asm("t6") = nullptr;
    asm volatile(
        ".global safe_memcpy_start \n"
        "safe_memcpy_start: \n"

        "   beqz %[n], safe_memcpy_end \n"

        "1: \n"
        "   lbu t0, (%[src]) \n"
        "   sb t0, (%[dest]) \n"
        "   addi %[src], %[src], 1 \n"
        "   addi %[dest], %[dest], 1 \n"
        "   addi %[n], %[n], -1 \n"
        "   bnez %[n], 1b \n"

        ".global safe_memcpy_end \n"
        "safe_memcpy_end: \n" // handle_safe_access_fault() sets t6 (fault_at) to the fault address and jumps here!

        : [src] "=r"(src),
        [dest] "=r"(dest),
        [n] "=r"(n),
        "=r"(asm_fault_at)
        : "[src]"(src),
        "[dest]"(dest),
        "[n]"(n)
        : "t0", "memory");

    if (n != 0) {
        fault_at = asm_fault_at;
        return false;
    }

    return true;
}

CODE_SECTION(".text.safemem")
NEVER_INLINE FLATTEN bool safe_memset(void* dest_ptr, int c, size_t n, void*& fault_at)
{
    fault_at = nullptr;

    FlatPtr dest = bit_cast<FlatPtr>(dest_ptr);
    if (!validate_virtual_address(dest)) {
        fault_at = dest_ptr;
        return false;
    }

    register void* asm_fault_at asm("t6") = nullptr;
    asm volatile(
        ".global safe_memset_start \n"
        "safe_memset_start: \n"

        "   beqz %[n], safe_memcpy_end \n"

        "1: \n"
        "   sb %[c], (%[dest]) \n"
        "   addi %[dest], %[dest], 1 \n"
        "   addi %[n], %[n], -1 \n"
        "   bnez %[n], 1b \n"

        ".global safe_memset_end \n"
        "safe_memset_end: \n" // handle_safe_access_fault() sets t6 (fault_at) to the fault address and jumps here!

        : [dest] "=r"(dest),
        [n] "=r"(n),
        "=r"(asm_fault_at)
        : "[dest]"(dest),
        "[n]"(n),
        [c] "r"(c)
        : "memory");

    if (n != 0) {
        fault_at = asm_fault_at;
        return false;
    }

    return true;
}

CODE_SECTION(".text.safemem")
NEVER_INLINE FLATTEN ssize_t safe_strnlen(char const* str, unsigned long max_n, void*& fault_at)
{
    // FIXME: Actually implement a safe strnlen.
    size_t len = 0;
    for (; len < max_n && *str; str++)
        len++;
    fault_at = nullptr;
    return len;

    // fault_at = nullptr;

    // if (!validate_virtual_address(bit_cast<FlatPtr>(str))) {
    //     fault_at = const_cast<char*>(str);
    //     return false;
    // }

    // ssize_t count = 0;
    // register void* asm_fault_at asm("t6");
    // asm volatile(
    //     ".global safe_strnlen_start \n"
    //     "safe_strnlen_start: \n"

    //     "   beqz %[max_n], 2f \n"

    //     "1: \n"
    //     "   add t0, %[str], %[count] \n"
    //     "   lbu t0, (t0) \n"
    //     "   beqz t0, 2f \n"
    //     "   addi %[count], %[count], 1 \n"
    //     "   bne %[count], %[max_n], 1b \n"
    //     "   j 2f\n"

    //     ".global safe_strnlen_end \n"
    //     "safe_strnlen_end: \n" // handle_safe_access_fault() sets t6 (fault_at) to the fault address and jumps here!

    //     "   li %[count], -1\n" // return -1 on fault
    //     "2: \n"

    //     : [count] "=r"(count),
    //     "=r"(fault_at)
    //     : [str] "r"(str),
    //     [max_n] "r"(max_n)
    //     : "t0", "memory");

    // if (count < 0)
    //     fault_at = asm_fault_at;

    // return count;
}

CODE_SECTION(".text.safemem.atomic")
NEVER_INLINE FLATTEN Optional<bool> safe_atomic_compare_exchange_relaxed(u32 volatile* var, u32& expected, u32 val)
{
    // FIXME: Handle access faults.
    return AK::atomic_compare_exchange_strong(var, expected, val, AK::memory_order_relaxed);
}

CODE_SECTION(".text.safemem.atomic")
NEVER_INLINE FLATTEN Optional<u32> safe_atomic_load_relaxed(u32 volatile* var)
{
    // FIXME: Handle access faults.
    return AK::atomic_load(var, AK::memory_order_relaxed);
}

CODE_SECTION(".text.safemem.atomic")
NEVER_INLINE FLATTEN Optional<u32> safe_atomic_fetch_add_relaxed(u32 volatile* var, u32 val)
{
    // FIXME: Handle access faults.
    return AK::atomic_fetch_add(var, val, AK::memory_order_relaxed);
}

CODE_SECTION(".text.safemem.atomic")
NEVER_INLINE FLATTEN Optional<u32> safe_atomic_exchange_relaxed(u32 volatile* var, u32 val)
{
    // FIXME: Handle access faults.
    return AK::atomic_exchange(var, val, AK::memory_order_relaxed);
}

CODE_SECTION(".text.safemem.atomic")
NEVER_INLINE FLATTEN bool safe_atomic_store_relaxed(u32 volatile* var, u32 val)
{
    // FIXME: Handle access faults.
    AK::atomic_store(var, val);
    return true;
}

bool handle_safe_access_fault(RegisterState& regs, FlatPtr fault_address)
{
    FlatPtr pc = regs.ip();

    if (pc >= bit_cast<FlatPtr>(&start_of_safemem_text) && pc < bit_cast<FlatPtr>(&end_of_safemem_text)) {
        // If we detect that the fault happened in safe_memcpy() safe_strnlen(),
        // or safe_memset() then resume at the appropriate _end label
        if (pc >= bit_cast<FlatPtr>(&safe_memcpy_start) && pc < bit_cast<FlatPtr>(&safe_memcpy_end))
            pc = bit_cast<FlatPtr>(&safe_memcpy_end);
        else if (pc >= bit_cast<FlatPtr>(&safe_memset_start) && pc < bit_cast<FlatPtr>(&safe_memset_end))
            pc = bit_cast<FlatPtr>(&safe_memset_end);
        // else if (pc >= bit_cast<FlatPtr>(&safe_strnlen_start) && pc < bit_cast<FlatPtr>(&safe_strnlen_end))
        //     pc = bit_cast<FlatPtr>(&safe_strnlen_end);
        else {
            dbgln("FIXME: Faulted while accessing userspace address {:p}.", fault_address);
            dbgln("       We need to jump back into the appropriate SafeMem function, set fault_at and return failure.");
            TODO_RISCV64();
        }

        regs.set_ip(pc);
        regs.x[30] = fault_address; // t6 is x31
        return true;
    } else if (pc >= bit_cast<FlatPtr>(&start_of_safemem_atomic_text) && pc < bit_cast<FlatPtr>(&end_of_safemem_atomic_text)) {
        dbgln("FIXME: Faulted while accessing userspace address {:p}.", fault_address);
        dbgln("       We need to jump back into the appropriate atomic SafeMem function and return failure.");
        TODO_RISCV64();
    }

    return false;
}

}
