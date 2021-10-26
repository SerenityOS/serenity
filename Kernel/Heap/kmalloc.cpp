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
#include <AK/NonnullOwnPtrVector.h>
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

static void kmalloc_allocate_backup_memory();

struct KmallocGlobalHeap {
    struct ExpandGlobalHeap {
        KmallocGlobalHeap& m_global_heap;

        ExpandGlobalHeap(KmallocGlobalHeap& global_heap)
            : m_global_heap(global_heap)
        {
        }

        bool m_adding { false };
        bool add_memory(size_t allocation_request)
        {
            if (!Memory::MemoryManager::is_initialized()) {
                if constexpr (KMALLOC_DEBUG) {
                    dmesgln("kmalloc: Cannot expand heap before MM is initialized!");
                }
                return false;
            }
            VERIFY(!m_adding);
            TemporaryChange change(m_adding, true);
            // At this point we have very little memory left. Any attempt to
            // kmalloc() could fail, so use our backup memory first, so we
            // can't really reliably allocate even a new region of memory.
            // This is why we keep a backup region, which we can
            auto region = move(m_global_heap.m_backup_memory);
            if (!region) {
                // Be careful to not log too much here. We don't want to trigger
                // any further calls to kmalloc(). We're already out of memory
                // and don't have any backup memory, either!
                if constexpr (KMALLOC_DEBUG) {
                    dmesgln("kmalloc: Cannot expand heap: no backup memory");
                }
                return false;
            }

            // At this point we should have at least enough memory from the
            // backup region to be able to log properly
            if constexpr (KMALLOC_DEBUG) {
                dmesgln("kmalloc: Adding memory to heap at {}, bytes: {}", region->vaddr(), region->size());
            }

            auto& subheap = m_global_heap.m_heap.add_subheap(region->vaddr().as_ptr(), region->size());
            m_global_heap.m_subheap_memory.append(region.release_nonnull());

            // Since we pulled in our backup heap, make sure we allocate another
            // backup heap before returning. Otherwise we potentially lose
            // the ability to expand the heap next time we get called.
            ScopeGuard guard([&]() {
                // We may need to defer allocating backup memory because the
                // heap expansion may have been triggered while holding some
                // other spinlock. If the expansion happens to need the same
                // spinlock we would deadlock. So, if we're in any lock, defer
                Processor::deferred_call_queue(kmalloc_allocate_backup_memory);
            });

            // Now that we added our backup memory, check if the backup heap
            // was big enough to likely satisfy the request
            if (subheap.free_bytes() < allocation_request) {
                // Looks like we probably need more
                size_t memory_size = Memory::page_round_up(decltype(m_global_heap.m_heap)::calculate_memory_for_bytes(allocation_request));
                // Add some more to the new heap. We're already using it for other
                // allocations not including the original allocation_request
                // that triggered heap expansion. If we don't allocate
                memory_size += 1 * MiB;

                auto new_region_or_error = MM.allocate_kernel_region(memory_size, "kmalloc subheap", Memory::Region::Access::ReadWrite, AllocationStrategy::AllocateNow);
                if (new_region_or_error.is_error()) {
                    dbgln("kmalloc: Could not expand heap to satisfy allocation of {} bytes", allocation_request);
                    return false;
                }

                region = new_region_or_error.release_value();
                dbgln("kmalloc: Adding even more memory to heap at {}, bytes: {}", region->vaddr(), region->size());

                m_global_heap.m_heap.add_subheap(region->vaddr().as_ptr(), region->size());
                m_global_heap.m_subheap_memory.append(region.release_nonnull());
            }
            return true;
        }

