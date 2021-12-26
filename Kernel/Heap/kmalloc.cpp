/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

/*
 * Really really *really* Q&D malloc() and free() implementations
 * just to get going. Don't ever let anyone see this shit. :^)
 */

#include <AK/Assertions.h>
#include <AK/Types.h>
#include <Kernel/Debug.h>
#include <Kernel/Heap/Heap.h>
#include <Kernel/Heap/kmalloc.h>
#include <Kernel/KSyms.h>
#include <Kernel/Locking/Spinlock.h>
#include <Kernel/Memory/MemoryManager.h>
#include <Kernel/Panic.h>
#include <Kernel/PerformanceManager.h>
#include <Kernel/Sections.h>
#include <Kernel/StdLib.h>

#if ARCH(I386)
static constexpr size_t CHUNK_SIZE = 32;
#else
static constexpr size_t CHUNK_SIZE = 64;
#endif

#define POOL_SIZE (2 * MiB)
#define ETERNAL_RANGE_SIZE (4 * MiB)

namespace std {
const nothrow_t nothrow;
}

static RecursiveSpinlock s_lock; // needs to be recursive because of dump_backtrace()

struct KmallocSubheap {
    KmallocSubheap(u8* base, size_t size)
        : allocator(base, size)
    {
    }

    IntrusiveListNode<KmallocSubheap> list_node;
    Heap<CHUNK_SIZE, KMALLOC_SCRUB_BYTE, KFREE_SCRUB_BYTE> allocator;
};

struct KmallocGlobalData {
    static constexpr size_t minimum_subheap_size = 1 * MiB;

    KmallocGlobalData(u8* initial_heap, size_t initial_heap_size)
    {
        add_subheap(initial_heap, initial_heap_size);
    }

    void add_subheap(u8* storage, size_t storage_size)
    {
        dbgln("Adding kmalloc subheap @ {} with size {}", storage, storage_size);
        static_assert(sizeof(KmallocSubheap) <= PAGE_SIZE);
        auto* subheap = new (storage) KmallocSubheap(storage + PAGE_SIZE, storage_size - PAGE_SIZE);
        subheaps.append(*subheap);
    }

    void* allocate(size_t size)
    {
        VERIFY(!expansion_in_progress);

        for (auto& subheap : subheaps) {
            if (auto* ptr = subheap.allocator.allocate(size))
                return ptr;
        }

        if (!try_expand(size)) {
            PANIC("OOM when trying to expand kmalloc heap.");
        }

        return allocate(size);
    }

    void deallocate(void* ptr, size_t size)
    {
        VERIFY(!expansion_in_progress);

        for (auto& subheap : subheaps) {
            if (subheap.allocator.contains(ptr)) {
                subheap.allocator.deallocate(ptr);
                return;
            }
        }

        PANIC("Bogus pointer passed to kfree_sized({:p}, {})", ptr, size);
    }

    size_t allocated_bytes() const
    {
        size_t total = 0;
        for (auto const& subheap : subheaps)
            total += subheap.allocator.allocated_bytes();
        return total;
    }

    size_t free_bytes() const
    {
        size_t total = 0;
        for (auto const& subheap : subheaps)
            total += subheap.allocator.free_bytes();
        return total;
    }

