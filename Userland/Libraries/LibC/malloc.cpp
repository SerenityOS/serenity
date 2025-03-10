/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, Peter Elliott <pelliott@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/BuiltinWrappers.h>
#include <AK/Debug.h>
#include <AK/ScopedValueRollback.h>
#include <AK/Vector.h>
#include <errno.h>
#include <mallocdefs.h>
#include <pthread.h>
#include <serenity.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/internals.h>
#include <sys/mman.h>
#include <syscall.h>

class PthreadMutexLocker {
public:
    ALWAYS_INLINE explicit PthreadMutexLocker(pthread_mutex_t& mutex)
        : m_mutex(mutex)
    {
        lock();
        __heap_is_stable = false;
    }
    ALWAYS_INLINE ~PthreadMutexLocker()
    {
        __heap_is_stable = true;
        unlock();
    }
    ALWAYS_INLINE void lock() { pthread_mutex_lock(&m_mutex); }
    ALWAYS_INLINE void unlock() { pthread_mutex_unlock(&m_mutex); }

private:
    pthread_mutex_t& m_mutex;
};

#define RECYCLE_BIG_ALLOCATIONS

static pthread_mutex_t s_malloc_mutex = PTHREAD_MUTEX_INITIALIZER;
bool __heap_is_stable = true;

constexpr size_t number_of_hot_chunked_blocks_to_keep_around = 16;
constexpr size_t number_of_cold_chunked_blocks_to_keep_around = 16;
constexpr size_t number_of_big_blocks_to_keep_around_per_size_class = 8;

static bool s_log_malloc = false;
static bool s_scrub_malloc = true;
static bool s_scrub_free = true;
static bool s_profiling = false;

struct MallocStats {
    size_t number_of_malloc_calls;

    size_t number_of_big_allocator_hits;
    size_t number_of_big_allocator_purge_hits;
    size_t number_of_big_allocs;

    size_t number_of_hot_empty_block_hits;
    size_t number_of_cold_empty_block_hits;
    size_t number_of_cold_empty_block_purge_hits;
    size_t number_of_block_allocs;
    size_t number_of_blocks_full;

    size_t number_of_free_calls;

    size_t number_of_big_allocator_keeps;
    size_t number_of_big_allocator_frees;

    size_t number_of_freed_full_blocks;
    size_t number_of_hot_keeps;
    size_t number_of_cold_keeps;
    size_t number_of_frees;
};
static MallocStats g_malloc_stats = {};

static size_t s_hot_empty_block_count { 0 };
static ChunkedBlock* s_hot_empty_blocks[number_of_hot_chunked_blocks_to_keep_around] { nullptr };
static size_t s_cold_empty_block_count { 0 };
static ChunkedBlock* s_cold_empty_blocks[number_of_cold_chunked_blocks_to_keep_around] { nullptr };

