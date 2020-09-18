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

#include <AK/InlineLinkedList.h>
#include <AK/LogStream.h>
#include <AK/ScopedValueRollback.h>
#include <AK/Vector.h>
#include <LibThread/Lock.h>
#include <assert.h>
#include <mallocdefs.h>
#include <serenity.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/internals.h>
#include <sys/mman.h>

// FIXME: Thread safety.

//#define MALLOC_DEBUG
#define RECYCLE_BIG_ALLOCATIONS

#define MAGIC_PAGE_HEADER 0x42657274     // 'Bert'
#define MAGIC_BIGALLOC_HEADER 0x42697267 // 'Birg'
#define PAGE_ROUND_UP(x) ((((size_t)(x)) + PAGE_SIZE - 1) & (~(PAGE_SIZE - 1)))

ALWAYS_INLINE static void ue_notify_malloc(const void* ptr, size_t size)
{
    send_secret_data_to_userspace_emulator(1, size, (FlatPtr)ptr);
}

ALWAYS_INLINE static void ue_notify_free(const void* ptr)
{
    send_secret_data_to_userspace_emulator(2, (FlatPtr)ptr, 0);
}

static LibThread::Lock& malloc_lock()
{
    static u32 lock_storage[sizeof(LibThread::Lock) / sizeof(u32)];
    return *reinterpret_cast<LibThread::Lock*>(&lock_storage);
}

constexpr size_t number_of_chunked_blocks_to_keep_around_per_size_class = 4;
constexpr size_t number_of_big_blocks_to_keep_around_per_size_class = 8;

static bool s_log_malloc = false;
static bool s_scrub_malloc = true;
static bool s_scrub_free = true;
static bool s_profiling = false;
static unsigned short size_classes[] = { 8, 16, 32, 64, 128, 252, 508, 1016, 2036, 4090, 8188, 16376, 32756, 0 };
static constexpr size_t num_size_classes = sizeof(size_classes) / sizeof(unsigned short);

struct MallocStats {
    size_t number_of_malloc_calls;

    size_t number_of_big_allocator_hits;
    size_t number_of_big_allocator_purge_hits;
    size_t number_of_big_allocs;

    size_t number_of_empty_block_hits;
    size_t number_of_empty_block_purge_hits;
    size_t number_of_block_allocs;
    size_t number_of_blocks_full;

    size_t number_of_free_calls;

    size_t number_of_big_allocator_keeps;
    size_t number_of_big_allocator_frees;

    size_t number_of_freed_full_blocks;
    size_t number_of_keeps;
    size_t number_of_frees;
};
static MallocStats g_malloc_stats = {};

constexpr size_t block_size = 64 * KiB;
constexpr size_t block_mask = ~(block_size - 1);

struct CommonHeader {
    size_t m_magic;
    size_t m_size;
};

struct BigAllocationBlock : public CommonHeader {
    BigAllocationBlock(size_t size)
    {
        m_magic = MAGIC_BIGALLOC_HEADER;
        m_size = size;
    }
    unsigned char* m_slot[0];
};

struct FreelistEntry {
    FreelistEntry* next;
};

struct ChunkedBlock
    : public CommonHeader
    , public InlineLinkedListNode<ChunkedBlock> {

    ChunkedBlock(size_t bytes_per_chunk)
    {
        m_magic = MAGIC_PAGE_HEADER;
        m_size = bytes_per_chunk;
        m_free_chunks = chunk_capacity();
        m_freelist = (FreelistEntry*)chunk(0);
        for (size_t i = 0; i < chunk_capacity(); ++i) {
            auto* entry = (FreelistEntry*)chunk(i);
            if (i != chunk_capacity() - 1)
                entry->next = (FreelistEntry*)chunk(i + 1);
            else
                entry->next = nullptr;
        }
    }

    ChunkedBlock* m_prev { nullptr };
    ChunkedBlock* m_next { nullptr };
    FreelistEntry* m_freelist { nullptr };
    unsigned short m_free_chunks { 0 };
    [[gnu::aligned(8)]] unsigned char m_slot[0];

    void* chunk(size_t index)
    {
        return &m_slot[index * m_size];
    }
    bool is_full() const { return m_free_chunks == 0; }
    size_t bytes_per_chunk() const { return m_size; }
    size_t free_chunks() const { return m_free_chunks; }
    size_t used_chunks() const { return chunk_capacity() - m_free_chunks; }
    size_t chunk_capacity() const { return (block_size - sizeof(ChunkedBlock)) / m_size; }
};

