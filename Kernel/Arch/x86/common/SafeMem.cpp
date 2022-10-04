/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Arch/Processor.h>
#include <Kernel/Arch/RegisterState.h>
#include <Kernel/Arch/SafeMem.h>

#define CODE_SECTION(section_name) __attribute__((section(section_name)))

extern "C" u8 start_of_safemem_text[];
extern "C" u8 end_of_safemem_text[];

extern "C" u8 safe_memcpy_ins_1[];
extern "C" u8 safe_memcpy_1_faulted[];
extern "C" u8 safe_memcpy_ins_2[];
extern "C" u8 safe_memcpy_2_faulted[];
extern "C" u8 safe_strnlen_ins[];
extern "C" u8 safe_strnlen_faulted[];
extern "C" u8 safe_memset_ins_1[];
extern "C" u8 safe_memset_1_faulted[];
extern "C" u8 safe_memset_ins_2[];
extern "C" u8 safe_memset_2_faulted[];

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

ALWAYS_INLINE bool validate_canonical_address(size_t address)
{
    auto most_significant_bits = Processor::current().virtual_address_bit_width() - 1;
    auto insignificant_bits = address >> most_significant_bits;
    return insignificant_bits == 0 || insignificant_bits == (0xffffffffffffffffull >> most_significant_bits);
}

CODE_SECTION(".text.safemem")
NEVER_INLINE bool safe_memcpy(void* dest_ptr, void const* src_ptr, size_t n, void*& fault_at)
{
    fault_at = nullptr;
    size_t dest = (size_t)dest_ptr;
    if (!validate_canonical_address(dest)) {
        fault_at = dest_ptr;
        return false;
    }
    size_t src = (size_t)src_ptr;
    if (!validate_canonical_address(src)) {
        fault_at = const_cast<void*>(src_ptr);
        return false;
    }
    size_t remainder;
    // FIXME: Support starting at an unaligned address.
    if (!(dest & 0x3) && !(src & 0x3) && n >= 12) {
        size_t size_ts = n / sizeof(size_t);
        asm volatile(
            ".globl safe_memcpy_ins_1 \n"
            "safe_memcpy_ins_1: \n"
            "rep movsq \n"
            ".globl safe_memcpy_1_faulted \n"
            "safe_memcpy_1_faulted: \n" // handle_safe_access_fault() set edx/rdx to the fault address!
            : "=S"(src),
            "=D"(dest),
            "=c"(remainder),
            [fault_at] "=d"(fault_at)
            : "S"(src),
            "D"(dest),
            "c"(size_ts)
            : "memory");
        if (remainder != 0)
            return false; // fault_at is already set!
        n -= size_ts * sizeof(size_t);
        if (n == 0) {
            fault_at = nullptr;
            return true;
        }
    }
    asm volatile(
        ".globl safe_memcpy_ins_2 \n"
        "safe_memcpy_ins_2: \n"
        "rep movsb \n"
        ".globl safe_memcpy_2_faulted \n"
        "safe_memcpy_2_faulted: \n" // handle_safe_access_fault() set edx/rdx to the fault address!
        : "=c"(remainder),
        [fault_at] "=d"(fault_at)
        : "S"(src),
        "D"(dest),
        "c"(n)
        : "memory");
    if (remainder != 0)
        return false; // fault_at is already set!
    fault_at = nullptr;
    return true;
}

CODE_SECTION(".text.safemem")
NEVER_INLINE ssize_t safe_strnlen(char const* str, size_t max_n, void*& fault_at)
{
    if (!validate_canonical_address((size_t)str)) {
        fault_at = const_cast<char*>(str);
        return false;
    }
    ssize_t count = 0;
    fault_at = nullptr;
    asm volatile(
        "1: \n"
        "test %[max_n], %[max_n] \n"
        "je 2f \n"
        "dec %[max_n] \n"
        ".globl safe_strnlen_ins \n"
        "safe_strnlen_ins: \n"
        "cmpb $0,(%[str], %[count], 1) \n"
        "je 2f \n"
        "inc %[count] \n"
        "jmp 1b \n"
        ".globl safe_strnlen_faulted \n"
        "safe_strnlen_faulted: \n" // handle_safe_access_fault() set edx/rdx to the fault address!
        "xor %[count_on_error], %[count_on_error] \n"
        "dec %[count_on_error] \n" // return -1 on fault
        "2:"
        : [count_on_error] "=c"(count),
        [fault_at] "=d"(fault_at)
        : [str] "b"(str),
        [count] "c"(count),
        [max_n] "d"(max_n));
    if (count >= 0)
        fault_at = nullptr;
    return count;
}