struct Allocator {
    size_t size { 0 };
    size_t block_count { 0 };
    ChunkedBlock::List usable_blocks;
    ChunkedBlock::List full_blocks;
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
alignas(Allocator) static u8 g_allocators_storage[sizeof(Allocator) * num_size_classes];
alignas(BigAllocator) static u8 g_big_allocators_storage[sizeof(BigAllocator)];

static inline Allocator (&allocators())[num_size_classes]
{
    return reinterpret_cast<Allocator(&)[num_size_classes]>(g_allocators_storage);
}

static inline BigAllocator (&big_allocators())[1]
{
    return reinterpret_cast<BigAllocator(&)[1]>(g_big_allocators_storage);
}

// --- BEGIN MATH ---
// This stuff is only used for checking if there exists an aligned block in a
// chunk. It has no bearing on the rest of the allocator, especially for
// regular malloc.

static inline unsigned long modulo(long a, long b)
{
    return (b + (a % b)) % b;
}

struct EuclideanResult {
    long x;
    long y;
    long gcd;
};

// Returns x, y, gcd.
static inline EuclideanResult extended_euclid(long a, long b)
{
    EuclideanResult old = { 1, 0, a };
    EuclideanResult current = { 0, 1, b };

    while (current.gcd != 0) {
        long quotient = old.gcd / current.gcd;

        EuclideanResult next = {
            old.x - quotient * current.x,
            old.y - quotient * current.y,
            old.gcd - quotient * current.gcd,
        };

        old = current;
        current = next;
    }

    return old;
}

static inline bool block_has_aligned_chunk(long align, long bytes_per_chunk, long chunk_capacity)
{
    // Never do math on a normal malloc.
    if ((size_t)align <= sizeof(ChunkedBlock))
        return true;

    // Solve the linear congruence n*bytes_per_chunk = -sizeof(ChunkedBlock) (mod align).
    auto [x, y, gcd] = extended_euclid(bytes_per_chunk % align, align);
    long constant = modulo(-sizeof(ChunkedBlock), align);
    if (constant % gcd != 0)
        // No solution. Chunk size is probably a multiple of align.
        return false;

    long n = modulo(x * (constant / gcd), align);
    if (x < 0)
        n = (n + align / gcd) % align;

    // Don't ask me to prove this.
    VERIFY(n > 0);
    return n < chunk_capacity;
}

// --- END MATH ---

static Allocator* allocator_for_size(size_t size, size_t& good_size, size_t align = 1)
{
    for (size_t i = 0; size_classes[i]; ++i) {
        auto& allocator = allocators()[i];
        if (size <= size_classes[i] && block_has_aligned_chunk(align, allocator.size, (ChunkedBlock::block_size - sizeof(ChunkedBlock)) / allocator.size)) {
            good_size = size_classes[i];
            return &allocator;
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

static ErrorOr<void*> os_alloc(size_t size, char const* name)
{
    int flags = MAP_ANONYMOUS | MAP_PRIVATE | MAP_PURGEABLE;
#if ARCH(X86_64)
    flags |= MAP_RANDOMIZED;
#endif
    auto* ptr = serenity_mmap(nullptr, size, PROT_READ | PROT_WRITE, flags, 0, 0, ChunkedBlock::block_size, name);
    VERIFY(ptr != nullptr);
    if (ptr == MAP_FAILED) {
        return ENOMEM;
    }
    return ptr;
}

static void os_free(void* ptr, size_t size)
{
    int rc = munmap(ptr, size);
    VERIFY(rc == 0);
}

static void* try_allocate_chunk_aligned(size_t align, ChunkedBlock& block)
{
    // These loops are guaranteed to run only once for a standard-aligned malloc.
    for (FreelistEntry** entry = &(block.m_freelist); *entry != nullptr; entry = &((*entry)->next)) {
        if ((reinterpret_cast<uintptr_t>(*entry) & (align - 1)) == 0) {
            --block.m_free_chunks;
            void* ptr = *entry;
            *entry = (*entry)->next; // Delete the entry.
            return ptr;
        }
    }
    for (; block.m_next_lazy_freelist_index < block.chunk_capacity(); block.m_next_lazy_freelist_index++) {
        void* ptr = block.m_slot + block.m_next_lazy_freelist_index * block.m_size;
        if ((reinterpret_cast<uintptr_t>(ptr) & (align - 1)) == 0) {
            --block.m_free_chunks;
            block.m_next_lazy_freelist_index++;
            return ptr;
        }
        auto* entry = (FreelistEntry*)ptr;
        entry->next = block.m_freelist;
        block.m_freelist = entry;
    }
    return nullptr;
}

enum class CallerWillInitializeMemory {
    No,
    Yes,
};

#ifndef NO_TLS
__thread bool s_allocation_enabled = true;
#endif

static ErrorOr<void*> malloc_impl(size_t size, size_t align, CallerWillInitializeMemory caller_will_initialize_memory)
{
#ifndef NO_TLS
    VERIFY(s_allocation_enabled);
#endif

    // Align must be a power of 2.
    if (popcount(align) != 1)
        return EINVAL;

    // FIXME: Support larger than 32KiB alignments (if you dare).
    if (sizeof(BigAllocationBlock) + align >= ChunkedBlock::block_size)
        return EINVAL;

    if (s_log_malloc)
        dbgln("LibC: malloc({})", size);

    if (!size) {
        // Legally we could just return a null pointer here, but this is more
        // compatible with existing software.
        size = 1;
    }

    g_malloc_stats.number_of_malloc_calls++;

    size_t good_size;
    auto* allocator = allocator_for_size(size, good_size, align);

    PthreadMutexLocker locker(s_malloc_mutex);

    if (!allocator) {
        size_t real_size = round_up_to_power_of_two(sizeof(BigAllocationBlock) + size + ((align > 16) ? align : 0), ChunkedBlock::block_size);
        if (real_size < size) {
            dbgln_if(MALLOC_DEBUG, "LibC: Detected overflow trying to do big allocation of size {} for {}", real_size, size);
            return ENOMEM;
        }
#ifdef RECYCLE_BIG_ALLOCATIONS
        if (auto* allocator = big_allocator_for_size(real_size)) {
            if (!allocator->blocks.is_empty()) {
                g_malloc_stats.number_of_big_allocator_hits++;
                auto* block = allocator->blocks.take_last();
                int rc = madvise(block, real_size, MADV_SET_NONVOLATILE);
                bool this_block_was_purged = rc == 1;
                if (rc < 0) {
                    perror("madvise");
                    VERIFY_NOT_REACHED();
                }
                if (mprotect(block, real_size, PROT_READ | PROT_WRITE) < 0) {
                    perror("mprotect");
                    VERIFY_NOT_REACHED();
                }
                if (this_block_was_purged) {
                    g_malloc_stats.number_of_big_allocator_purge_hits++;
                    new (block) BigAllocationBlock(real_size);
                }

                return reinterpret_cast<void*>(round_up_to_power_of_two(reinterpret_cast<uintptr_t>(&block->m_slot[0]), align));
            }
        }
#endif
        auto* block = (BigAllocationBlock*)TRY(os_alloc(real_size, "malloc: BigAllocationBlock"));
        g_malloc_stats.number_of_big_allocs++;
        new (block) BigAllocationBlock(real_size);

        return reinterpret_cast<void*>(round_up_to_power_of_two(reinterpret_cast<uintptr_t>(&block->m_slot[0]), align));
    }

    ChunkedBlock* block = nullptr;
    void* ptr = nullptr;
    for (auto& current : allocator->usable_blocks) {
        if (current.free_chunks()) {
            ptr = try_allocate_chunk_aligned(align, current);
            if (ptr) {
                block = &current;
                break;
            }
        }
    }

    if (!block && s_hot_empty_block_count) {
        g_malloc_stats.number_of_hot_empty_block_hits++;
        block = s_hot_empty_blocks[--s_hot_empty_block_count];
        if (block->m_size != good_size) {
            new (block) ChunkedBlock(good_size);
            char buffer[64];
            snprintf(buffer, sizeof(buffer), "malloc: ChunkedBlock(%zu)", good_size);
            set_mmap_name(block, ChunkedBlock::block_size, buffer);
        }
        allocator->usable_blocks.append(*block);
    }

    if (!block && s_cold_empty_block_count) {
        g_malloc_stats.number_of_cold_empty_block_hits++;
        block = s_cold_empty_blocks[--s_cold_empty_block_count];
        int rc = madvise(block, ChunkedBlock::block_size, MADV_SET_NONVOLATILE);
        bool this_block_was_purged = rc == 1;
        if (rc < 0) {
            perror("madvise");
            VERIFY_NOT_REACHED();
        }
        rc = mprotect(block, ChunkedBlock::block_size, PROT_READ | PROT_WRITE);
        if (rc < 0) {
            perror("mprotect");
            VERIFY_NOT_REACHED();
        }
        if (this_block_was_purged || block->m_size != good_size) {
            if (this_block_was_purged)
                g_malloc_stats.number_of_cold_empty_block_purge_hits++;
            new (block) ChunkedBlock(good_size);
        }
        allocator->usable_blocks.append(*block);
    }

    if (!block) {
        g_malloc_stats.number_of_block_allocs++;
        char buffer[64];
        snprintf(buffer, sizeof(buffer), "malloc: ChunkedBlock(%zu)", good_size);
        block = (ChunkedBlock*)TRY(os_alloc(ChunkedBlock::block_size, buffer));
        new (block) ChunkedBlock(good_size);
        allocator->usable_blocks.append(*block);
        ++allocator->block_count;
    }

    if (!ptr) {
        ptr = try_allocate_chunk_aligned(align, *block);
    }

    VERIFY(ptr);
    if (block->is_full()) {
        g_malloc_stats.number_of_blocks_full++;
        dbgln_if(MALLOC_DEBUG, "Block {:p} is now full in size class {}", block, good_size);
        allocator->usable_blocks.remove(*block);
        allocator->full_blocks.append(*block);
    }
    dbgln_if(MALLOC_DEBUG, "LibC: allocated {:p} (chunk in block {:p}, size {})", ptr, block, block->bytes_per_chunk());

    if (s_scrub_malloc && caller_will_initialize_memory == CallerWillInitializeMemory::No)
        memset(ptr, MALLOC_SCRUB_BYTE, block->m_size);

    return ptr;
}

static void free_impl(void* ptr)
{
#ifndef NO_TLS
    VERIFY(s_allocation_enabled);
#endif

    ScopedValueRollback rollback(errno);

    if (!ptr)
        return;

    g_malloc_stats.number_of_free_calls++;

    void* block_base = (void*)((FlatPtr)ptr & ChunkedBlock::ChunkedBlock::block_mask);
    size_t magic = *(size_t*)block_base;

    PthreadMutexLocker locker(s_malloc_mutex);

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
                    VERIFY_NOT_REACHED();
                }
                if (madvise(block, this_block_size, MADV_SET_VOLATILE) != 0) {
                    perror("madvise");
                    VERIFY_NOT_REACHED();
                }
                return;
            }
        }
#endif
        g_malloc_stats.number_of_big_allocator_frees++;
        os_free(block, block->m_size);
        return;
    }

