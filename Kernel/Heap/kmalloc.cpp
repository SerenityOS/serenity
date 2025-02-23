/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Assertions.h>
#include <AK/Types.h>
#include <Kernel/Arch/PageDirectory.h>
#include <Kernel/Debug.h>
#include <Kernel/Heap/Heap.h>
#include <Kernel/Heap/kmalloc.h>
#include <Kernel/KSyms.h>
#include <Kernel/Library/Panic.h>
#include <Kernel/Library/StdLib.h>
#include <Kernel/Locking/Spinlock.h>
#include <Kernel/Memory/MemoryManager.h>
#include <Kernel/Sections.h>
#include <Kernel/Security/AddressSanitizer.h>
#include <Kernel/Tasks/PerformanceManager.h>

#if ARCH(X86_64) || ARCH(AARCH64) || ARCH(RISCV64)
static constexpr size_t CHUNK_SIZE = 64;
#else
#    error Unknown architecture
#endif
static_assert(is_power_of_two(CHUNK_SIZE));

static constexpr size_t INITIAL_KMALLOC_MEMORY_SIZE = 16 * MiB;
static constexpr size_t KMALLOC_DEFAULT_ALIGNMENT = 16;

// Treat the heap as logically separate from .bss
__attribute__((section(".heap"))) static u8 initial_kmalloc_memory[INITIAL_KMALLOC_MEMORY_SIZE];

namespace std {
nothrow_t const nothrow;
}

// FIXME: Figure out whether this can be MemoryManager.
static RecursiveSpinlock<LockRank::None> s_lock {}; // needs to be recursive because of dump_backtrace()

struct KmallocSubheap {
    KmallocSubheap(u8* base, size_t size)
        : allocator(base, size)
    {
    }

    IntrusiveListNode<KmallocSubheap> list_node;
    using List = IntrusiveList<&KmallocSubheap::list_node>;
    Heap<CHUNK_SIZE, KMALLOC_SCRUB_BYTE, KFREE_SCRUB_BYTE> allocator;
};

class KmallocSlabBlock {
public:
    static constexpr size_t block_size = 64 * KiB;
    static constexpr FlatPtr block_mask = ~(block_size - 1);

    KmallocSlabBlock(size_t slab_size)
        : m_slab_size(slab_size)
        , m_slab_count((block_size - sizeof(KmallocSlabBlock)) / slab_size)
    {
        for (size_t i = 0; i < m_slab_count; ++i) {
            auto* freelist_entry = (FreelistEntry*)(void*)(&m_data[i * slab_size]);
            freelist_entry->next = m_freelist;
            m_freelist = freelist_entry;
        }
    }

    void* allocate([[maybe_unused]] size_t requested_size)
    {
        VERIFY(m_freelist);
        ++m_allocated_slabs;
#ifdef HAS_ADDRESS_SANITIZER
        AddressSanitizer::fill_shadow((FlatPtr)m_freelist, sizeof(FreelistEntry::next), Kernel::AddressSanitizer::ShadowType::Unpoisoned8Bytes);
#endif
        auto* ptr = exchange(m_freelist, m_freelist->next);
#ifdef HAS_ADDRESS_SANITIZER
        AddressSanitizer::mark_region((FlatPtr)ptr, requested_size, m_slab_size, AddressSanitizer::ShadowType::Malloc);
#endif
        return ptr;
    }

    void deallocate(void* ptr)
    {
        VERIFY(ptr >= &m_data && ptr < ((u8*)this + block_size));
        --m_allocated_slabs;
        auto* freelist_entry = (FreelistEntry*)ptr;
#ifdef HAS_ADDRESS_SANITIZER
        AddressSanitizer::fill_shadow((FlatPtr)freelist_entry, sizeof(FreelistEntry::next), Kernel::AddressSanitizer::ShadowType::Unpoisoned8Bytes);
#endif
        freelist_entry->next = m_freelist;
#ifdef HAS_ADDRESS_SANITIZER
        AddressSanitizer::fill_shadow((FlatPtr)freelist_entry, m_slab_size, AddressSanitizer::ShadowType::Free);
#endif
        m_freelist = freelist_entry;
    }

