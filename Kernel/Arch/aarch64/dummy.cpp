/*
 * Copyright (c) 2021, Nico Weber <thakis@chromium.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Types.h>
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
}

extern "C" {

// StdLib.cpp
[[noreturn]] void __stack_chk_fail();
[[noreturn]] void __stack_chk_fail()
{
    for (;;) { }
}

int memcmp(const void*, const void*, size_t);
int memcmp(const void*, const void*, size_t) { return 0; }

int strcmp(char const*, const char*);
int strcmp(char const*, const char*) { return 0; }

// kstdio.h
void kernelputstr(const char*, size_t);
void kernelputstr(const char*, size_t) { }

void kernelcriticalputstr(const char*, size_t);
void kernelcriticalputstr(const char*, size_t) { }

void kernelearlyputstr(const char*, size_t);
void kernelearlyputstr(const char*, size_t) { }

// MiniStdLib.cpp
void* memcpy(void*, const void*, size_t);
void* memcpy(void*, const void*, size_t) { return nullptr; }

void* memmove(void*, const void*, size_t);
void* memmove(void*, const void*, size_t) { return nullptr; }

void* memset(void*, int, size_t);
void* memset(void*, int, size_t) { return nullptr; }

size_t strlen(const char*);
size_t strlen(const char*) { return 0; }
}
