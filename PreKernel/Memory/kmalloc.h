/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>

#ifndef PAGE_SIZE
#    define PAGE_SIZE 4096
#endif

// The C++ standard specifies that the nothrow allocation tag should live in the std namespace.
// Otherwise, `new (std::nothrow)` calls wouldn't get resolved.
namespace std {
struct nothrow_t {
    explicit nothrow_t() = default;
};

extern const nothrow_t nothrow;
};

struct kmalloc_stats {
    size_t bytes_allocated;
    size_t bytes_free;
    size_t bytes_eternal;
};
void get_kmalloc_stats(kmalloc_stats&);

void kmalloc_eternal_init();
void kmalloc_init();

void kfree_sized(void*, size_t);

[[gnu::malloc, gnu::returns_nonnull, gnu::alloc_size(1)]] void* kmalloc_eternal(size_t);

void* krealloc(void*, size_t);
void kfree(void*);
void* kmalloc(size_t);
extern "C" {
size_t kmalloc_good_size(size_t size);
}

extern "C" {
void declare_emergency_kmalloc();
}

inline void* operator new(size_t, void* p) { return p; }
inline void* operator new[](size_t, void* p) { return p; }

[[nodiscard]] void* operator new(size_t size);
[[nodiscard]] void* operator new(size_t size, const std::nothrow_t&) noexcept;
void operator delete(void* ptr) noexcept;
void operator delete(void* ptr, size_t) noexcept;
[[nodiscard]] void* operator new[](size_t size);
[[nodiscard]] void* operator new[](size_t size, const std::nothrow_t&) noexcept;
void operator delete[](void* ptrs) noexcept;
void operator delete[](void* ptr, size_t) noexcept;
