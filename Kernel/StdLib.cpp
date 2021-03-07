/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
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

#include <AK/Assertions.h>
#include <AK/MemMem.h>
#include <AK/String.h>
#include <AK/Types.h>
#include <Kernel/Arch/x86/CPU.h>
#include <Kernel/Arch/x86/SmapDisabler.h>
#include <Kernel/Heap/kmalloc.h>
#include <Kernel/StdLib.h>
#include <Kernel/VM/MemoryManager.h>

String copy_string_from_user(const char* user_str, size_t user_str_size)
{
    bool is_user = Kernel::is_user_range(VirtualAddress(user_str), user_str_size);
    if (!is_user)
        return {};
    Kernel::SmapDisabler disabler;
    void* fault_at;
    ssize_t length = Kernel::safe_strnlen(user_str, user_str_size, fault_at);
    if (length < 0) {
        dbgln("copy_string_from_user({:p}, {}) failed at {} (strnlen)", static_cast<const void*>(user_str), user_str_size, VirtualAddress { fault_at });
        return {};
    }
    if (length == 0)
        return String::empty();

    char* buffer;
    auto copied_string = StringImpl::create_uninitialized((size_t)length, buffer);
    if (!Kernel::safe_memcpy(buffer, user_str, (size_t)length, fault_at)) {
        dbgln("copy_string_from_user({:p}, {}) failed at {} (memcpy)", static_cast<const void*>(user_str), user_str_size, VirtualAddress { fault_at });
        return {};
    }
    return copied_string;
}

String copy_string_from_user(Userspace<const char*> user_str, size_t user_str_size)
{
    return copy_string_from_user(user_str.unsafe_userspace_ptr(), user_str_size);
}

[[nodiscard]] Optional<Time> copy_time_from_user(const timespec* ts_user)
{
    timespec ts;
    if (!copy_from_user(&ts, ts_user, sizeof(timespec))) {
        return {};
    }
    return Time::from_timespec(ts);
}
[[nodiscard]] Optional<Time> copy_time_from_user(const timeval* tv_user)
{
    timeval tv;
    if (!copy_from_user(&tv, tv_user, sizeof(timeval))) {
        return {};
    }
    return Time::from_timeval(tv);
}

template<>
[[nodiscard]] Optional<Time> copy_time_from_user<const timeval>(Userspace<const timeval*> src) { return copy_time_from_user(src.unsafe_userspace_ptr()); }
template<>
[[nodiscard]] Optional<Time> copy_time_from_user<timeval>(Userspace<timeval*> src) { return copy_time_from_user(src.unsafe_userspace_ptr()); }
template<>
[[nodiscard]] Optional<Time> copy_time_from_user<const timespec>(Userspace<const timespec*> src) { return copy_time_from_user(src.unsafe_userspace_ptr()); }
template<>
[[nodiscard]] Optional<Time> copy_time_from_user<timespec>(Userspace<timespec*> src) { return copy_time_from_user(src.unsafe_userspace_ptr()); }

Optional<u32> user_atomic_fetch_add_relaxed(volatile u32* var, u32 val)
{
    if (FlatPtr(var) & 3)
        return {}; // not aligned!
    bool is_user = Kernel::is_user_range(VirtualAddress(FlatPtr(var)), sizeof(*var));
    if (!is_user)
        return {};
    Kernel::SmapDisabler disabler;
    return Kernel::safe_atomic_fetch_add_relaxed(var, val);
}

Optional<u32> user_atomic_exchange_relaxed(volatile u32* var, u32 val)
{
    if (FlatPtr(var) & 3)
        return {}; // not aligned!
    bool is_user = Kernel::is_user_range(VirtualAddress(FlatPtr(var)), sizeof(*var));
    if (!is_user)
        return {};
    Kernel::SmapDisabler disabler;
    return Kernel::safe_atomic_exchange_relaxed(var, val);
}

Optional<u32> user_atomic_load_relaxed(volatile u32* var)
{
    if (FlatPtr(var) & 3)
        return {}; // not aligned!
    bool is_user = Kernel::is_user_range(VirtualAddress(FlatPtr(var)), sizeof(*var));
    if (!is_user)
        return {};
    Kernel::SmapDisabler disabler;
    return Kernel::safe_atomic_load_relaxed(var);
}

bool user_atomic_store_relaxed(volatile u32* var, u32 val)
{
    if (FlatPtr(var) & 3)
        return false; // not aligned!
    bool is_user = Kernel::is_user_range(VirtualAddress(FlatPtr(var)), sizeof(*var));
    if (!is_user)
        return false;
    Kernel::SmapDisabler disabler;
    return Kernel::safe_atomic_store_relaxed(var, val);
}