    VERIFY(magic == MAGIC_PAGE_HEADER);
    auto* block = (ChunkedBlock*)block_base;

    dbgln_if(MALLOC_DEBUG, "LibC: freeing {:p} in allocator {:p} (size={}, used={})", ptr, block, block->bytes_per_chunk(), block->used_chunks());

    if (s_scrub_free)
        memset(ptr, FREE_SCRUB_BYTE, block->bytes_per_chunk());

    auto* entry = (FreelistEntry*)ptr;
    entry->next = block->m_freelist;
    block->m_freelist = entry;

    if (block->is_full()) {
        size_t good_size;
        auto* allocator = allocator_for_size(block->m_size, good_size);
        dbgln_if(MALLOC_DEBUG, "Block {:p} no longer full in size class {}", block, good_size);
        g_malloc_stats.number_of_freed_full_blocks++;
        allocator->full_blocks.remove(*block);
        allocator->usable_blocks.prepend(*block);
    }

    ++block->m_free_chunks;

    if (!block->used_chunks()) {
        size_t good_size;
        auto* allocator = allocator_for_size(block->m_size, good_size);
        if (s_hot_empty_block_count < number_of_hot_chunked_blocks_to_keep_around) {
            dbgln_if(MALLOC_DEBUG, "Keeping hot block {:p} around", block);
            g_malloc_stats.number_of_hot_keeps++;
            allocator->usable_blocks.remove(*block);
            s_hot_empty_blocks[s_hot_empty_block_count++] = block;
            return;
        }
        if (s_cold_empty_block_count < number_of_cold_chunked_blocks_to_keep_around) {
            dbgln_if(MALLOC_DEBUG, "Keeping cold block {:p} around", block);
            g_malloc_stats.number_of_cold_keeps++;
            allocator->usable_blocks.remove(*block);
            s_cold_empty_blocks[s_cold_empty_block_count++] = block;
            mprotect(block, ChunkedBlock::block_size, PROT_NONE);
            madvise(block, ChunkedBlock::block_size, MADV_SET_VOLATILE);
            return;
        }
        dbgln_if(MALLOC_DEBUG, "Releasing block {:p} for size class {}", block, good_size);
        g_malloc_stats.number_of_frees++;
        allocator->usable_blocks.remove(*block);
        --allocator->block_count;
        os_free(block, ChunkedBlock::block_size);
    }
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/malloc.html
void* malloc(size_t size)
{
    auto ptr_or_error = malloc_impl(size, 16, CallerWillInitializeMemory::No);

    if (ptr_or_error.is_error()) {
        errno = ptr_or_error.error().code();
        return nullptr;
    }

    if (s_profiling)
        perf_event(PERF_EVENT_MALLOC, size, reinterpret_cast<FlatPtr>(ptr_or_error.value()));

    return ptr_or_error.value();
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/free.html
void free(void* ptr)
{
    if (s_profiling)
        perf_event(PERF_EVENT_FREE, reinterpret_cast<FlatPtr>(ptr), 0);
    free_impl(ptr);
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/calloc.html
void* calloc(size_t count, size_t size)
{
    if (Checked<size_t>::multiplication_would_overflow(count, size)) {
        errno = ENOMEM;
        return nullptr;
    }
    size_t new_size = count * size;
    auto ptr_or_error = malloc_impl(new_size, 16, CallerWillInitializeMemory::Yes);

    if (ptr_or_error.is_error()) {
        errno = ptr_or_error.error().code();
        return nullptr;
    }

    memset(ptr_or_error.value(), 0, new_size);
    return ptr_or_error.value();
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/posix_memalign.html
int posix_memalign(void** memptr, size_t alignment, size_t size)
{
    auto ptr_or_error = malloc_impl(size, alignment, CallerWillInitializeMemory::No);

    if (ptr_or_error.is_error())
        return ptr_or_error.error().code();

    *memptr = ptr_or_error.value();
    return 0;
}

void* aligned_alloc(size_t alignment, size_t size)
{
    auto ptr_or_error = malloc_impl(size, alignment, CallerWillInitializeMemory::No);

    if (ptr_or_error.is_error()) {
        errno = ptr_or_error.error().code();
        return nullptr;
    }

    return ptr_or_error.value();
}

size_t malloc_size(void const* ptr)
{
    if (!ptr)
        return 0;
    void* page_base = (void*)((FlatPtr)ptr & ChunkedBlock::block_mask);
    auto* header = (CommonHeader const*)page_base;
    auto size = header->m_size;
    if (header->m_magic == MAGIC_BIGALLOC_HEADER)
        size -= sizeof(BigAllocationBlock);
    else
        VERIFY(header->m_magic == MAGIC_PAGE_HEADER);
    return size;
}

size_t malloc_good_size(size_t size)
{
    size_t good_size;
    allocator_for_size(size, good_size);
    return good_size;
}

void* realloc(void* ptr, size_t size)
{
    if (!ptr)
        return malloc(size);
    if (!size) {
        free(ptr);
        return nullptr;
    }

    auto existing_allocation_size = malloc_size(ptr);

    if (size <= existing_allocation_size) {
        return ptr;
    }
    auto* new_ptr = malloc(size);
    if (new_ptr) {
        memcpy(new_ptr, ptr, min(existing_allocation_size, size));
        free(ptr);
    }
    return new_ptr;
}

void __malloc_init()
{
    if (secure_getenv("LIBC_NOSCRUB_MALLOC"))
        s_scrub_malloc = false;
    if (secure_getenv("LIBC_NOSCRUB_FREE"))
        s_scrub_free = false;
    if (secure_getenv("LIBC_LOG_MALLOC"))
        s_log_malloc = true;
    if (secure_getenv("LIBC_PROFILE_MALLOC"))
        s_profiling = true;

    for (size_t i = 0; i < num_size_classes; ++i) {
        new (&allocators()[i]) Allocator();
        allocators()[i].size = size_classes[i];
    }

    new (&big_allocators()[0])(BigAllocator);
}

void serenity_dump_malloc_stats()
{
    dbgln("# malloc() calls: {}", g_malloc_stats.number_of_malloc_calls);
    dbgln();
    dbgln("big alloc hits: {}", g_malloc_stats.number_of_big_allocator_hits);
    dbgln("big alloc hits that were purged: {}", g_malloc_stats.number_of_big_allocator_purge_hits);
    dbgln("big allocs: {}", g_malloc_stats.number_of_big_allocs);
    dbgln();
    dbgln("empty hot block hits: {}", g_malloc_stats.number_of_hot_empty_block_hits);
    dbgln("empty cold block hits: {}", g_malloc_stats.number_of_cold_empty_block_hits);
    dbgln("empty cold block hits that were purged: {}", g_malloc_stats.number_of_cold_empty_block_purge_hits);
    dbgln("block allocs: {}", g_malloc_stats.number_of_block_allocs);
    dbgln("filled blocks: {}", g_malloc_stats.number_of_blocks_full);
    dbgln();
    dbgln("# free() calls: {}", g_malloc_stats.number_of_free_calls);
    dbgln();
    dbgln("big alloc keeps: {}", g_malloc_stats.number_of_big_allocator_keeps);
    dbgln("big alloc frees: {}", g_malloc_stats.number_of_big_allocator_frees);
    dbgln();
    dbgln("full block frees: {}", g_malloc_stats.number_of_freed_full_blocks);
    dbgln("number of hot keeps: {}", g_malloc_stats.number_of_hot_keeps);
    dbgln("number of cold keeps: {}", g_malloc_stats.number_of_cold_keeps);
    dbgln("number of frees: {}", g_malloc_stats.number_of_frees);
}
}
