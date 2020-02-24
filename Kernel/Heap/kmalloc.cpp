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
#include <AK/Types.h>
#include <Kernel/Arch/i386/CPU.h>
#include <Kernel/Heap/kmalloc.h>
#include <Kernel/KSyms.h>
#include <Kernel/Process.h>
#include <Kernel/Scheduler.h>
#include <LibBareMetal/StdLib.h>

#define SANITIZE_KMALLOC

struct AllocationHeader
{
    size_t allocation_size_in_chunks;
    u8 data[0];
};

#define BASE_PHYSICAL (0xc0000000 + (4 * MB))
#define CHUNK_SIZE 32
#define POOL_SIZE (3 * MB)

#define ETERNAL_BASE_PHYSICAL (0xc0000000 + (2 * MB))
#define ETERNAL_RANGE_SIZE (2 * MB)

static u8 alloc_map[POOL_SIZE / CHUNK_SIZE / 8];

volatile size_t sum_alloc = 0;
volatile size_t sum_free = POOL_SIZE;
volatile size_t kmalloc_sum_eternal = 0;

u32 g_kmalloc_call_count;
u32 g_kfree_call_count;
bool g_dump_kmalloc_stacks;

static u8* s_next_eternal_ptr;
static u8* s_end_of_eternal_range;

void kmalloc_init()
{
    memset(&alloc_map, 0, sizeof(alloc_map));
    memset((void*)BASE_PHYSICAL, 0, POOL_SIZE);

    kmalloc_sum_eternal = 0;
    sum_alloc = 0;
    sum_free = POOL_SIZE;

    s_next_eternal_ptr = (u8*)ETERNAL_BASE_PHYSICAL;
    s_end_of_eternal_range = s_next_eternal_ptr + ETERNAL_RANGE_SIZE;
}

void* kmalloc_eternal(size_t size)
{
    void* ptr = s_next_eternal_ptr;
    s_next_eternal_ptr += size;
    ASSERT(s_next_eternal_ptr < s_end_of_eternal_range);
    kmalloc_sum_eternal += size;
    return ptr;
}

void* kmalloc_aligned(size_t size, size_t alignment)
{
    void* ptr = kmalloc(size + alignment + sizeof(void*));
    size_t max_addr = (size_t)ptr + alignment;
    void* aligned_ptr = (void*)(max_addr - (max_addr % alignment));
    ((void**)aligned_ptr)[-1] = ptr;
    return aligned_ptr;
}

void kfree_aligned(void* ptr)
{
    kfree(((void**)ptr)[-1]);
}

void* kmalloc_page_aligned(size_t size)
{
    void* ptr = kmalloc_aligned(size, PAGE_SIZE);
    size_t d = (size_t)ptr;
    ASSERT((d & PAGE_MASK) == d);
    return ptr;
}

void* kmalloc_impl(size_t size)
{
    Kernel::InterruptDisabler disabler;
    ++g_kmalloc_call_count;

    if (g_dump_kmalloc_stacks && Kernel::ksyms_ready) {
        dbg() << "kmalloc(" << size << ")";
        Kernel::dump_backtrace();
    }

    // We need space for the AllocationHeader at the head of the block.
    size_t real_size = size + sizeof(AllocationHeader);

    if (sum_free < real_size) {
        Kernel::dump_backtrace();
        kprintf("%s(%u) kmalloc(): PANIC! Out of memory (sucks, dude)\nsum_free=%u, real_size=%u\n", Kernel::Process::current->name().characters(), Kernel::Process::current->pid(), sum_free, real_size);
        Kernel::hang();
    }

    size_t chunks_needed = real_size / CHUNK_SIZE;
    if (real_size % CHUNK_SIZE)
        ++chunks_needed;

    size_t chunks_here = 0;
    size_t first_chunk = 0;

    for (size_t i = 0; i < (POOL_SIZE / CHUNK_SIZE / 8); ++i) {
        if (alloc_map[i] == 0xff) {
            // Skip over completely full bucket.
            chunks_here = 0;
            continue;
        }
        // FIXME: This scan can be optimized further with LZCNT.
        for (size_t j = 0; j < 8; ++j) {
            if (!(alloc_map[i] & (1 << j))) {
                if (chunks_here == 0) {
                    // Mark where potential allocation starts.
                    first_chunk = i * 8 + j;
                }

                ++chunks_here;

                if (chunks_here == chunks_needed) {
                    auto* a = (AllocationHeader*)(BASE_PHYSICAL + (first_chunk * CHUNK_SIZE));
                    u8* ptr = a->data;
                    a->allocation_size_in_chunks = chunks_needed;

                    for (size_t k = first_chunk; k < (first_chunk + chunks_needed); ++k) {
                        alloc_map[k / 8] |= 1 << (k % 8);
                    }

                    sum_alloc += a->allocation_size_in_chunks * CHUNK_SIZE;
                    sum_free -= a->allocation_size_in_chunks * CHUNK_SIZE;
#ifdef SANITIZE_KMALLOC
                    memset(ptr, KMALLOC_SCRUB_BYTE, (a->allocation_size_in_chunks * CHUNK_SIZE) - sizeof(AllocationHeader));
#endif
                    return ptr;
                }
            } else {
                // This is in use, so restart chunks_here counter.
                chunks_here = 0;
            }
        }
    }

    kprintf("%s(%u) kmalloc(): PANIC! Out of memory (no suitable block for size %u)\n", Kernel::Process::current->name().characters(), Kernel::Process::current->pid(), size);
    Kernel::dump_backtrace();
    Kernel::hang();
}

void kfree(void* ptr)
{
    if (!ptr)
        return;

    Kernel::InterruptDisabler disabler;
    ++g_kfree_call_count;

    auto* a = (AllocationHeader*)((((u8*)ptr) - sizeof(AllocationHeader)));
    uintptr_t start = ((uintptr_t)a - (uintptr_t)BASE_PHYSICAL) / CHUNK_SIZE;

    for (size_t k = start; k < (start + a->allocation_size_in_chunks); ++k)
        alloc_map[k / 8] &= ~(1 << (k % 8));

    sum_alloc -= a->allocation_size_in_chunks * CHUNK_SIZE;
    sum_free += a->allocation_size_in_chunks * CHUNK_SIZE;

#ifdef SANITIZE_KMALLOC
    memset(a, KFREE_SCRUB_BYTE, a->allocation_size_in_chunks * CHUNK_SIZE);
#endif
}

void* krealloc(void* ptr, size_t new_size)
{
    if (!ptr)
        return kmalloc(new_size);

    Kernel::InterruptDisabler disabler;

    auto* a = (AllocationHeader*)((((u8*)ptr) - sizeof(AllocationHeader)));
    size_t old_size = a->allocation_size_in_chunks * CHUNK_SIZE;

    if (old_size == new_size)
        return ptr;

    auto* new_ptr = kmalloc(new_size);
    memcpy(new_ptr, ptr, min(old_size, new_size));
    kfree(ptr);
    return new_ptr;
}

void* operator new(size_t size)
{
    return kmalloc(size);
}

void* operator new[](size_t size)
{
    return kmalloc(size);
}
