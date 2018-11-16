/*
 * Really really *really* Q&D malloc() and free() implementations
 * just to get going. Don't ever let anyone see this shit. :^)
 */

#include "types.h"
#include "kmalloc.h"
#include "StdLib.h"
#include "i386.h"
#include "system.h"
#include <AK/Assertions.h>

#define SANITIZE_KMALLOC

typedef struct
{
    dword start;
    dword nchunk;
} PACKED allocation_t;

#define CHUNK_SIZE  128
#define POOL_SIZE   (1024 * 1024)

#define PAGE_ALIGNED_BASE_PHYSICAL 0x300000
#define ETERNAL_BASE_PHYSICAL 0x200000
#define BASE_PHYS   0x100000

#define RANGE_SIZE 0x100000

static byte alloc_map[POOL_SIZE / CHUNK_SIZE / 8];

volatile dword sum_alloc = 0;
volatile dword sum_free = POOL_SIZE;
volatile size_t kmalloc_sum_eternal = 0;
volatile size_t kmalloc_sum_page_aligned = 0;

static byte* s_next_eternal_ptr;
static byte* s_next_page_aligned_ptr;

static byte* s_end_of_eternal_range;
static byte* s_end_of_page_aligned_range;

bool is_kmalloc_address(void* ptr)
{
    if (ptr >= (byte*)ETERNAL_BASE_PHYSICAL && ptr < s_next_eternal_ptr)
        return true;
    if (ptr >= (byte*)PAGE_ALIGNED_BASE_PHYSICAL && ptr < s_next_page_aligned_ptr)
        return true;
    return (dword)ptr >= BASE_PHYS && (dword)ptr <= (BASE_PHYS + POOL_SIZE);
}

void kmalloc_init()
{
    memset( &alloc_map, 0, sizeof(alloc_map) );
    memset( (void *)BASE_PHYS, 0, POOL_SIZE );

    kmalloc_sum_eternal = 0;
    kmalloc_sum_page_aligned = 0;
    sum_alloc = 0;
    sum_free = POOL_SIZE;

    s_next_eternal_ptr = (byte*)ETERNAL_BASE_PHYSICAL;
    s_next_page_aligned_ptr = (byte*)PAGE_ALIGNED_BASE_PHYSICAL;

    s_end_of_eternal_range = s_next_eternal_ptr + RANGE_SIZE;
    s_end_of_page_aligned_range = s_next_page_aligned_ptr + RANGE_SIZE;
}

void* kmalloc_eternal(size_t size)
{
    void* ptr = s_next_eternal_ptr;
    s_next_eternal_ptr += size;
    ASSERT(s_next_eternal_ptr < s_end_of_eternal_range);
    kmalloc_sum_eternal += size;
    return ptr;
}

void* kmalloc_page_aligned(size_t size)
{
    ASSERT((size % PAGE_SIZE) == 0);
    void* ptr = s_next_page_aligned_ptr;
    s_next_page_aligned_ptr += size;
    ASSERT(s_next_page_aligned_ptr < s_end_of_page_aligned_range);
    kmalloc_sum_page_aligned += size;
    return ptr;
}


void* kmalloc(dword size)
{
    InterruptDisabler disabler;

    dword chunks_needed, chunks_here, first_chunk;
    dword real_size;
    dword i, j, k;

    /* We need space for the allocation_t structure at the head of the block. */
    real_size = size + sizeof(allocation_t);

    if (sum_free < real_size) {
        kprintf("kmalloc(): PANIC! Out of memory (sucks, dude)\nsum_free=%u, real_size=%x\n", sum_free, real_size);
        HANG;
        return 0L;
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
                    auto* a = (allocation_t *)(BASE_PHYS + (first_chunk * CHUNK_SIZE));
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

    kprintf("kmalloc(): PANIC! Out of memory (no suitable block for size %u)\n", size);
    HANG;

    return nullptr;
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

void* operator new(unsigned int size)
{
    return kmalloc(size);
}

void* operator new[](unsigned int size)
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
