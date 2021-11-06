/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Debug.h>
#include <AK/ScopedValueRollback.h>
#include <AK/Vector.h>
#include <LibELF/AuxiliaryVector.h>
#include <assert.h>
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
static bool s_in_userspace_emulator = false;

ALWAYS_INLINE static void ue_notify_malloc(const void* ptr, size_t size)
{
    if (s_in_userspace_emulator)
        syscall(SC_emuctl, 1, size, (FlatPtr)ptr);
}

ALWAYS_INLINE static void ue_notify_free(const void* ptr)
{
    if (s_in_userspace_emulator)
        syscall(SC_emuctl, 2, (FlatPtr)ptr, 0);
}

ALWAYS_INLINE static void ue_notify_realloc(const void* ptr, size_t size)
{
    if (s_in_userspace_emulator)
        syscall(SC_emuctl, 3, size, (FlatPtr)ptr);
}

ALWAYS_INLINE static void ue_notify_chunk_size_changed(const void* block, size_t chunk_size)
{
    if (s_in_userspace_emulator)
        syscall(SC_emuctl, 4, chunk_size, (FlatPtr)block);
}

struct MemoryAuditingSuppressor {
    ALWAYS_INLINE MemoryAuditingSuppressor()
    {
        if (s_in_userspace_emulator)
            syscall(SC_emuctl, 7);
    }
    ALWAYS_INLINE ~MemoryAuditingSuppressor()
    {
        if (s_in_userspace_emulator)
            syscall(SC_emuctl, 8);
    }
};

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
    int flags = MAP_ANONYMOUS | MAP_PRIVATE | MAP_PURGEABLE;
#if ARCH(X86_64)
    flags |= MAP_RANDOMIZED;
#endif
    auto* ptr = serenity_mmap(nullptr, size, PROT_READ | PROT_WRITE, flags, 0, 0, ChunkedBlock::block_size, name);
    VERIFY(ptr != MAP_FAILED);
    return ptr;
}

static void os_free(void* ptr, size_t size)
{
    int rc = munmap(ptr, size);
    assert(rc == 0);
}

enum class CallerWillInitializeMemory {
    No,
    Yes,
};