struct Allocator {
    size_t size { 0 };
    size_t block_count { 0 };
    size_t empty_block_count { 0 };
    ChunkedBlock* empty_blocks[number_of_chunked_blocks_to_keep_around_per_size_class] { nullptr };
    InlineLinkedList<ChunkedBlock> usable_blocks;
    InlineLinkedList<ChunkedBlock> full_blocks;
};

struct BigAllocator {
    Vector<BigAllocationBlock*, number_of_big_blocks_to_keep_around_per_size_class> blocks;
};

// Allocators will be initialized in __malloc_init.
// We can not rely on global constructors to initialize them,
// because they must be initialized before other global constructors
// are run. Similarly, we can not allow global destructors to destruct
// them. We could have used AK::NeverDestoyed to prevent the latter,
// but it would have not helped with the former.
static u8 g_allocators_storage[sizeof(Allocator) * num_size_classes];
static u8 g_big_allocators_storage[sizeof(BigAllocator)];

static inline Allocator (&allocators())[num_size_classes]
{
    return reinterpret_cast<Allocator(&)[num_size_classes]>(g_allocators_storage);
}

static inline BigAllocator (&big_allocators())[1]
{
    return reinterpret_cast<BigAllocator(&)[1]>(g_big_allocators_storage);
}

static Allocator* allocator_for_size(size_t size, size_t& good_size)
{
    for (size_t i = 0; size_classes[i]; ++i) {
        if (size <= size_classes[i]) {
            good_size = size_classes[i];
            return &allocators()[i];
        }
    }
    good_size = PAGE_ROUND_UP(size);
    return nullptr;
}

#ifdef RECYCLE_BIG_ALLOCATIONS
static BigAllocator* big_allocator_for_size(size_t size)
{
    if (size == 65536)
        return &big_allocators()[0];
    return nullptr;
}
#endif