Optional<bool> user_atomic_compare_exchange_relaxed(volatile u32* var, u32& expected, u32 val)
{
    if (FlatPtr(var) & 3)
        return {}; // not aligned!
    VERIFY(!Kernel::is_user_range(VirtualAddress(&expected), sizeof(expected)));
    bool is_user = Kernel::is_user_range(VirtualAddress(FlatPtr(var)), sizeof(*var));
    if (!is_user)
        return {};
    Kernel::SmapDisabler disabler;
    return Kernel::safe_atomic_compare_exchange_relaxed(var, expected, val);
}

Optional<u32> user_atomic_fetch_and_relaxed(volatile u32* var, u32 val)
{
    if (FlatPtr(var) & 3)
        return {}; // not aligned!
    bool is_user = Kernel::is_user_range(VirtualAddress(FlatPtr(var)), sizeof(*var));
    if (!is_user)
        return {};
    Kernel::SmapDisabler disabler;
    return Kernel::safe_atomic_fetch_and_relaxed(var, val);
}

Optional<u32> user_atomic_fetch_and_not_relaxed(volatile u32* var, u32 val)
{
    if (FlatPtr(var) & 3)
        return {}; // not aligned!
    bool is_user = Kernel::is_user_range(VirtualAddress(FlatPtr(var)), sizeof(*var));
    if (!is_user)
        return {};
    Kernel::SmapDisabler disabler;
    return Kernel::safe_atomic_fetch_and_not_relaxed(var, val);
}

Optional<u32> user_atomic_fetch_or_relaxed(volatile u32* var, u32 val)
{
    if (FlatPtr(var) & 3)
        return {}; // not aligned!
    bool is_user = Kernel::is_user_range(VirtualAddress(FlatPtr(var)), sizeof(*var));
    if (!is_user)
        return {};
    Kernel::SmapDisabler disabler;
    return Kernel::safe_atomic_fetch_or_relaxed(var, val);
}

Optional<u32> user_atomic_fetch_xor_relaxed(volatile u32* var, u32 val)
{
    if (FlatPtr(var) & 3)
        return {}; // not aligned!
    bool is_user = Kernel::is_user_range(VirtualAddress(FlatPtr(var)), sizeof(*var));
    if (!is_user)
        return {};
    Kernel::SmapDisabler disabler;
    return Kernel::safe_atomic_fetch_xor_relaxed(var, val);
}