CODE_SECTION(".text.safemem")
NEVER_INLINE bool safe_memset(void* dest_ptr, int c, size_t n, void*& fault_at)
{
    fault_at = nullptr;
    size_t dest = (size_t)dest_ptr;
    if (!validate_canonical_address(dest)) {
        fault_at = dest_ptr;
        return false;
    }
    size_t remainder;
    // FIXME: Support starting at an unaligned address.
    if (!(dest & 0x3) && n >= 12) {
        size_t size_ts = n / sizeof(size_t);
        size_t expanded_c = (u8)c;
        expanded_c |= expanded_c << 8;
        expanded_c |= expanded_c << 16;
        asm volatile(
            ".globl safe_memset_ins_1 \n"
            "safe_memset_ins_1: \n"
            "rep stosq \n"
            ".globl safe_memset_1_faulted \n"
            "safe_memset_1_faulted: \n" // handle_safe_access_fault() set edx/rdx to the fault address!
            : "=D"(dest),
            "=c"(remainder),
            [fault_at] "=d"(fault_at)
            : "D"(dest),
            "a"(expanded_c),
            "c"(size_ts)
            : "memory");
        if (remainder != 0)
            return false; // fault_at is already set!
        n -= size_ts * sizeof(size_t);
        if (n == 0) {
            fault_at = nullptr;
            return true;
        }
    }
    asm volatile(
        ".globl safe_memset_ins_2 \n"
        "safe_memset_ins_2: \n"
        "rep stosb \n"
        ".globl safe_memset_2_faulted \n"
        "safe_memset_2_faulted: \n" // handle_safe_access_fault() set edx/rdx to the fault address!
        : "=D"(dest),
        "=c"(remainder),
        [fault_at] "=d"(fault_at)
        : "D"(dest),
        "c"(n),
        "a"(c)
        : "memory");
    if (remainder != 0)
        return false; // fault_at is already set!
    fault_at = nullptr;
    return true;
}

CODE_SECTION(".text.safemem.atomic")
NEVER_INLINE Optional<u32> safe_atomic_fetch_add_relaxed(u32 volatile* var, u32 val)
{
    u32 result;
    bool error;
    asm volatile(
        "xor %[error], %[error] \n"
        ".globl safe_atomic_fetch_add_relaxed_ins \n"
        "safe_atomic_fetch_add_relaxed_ins: \n"
        "lock xadd %[result], %[var] \n"
        ".globl safe_atomic_fetch_add_relaxed_faulted \n"
        "safe_atomic_fetch_add_relaxed_faulted: \n"
        : [error] "=d"(error), [result] "=a"(result), [var] "=m"(*var)
        : [val] "a"(val)
        : "memory");
    if (error)
        return {};
    return result;
}

CODE_SECTION(".text.safemem.atomic")
NEVER_INLINE Optional<u32> safe_atomic_exchange_relaxed(u32 volatile* var, u32 val)
{
    u32 result;
    bool error;
    asm volatile(
        "xor %[error], %[error] \n"
        ".globl safe_atomic_exchange_relaxed_ins \n"
        "safe_atomic_exchange_relaxed_ins: \n"
        "xchg %[val], %[var] \n"
        ".globl safe_atomic_exchange_relaxed_faulted \n"
        "safe_atomic_exchange_relaxed_faulted: \n"
        : [error] "=d"(error), "=a"(result), [var] "=m"(*var)
        : [val] "a"(val)
        : "memory");
    if (error)
        return {};
    return result;
}

CODE_SECTION(".text.safemem.atomic")
NEVER_INLINE Optional<u32> safe_atomic_load_relaxed(u32 volatile* var)
{
    u32 result;
    bool error;
    asm volatile(
        "xor %[error], %[error] \n"
        ".globl safe_atomic_load_relaxed_ins \n"
        "safe_atomic_load_relaxed_ins: \n"
        "mov (%[var]), %[result] \n"
        ".globl safe_atomic_load_relaxed_faulted \n"
        "safe_atomic_load_relaxed_faulted: \n"
        : [error] "=d"(error), [result] "=c"(result)
        : [var] "b"(var)
        : "memory");
    if (error)
        return {};
    return result;
}

