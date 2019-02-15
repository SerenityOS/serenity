/*
 * Really really *really* Q&D malloc() and free() implementations
 * just to get going. Don't ever let anyone see this shit. :^)
 */

#include "types.h"
#include "kmalloc.h"
#include "StdLib.h"
#include "i386.h"
#include "system.h"
#include "Process.h"
#include "Scheduler.h"
#include <AK/Assertions.h>

#define SANITIZE_KMALLOC

struct [[gnu::packed]] allocation_t {
    dword start;
    dword nchunk;
};

#define CHUNK_SIZE  128
#define POOL_SIZE   (1024 * 1024)

#define ETERNAL_BASE_PHYSICAL 0x100000
#define ETERNAL_RANGE_SIZE 0x100000

#define BASE_PHYSICAL 0x200000
#define RANGE_SIZE 0x100000

static byte alloc_map[POOL_SIZE / CHUNK_SIZE / 8];

volatile size_t sum_alloc = 0;
volatile size_t sum_free = POOL_SIZE;
volatile size_t kmalloc_sum_eternal = 0;

static byte* s_next_eternal_ptr;
static byte* s_end_of_eternal_range;

bool is_kmalloc_address(const void* ptr)
{
    if (ptr >= (byte*)ETERNAL_BASE_PHYSICAL && ptr < s_next_eternal_ptr)
        return true;
    return (dword)ptr >= BASE_PHYSICAL && (dword)ptr <= (BASE_PHYSICAL + POOL_SIZE);
}

void kmalloc_init()
{
    memset(&alloc_map, 0, sizeof(alloc_map));
    memset((void *)BASE_PHYSICAL, 0, POOL_SIZE);

    kmalloc_sum_eternal = 0;
    sum_alloc = 0;
    sum_free = POOL_SIZE;

    s_next_eternal_ptr = (byte*)ETERNAL_BASE_PHYSICAL;
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
    dword max_addr = (dword)ptr + alignment;
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
    dword d = (dword)ptr;
    ASSERT((d & PAGE_MASK) == d);
    return ptr;
}

void* kmalloc_impl(dword size)
{
    InterruptDisabler disabler;

    dword chunks_needed, chunks_here, first_chunk;
    dword real_size;
    dword i, j, k;

    /* We need space for the allocation_t structure at the head of the block. */
    real_size = size + sizeof(allocation_t);

    if (sum_free < real_size) {
        kprintf("%s<%u> kmalloc(): PANIC! Out of memory (sucks, dude)\nsum_free=%u, real_size=%x\n", current->name().characters(), current->pid(), sum_free, real_size);
        hang();
    }

    chunks_needed = real_size / CHUNK_SIZE;
    if( real_size % CHUNK_SIZE )
        chunks_needed++;

    chunks_here = 0;
    first_chunk = 0;

    for( i = 0; i < (POOL_SIZE / CHUNK_SIZE / 8); ++i )
    {
        if (alloc_map[i] == 0xff) {
            // Skip over completely full bucket.
            chunks_here = 0;
            continue;
        }
        // FIXME: This scan can be optimized further with LZCNT.
        for( j = 0; j < 8; ++j )
        {
            if( !(alloc_map[i] & (1<<j)) )
            {
                if( chunks_here == 0 )
                {
                    /* Mark where potential allocation starts. */
                    first_chunk = i * 8 + j;
                }

                chunks_here++;

                if( chunks_here == chunks_needed )
                {
                    auto* a = (allocation_t *)(BASE_PHYSICAL + (first_chunk * CHUNK_SIZE));
                    byte *ptr = (byte *)a;
                    ptr += sizeof(allocation_t);
                    a->nchunk = chunks_needed;
                    a->start = first_chunk;

                    for( k = first_chunk; k < (first_chunk + chunks_needed); ++k )
                    {
                        alloc_map[k / 8] |= 1 << (k % 8);
                    }

                    sum_alloc += a->nchunk * CHUNK_SIZE;
                    sum_free  -= a->nchunk * CHUNK_SIZE;
#ifdef SANITIZE_KMALLOC
                    memset(ptr, 0xbb, (a->nchunk * CHUNK_SIZE) - sizeof(allocation_t));
#endif
                    return ptr;
                }
            }
            else
            {
                /* This is in use, so restart chunks_here counter. */
                chunks_here = 0;
            }
        }
    }

    kprintf("%s<%u> kmalloc(): PANIC! Out of memory (no suitable block for size %u)\n", current->name().characters(), current->pid(), size);
    hang();
}

void kfree(void *ptr)
{
    if( !ptr )
        return;

    InterruptDisabler disabler;

    allocation_t *a = (allocation_t *)((((byte *)ptr) - sizeof(allocation_t)));

#if 0
    dword hdr = (dword)a;
    dword mhdr = hdr & ~0x7;
    kprintf("hdr / mhdr %p / %p\n", hdr, mhdr);
    ASSERT(hdr == mhdr);
#endif

    for (dword k = a->start; k < (a->start + a->nchunk); ++k) {
        alloc_map[k / 8] &= ~(1 << (k % 8));
    }

    sum_alloc -= a->nchunk * CHUNK_SIZE;
    sum_free  += a->nchunk * CHUNK_SIZE;

#ifdef SANITIZE_KMALLOC
    memset(a, 0xaa, a->nchunk * CHUNK_SIZE);
#endif
}

void* operator new(size_t size)
{
    return kmalloc(size);
}

void* operator new[](size_t size)
{
    return kmalloc(size);
}

void operator delete(void* ptr)
{
    return kfree(ptr);
}

void operator delete[](void* ptr)
{
    return kfree(ptr);
}

void operator delete(void* ptr, unsigned int)
{
    return kfree(ptr);
}

void operator delete[](void* ptr, unsigned int)
{
    return kfree(ptr);
}