    bool is_full() const
    {
        return m_freelist == nullptr;
    }

    size_t allocated_bytes() const
    {
        return m_allocated_slabs * m_slab_size;
    }

    size_t free_bytes() const
    {
        return (m_slab_count - m_allocated_slabs) * m_slab_size;
    }

    IntrusiveListNode<KmallocSlabBlock> list_node;
    using List = IntrusiveList<&KmallocSlabBlock::list_node>;

private:
    struct FreelistEntry {
        FreelistEntry* next;
    };

    FreelistEntry* m_freelist { nullptr };

    size_t m_slab_size { 0 };
    size_t m_slab_count { 0 };
    size_t m_allocated_slabs { 0 };

    [[gnu::aligned(16)]] u8 m_data[];
};

class KmallocSlabheap {
public:
    KmallocSlabheap(size_t slab_size)
        : m_slab_size(slab_size)
    {
    }

    size_t slab_size() const { return m_slab_size; }

    void* allocate(size_t requested_size, [[maybe_unused]] CallerWillInitializeMemory caller_will_initialize_memory)
    {
        if (m_usable_blocks.is_empty()) {
            // FIXME: This allocation wastes `block_size` bytes due to the implementation of kmalloc_aligned().
            //        Handle this with a custom VM+page allocator instead of using kmalloc_aligned().
            auto* slot = kmalloc_aligned(KmallocSlabBlock::block_size, KmallocSlabBlock::block_size);
            if (!slot) {
                dbgln_if(KMALLOC_DEBUG, "OOM while growing slabheap ({})", m_slab_size);
                return nullptr;
            }
            auto* block = new (slot) KmallocSlabBlock(m_slab_size);
            m_usable_blocks.append(*block);
        }
        auto* block = m_usable_blocks.first();
        auto* ptr = block->allocate(requested_size);
        if (block->is_full())
            m_full_blocks.append(*block);

#ifndef HAS_ADDRESS_SANITIZER
        if (caller_will_initialize_memory == CallerWillInitializeMemory::No) {
            memset(ptr, KMALLOC_SCRUB_BYTE, m_slab_size);
        }
#endif
        return ptr;
    }

    void deallocate(void* ptr)
    {
#ifndef HAS_ADDRESS_SANITIZER
        memset(ptr, KFREE_SCRUB_BYTE, m_slab_size);
#endif

        auto* block = (KmallocSlabBlock*)((FlatPtr)ptr & KmallocSlabBlock::block_mask);
        bool block_was_full = block->is_full();
        block->deallocate(ptr);
        if (block_was_full)
            m_usable_blocks.append(*block);
    }

    size_t allocated_bytes() const
    {
        size_t total = m_full_blocks.size_slow() * KmallocSlabBlock::block_size;
        for (auto const& slab_block : m_usable_blocks)
            total += slab_block.allocated_bytes();
        return total;
    }

    size_t free_bytes() const
    {
        size_t total = 0;
        for (auto const& slab_block : m_usable_blocks)
            total += slab_block.free_bytes();
        return total;
    }

    bool try_purge()
    {
        bool did_purge = false;

        // Note: We cannot remove children from the list when using a structured loop,
        //       Because we need to advance the iterator before we delete the underlying
        //       value, so we have to iterate manually

        auto block = m_usable_blocks.begin();
        while (block != m_usable_blocks.end()) {
            if (block->allocated_bytes() != 0) {
                ++block;
                continue;
            }
            auto& block_to_remove = *block;
            ++block;
            block_to_remove.list_node.remove();
            block_to_remove.~KmallocSlabBlock();
            kfree_sized(&block_to_remove, KmallocSlabBlock::block_size);

            did_purge = true;
        }
        return did_purge;
    }

private:
    size_t m_slab_size { 0 };