extern "C" {

static void* os_alloc(size_t size, const char* name)
{
    auto* ptr = serenity_mmap(nullptr, size, PROT_READ | PROT_WRITE, MAP_ANONYMOUS | MAP_PRIVATE | MAP_PURGEABLE, 0, 0, block_size, name);
    ASSERT(ptr != MAP_FAILED);
    return ptr;
}

static void os_free(void* ptr, size_t size)
{
    int rc = munmap(ptr, size);
    assert(rc == 0);
}

static void* malloc_impl(size_t size)
{
    LOCKER(malloc_lock());

    if (s_log_malloc)
        dbgprintf("LibC: malloc(%zu)\n", size);

    if (!size)
        return nullptr;

    g_malloc_stats.number_of_malloc_calls++;

    size_t good_size;
    auto* allocator = allocator_for_size(size, good_size);

    if (!allocator) {
        size_t real_size = round_up_to_power_of_two(sizeof(BigAllocationBlock) + size, block_size);
#ifdef RECYCLE_BIG_ALLOCATIONS
        if (auto* allocator = big_allocator_for_size(real_size)) {
            if (!allocator->blocks.is_empty()) {
                g_malloc_stats.number_of_big_allocator_hits++;
                auto* block = allocator->blocks.take_last();
                int rc = madvise(block, real_size, MADV_SET_NONVOLATILE);
                bool this_block_was_purged = rc == 1;
                if (rc < 0) {
                    perror("madvise");
                    ASSERT_NOT_REACHED();
                }
                if (mprotect(block, real_size, PROT_READ | PROT_WRITE) < 0) {
                    perror("mprotect");
                    ASSERT_NOT_REACHED();
                }
                if (this_block_was_purged) {
                    g_malloc_stats.number_of_big_allocator_purge_hits++;
                    new (block) BigAllocationBlock(real_size);
                }

                ue_notify_malloc(&block->m_slot[0], size);
                return &block->m_slot[0];
            }
        }
#endif
        g_malloc_stats.number_of_big_allocs++;
        auto* block = (BigAllocationBlock*)os_alloc(real_size, "malloc: BigAllocationBlock");
        new (block) BigAllocationBlock(real_size);
        ue_notify_malloc(&block->m_slot[0], size);
        return &block->m_slot[0];
    }

    ChunkedBlock* block = nullptr;

    for (block = allocator->usable_blocks.head(); block; block = block->next()) {
        if (block->free_chunks())
            break;
    }

    if (!block && allocator->empty_block_count) {
        g_malloc_stats.number_of_empty_block_hits++;
        block = allocator->empty_blocks[--allocator->empty_block_count];
        int rc = madvise(block, block_size, MADV_SET_NONVOLATILE);
        bool this_block_was_purged = rc == 1;
        if (rc < 0) {
            perror("madvise");
            ASSERT_NOT_REACHED();
        }
        rc = mprotect(block, block_size, PROT_READ | PROT_WRITE);
        if (rc < 0) {
            perror("mprotect");
            ASSERT_NOT_REACHED();
        }
        if (this_block_was_purged) {
            g_malloc_stats.number_of_empty_block_purge_hits++;
            new (block) ChunkedBlock(good_size);
        }
        allocator->usable_blocks.append(block);
    }

    if (!block) {
        g_malloc_stats.number_of_block_allocs++;
        char buffer[64];
        snprintf(buffer, sizeof(buffer), "malloc: ChunkedBlock(%zu)", good_size);
        block = (ChunkedBlock*)os_alloc(block_size, buffer);
        new (block) ChunkedBlock(good_size);
        allocator->usable_blocks.append(block);
        ++allocator->block_count;
    }

    --block->m_free_chunks;
    void* ptr = block->m_freelist;
    block->m_freelist = block->m_freelist->next;
    if (block->is_full()) {
        g_malloc_stats.number_of_blocks_full++;
#ifdef MALLOC_DEBUG
        dbgprintf("Block %p is now full in size class %zu\n", block, good_size);
#endif
        allocator->usable_blocks.remove(block);
        allocator->full_blocks.append(block);
    }
#ifdef MALLOC_DEBUG
    dbgprintf("LibC: allocated %p (chunk in block %p, size %zu)\n", ptr, block, block->bytes_per_chunk());
#endif

    if (s_scrub_malloc)
        memset(ptr, MALLOC_SCRUB_BYTE, block->m_size);

    ue_notify_malloc(ptr, size);
    return ptr;
}

static void free_impl(void* ptr)
{
    ScopedValueRollback rollback(errno);

    if (!ptr)
        return;

    g_malloc_stats.number_of_free_calls++;

    LOCKER(malloc_lock());

    void* block_base = (void*)((FlatPtr)ptr & block_mask);
    size_t magic = *(size_t*)block_base;

    if (magic == MAGIC_BIGALLOC_HEADER) {
        auto* block = (BigAllocationBlock*)block_base;
#ifdef RECYCLE_BIG_ALLOCATIONS
        if (auto* allocator = big_allocator_for_size(block->m_size)) {
            if (allocator->blocks.size() < number_of_big_blocks_to_keep_around_per_size_class) {
                g_malloc_stats.number_of_big_allocator_keeps++;
                allocator->blocks.append(block);
                size_t this_block_size = block->m_size;
                if (mprotect(block, this_block_size, PROT_NONE) < 0) {
                    perror("mprotect");
                    ASSERT_NOT_REACHED();
                }
                if (madvise(block, this_block_size, MADV_SET_VOLATILE) != 0) {
                    perror("madvise");
                    ASSERT_NOT_REACHED();
                }
                return;
            }
        }
#endif
        g_malloc_stats.number_of_big_allocator_frees++;
        os_free(block, block->m_size);
        return;
    }

    assert(magic == MAGIC_PAGE_HEADER);
    auto* block = (ChunkedBlock*)block_base;

#ifdef MALLOC_DEBUG
    dbgprintf("LibC: freeing %p in allocator %p (size=%u, used=%u)\n", ptr, block, block->bytes_per_chunk(), block->used_chunks());
#endif

    if (s_scrub_free)
        memset(ptr, FREE_SCRUB_BYTE, block->bytes_per_chunk());

    auto* entry = (FreelistEntry*)ptr;
    entry->next = block->m_freelist;
    block->m_freelist = entry;

    if (block->is_full()) {
        size_t good_size;
        auto* allocator = allocator_for_size(block->m_size, good_size);
#ifdef MALLOC_DEBUG
        dbgprintf("Block %p no longer full in size class %u\n", block, good_size);
#endif
        g_malloc_stats.number_of_freed_full_blocks++;
        allocator->full_blocks.remove(block);
        allocator->usable_blocks.prepend(block);
    }

    ++block->m_free_chunks;

    if (!block->used_chunks()) {
        size_t good_size;
        auto* allocator = allocator_for_size(block->m_size, good_size);
        if (allocator->block_count < number_of_chunked_blocks_to_keep_around_per_size_class) {
#ifdef MALLOC_DEBUG
            dbgprintf("Keeping block %p around for size class %u\n", block, good_size);
#endif
            g_malloc_stats.number_of_keeps++;
            allocator->usable_blocks.remove(block);
            allocator->empty_blocks[allocator->empty_block_count++] = block;
            mprotect(block, block_size, PROT_NONE);
            madvise(block, block_size, MADV_SET_VOLATILE);
            return;
        }
#ifdef MALLOC_DEBUG
        dbgprintf("Releasing block %p for size class %u\n", block, good_size);
#endif
        g_malloc_stats.number_of_frees++;
        allocator->usable_blocks.remove(block);
        --allocator->block_count;
        os_free(block, block_size);
    }
}

[[gnu::flatten]] void* malloc(size_t size)
{
    void* ptr = malloc_impl(size);
    if (s_profiling)
        perf_event(PERF_EVENT_MALLOC, size, reinterpret_cast<FlatPtr>(ptr));
    return ptr;
}

[[gnu::flatten]] void free(void* ptr)
{
    if (s_profiling)
        perf_event(PERF_EVENT_FREE, reinterpret_cast<FlatPtr>(ptr), 0);
    free_impl(ptr);
    ue_notify_free(ptr);
}

void* calloc(size_t count, size_t size)
{
    size_t new_size = count * size;
    auto* ptr = malloc(new_size);
    if (ptr)
        memset(ptr, 0, new_size);
    return ptr;
}

size_t malloc_size(void* ptr)
{
    if (!ptr)
        return 0;
    LOCKER(malloc_lock());
    void* page_base = (void*)((FlatPtr)ptr & block_mask);
    auto* header = (const CommonHeader*)page_base;
    auto size = header->m_size;
    if (header->m_magic == MAGIC_BIGALLOC_HEADER)
        size -= sizeof(CommonHeader);
    return size;
}

void* realloc(void* ptr, size_t size)
{
    if (!ptr)
        return malloc(size);
    if (!size)
        return nullptr;

    LOCKER(malloc_lock());
    auto existing_allocation_size = malloc_size(ptr);
    if (size <= existing_allocation_size)
        return ptr;
    auto* new_ptr = malloc(size);
    if (new_ptr) {
        memcpy(new_ptr, ptr, min(existing_allocation_size, size));
        free(ptr);
    }
    return new_ptr;
}

void __malloc_init()
{
    new (&malloc_lock()) LibThread::Lock();
    if (getenv("LIBC_NOSCRUB_MALLOC"))
        s_scrub_malloc = false;
    if (getenv("LIBC_NOSCRUB_FREE"))
        s_scrub_free = false;
    if (getenv("LIBC_LOG_MALLOC"))
        s_log_malloc = true;
    if (getenv("LIBC_PROFILE_MALLOC"))
        s_profiling = true;

    for (size_t i = 0; i < num_size_classes; ++i) {
        new (&allocators()[i]) Allocator();
        allocators()[i].size = size_classes[i];
    }

    new (&big_allocators()[0])(BigAllocator);
}

void serenity_dump_malloc_stats()
{
    dbg() << "# malloc() calls: " << g_malloc_stats.number_of_malloc_calls;
    dbg();
    dbg() << "big alloc hits: " << g_malloc_stats.number_of_big_allocator_hits;
    dbg() << "big alloc hits that were purged: " << g_malloc_stats.number_of_big_allocator_purge_hits;
    dbg() << "big allocs: " << g_malloc_stats.number_of_big_allocs;
    dbg();
    dbg() << "empty block hits: " << g_malloc_stats.number_of_empty_block_hits;
    dbg() << "empty block hits that were purged: " << g_malloc_stats.number_of_empty_block_purge_hits;
    dbg() << "block allocs: " << g_malloc_stats.number_of_block_allocs;
    dbg() << "filled blocks: " << g_malloc_stats.number_of_blocks_full;
    dbg();
    dbg() << "# free() calls: " << g_malloc_stats.number_of_free_calls;
    dbg();
    dbg() << "big alloc keeps: " << g_malloc_stats.number_of_big_allocator_keeps;
    dbg() << "big alloc frees: " << g_malloc_stats.number_of_big_allocator_frees;
    dbg();
    dbg() << "full block frees: " << g_malloc_stats.number_of_freed_full_blocks;
    dbg() << "number of keeps: " << g_malloc_stats.number_of_keeps;
    dbg() << "number of frees: " << g_malloc_stats.number_of_frees;
}
}
