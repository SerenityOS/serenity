/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StdLibExtras.h>
#include <AK/Types.h>
#include <Kernel/Prekernel/Assertions.h>
#include <Kernel/Prekernel/Memory/Management.h>
#include <Kernel/Prekernel/Memory/PhysicalRange.h>
#include <Kernel/Prekernel/Memory/kmalloc.h>
#include <Kernel/Prekernel/StdLibExtras.h>

static u8* s_next_eternal_ptr;
static u64 s_eternal_allocated_count = 0;
static u8* s_end_of_eternal_range;

static Prekernel::PhysicalRange s_biggest_available_memory_range;
static PhysicalAddress s_main_bump_allocation_ptr;
bool s_emergency_kmalloc = true;

static u64 s_kmalloc_eternal_call_count = 0;

void get_kmalloc_stats(kmalloc_stats& stats)
{
    stats.bytes_allocated = (s_biggest_available_memory_range.base_address.offset(s_biggest_available_memory_range.length).get()
        - s_main_bump_allocation_ptr.get());
    stats.bytes_eternal = s_eternal_allocated_count;
    stats.bytes_free = 0;
}

void declare_emergency_kmalloc()
{
    s_emergency_kmalloc = true;
}

namespace std {
const nothrow_t nothrow;
}

// Note: 100 KiB seems like a reasonable amount of memory for a bootloader
// In any case, we can reduce the amount of eternal "heap" and use the e820 memory map
// to find new memory ranges to allocate from.
#define ETERNAL_RANGE_SIZE (100 * KiB)

__attribute__((section(".heap"))) static u8 kmalloc_eternal_heap[ETERNAL_RANGE_SIZE];

void kfree_sized(void*, size_t)
{
}

void kmalloc_init()
{
    VERIFY(Prekernel::MemoryManagement::the().was_initialized());
    auto biggest_available_memory_range_candidate = Prekernel::MemoryManagement::the().try_to_find_the_biggest_available_range();
    VERIFY(biggest_available_memory_range_candidate.has_value());
    auto biggest_available_memory_range = biggest_available_memory_range_candidate.value();
    // Note: This ensures we boot with a machine that has memory above the 1 MiB low memory barrier
    // If we happen to encounter a machine without memory above it, it's probably a mistake in the code
    // or a very old machine we should not care to support.
    VERIFY(biggest_available_memory_range.base_address >= PhysicalAddress((1 * MiB)));
    // Note: This ensures we boot with a machine that has at least 32 MiB of RAM above the 1 MiB low memory
    // barrier. Anything that has less amount of memory than that, is not worth supporting.
    VERIFY(biggest_available_memory_range.length >= (32 * MiB));
    s_biggest_available_memory_range = biggest_available_memory_range;

    // Note: This is essentially a bump allocation that starts at a high address
    // and goes "down" to a lower address on each allocation.
    s_main_bump_allocation_ptr = s_biggest_available_memory_range.base_address.offset(s_biggest_available_memory_range.length);
    dbgln("heap main bump allocation start de-offseting at {}, end at {}", s_main_bump_allocation_ptr, s_biggest_available_memory_range.base_address);
    s_emergency_kmalloc = false;
}

void kmalloc_eternal_init()
{
    // Zero out heap since it's placed after end_of_kernel_bss.
    memset(kmalloc_eternal_heap, 0, sizeof(kmalloc_eternal_heap));

    s_next_eternal_ptr = kmalloc_eternal_heap;
    s_end_of_eternal_range = s_next_eternal_ptr + sizeof(kmalloc_eternal_heap);
}

// Note: We only need a simple "bump allocator", so kmalloc_eternal will provide that for us.
// Note: We don't need a locking because there are no threads in a bootloader.
void* kmalloc_eternal(size_t size)
{
    size = round_up_to_power_of_two(size, sizeof(void*));
    void* ptr = s_next_eternal_ptr;
    s_next_eternal_ptr += size;
    s_eternal_allocated_count += size;
    s_kmalloc_eternal_call_count++;
    // FIXME: Add an assertion to ensure we don't allocate too much in the bootloader.
    return ptr;
}

void* kmalloc(size_t size)
{
    if (s_main_bump_allocation_ptr.is_null() || s_emergency_kmalloc) {
        return kmalloc_eternal(size);
    } else {
        // Note: We simply subtract the amount of allocated bytes and return it
        // This is essentially a bump allocation that starts at a high address
        // and goes "down" to a lower address.

        // Note: Always ensure we are above the base address of the selected range!

        // Note: We can't print anything explictly here (besides assertions), unless we temporarily
        // declare "emergency" kmalloc to ensure we don't recursively call this. For this to happen
        // the following pattern should be used:
        // ```c++
        // s_emergency_kmalloc = true;
        // dbgln("bump at {}", s_main_bump_allocation_ptr);
        // s_emergency_kmalloc = false;
        // ```

        // FIXME: We are not yet ready for providing addresses anywhere we want
        // because we need to handle Virtual memory mappings.
        VERIFY_NOT_REACHED();
        VERIFY(s_main_bump_allocation_ptr > s_biggest_available_memory_range.base_address);
        s_main_bump_allocation_ptr.set(s_main_bump_allocation_ptr.get() - size);
        return s_main_bump_allocation_ptr.as_ptr();
    }
}

void* krealloc(void*, size_t size)
{
    // Just reallocate everything again.
    return kmalloc(size);
}

void kfree(void*)
{
    // Note: We don't really want to mess with freeing resources, because this is a bootloader.
    // Just Don't do anything.
}

void* operator new(size_t size)
{
    void* ptr = kmalloc(size);
    VERIFY(ptr);
    return ptr;
}

void* operator new(size_t size, const std::nothrow_t&) noexcept
{
    return kmalloc(size);
}

void* operator new[](size_t size)
{
    void* ptr = kmalloc(size);
    VERIFY(ptr);
    return ptr;
}

void* operator new[](size_t size, const std::nothrow_t&) noexcept
{
    return kmalloc(size);
}

void operator delete(void* ptr) noexcept
{
    return kfree(ptr);
}

void operator delete(void* ptr, size_t) noexcept
{
    return kfree(ptr);
}

void operator delete[](void* ptr) noexcept
{
    return kfree(ptr);
}

void operator delete[](void* ptr, size_t) noexcept
{
    return kfree(ptr);
}

size_t kmalloc_good_size(size_t size)
{
    return size;
}
