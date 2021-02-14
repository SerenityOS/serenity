/*
 * Copyright (c) 2021, Brian Gianforcaro <b.gianfo@gmail.com>
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

void __asan_before_dynamic_init(const char*);
void __asan_before_dynamic_init(const char* /* module_name */)
{
}

void __asan_after_dynamic_init();
void __asan_after_dynamic_init()
{
}
}

#endif
