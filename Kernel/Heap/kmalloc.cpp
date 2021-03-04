/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
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

/*
 * Really really *really* Q&D malloc() and free() implementations
 * just to get going. Don't ever let anyone see this shit. :^)
 */

#include <AK/Assertions.h>
#include <AK/NonnullOwnPtrVector.h>
#include <AK/Optional.h>
#include <AK/StringView.h>
#include <AK/Types.h>
#include <Kernel/Arch/i386/CPU.h>
#include <Kernel/Heap/Heap.h>
#include <Kernel/Heap/kmalloc.h>
#include <Kernel/KSyms.h>
#include <Kernel/Panic.h>
#include <Kernel/Process.h>
#include <Kernel/Scheduler.h>
#include <Kernel/SpinLock.h>
#include <Kernel/StdLib.h>
#include <Kernel/VM/MemoryManager.h>

#define SANITIZE_KMALLOC

#define CHUNK_SIZE 32
#define POOL_SIZE (2 * MiB)
#define ETERNAL_RANGE_SIZE (2 * MiB)

static RecursiveSpinLock s_lock; // needs to be recursive because of dump_backtrace()

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
            if (!MemoryManager::is_initialized()) {
                klog() << "kmalloc(): Cannot expand heap before MM is initialized!";
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
                klog() << "kmalloc(): Cannot expand heap: no backup memory";
                return false;
            }

            // At this point we should have at least enough memory from the
            // backup region to be able to log properly
            klog() << "kmalloc(): Adding memory to heap at " << region->vaddr() << ", bytes: " << region->size();

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
                Processor::current().deferred_call_queue(kmalloc_allocate_backup_memory);
            });

            // Now that we added our backup memory, check if the backup heap
            // was big enough to likely satisfy the request
            if (subheap.free_bytes() < allocation_request) {
                // Looks like we probably need more
                size_t memory_size = page_round_up(decltype(m_global_heap.m_heap)::calculate_memory_for_bytes(allocation_request));
                // Add some more to the new heap. We're already using it for other
                // allocations not including the original allocation_request
                // that triggered heap expansion. If we don't allocate
                memory_size += 1 * MiB;
                region = MM.allocate_kernel_region(memory_size, "kmalloc subheap", Region::Access::Read | Region::Access::Write, AllocationStrategy::AllocateNow);
                if (region) {
                    klog() << "kmalloc(): Adding even more memory to heap at " << region->vaddr() << ", bytes: " << region->size();

                    m_global_heap.m_heap.add_subheap(region->vaddr().as_ptr(), region->size());
                    m_global_heap.m_subheap_memory.append(region.release_nonnull());
                } else {
                    klog() << "kmalloc(): Could not expand heap to satisfy allocation of " << allocation_request << " bytes";
                    return false;
                }
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
                        klog() << "kmalloc(): Using removed memory as backup: " << region->vaddr() << ", bytes: " << region->size();
                        m_global_heap.m_backup_memory = move(region);
                    } else {
                        klog() << "kmalloc(): Queue removing memory from heap at " << region->vaddr() << ", bytes: " << region->size();
                        Processor::deferred_call_queue([this, region = move(region)]() mutable {
                            // We need to defer freeing the region to prevent a potential
                            // deadlock since we are still holding the kmalloc lock
                            // We don't really need to do anything other than holding
                            // onto the region. Unless we already used the backup
                            // memory, in which case we want to use the region as the
                            // new backup.
                            ScopedSpinLock lock(s_lock);
                            if (!m_global_heap.m_backup_memory) {
                                klog() << "kmalloc(): Queued memory region at " << region->vaddr() << ", bytes: " << region->size() << " will be used as new backup";
                                m_global_heap.m_backup_memory = move(region);
                            } else {
                                klog() << "kmalloc(): Queued memory region at " << region->vaddr() << ", bytes: " << region->size() << " will be freed now";
                            }
                        });
                    }
                    return true;
                }
            }

            klog() << "kmalloc(): Cannot remove memory from heap: " << VirtualAddress(memory);
            return false;
        }
    };
    typedef ExpandableHeap<CHUNK_SIZE, KMALLOC_SCRUB_BYTE, KFREE_SCRUB_BYTE, ExpandGlobalHeap> HeapType;

    HeapType m_heap;
    NonnullOwnPtrVector<Region> m_subheap_memory;
    OwnPtr<Region> m_backup_memory;

    KmallocGlobalHeap(u8* memory, size_t memory_size)
        : m_heap(memory, memory_size, ExpandGlobalHeap(*this))
    {
    }
    void allocate_backup_memory()
    {
        if (m_backup_memory)
            return;
        m_backup_memory = MM.allocate_kernel_region(1 * MiB, "kmalloc subheap", Region::Access::Read | Region::Access::Write, AllocationStrategy::AllocateNow);
    }

    size_t backup_memory_bytes() const
    {
        return m_backup_memory ? m_backup_memory->size() : 0;
    }
};

