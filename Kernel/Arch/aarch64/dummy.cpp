/*
 * Copyright (c) 2021, Nico Weber <thakis@chromium.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Types.h>
#include <Kernel/KString.h>
#include <Kernel/KSyms.h>
#include <Kernel/Sections.h>

// init.cpp
extern size_t __stack_chk_guard;
READONLY_AFTER_INIT size_t __stack_chk_guard;

// This is a temporary file to get a non-empty Kernel binary on aarch64.
// The prekernel currently never jumps to the kernel. This is dead code.
void dummy();
void dummy() { }

// Assertions.h
[[noreturn]] void __assertion_failed(const char*, const char*, unsigned, const char*);

[[noreturn]] void __assertion_failed(const char*, const char*, unsigned, const char*)
{
    for (;;) { }
}

// kmalloc.h
size_t kmalloc_good_size(size_t);
size_t kmalloc_good_size(size_t) { return 0; }

void kfree_sized(void*, size_t);
void kfree_sized(void*, size_t) { }

void* kmalloc(size_t);
void* kmalloc(size_t) { return nullptr; }

void* operator new(size_t size) { return kmalloc(size); }
void* operator new(size_t size, std::align_val_t) { return kmalloc(size); }

namespace Kernel {

void dump_backtrace(PrintToScreen) { }

// KString.cpp
ErrorOr<NonnullOwnPtr<KString>> KString::try_create_uninitialized(size_t, char*&) { return ENOMEM; }
ErrorOr<NonnullOwnPtr<KString>> KString::try_create(StringView) { return ENOMEM; }
void KString::operator delete(void*) { }

// SafeMem.h
bool safe_memset(void*, int, size_t, void*&);
bool safe_memset(void*, int, size_t, void*&) { return false; }

ssize_t safe_strnlen(char const*, unsigned long, void*&);
ssize_t safe_strnlen(char const*, unsigned long, void*&) { return 0; }

bool safe_memcpy(void*, void const*, unsigned long, void*&);
bool safe_memcpy(void*, void const*, unsigned long, void*&) { return false; }

Optional<bool> safe_atomic_compare_exchange_relaxed(volatile u32*, u32&, u32);
Optional<bool> safe_atomic_compare_exchange_relaxed(volatile u32*, u32&, u32) { return {}; }

Optional<u32> safe_atomic_load_relaxed(volatile u32*);
Optional<u32> safe_atomic_load_relaxed(volatile u32*) { return {}; }

Optional<u32> safe_atomic_fetch_add_relaxed(volatile u32*, u32);
Optional<u32> safe_atomic_fetch_add_relaxed(volatile u32*, u32) { return {}; }

Optional<u32> safe_atomic_exchange_relaxed(volatile u32*, u32);
Optional<u32> safe_atomic_exchange_relaxed(volatile u32*, u32) { return {}; }

bool safe_atomic_store_relaxed(volatile u32*, u32);
bool safe_atomic_store_relaxed(volatile u32*, u32) { return {}; }

}

extern "C" {

FlatPtr kernel_mapping_base;

void kernelputstr(const char*, size_t);
void kernelputstr(const char*, size_t) { }

void kernelcriticalputstr(const char*, size_t);
void kernelcriticalputstr(const char*, size_t) { }

void kernelearlyputstr(const char*, size_t);
void kernelearlyputstr(const char*, size_t) { }
}
