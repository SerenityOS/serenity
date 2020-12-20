/*
 * Copyright (c) 2020, the SerenityOS developers.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <Kernel/Arch/i386/CPU.h>
#include <Kernel/Arch/i386/SafeMem.h>

#define CODE_SECTION(section_name) __attribute__((section(section_name)))

extern "C" u8* start_of_safemem_text;
extern "C" u8* end_of_safemem_text;

extern "C" u8* safe_memcpy_ins_1;
extern "C" u8* safe_memcpy_1_faulted;
extern "C" u8* safe_memcpy_ins_2;
extern "C" u8* safe_memcpy_2_faulted;
extern "C" u8* safe_strnlen_ins;
extern "C" u8* safe_strnlen_faulted;
extern "C" u8* safe_memset_ins_1;
extern "C" u8* safe_memset_1_faulted;
extern "C" u8* safe_memset_ins_2;
extern "C" u8* safe_memset_2_faulted;

extern "C" u8* start_of_safemem_atomic_text;
extern "C" u8* end_of_safemem_atomic_text;

extern "C" u8* safe_atomic_fetch_add_relaxed_ins;
extern "C" u8* safe_atomic_fetch_add_relaxed_faulted;
extern "C" u8* safe_atomic_exchange_relaxed_ins;
extern "C" u8* safe_atomic_exchange_relaxed_faulted;
extern "C" u8* safe_atomic_load_relaxed_ins;
extern "C" u8* safe_atomic_load_relaxed_faulted;
extern "C" u8* safe_atomic_store_relaxed_ins;
extern "C" u8* safe_atomic_store_relaxed_faulted;
extern "C" u8* safe_atomic_compare_exchange_relaxed_ins;
extern "C" u8* safe_atomic_compare_exchange_relaxed_faulted;

namespace Kernel {

CODE_SECTION(".text.safemem")
bool safe_memcpy(void* dest_ptr, const void* src_ptr, size_t n, void*& fault_at)
{
    fault_at = nullptr;
    size_t dest = (size_t)dest_ptr;
    size_t src = (size_t)src_ptr;
    size_t remainder;
    // FIXME: Support starting at an unaligned address.
    if (!(dest & 0x3) && !(src & 0x3) && n >= 12) {
        size_t size_ts = n / sizeof(size_t);
        asm volatile(
            "safe_memcpy_ins_1: \n"
            "rep movsl \n"
            "safe_memcpy_1_faulted: \n" // handle_safe_access_fault() set edx to the fault address!
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
        "safe_memcpy_ins_2: \n"
        "rep movsb \n"
        "safe_memcpy_2_faulted: \n" // handle_safe_access_fault() set edx to the fault address!
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
ssize_t safe_strnlen(const char* str, size_t max_n, void*& fault_at)
{
    ssize_t count = 0;
    fault_at = nullptr;
    asm volatile(
        "1: \n"
        "test %[max_n], %[max_n] \n"
        "je 2f \n"
        "dec %[max_n] \n"
        "safe_strnlen_ins: \n"
        "cmpb $0,(%[str], %[count], 1) \n"
        "je 2f \n"
        "inc %[count] \n"
        "jmp 1b \n"
        "safe_strnlen_faulted: \n" // handle_safe_access_fault() set edx to the fault address!
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
bool safe_memset(void* dest_ptr, int c, size_t n, void*& fault_at)
{
    fault_at = nullptr;
    size_t dest = (size_t)dest_ptr;
    size_t remainder;
    // FIXME: Support starting at an unaligned address.
    if (!(dest & 0x3) && n >= 12) {
        size_t size_ts = n / sizeof(size_t);
        size_t expanded_c = (u8)c;
        expanded_c |= expanded_c << 8;
        expanded_c |= expanded_c << 16;
        asm volatile(
            "safe_memset_ins_1: \n"
            "rep stosl \n"
            "safe_memset_1_faulted: \n" // handle_safe_access_fault() set edx to the fault address!
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
        if (remainder == 0) {
            fault_at = nullptr;
            return true;
        }
    }
    asm volatile(
        "safe_memset_ins_2: \n"
        "rep stosb \n"
        "safe_memset_2_faulted: \n" // handle_safe_access_fault() set edx to the fault address!
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
Optional<u32> safe_atomic_fetch_add_relaxed(volatile u32* var, u32 val)
{
    u32 result;
    bool error;
    asm volatile(
        "xor %[error], %[error] \n"
        "safe_atomic_fetch_add_relaxed_ins: \n"
        "lock xadd %[result], %[var] \n"
        "safe_atomic_fetch_add_relaxed_faulted: \n"
        : [error] "=d"(error), [result] "=a"(result), [var] "=m"(*var)
        : [val] "a"(val)
        : "memory");
    if (error)
        return {};
    return result;
}

CODE_SECTION(".text.safemem.atomic")
Optional<u32> safe_atomic_exchange_relaxed(volatile u32* var, u32 val)
{
    u32 result;
    bool error;
    asm volatile(
        "xor %[error], %[error] \n"
        "safe_atomic_exchange_relaxed_ins: \n"
        "xchg %[val], %[var] \n"
        "safe_atomic_exchange_relaxed_faulted: \n"
        : [error] "=d"(error), "=a"(result), [var] "=m"(*var)
        : [val] "a"(val)
        : "memory");
    if (error)
        return {};
    return result;
}

CODE_SECTION(".text.safemem.atomic")
Optional<u32> safe_atomic_load_relaxed(volatile u32* var)
{
    u32 result;
    bool error;
    asm volatile(
        "xor %[error], %[error] \n"
        "safe_atomic_load_relaxed_ins: \n"
        "mov (%[var]), %[result] \n"
        "safe_atomic_load_relaxed_faulted: \n"
        : [error] "=d"(error), [result] "=c"(result)
        : [var] "b"(var)
        : "memory");
    if (error)
        return {};
    return result;
}

CODE_SECTION(".text.safemem.atomic")
bool safe_atomic_store_relaxed(volatile u32* var, u32 val)
{
    bool error;
    asm volatile(
        "xor %[error], %[error] \n"
        "safe_atomic_store_relaxed_ins: \n"
        "xchg %[val], %[var] \n"
        "safe_atomic_store_relaxed_faulted: \n"
        : [error] "=d"(error), [var] "=m"(*var)
        : [val] "r"(val)
        : "memory");
    return !error;
}

CODE_SECTION(".text.safemem.atomic")
Optional<bool> safe_atomic_compare_exchange_relaxed(volatile u32* var, u32& expected, u32 val)
{
    // NOTE: accessing expected is NOT protected as it should always point
    // to a valid location in kernel memory!
    bool error;
    bool did_exchange;
    asm volatile(
        "xor %[error], %[error] \n"
        "safe_atomic_compare_exchange_relaxed_ins: \n"
        "lock cmpxchg %[val], %[var] \n"
        "safe_atomic_compare_exchange_relaxed_faulted: \n"
        : [error] "=d"(error), "=a"(expected), [var] "=m"(*var), "=@ccz"(did_exchange)
        : "a"(expected), [val] "b"(val)
        : "memory");
    if (error)
        return {};
    return did_exchange;
}

bool handle_safe_access_fault(RegisterState& regs, u32 fault_address)
{
    if (regs.eip >= (FlatPtr)&start_of_safemem_text && regs.eip < (FlatPtr)&end_of_safemem_text) {
        // If we detect that the fault happened in safe_memcpy() safe_strnlen(),
        // or safe_memset() then resume at the appropriate _faulted label
        if (regs.eip == (FlatPtr)&safe_memcpy_ins_1)
            regs.eip = (FlatPtr)&safe_memcpy_1_faulted;
        else if (regs.eip == (FlatPtr)&safe_memcpy_ins_2)
            regs.eip = (FlatPtr)&safe_memcpy_2_faulted;
        else if (regs.eip == (FlatPtr)&safe_strnlen_ins)
            regs.eip = (FlatPtr)&safe_strnlen_faulted;
        else if (regs.eip == (FlatPtr)&safe_memset_ins_1)
            regs.eip = (FlatPtr)&safe_memset_1_faulted;
        else if (regs.eip == (FlatPtr)&safe_memset_ins_2)
            regs.eip = (FlatPtr)&safe_memset_2_faulted;
        else
            return false;

        regs.edx = fault_address;
        return true;
    }
    if (regs.eip >= (FlatPtr)&start_of_safemem_atomic_text && regs.eip < (FlatPtr)&end_of_safemem_atomic_text) {
        // If we detect that a fault happened in one of the atomic safe_
        // functions, resume at the appropriate _faulted label and set
        // the edx register to 1 to indicate an error
        if (regs.eip == (FlatPtr)&safe_atomic_fetch_add_relaxed_ins)
            regs.eip = (FlatPtr)&safe_atomic_fetch_add_relaxed_faulted;
        else if (regs.eip == (FlatPtr)&safe_atomic_exchange_relaxed_ins)
            regs.eip = (FlatPtr)&safe_atomic_exchange_relaxed_faulted;
        else if (regs.eip == (FlatPtr)&safe_atomic_load_relaxed_ins)
            regs.eip = (FlatPtr)&safe_atomic_load_relaxed_faulted;
        else if (regs.eip == (FlatPtr)&safe_atomic_store_relaxed_ins)
            regs.eip = (FlatPtr)&safe_atomic_store_relaxed_faulted;
        else if (regs.eip == (FlatPtr)&safe_atomic_compare_exchange_relaxed_ins)
            regs.eip = (FlatPtr)&safe_atomic_compare_exchange_relaxed_faulted;
        else
            return false;

        regs.edx = 1;
        return true;
    }
    return false;
}

}
