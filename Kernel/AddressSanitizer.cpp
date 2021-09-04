/*
 * Copyright (c) 2021, Brian Gianforcaro <bgianf@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#if defined(__SANITIZE_ADDRESS__)

#    include <Kernel/AddressSanitizer.h>

void Kernel::AddressSanitizer::shadow_va_check_load(unsigned long address, size_t size, void* return_address)
{
    (void)address;
    (void)size;
    (void)return_address;
}

void Kernel::AddressSanitizer::shadow_va_check_store(unsigned long address, size_t size, void* return_address)
{
    (void)address;
    (void)size;
    (void)return_address;
}

using namespace Kernel;
using namespace Kernel::AddressSanitizer;

extern "C" {

// Define a macro to easily declare the KASAN load and store callbacks for
// the various sizes of data type.
//
#    define ADDRESS_SANITIZER_LOAD_STORE(size)                                 \
        void __asan_load##size(unsigned long);                                 \
        void __asan_load##size(unsigned long address)                          \
        {                                                                      \
            shadow_va_check_load(address, size, __builtin_return_address(0));  \
        }                                                                      \
        void __asan_load##size##_noabort(unsigned long);                       \
        void __asan_load##size##_noabort(unsigned long address)                \
        {                                                                      \
            shadow_va_check_load(address, size, __builtin_return_address(0));  \
        }                                                                      \
        void __asan_store##size(unsigned long);                                \
        void __asan_store##size(unsigned long address)                         \
        {                                                                      \
            shadow_va_check_store(address, size, __builtin_return_address(0)); \
        }                                                                      \
        void __asan_store##size##_noabort(unsigned long);                      \
        void __asan_store##size##_noabort(unsigned long address)               \
        {                                                                      \
            shadow_va_check_store(address, size, __builtin_return_address(0)); \
        }

ADDRESS_SANITIZER_LOAD_STORE(1);
ADDRESS_SANITIZER_LOAD_STORE(2);
ADDRESS_SANITIZER_LOAD_STORE(4);
ADDRESS_SANITIZER_LOAD_STORE(8);
ADDRESS_SANITIZER_LOAD_STORE(16);

#    undef ADDRESS_SANITIZER_LOAD_STORE

void __asan_loadN(unsigned long, size_t);
void __asan_loadN(unsigned long address, size_t size)
{
    shadow_va_check_load(address, size, __builtin_return_address(0));
}

void __asan_loadN_noabort(unsigned long, size_t);
void __asan_loadN_noabort(unsigned long address, size_t size)
{
    shadow_va_check_load(address, size, __builtin_return_address(0));
}

void __asan_storeN(unsigned long, size_t);
void __asan_storeN(unsigned long address, size_t size)
{
    shadow_va_check_store(address, size, __builtin_return_address(0));
}

void __asan_storeN_noabort(unsigned long, size_t);
void __asan_storeN_noabort(unsigned long address, size_t size)
{
    shadow_va_check_store(address, size, __builtin_return_address(0));
}

// Performs shadow memory cleanup of the current thread's stack before a
// function marked with the [[noreturn]] attribute is called.
//
void __asan_handle_no_return(void);
void __asan_handle_no_return(void)
{
}

void __asan_before_dynamic_init(char const*);
void __asan_before_dynamic_init(char const* /* module_name */)
{
}

void __asan_after_dynamic_init();
void __asan_after_dynamic_init()
{
}
}

#endif