    bool try_expand(size_t allocation_request)
    {
        VERIFY(!expansion_in_progress);
        TemporaryChange change(expansion_in_progress, true);

        auto new_subheap_base = expansion_data->next_virtual_address;
        Checked<size_t> padded_allocation_request = allocation_request;
        padded_allocation_request *= 2;
        padded_allocation_request += PAGE_SIZE;
        if (padded_allocation_request.has_overflow()) {
            PANIC("Integer overflow during kmalloc heap expansion");
        }
        size_t new_subheap_size = max(minimum_subheap_size, Memory::page_round_up(padded_allocation_request.value()));

        dbgln("Unable to allocate {}, expanding kmalloc heap", allocation_request);

        if (!expansion_data->virtual_range.contains(new_subheap_base, new_subheap_size)) {
            // FIXME: Dare to return false and allow kmalloc() to fail!
            PANIC("Out of address space when expanding kmalloc heap.");
        }

        auto physical_pages_or_error = MM.commit_user_physical_pages(new_subheap_size / PAGE_SIZE);
        if (physical_pages_or_error.is_error()) {
            // FIXME: Dare to return false!
            PANIC("Out of physical pages when expanding kmalloc heap.");
        }
        auto physical_pages = physical_pages_or_error.release_value();

        expansion_data->next_virtual_address = expansion_data->next_virtual_address.offset(new_subheap_size);

        auto cpu_supports_nx = Processor::current().has_feature(CPUFeature::NX);

        SpinlockLocker mm_locker(Memory::s_mm_lock);
        SpinlockLocker pd_locker(MM.kernel_page_directory().get_lock());

        for (auto vaddr = new_subheap_base; !physical_pages.is_empty(); vaddr = vaddr.offset(PAGE_SIZE)) {
            // FIXME: We currently leak physical memory when mapping it into the kmalloc heap.
            auto& page = physical_pages.take_one().leak_ref();
            auto* pte = MM.pte(MM.kernel_page_directory(), vaddr);
            VERIFY(pte);
            pte->set_physical_page_base(page.paddr().get());
            pte->set_global(true);
            pte->set_user_allowed(false);
            pte->set_writable(true);
            if (cpu_supports_nx)
                pte->set_execute_disabled(true);
            pte->set_present(true);
        }

        MM.flush_tlb(&MM.kernel_page_directory(), new_subheap_base, new_subheap_size / PAGE_SIZE);

        add_subheap(new_subheap_base.as_ptr(), new_subheap_size);
        return true;
    }

    void enable_expansion()
    {
        // FIXME: This range can be much bigger on 64-bit, but we need to figure something out for 32-bit.
        auto virtual_range = MM.kernel_page_directory().range_allocator().try_allocate_anywhere(64 * MiB, 1 * MiB);

        expansion_data = KmallocGlobalData::ExpansionData {
            .virtual_range = virtual_range.value(),
            .next_virtual_address = virtual_range.value().base(),
        };

        // Make sure the entire kmalloc VM range is backed by page tables.
        // This avoids having to deal with lazy page table allocation during heap expansion.
        SpinlockLocker mm_locker(Memory::s_mm_lock);
        SpinlockLocker pd_locker(MM.kernel_page_directory().get_lock());
        for (auto vaddr = virtual_range.value().base(); vaddr < virtual_range.value().end(); vaddr = vaddr.offset(PAGE_SIZE)) {
            MM.ensure_pte(MM.kernel_page_directory(), vaddr);
        }
    }

    struct ExpansionData {
        Memory::VirtualRange virtual_range;
        VirtualAddress next_virtual_address;
    };
    Optional<ExpansionData> expansion_data;

    IntrusiveList<&KmallocSubheap::list_node> subheaps;

    bool expansion_in_progress { false };
};

READONLY_AFTER_INIT static KmallocGlobalData* g_kmalloc_global;
alignas(KmallocGlobalData) static u8 g_kmalloc_global_heap[sizeof(KmallocGlobalData)];

// Treat the heap as logically separate from .bss
__attribute__((section(".heap"))) static u8 kmalloc_eternal_heap[ETERNAL_RANGE_SIZE];
__attribute__((section(".heap"))) static u8 kmalloc_pool_heap[POOL_SIZE];

static size_t g_kmalloc_bytes_eternal = 0;
static size_t g_kmalloc_call_count;
static size_t g_kfree_call_count;
static size_t g_nested_kfree_calls;
bool g_dump_kmalloc_stacks;

static u8* s_next_eternal_ptr;
READONLY_AFTER_INIT static u8* s_end_of_eternal_range;

void kmalloc_enable_expand()
{
    g_kmalloc_global->enable_expansion();
}

static inline void kmalloc_verify_nospinlock_held()
{
    // Catch bad callers allocating under spinlock.
    if constexpr (KMALLOC_VERIFY_NO_SPINLOCK_HELD) {
        VERIFY(!Processor::in_critical());
    }
}

UNMAP_AFTER_INIT void kmalloc_init()
{
    // Zero out heap since it's placed after end_of_kernel_bss.
    memset(kmalloc_eternal_heap, 0, sizeof(kmalloc_eternal_heap));
    memset(kmalloc_pool_heap, 0, sizeof(kmalloc_pool_heap));
    g_kmalloc_global = new (g_kmalloc_global_heap) KmallocGlobalData(kmalloc_pool_heap, sizeof(kmalloc_pool_heap));

    s_lock.initialize();

    s_next_eternal_ptr = kmalloc_eternal_heap;
    s_end_of_eternal_range = s_next_eternal_ptr + sizeof(kmalloc_eternal_heap);
}