        bool remove_memory(void* memory)
        {
            // This is actually relatively unlikely to happen, because it requires that all
            // allocated memory in a subheap to be freed. Only then the subheap can be removed...
            for (size_t i = 0; i < m_global_heap.m_subheap_memory.size(); i++) {
                if (m_global_heap.m_subheap_memory[i].vaddr().as_ptr() == memory) {
                    auto region = m_global_heap.m_subheap_memory.take(i);
                    if (!m_global_heap.m_backup_memory) {
                        if constexpr (KMALLOC_DEBUG) {
                            dmesgln("kmalloc: Using removed memory as backup: {}, bytes: {}", region->vaddr(), region->size());
                        }
                        m_global_heap.m_backup_memory = move(region);
                    } else {
                        if constexpr (KMALLOC_DEBUG) {
                            dmesgln("kmalloc: Queue removing memory from heap at {}, bytes: {}", region->vaddr(), region->size());
                        }
                        Processor::deferred_call_queue([this, region = move(region)]() mutable {
                            // We need to defer freeing the region to prevent a potential
                            // deadlock since we are still holding the kmalloc lock
                            // We don't really need to do anything other than holding
                            // onto the region. Unless we already used the backup
                            // memory, in which case we want to use the region as the
                            // new backup.
                            SpinlockLocker lock(s_lock);
                            if (!m_global_heap.m_backup_memory) {
                                if constexpr (KMALLOC_DEBUG) {
                                    dmesgln("kmalloc: Queued memory region at {}, bytes: {} will be used as new backup", region->vaddr(), region->size());
                                }
                                m_global_heap.m_backup_memory = move(region);
                            } else {
                                if constexpr (KMALLOC_DEBUG) {
                                    dmesgln("kmalloc: Queued memory region at {}, bytes: {} will be freed now", region->vaddr(), region->size());
                                }
                            }
                        });
                    }
                    return true;
                }
            }

            if constexpr (KMALLOC_DEBUG) {
                dmesgln("kmalloc: Cannot remove memory from heap: {}", VirtualAddress(memory));
            }
            return false;
        }
    };
    using HeapType = ExpandableHeap<CHUNK_SIZE, KMALLOC_SCRUB_BYTE, KFREE_SCRUB_BYTE, ExpandGlobalHeap>;

    HeapType m_heap;
    NonnullOwnPtrVector<Memory::Region> m_subheap_memory;
    OwnPtr<Memory::Region> m_backup_memory;

    KmallocGlobalHeap(u8* memory, size_t memory_size)
        : m_heap(memory, memory_size, ExpandGlobalHeap(*this))
    {
    }
    void allocate_backup_memory()
    {
        if (m_backup_memory)
            return;
        m_backup_memory = MM.allocate_kernel_region(1 * MiB, "kmalloc subheap", Memory::Region::Access::ReadWrite, AllocationStrategy::AllocateNow).release_value();
    }

    size_t backup_memory_bytes() const
    {
        return m_backup_memory ? m_backup_memory->size() : 0;
    }
};

READONLY_AFTER_INIT static KmallocGlobalHeap* g_kmalloc_global;
alignas(KmallocGlobalHeap) static u8 g_kmalloc_global_heap[sizeof(KmallocGlobalHeap)];

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

static void kmalloc_allocate_backup_memory()
{
    g_kmalloc_global->allocate_backup_memory();
}

void kmalloc_enable_expand()
{
    g_kmalloc_global->allocate_backup_memory();
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
    g_kmalloc_global = new (g_kmalloc_global_heap) KmallocGlobalHeap(kmalloc_pool_heap, sizeof(kmalloc_pool_heap));

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

    void* ptr = g_kmalloc_global->m_heap.allocate(size);

    Thread* current_thread = Thread::current();
    if (!current_thread)
        current_thread = Processor::idle_thread();
    if (current_thread)
        PerformanceManager::add_kmalloc_perf_event(*current_thread, size, (FlatPtr)ptr);

    return ptr;
}

void kfree_sized(void* ptr, size_t size)
{
    (void)size;
    return kfree(ptr);
}

void kfree(void* ptr)
{
    if (!ptr)
        return;

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

    g_kmalloc_global->m_heap.deallocate(ptr);
    --g_nested_kfree_calls;
}

size_t kmalloc_good_size(size_t size)
{
    return size;
}

[[gnu::malloc, gnu::alloc_size(1), gnu::alloc_align(2)]] static void* kmalloc_aligned_cxx(size_t size, size_t alignment)
{
    VERIFY(alignment <= 4096);
    void* ptr = kmalloc(size + alignment + sizeof(ptrdiff_t));
    if (ptr == nullptr)
        return nullptr;
    size_t max_addr = (size_t)ptr + alignment;
    void* aligned_ptr = (void*)(max_addr - (max_addr % alignment));
    ((ptrdiff_t*)aligned_ptr)[-1] = (ptrdiff_t)((u8*)aligned_ptr - (u8*)ptr);
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
    void* ptr = kmalloc_aligned_cxx(size, (size_t)al);
    VERIFY(ptr);
    return ptr;
}

void* operator new(size_t size, std::align_val_t al, const std::nothrow_t&) noexcept
{
    return kmalloc_aligned_cxx(size, (size_t)al);
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
    stats.bytes_allocated = g_kmalloc_global->m_heap.allocated_bytes();
    stats.bytes_free = g_kmalloc_global->m_heap.free_bytes() + g_kmalloc_global->backup_memory_bytes();
    stats.bytes_eternal = g_kmalloc_bytes_eternal;
    stats.kmalloc_call_count = g_kmalloc_call_count;
    stats.kfree_call_count = g_kfree_call_count;
}