READONLY_AFTER_INIT static KmallocGlobalHeap* g_kmalloc_global;
static u8 g_kmalloc_global_heap[sizeof(KmallocGlobalHeap)];

// Treat the heap as logically separate from .bss
__attribute__((section(".heap"))) static u8 kmalloc_eternal_heap[ETERNAL_RANGE_SIZE];
__attribute__((section(".heap"))) static u8 kmalloc_pool_heap[POOL_SIZE];

static size_t g_kmalloc_bytes_eternal = 0;
static size_t g_kmalloc_call_count;
static size_t g_kfree_call_count;
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

UNMAP_AFTER_INIT void kmalloc_init()
{
    // Zero out heap since it's placed after end_of_kernel_bss.
    memset(kmalloc_eternal_heap, 0, sizeof(kmalloc_eternal_heap));
    memset(kmalloc_pool_heap, 0, sizeof(kmalloc_pool_heap));
    g_kmalloc_global = new (g_kmalloc_global_heap) KmallocGlobalHeap(kmalloc_pool_heap, sizeof(kmalloc_pool_heap));

    s_lock.initialize();

    s_next_eternal_ptr = kmalloc_eternal_heap;
    s_end_of_eternal_range = s_next_eternal_ptr + sizeof(kmalloc_pool_heap);
}

void* kmalloc_eternal(size_t size)
{
    size = round_up_to_power_of_two(size, sizeof(void*));

    ScopedSpinLock lock(s_lock);
    void* ptr = s_next_eternal_ptr;
    s_next_eternal_ptr += size;
    VERIFY(s_next_eternal_ptr < s_end_of_eternal_range);
    g_kmalloc_bytes_eternal += size;
    return ptr;
}

void* kmalloc(size_t size)
{
    ScopedSpinLock lock(s_lock);
    ++g_kmalloc_call_count;

    if (g_dump_kmalloc_stacks && Kernel::g_kernel_symbols_available) {
        dbgln("kmalloc({})", size);
        Kernel::dump_backtrace();
    }

    void* ptr = g_kmalloc_global->m_heap.allocate(size);
    if (!ptr) {
        PANIC("kmalloc: Out of memory (requested size: {})", size);
    }

    return ptr;
}

void kfree(void* ptr)
{
    if (!ptr)
        return;

    ScopedSpinLock lock(s_lock);
    ++g_kfree_call_count;

    g_kmalloc_global->m_heap.deallocate(ptr);
}

void* krealloc(void* ptr, size_t new_size)
{
    ScopedSpinLock lock(s_lock);
    return g_kmalloc_global->m_heap.reallocate(ptr, new_size);
}

void* operator new(size_t size)
{
    return kmalloc(size);
}

void* operator new[](size_t size)
{
    return kmalloc(size);
}

void get_kmalloc_stats(kmalloc_stats& stats)
{
    ScopedSpinLock lock(s_lock);
    stats.bytes_allocated = g_kmalloc_global->m_heap.allocated_bytes();
    stats.bytes_free = g_kmalloc_global->m_heap.free_bytes() + g_kmalloc_global->backup_memory_bytes();
    stats.bytes_eternal = g_kmalloc_bytes_eternal;
    stats.kmalloc_call_count = g_kmalloc_call_count;
    stats.kfree_call_count = g_kfree_call_count;
}