void* kmalloc_eternal(size_t size)
{
    kmalloc_verify_nospinlock_held();

    size = round_up_to_power_of_two(size, sizeof(void*));

    SpinlockLocker lock(s_lock);
    void* ptr = s_next_eternal_ptr;
    s_next_eternal_ptr += size;
    VERIFY(s_next_eternal_ptr < s_end_of_eternal_range);
    g_kmalloc_bytes_eternal += size;
    return ptr;
}

void* kmalloc(size_t size)
{
    kmalloc_verify_nospinlock_held();
    SpinlockLocker lock(s_lock);
    ++g_kmalloc_call_count;

    if (g_dump_kmalloc_stacks && Kernel::g_kernel_symbols_available) {
        dbgln("kmalloc({})", size);
        Kernel::dump_backtrace();
    }

    void* ptr = g_kmalloc_global->allocate(size);

    Thread* current_thread = Thread::current();
    if (!current_thread)
        current_thread = Processor::idle_thread();
    if (current_thread)
        PerformanceManager::add_kmalloc_perf_event(*current_thread, size, (FlatPtr)ptr);

    return ptr;
}

void kfree_sized(void* ptr, size_t size)
{
    if (!ptr)
        return;

    VERIFY(size > 0);

    kmalloc_verify_nospinlock_held();
    SpinlockLocker lock(s_lock);
    ++g_kfree_call_count;
    ++g_nested_kfree_calls;

    if (g_nested_kfree_calls == 1) {
        Thread* current_thread = Thread::current();
        if (!current_thread)
            current_thread = Processor::idle_thread();
        if (current_thread)
            PerformanceManager::add_kfree_perf_event(*current_thread, 0, (FlatPtr)ptr);
    }

    g_kmalloc_global->deallocate(ptr, size);
    --g_nested_kfree_calls;
}

size_t kmalloc_good_size(size_t size)
{
    return size;
}

void* kmalloc_aligned(size_t size, size_t alignment)
{
    VERIFY(alignment <= 4096);
    Checked<size_t> real_allocation_size = size;
    real_allocation_size += alignment;
    real_allocation_size += sizeof(ptrdiff_t) + sizeof(size_t);
    void* ptr = kmalloc(real_allocation_size.value());
    if (ptr == nullptr)
        return nullptr;
    size_t max_addr = (size_t)ptr + alignment;
    void* aligned_ptr = (void*)(max_addr - (max_addr % alignment));
    ((ptrdiff_t*)aligned_ptr)[-1] = (ptrdiff_t)((u8*)aligned_ptr - (u8*)ptr);
    ((size_t*)aligned_ptr)[-2] = real_allocation_size.value();
    return aligned_ptr;
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

void* operator new(size_t size, std::align_val_t al)
{
    void* ptr = kmalloc_aligned(size, (size_t)al);
    VERIFY(ptr);
    return ptr;
}

void* operator new(size_t size, std::align_val_t al, const std::nothrow_t&) noexcept
{
    return kmalloc_aligned(size, (size_t)al);
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

void operator delete(void*) noexcept
{
    // All deletes in kernel code should have a known size.
    VERIFY_NOT_REACHED();
}

void operator delete(void* ptr, size_t size) noexcept
{
    return kfree_sized(ptr, size);
}

void operator delete(void* ptr, size_t, std::align_val_t) noexcept
{
    return kfree_aligned(ptr);
}

void operator delete[](void*) noexcept
{
    // All deletes in kernel code should have a known size.
    VERIFY_NOT_REACHED();
}

void operator delete[](void* ptr, size_t size) noexcept
{
    return kfree_sized(ptr, size);
}

void get_kmalloc_stats(kmalloc_stats& stats)
{
    SpinlockLocker lock(s_lock);
    stats.bytes_allocated = g_kmalloc_global->allocated_bytes();
    stats.bytes_free = g_kmalloc_global->free_bytes();
    stats.bytes_eternal = g_kmalloc_bytes_eternal;
    stats.kmalloc_call_count = g_kmalloc_call_count;
    stats.kfree_call_count = g_kfree_call_count;
}