CODE_SECTION(".text.safemem.atomic")
NEVER_INLINE bool safe_atomic_store_relaxed(u32 volatile* var, u32 val)
{
    bool error;
    asm volatile(
        "xor %[error], %[error] \n"
        ".globl safe_atomic_store_relaxed_ins \n"
        "safe_atomic_store_relaxed_ins: \n"
        "xchg %[val], %[var] \n"
        ".globl safe_atomic_store_relaxed_faulted \n"
        "safe_atomic_store_relaxed_faulted: \n"
        : [error] "=d"(error), [var] "=m"(*var)
        : [val] "r"(val)
        : "memory");
    return !error;
}

CODE_SECTION(".text.safemem.atomic")
NEVER_INLINE Optional<bool> safe_atomic_compare_exchange_relaxed(u32 volatile* var, u32& expected, u32 val)
{
    // NOTE: accessing expected is NOT protected as it should always point
    // to a valid location in kernel memory!
    bool error;
    bool did_exchange;
    asm volatile(
        "xor %[error], %[error] \n"
        ".globl safe_atomic_compare_exchange_relaxed_ins \n"
        "safe_atomic_compare_exchange_relaxed_ins: \n"
        "lock cmpxchg %[val], %[var] \n"
        ".globl safe_atomic_compare_exchange_relaxed_faulted \n"
        "safe_atomic_compare_exchange_relaxed_faulted: \n"
        : [error] "=d"(error), "=a"(expected), [var] "=m"(*var), "=@ccz"(did_exchange)
        : "a"(expected), [val] "b"(val)
        : "memory");
    if (error)
        return {};
    return did_exchange;
}

bool handle_safe_access_fault(RegisterState& regs, FlatPtr fault_address)
{
    FlatPtr ip = regs.ip();
    ;
    if (ip >= (FlatPtr)&start_of_safemem_text && ip < (FlatPtr)&end_of_safemem_text) {
        // If we detect that the fault happened in safe_memcpy() safe_strnlen(),
        // or safe_memset() then resume at the appropriate _faulted label
        if (ip == (FlatPtr)safe_memcpy_ins_1)
            ip = (FlatPtr)safe_memcpy_1_faulted;
        else if (ip == (FlatPtr)safe_memcpy_ins_2)
            ip = (FlatPtr)safe_memcpy_2_faulted;
        else if (ip == (FlatPtr)safe_strnlen_ins)
            ip = (FlatPtr)safe_strnlen_faulted;
        else if (ip == (FlatPtr)safe_memset_ins_1)
            ip = (FlatPtr)safe_memset_1_faulted;
        else if (ip == (FlatPtr)safe_memset_ins_2)
            ip = (FlatPtr)safe_memset_2_faulted;
        else
            return false;

        regs.set_ip(ip);
        regs.set_dx(fault_address);
        return true;
    }
    if (ip >= (FlatPtr)&start_of_safemem_atomic_text && ip < (FlatPtr)&end_of_safemem_atomic_text) {
        // If we detect that a fault happened in one of the atomic safe_
        // functions, resume at the appropriate _faulted label and set
        // the edx/rdx register to 1 to indicate an error
        if (ip == (FlatPtr)safe_atomic_fetch_add_relaxed_ins)
            ip = (FlatPtr)safe_atomic_fetch_add_relaxed_faulted;
        else if (ip == (FlatPtr)safe_atomic_exchange_relaxed_ins)
            ip = (FlatPtr)safe_atomic_exchange_relaxed_faulted;
        else if (ip == (FlatPtr)safe_atomic_load_relaxed_ins)
            ip = (FlatPtr)safe_atomic_load_relaxed_faulted;
        else if (ip == (FlatPtr)safe_atomic_store_relaxed_ins)
            ip = (FlatPtr)safe_atomic_store_relaxed_faulted;
        else if (ip == (FlatPtr)safe_atomic_compare_exchange_relaxed_ins)
            ip = (FlatPtr)safe_atomic_compare_exchange_relaxed_faulted;
        else
            return false;

        regs.set_ip(ip);
        regs.set_dx(1);
        return true;
    }
    return false;
}

}