extern "C" {

bool copy_to_user(void* dest_ptr, const void* src_ptr, size_t n)
{
    bool is_user = Kernel::is_user_range(VirtualAddress(dest_ptr), n);
    if (!is_user)
        return false;
    VERIFY(!Kernel::is_user_range(VirtualAddress(src_ptr), n));
    Kernel::SmapDisabler disabler;
    void* fault_at;
    if (!Kernel::safe_memcpy(dest_ptr, src_ptr, n, fault_at)) {
        VERIFY(VirtualAddress(fault_at) >= VirtualAddress(dest_ptr) && VirtualAddress(fault_at) <= VirtualAddress((FlatPtr)dest_ptr + n));
        dbgln("copy_to_user({:p}, {:p}, {}) failed at {}", dest_ptr, src_ptr, n, VirtualAddress { fault_at });
        return false;
    }
    return true;
}

bool copy_from_user(void* dest_ptr, const void* src_ptr, size_t n)
{
    bool is_user = Kernel::is_user_range(VirtualAddress(src_ptr), n);
    if (!is_user)
        return false;
    VERIFY(!Kernel::is_user_range(VirtualAddress(dest_ptr), n));
    Kernel::SmapDisabler disabler;
    void* fault_at;
    if (!Kernel::safe_memcpy(dest_ptr, src_ptr, n, fault_at)) {
        VERIFY(VirtualAddress(fault_at) >= VirtualAddress(src_ptr) && VirtualAddress(fault_at) <= VirtualAddress((FlatPtr)src_ptr + n));
        dbgln("copy_from_user({:p}, {:p}, {}) failed at {}", dest_ptr, src_ptr, n, VirtualAddress { fault_at });
        return false;
    }
    return true;
}

void* memcpy(void* dest_ptr, const void* src_ptr, size_t n)
{
    size_t dest = (size_t)dest_ptr;
    size_t src = (size_t)src_ptr;
    // FIXME: Support starting at an unaligned address.
    if (!(dest & 0x3) && !(src & 0x3) && n >= 12) {
        size_t size_ts = n / sizeof(size_t);
        asm volatile(
            "rep movsl\n"
            : "=S"(src), "=D"(dest)
            : "S"(src), "D"(dest), "c"(size_ts)
            : "memory");
        n -= size_ts * sizeof(size_t);
        if (n == 0)
            return dest_ptr;
    }
    asm volatile(
        "rep movsb\n" ::"S"(src), "D"(dest), "c"(n)
        : "memory");
    return dest_ptr;
}

void* memmove(void* dest, const void* src, size_t n)
{
    if (dest < src)
        return memcpy(dest, src, n);

    u8* pd = (u8*)dest;
    const u8* ps = (const u8*)src;
    for (pd += n, ps += n; n--;)
        *--pd = *--ps;
    return dest;
}

const void* memmem(const void* haystack, size_t haystack_length, const void* needle, size_t needle_length)
{
    return AK::memmem(haystack, haystack_length, needle, needle_length);
}

[[nodiscard]] bool memset_user(void* dest_ptr, int c, size_t n)
{
    bool is_user = Kernel::is_user_range(VirtualAddress(dest_ptr), n);
    if (!is_user)
        return false;
    Kernel::SmapDisabler disabler;
    void* fault_at;
    if (!Kernel::safe_memset(dest_ptr, c, n, fault_at)) {
        dbgln("memset_user({:p}, {}, {}) failed at {}", dest_ptr, c, n, VirtualAddress { fault_at });
        return false;
    }
    return true;
}

void* memset(void* dest_ptr, int c, size_t n)
{
    size_t dest = (size_t)dest_ptr;
    // FIXME: Support starting at an unaligned address.
    if (!(dest & 0x3) && n >= 12) {
        size_t size_ts = n / sizeof(size_t);
        size_t expanded_c = (u8)c;
        expanded_c |= expanded_c << 8;
        expanded_c |= expanded_c << 16;
        asm volatile(
            "rep stosl\n"
            : "=D"(dest)
            : "D"(dest), "c"(size_ts), "a"(expanded_c)
            : "memory");
        n -= size_ts * sizeof(size_t);
        if (n == 0)
            return dest_ptr;
    }
    asm volatile(
        "rep stosb\n"
        : "=D"(dest), "=c"(n)
        : "0"(dest), "1"(n), "a"(c)
        : "memory");
    return dest_ptr;
}

size_t strlen(const char* str)
{
    size_t len = 0;
    while (*(str++))
        ++len;
    return len;
}

size_t strnlen(const char* str, size_t maxlen)
{
    size_t len = 0;
    for (; len < maxlen && *str; str++)
        len++;
    return len;
}

int strcmp(const char* s1, const char* s2)
{
    for (; *s1 == *s2; ++s1, ++s2) {
        if (*s1 == 0)
            return 0;
    }
    return *(const u8*)s1 < *(const u8*)s2 ? -1 : 1;
}

int memcmp(const void* v1, const void* v2, size_t n)
{
    auto* s1 = (const u8*)v1;
    auto* s2 = (const u8*)v2;
    while (n-- > 0) {
        if (*s1++ != *s2++)
            return s1[-1] < s2[-1] ? -1 : 1;
    }
    return 0;
}

int strncmp(const char* s1, const char* s2, size_t n)
{
    if (!n)
        return 0;
    do {
        if (*s1 != *s2++)
            return *(const unsigned char*)s1 - *(const unsigned char*)--s2;
        if (*s1++ == 0)
            break;
    } while (--n);
    return 0;
}

char* strstr(const char* haystack, const char* needle)
{
    char nch;
    char hch;

    if ((nch = *needle++) != 0) {
        size_t len = strlen(needle);
        do {
            do {
                if ((hch = *haystack++) == 0)
                    return nullptr;
            } while (hch != nch);
        } while (strncmp(haystack, needle, len) != 0);
        --haystack;
    }
    return const_cast<char*>(haystack);
}

void* realloc(void* p, size_t s)
{
    return krealloc(p, s);
}

void free(void* p)
{
    return kfree(p);
}

// Functions that are automatically called by the C++ compiler.
// Declare them first, to tell the silly compiler that they are indeed being used.
[[noreturn]] void __stack_chk_fail();
[[noreturn]] void __stack_chk_fail_local();
extern "C" int __cxa_atexit(void (*)(void*), void*, void*);
[[noreturn]] void __cxa_pure_virtual();

[[noreturn]] void __stack_chk_fail()
{
    VERIFY_NOT_REACHED();
}

[[noreturn]] void __stack_chk_fail_local()
{
    VERIFY_NOT_REACHED();
}

extern "C" int __cxa_atexit(void (*)(void*), void*, void*)
{
    VERIFY_NOT_REACHED();
    return 0;
}

[[noreturn]] void __cxa_pure_virtual()
{
    VERIFY_NOT_REACHED();
}
}