    KmallocSlabBlock::List m_usable_blocks;
    KmallocSlabBlock::List m_full_blocks;
};

struct KmallocGlobalData {
    static constexpr size_t minimum_subheap_size = 1 * MiB;

    KmallocGlobalData(u8* initial_heap, size_t initial_heap_size)
    {
        add_subheap(initial_heap, initial_heap_size);
    }

    void add_subheap(u8* storage, size_t storage_size)
    {
        dbgln_if(KMALLOC_DEBUG, "Adding kmalloc subheap @ {} with size {}", storage, storage_size);
        static_assert(sizeof(KmallocSubheap) <= PAGE_SIZE);
        auto* subheap = new (storage) KmallocSubheap(storage + PAGE_SIZE, storage_size - PAGE_SIZE);
        subheaps.append(*subheap);
    }

    void* allocate(size_t size, size_t alignment, CallerWillInitializeMemory caller_will_initialize_memory)
    {
        VERIFY(!expansion_in_progress);

        for (auto& slabheap : slabheaps) {
            if (size <= slabheap.slab_size() && alignment <= slabheap.slab_size())
                return slabheap.allocate(size, caller_will_initialize_memory);
        }

        for (auto& subheap : subheaps) {
            if (auto* ptr = subheap.allocator.allocate(size, alignment, caller_will_initialize_memory))
                return ptr;
        }

        // NOTE: This size calculation is a mirror of kmalloc_aligned(KmallocSlabBlock)
        if (size <= KmallocSlabBlock::block_size * 2 + sizeof(ptrdiff_t) + sizeof(size_t)) {
            // FIXME: We should propagate a freed pointer, to find the specific subheap it belonged to
            //        This would save us iterating over them in the next step and remove a recursion
            bool did_purge = false;
            for (auto& slabheap : slabheaps) {
                if (slabheap.try_purge()) {
                    dbgln_if(KMALLOC_DEBUG, "Kmalloc purged block(s) from slabheap of size {} to avoid expansion", slabheap.slab_size());
                    did_purge = true;
                    break;
                }
            }
            if (did_purge)
                return allocate(size, alignment, caller_will_initialize_memory);
        }

        if (!try_expand(size)) {
            dbgln_if(KMALLOC_DEBUG, "OOM when trying to expand kmalloc heap");
            return nullptr;
        }

        return allocate(size, alignment, caller_will_initialize_memory);
    }

    void deallocate(void* ptr, size_t size)
    {
        VERIFY(!expansion_in_progress);
        VERIFY(is_valid_kmalloc_address(VirtualAddress { ptr }));

        for (auto& slabheap : slabheaps) {
            if (size <= slabheap.slab_size())
                return slabheap.deallocate(ptr);
        }

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
        for (auto const& slabheap : slabheaps)
            total += slabheap.allocated_bytes();
        return total;
    }

    size_t free_bytes() const
    {
        size_t total = 0;
        for (auto const& subheap : subheaps)
            total += subheap.allocator.free_bytes();
        for (auto const& slabheap : slabheaps)
            total += slabheap.free_bytes();
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
        auto rounded_allocation_request = Memory::page_round_up(padded_allocation_request.value());
        if (rounded_allocation_request.is_error()) {
            PANIC("Integer overflow computing pages for kmalloc heap expansion");
        }
        size_t new_subheap_size = max(minimum_subheap_size, rounded_allocation_request.value());

        dbgln_if(KMALLOC_DEBUG, "Unable to allocate {}, expanding kmalloc heap", allocation_request);

        if (!expansion_data->virtual_range.contains(new_subheap_base, new_subheap_size)) {
            dbgln_if(KMALLOC_DEBUG, "Out of address space when expanding kmalloc heap");
            return false;
        }

        auto physical_pages_or_error = MM.commit_physical_pages(new_subheap_size / PAGE_SIZE);
        if (physical_pages_or_error.is_error()) {
            dbgln_if(KMALLOC_DEBUG, "Out of address space when expanding kmalloc heap");
            return false;
        }
        auto physical_pages = physical_pages_or_error.release_value();

        expansion_data->next_virtual_address = expansion_data->next_virtual_address.offset(new_subheap_size);

        auto cpu_supports_nx = Processor::current().has_nx();

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

        add_subheap(new_subheap_base.as_ptr(), new_subheap_size);
        return true;
    }

