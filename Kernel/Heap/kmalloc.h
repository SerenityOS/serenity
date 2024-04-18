/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>
#include <Kernel/API/POSIX/sys/limits.h> // For PAGE_SIZE. Everyone needs PAGE_SIZE

#define KMALLOC_SCRUB_BYTE 0xbb
#define KFREE_SCRUB_BYTE 0xaa

#define MAKE_ALIGNED_ALLOCATED(type, alignment)                              \
public:                                                                      \
    [[nodiscard]] void* operator new(size_t)                                 \
    {                                                                        \
        void* ptr = kmalloc_aligned(sizeof(type), alignment);                \
        VERIFY(ptr);                                                         \
        return ptr;                                                          \
    }                                                                        \
    [[nodiscard]] void* operator new(size_t, std::nothrow_t const&) noexcept \
    {                                                                        \
        return kmalloc_aligned(sizeof(type), alignment);                     \
    }                                                                        \
    void operator delete(void* ptr) noexcept                                 \
    {                                                                        \
        kfree_sized(ptr, sizeof(type));                                      \
    }                                                                        \
                                                                             \
private:

// The C++ standard specifies that the nothrow allocation tag should live in the std namespace.
// Otherwise, `new (std::nothrow)` calls wouldn't get resolved.
namespace std { // NOLINT(cert-dcl58-cpp) These declarations must be in ::std and we are not using <new>
struct nothrow_t {
    explicit nothrow_t() = default;
};

extern nothrow_t const nothrow;

enum class align_val_t : size_t {};
};

void kmalloc_init();

void kfree_sized(void*, size_t);

struct kmalloc_stats {
    size_t bytes_allocated;
    size_t bytes_free;
    size_t kmalloc_call_count;
    size_t kfree_call_count;
};
void get_kmalloc_stats(kmalloc_stats&);

extern bool g_dump_kmalloc_stacks;

inline void* operator new(size_t, void* p) { return p; }
inline void* operator new[](size_t, void* p) { return p; }

[[nodiscard]] void* operator new(size_t size);
[[nodiscard]] void* operator new(size_t size, std::nothrow_t const&) noexcept;
[[nodiscard]] void* operator new(size_t size, std::align_val_t);
[[nodiscard]] void* operator new(size_t size, std::align_val_t, std::nothrow_t const&) noexcept;

void operator delete(void* ptr) noexcept DISALLOW("All deletes in the kernel should have a known size.");
void operator delete(void* ptr, size_t) noexcept;
void operator delete(void* ptr, std::align_val_t) noexcept DISALLOW("All deletes in the kernel should have a known size.");
void operator delete(void* ptr, size_t, std::align_val_t) noexcept;

[[nodiscard]] void* operator new[](size_t size);
[[nodiscard]] void* operator new[](size_t size, std::nothrow_t const&) noexcept;

void operator delete[](void* ptrs) noexcept DISALLOW("All deletes in the kernel should have a known size.");
void operator delete[](void* ptr, size_t) noexcept;

[[gnu::malloc, gnu::alloc_size(1)]] void* kmalloc(size_t);
[[gnu::malloc, gnu::alloc_size(1, 2)]] void* kcalloc(size_t, size_t);

[[gnu::malloc, gnu::alloc_size(1), gnu::alloc_align(2)]] void* kmalloc_aligned(size_t size, size_t alignment);

size_t kmalloc_good_size(size_t);

void kmalloc_enable_expand();