static void* malloc_impl(size_t size, CallerWillInitializeMemory caller_will_initialize_memory)
{
    if (s_log_malloc)
        dbgln("LibC: malloc({})", size);

    if (!size) {
        // Legally we could just return a null pointer here, but this is more
        // compatible with existing software.
        size = 1;
    }

    g_malloc_stats.number_of_malloc_calls++;

    size_t good_size;
    auto* allocator = allocator_for_size(size, good_size);

    PthreadMutexLocker locker(s_malloc_mutex);

    if (!allocator) {
        size_t real_size = round_up_to_power_of_two(sizeof(BigAllocationBlock) + size, ChunkedBlock::block_size);
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
    for (auto& current : allocator->usable_blocks) {
        if (current.free_chunks()) {
            block = &current;
            break;
        }
    }

    if (!block && s_hot_empty_block_count) {
        g_malloc_stats.number_of_hot_empty_block_hits++;
        block = s_hot_empty_blocks[--s_hot_empty_block_count];
        if (block->m_size != good_size) {
            new (block) ChunkedBlock(good_size);
            ue_notify_chunk_size_changed(block, good_size);
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
            ue_notify_chunk_size_changed(block, good_size);
        }
        allocator->usable_blocks.append(*block);
    }

    if (!block) {
        g_malloc_stats.number_of_block_allocs++;
        char buffer[64];
        snprintf(buffer, sizeof(buffer), "malloc: ChunkedBlock(%zu)", good_size);
        block = (ChunkedBlock*)os_alloc(ChunkedBlock::block_size, buffer);
        new (block) ChunkedBlock(good_size);
        allocator->usable_blocks.append(*block);
        ++allocator->block_count;
    }

    --block->m_free_chunks;
    void* ptr = block->m_freelist;
    if (ptr) {
        block->m_freelist = block->m_freelist->next;
    } else {
        ptr = block->m_slot + block->m_next_lazy_freelist_index * block->m_size;
        block->m_next_lazy_freelist_index++;
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

    ue_notify_malloc(ptr, size);
    return ptr;
}

static void free_impl(void* ptr)
{
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

    assert(magic == MAGIC_PAGE_HEADER);
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

void* malloc(size_t size)
{
    MemoryAuditingSuppressor suppressor;
    void* ptr = malloc_impl(size, CallerWillInitializeMemory::No);
    if (s_profiling)
        perf_event(PERF_EVENT_MALLOC, size, reinterpret_cast<FlatPtr>(ptr));
    return ptr;
}

// This is a Microsoft extension, and is not found on other Unix-like systems.
// FIXME: Implement aligned_alloc() instead
//
// This is used in libc++ to implement C++17 aligned new/delete.
//
// Both Unix-y alternatives to _aligned_malloc(), the C11 aligned_alloc() and
// posix_memalign() say that the resulting pointer can be deallocated with
// regular free(), which means that the allocator has to keep track of the
// requested alignments. By contrast, _aligned_malloc() is paired with
// _aligned_free(), so it can be easily implemented on top of malloc().
void* _aligned_malloc(size_t size, size_t alignment)
{
    if (__builtin_popcount(alignment) != 1) {
        errno = EINVAL;
        return nullptr;
    }
    alignment = max(alignment, sizeof(void*));
    if (Checked<size_t>::addition_would_overflow(size, alignment)) {
        errno = ENOMEM;
        return nullptr;
    }
    void* ptr = malloc(size + alignment);
    if (!ptr) {
        errno = ENOMEM;
        return nullptr;
    }
    auto aligned_ptr = (void*)(((FlatPtr)ptr + alignment) & ~(alignment - 1));
    ((void**)aligned_ptr)[-1] = ptr;
    return aligned_ptr;
}

void free(void* ptr)
{
    MemoryAuditingSuppressor suppressor;
    if (s_profiling)
        perf_event(PERF_EVENT_FREE, reinterpret_cast<FlatPtr>(ptr), 0);
    ue_notify_free(ptr);
    free_impl(ptr);
}

void _aligned_free(void* ptr)
{
    if (ptr)
        free(((void**)ptr)[-1]);
}

void* calloc(size_t count, size_t size)
{
    MemoryAuditingSuppressor suppressor;
    if (Checked<size_t>::multiplication_would_overflow(count, size)) {
        errno = ENOMEM;
        return nullptr;
    }
    size_t new_size = count * size;
    auto* ptr = malloc_impl(new_size, CallerWillInitializeMemory::Yes);
    if (ptr)
        memset(ptr, 0, new_size);
    return ptr;
}

size_t malloc_size(void* ptr)
{
    MemoryAuditingSuppressor suppressor;
    if (!ptr)
        return 0;
    void* page_base = (void*)((FlatPtr)ptr & ChunkedBlock::block_mask);
    auto* header = (const CommonHeader*)page_base;
    auto size = header->m_size;
    if (header->m_magic == MAGIC_BIGALLOC_HEADER)
        size -= sizeof(CommonHeader);
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
    MemoryAuditingSuppressor suppressor;
    if (!ptr)
        return malloc(size);
    if (!size) {
        free(ptr);
        return nullptr;
    }

    auto existing_allocation_size = malloc_size(ptr);

    if (size <= existing_allocation_size) {
        ue_notify_realloc(ptr, size);
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
    s_in_userspace_emulator = (int)syscall(SC_emuctl, 0) != -ENOSYS;
    if (s_in_userspace_emulator) {
        // Don't bother scrubbing memory if we're running in UE since it
        // keeps track of heap memory anyway.
        s_scrub_malloc = false;
        s_scrub_free = false;
    }

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
