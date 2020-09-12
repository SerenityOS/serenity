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
#include <Kernel/Arch/i386/CPU.h>
#include <Kernel/Heap/kmalloc.h>
#include <Kernel/StdLib.h>
#include <Kernel/VM/MemoryManager.h>

String copy_string_from_user(const char* user_str, size_t user_str_size)
{
    bool is_user = Kernel::is_user_range(VirtualAddress(user_str), user_str_size);
    ASSERT(is_user); // For now assert to catch bugs, but technically not an error
    if (!is_user)
        return {};
    Kernel::SmapDisabler disabler;
    void* fault_at;
    ssize_t length = Kernel::safe_strnlen(user_str, user_str_size, fault_at);
    if (length < 0) {
        klog() << "copy_string_from_user(" << user_str << ", " << user_str_size << ") failed at " << VirtualAddress(fault_at) << " (strnlen)";
        return {};
    }
    if (length == 0)
        return String::empty();

    char* buffer;
    auto copied_string = StringImpl::create_uninitialized((size_t)length, buffer);
    if (!Kernel::safe_memcpy(buffer, user_str, (size_t)length, fault_at)) {
        klog() << "copy_string_from_user(" << user_str << ", " << user_str_size << ") failed at " << VirtualAddress(fault_at) << " (memcpy)";
        return {};
    }
    return copied_string;
}

String copy_string_from_user(Userspace<const char*> user_str, size_t user_str_size)
{
    return copy_string_from_user(user_str.unsafe_userspace_ptr(), user_str_size);
}

extern "C" {

bool copy_to_user(void* dest_ptr, const void* src_ptr, size_t n)
{
    bool is_user = Kernel::is_user_range(VirtualAddress(dest_ptr), n);
    ASSERT(is_user); // For now assert to catch bugs, but technically not an error
    if (!is_user)
        return false;
    ASSERT(!Kernel::is_user_range(VirtualAddress(src_ptr), n));
    Kernel::SmapDisabler disabler;
    void* fault_at;
    if (!Kernel::safe_memcpy(dest_ptr, src_ptr, n, fault_at)) {
        ASSERT(VirtualAddress(fault_at) >= VirtualAddress(dest_ptr) && VirtualAddress(fault_at) <= VirtualAddress((FlatPtr)dest_ptr + n));
        klog() << "copy_to_user(" << dest_ptr << ", " << src_ptr << ", " << n << ") failed at " << VirtualAddress(fault_at);
        return false;
    }
    return true;
}

bool copy_from_user(void* dest_ptr, const void* src_ptr, size_t n)
{
    bool is_user = Kernel::is_user_range(VirtualAddress(src_ptr), n);
    ASSERT(is_user); // For now assert to catch bugs, but technically not an error
    if (!is_user)
        return false;
    ASSERT(!Kernel::is_user_range(VirtualAddress(dest_ptr), n));
    Kernel::SmapDisabler disabler;
    void* fault_at;
    if (!Kernel::safe_memcpy(dest_ptr, src_ptr, n, fault_at)) {
        ASSERT(VirtualAddress(fault_at) >= VirtualAddress(src_ptr) && VirtualAddress(fault_at) <= VirtualAddress((FlatPtr)src_ptr + n));
        klog() << "copy_from_user(" << dest_ptr << ", " << src_ptr << ", " << n << ") failed at " << VirtualAddress(fault_at);
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
    ASSERT(is_user); // For now assert to catch bugs, but technically not an error
    if (!is_user)
        return false;
    Kernel::SmapDisabler disabler;
    void* fault_at;
    if (!Kernel::safe_memset(dest_ptr, c, n, fault_at)) {
        klog() << "memset(" << dest_ptr << ", " << n << ") failed at " << VirtualAddress(fault_at);
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
    ASSERT_NOT_REACHED();
}

[[noreturn]] void __stack_chk_fail_local()
{
    ASSERT_NOT_REACHED();
}

extern "C" int __cxa_atexit(void (*)(void*), void*, void*)
{
    ASSERT_NOT_REACHED();
    return 0;
}

[[noreturn]] void __cxa_pure_virtual()
{
    ASSERT_NOT_REACHED();
}
}