    void enable_expansion()
    {
        // FIXME: This range can be much bigger on 64-bit, but we need to figure something out for 32-bit.
        auto reserved_region = MUST(MM.allocate_unbacked_region_anywhere(64 * MiB, 1 * MiB));

        expansion_data = KmallocGlobalData::ExpansionData {
            .virtual_range = reserved_region->range(),
            .next_virtual_address = reserved_region->range().base(),
        };

        // Make sure the entire kmalloc VM range is backed by page tables.
        // This avoids having to deal with lazy page table allocation during heap expansion.
        SpinlockLocker pd_locker(MM.kernel_page_directory().get_lock());
        for (auto vaddr = reserved_region->range().base(); vaddr < reserved_region->range().end(); vaddr = vaddr.offset(PAGE_SIZE)) {
            MM.ensure_pte(MM.kernel_page_directory(), vaddr);
        }

        (void)reserved_region.leak_ptr();
    }

    struct ExpansionData {
        Memory::VirtualRange virtual_range;
        VirtualAddress next_virtual_address;
    };
    Optional<ExpansionData> expansion_data;

    bool is_valid_kmalloc_address(VirtualAddress vaddr) const
    {
        if (vaddr.as_ptr() >= initial_kmalloc_memory && vaddr.as_ptr() < (initial_kmalloc_memory + INITIAL_KMALLOC_MEMORY_SIZE))
            return true;

        if (!expansion_data.has_value())
            return false;

        return expansion_data->virtual_range.contains(vaddr);
    }

    KmallocSubheap::List subheaps;

    KmallocSlabheap slabheaps[6] = { 16, 32, 64, 128, 256, 512 };

    bool expansion_in_progress { false };
};

READONLY_AFTER_INIT static KmallocGlobalData* g_kmalloc_global;
alignas(KmallocGlobalData) static u8 g_kmalloc_global_heap[sizeof(KmallocGlobalData)];

static size_t g_kmalloc_call_count;
static size_t g_kfree_call_count;
static size_t g_nested_kfree_calls;
bool g_dump_kmalloc_stacks;

void kmalloc_enable_expand()
{
    g_kmalloc_global->enable_expansion();
}

UNMAP_AFTER_INIT void kmalloc_init()
{
    // Zero out heap since it's placed after end_of_kernel_bss.
    memset(initial_kmalloc_memory, 0, sizeof(initial_kmalloc_memory));
    g_kmalloc_global = new (g_kmalloc_global_heap) KmallocGlobalData(initial_kmalloc_memory, sizeof(initial_kmalloc_memory));

    s_lock.initialize();
}

static void* kmalloc_impl(size_t size, size_t alignment, CallerWillInitializeMemory caller_will_initialize_memory)
{
    // Catch bad callers allocating under spinlock.
    if constexpr (KMALLOC_VERIFY_NO_SPINLOCK_HELD) {
        Processor::verify_no_spinlocks_held();
    }

    // Alignment must be a power of two.
    VERIFY(is_power_of_two(alignment));

    SpinlockLocker lock(s_lock);
    ++g_kmalloc_call_count;

    if (g_dump_kmalloc_stacks && Kernel::g_kernel_symbols_available.was_set()) {
        dbgln("kmalloc({})", size);
        Kernel::dump_backtrace();
    }

    void* ptr = g_kmalloc_global->allocate(size, alignment, caller_will_initialize_memory);

    Thread* current_thread = Thread::current();
    if (!current_thread)
        current_thread = Processor::idle_thread();
    if (current_thread) {
        // FIXME: By the time we check this, we have already allocated above.
        //        This means that in the case of an infinite recursion, we can't catch it this way.
        VERIFY(current_thread->is_allocation_enabled());
        PerformanceManager::add_kmalloc_perf_event(*current_thread, size, (FlatPtr)ptr);
    }

    return ptr;
}

void* kmalloc(size_t size)
{
    return kmalloc_impl(size, KMALLOC_DEFAULT_ALIGNMENT, CallerWillInitializeMemory::No);
}

void* kcalloc(size_t count, size_t size)
{
    if (Checked<size_t>::multiplication_would_overflow(count, size))
        return nullptr;
    size_t new_size = count * size;
    auto* ptr = kmalloc_impl(new_size, KMALLOC_DEFAULT_ALIGNMENT, CallerWillInitializeMemory::Yes);
    if (ptr)
        memset(ptr, 0, new_size);
    return ptr;
}

void kfree_sized(void* ptr, size_t size)
{
    if (!ptr)
        return;

    VERIFY(size > 0);

    // Catch bad callers allocating under spinlock.
    if constexpr (KMALLOC_VERIFY_NO_SPINLOCK_HELD) {
        Processor::verify_no_spinlocks_held();
    }

    SpinlockLocker lock(s_lock);
    ++g_kfree_call_count;
    ++g_nested_kfree_calls;

    if (g_nested_kfree_calls == 1) {
        Thread* current_thread = Thread::current();
        if (!current_thread)
            current_thread = Processor::idle_thread();
        if (current_thread) {
            VERIFY(current_thread->is_allocation_enabled());
            PerformanceManager::add_kfree_perf_event(*current_thread, 0, (FlatPtr)ptr);
        }
    }

    g_kmalloc_global->deallocate(ptr, size);
    --g_nested_kfree_calls;
}

size_t kmalloc_good_size(size_t size)
{
    VERIFY(size > 0);
    // NOTE: There's no need to take the kmalloc lock, as the kmalloc slab-heaps (and their sizes) are constant
    for (auto const& slabheap : g_kmalloc_global->slabheaps) {
        if (size <= slabheap.slab_size())
            return slabheap.slab_size();
    }
    return round_up_to_power_of_two(size + Heap<CHUNK_SIZE>::AllocationHeaderSize, CHUNK_SIZE) - Heap<CHUNK_SIZE>::AllocationHeaderSize;
}

void* kmalloc_aligned(size_t size, size_t alignment)
{
    return kmalloc_impl(size, alignment, CallerWillInitializeMemory::No);
}

void* operator new(size_t size)
{
    void* ptr = kmalloc(size);
    VERIFY(ptr);
    return ptr;
}

void* operator new(size_t size, std::nothrow_t const&) noexcept
{
    return kmalloc(size);
}

void* operator new(size_t size, std::align_val_t al)
{
    void* ptr = kmalloc_aligned(size, (size_t)al);
    VERIFY(ptr);
    return ptr;
}

void* operator new(size_t size, std::align_val_t al, std::nothrow_t const&) noexcept
{
    return kmalloc_aligned(size, (size_t)al);
}

void* operator new[](size_t size)
{
    void* ptr = kmalloc(size);
    VERIFY(ptr);
    return ptr;
}

void* operator new[](size_t size, std::nothrow_t const&) noexcept
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

void operator delete(void* ptr, size_t size, std::align_val_t) noexcept
{
    return kfree_sized(ptr, size);
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
    stats.kmalloc_call_count = g_kmalloc_call_count;
    stats.kfree_call_count = g_kfree_call_count;
}
